package com.dtt.organization.controller;


import com.dtt.organization.dto.UserInfo;
import com.dtt.organization.model.TrustedUsersEntity;
import com.dtt.organization.repository.TrustedUsersRepository;
import com.dtt.organization.security.CustomUserDetailsService;
import com.dtt.organization.service.iface.RestService;
import com.dtt.organization.service.impl.OrganizationServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.ModelAndView;

import java.util.Optional;
import org.springframework.core.ParameterizedTypeReference;

@Controller
public class LoginController {


    RestTemplate restTemplate = new RestTemplate();

    @Value("${idp.idpUrl}")
    private String idp;

    @Value("${idp.openid}")
    private boolean openId;

    @Value("${idp.clientId}")
    private String clientId;

    @Value("${idp.redirectUri}")
    private String redirectUri;

    @Value("${idp.scope}")
    private String scope;

    @Value("${idp.logoutUrl}")
    private String logoutUrl;


    @Value("${idp.authorizationHeaderName}")
    private String authorizationHeaderName;


    @Value("${url.login.user.photo}")
    private String loginUserPhoto;



    private final TrustedUsersRepository trustedUsersRepository;
    private final OrganizationServiceImpl organizationService;
    private final CustomUserDetailsService customUserDetailsService;
    private final RestService restService;
    public LoginController(TrustedUsersRepository trustedUsersRepository,
                           OrganizationServiceImpl organizationService,
                           CustomUserDetailsService customUserDetailsService,
                           RestService restService) {
        this.trustedUsersRepository = trustedUsersRepository;
        this.organizationService = organizationService;
        this.customUserDetailsService = customUserDetailsService;
        this.restService = restService;
    }
    private static final String ATTR_MESSAGE = "message";
    private static final String LOGIN_VIEW = "login";
    private static final String REDIRECT_ROOT = "redirect:/";
    private static final String MSG_NOT_TRUSTED = "Not a Trusted User";
    private static final String MSG_PHOTO_ERROR = "Error fetching user photo";

    private static Logger logger = LoggerFactory.getLogger(LoginController.class);


    private static final  String CLASS = "Login Controller";



    @GetMapping("/")

    public ModelAndView getLoginPage(Model model, HttpSession session){
        String state= java.util.UUID.randomUUID().toString();
        String nonce = java.util.UUID.randomUUID().toString();
        String idpUrl;
        if(openId)
            idpUrl =idp+"client_id="+clientId+"&redirect_uri="+redirectUri+"&response_type=code"+"&scope="+scope+"&state="+state+"&nonce="+nonce+"&request="+restService.generateJWTWithRsa(true, state, nonce);
        else
            idpUrl =idp+"client_id="+clientId+"&redirect_uri="+redirectUri+"&response_type=code"+"&scope="+scope.replace("openid ","")+"&state="+state+"&nonce="+nonce;

        model.addAttribute("idpUrl",idpUrl);
        model.addAttribute("logoutUrl",logoutUrl);
        session.setAttribute("state", state);
        session.setAttribute("nonce", nonce);
        return new ModelAndView(LOGIN_VIEW);

    }


    @GetMapping("/eoi-redirect")
    public ModelAndView callback(
            @RequestParam(name = "code") Optional<String> code,
            @RequestParam(name = "state") Optional<String> state,
            @RequestParam(name = "error") Optional<String> error,
            Model model,
            HttpServletRequest request,
            HttpServletResponse response,
            HttpSession session) {


        try {
            logger.debug("{} Inside callback try block", CLASS);
            if (!code.isPresent()) {

                logger.warn("{} Missing authorization code. Redirecting to login.", CLASS);
                return new ModelAndView(REDIRECT_ROOT);
            }


            logger.info("{} Authorization code present, fetching user info...", CLASS);

            UserInfo userInfo = restService.getUserInfo(code.get(), request);
            if (userInfo == null) {

                logger.warn("{} UserInfo is null. Redirecting to login.", CLASS);
                return new ModelAndView(REDIRECT_ROOT);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.set(authorizationHeaderName, "Bearer " + userInfo.getAccessToken());
            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(headers);

            ResponseEntity<ApiResponses<?>> photoResponse =
                    restTemplate.exchange(
                            loginUserPhoto,
                            HttpMethod.GET,
                            entity,
                            new ParameterizedTypeReference<ApiResponses<?>>() {}
                    );

            ApiResponses<?> photoBody = photoResponse.getBody();

            if (photoBody == null || !photoBody.isSuccess()) {
                logger.error("{} Failed to fetch user photo", CLASS);
                model.addAttribute(ATTR_MESSAGE, MSG_PHOTO_ERROR);
                return new ModelAndView(LOGIN_VIEW);
            }


            logger.info(CLASS + "Session attributes set for user: {}", userInfo.getEmail());

            String email = userInfo.getEmail();
            TrustedUsersEntity trustedSpocs =  trustedUsersRepository.findByEmail(email);

            ApiResponses<?> forms = organizationService.syncDataFromAdmin(email);
            if (forms.getResult() == null && (trustedSpocs == null)) {
                logger.warn("{} User is not trusted: {}", CLASS, email);
                model.addAttribute(ATTR_MESSAGE, MSG_NOT_TRUSTED);
                return new ModelAndView(LOGIN_VIEW);
            }
            if(forms.getResult()!=null){
                return new ModelAndView("redirect:/dashboard");
            }

            if (trustedSpocs != null) {


                UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);


                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );


                SecurityContextHolder.getContext().setAuthentication(auth);


                session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());


                return new ModelAndView("redirect:/dashboard");
            }

            else{
                model.addAttribute(ATTR_MESSAGE, MSG_NOT_TRUSTED);
                return new ModelAndView(LOGIN_VIEW);
            }



        } catch (Exception e) {

            logger.error("{} Error in callback", CLASS, e);

            return new ModelAndView(REDIRECT_ROOT);
        }
    }


    @GetMapping("/logout")
    @CacheEvict
    public ModelAndView idpLogout(HttpSession session, HttpServletResponse response, Model model, HttpServletRequest request)
    {


        logger.info("::::::::IDP logout::::::::::");
        UserInfo userInfo= (UserInfo) session.getAttribute("userInfo");


        if(userInfo!=null)
        {

            session.invalidate();

            response.setHeader("Cache-Control", "no-store, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");


            return new ModelAndView(REDIRECT_ROOT);

        }

        response.setHeader("Cache-Control", "no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");

        return new ModelAndView(REDIRECT_ROOT);

    }

    @GetMapping("/api/get/ssp/status")
    public String status(){
        return "Up and running";
    }







}
