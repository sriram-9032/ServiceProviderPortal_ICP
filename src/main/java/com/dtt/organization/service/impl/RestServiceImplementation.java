package com.dtt.organization.service.impl;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.dtt.organization.dto.JwkNew;
import com.dtt.organization.dto.UserInfo;
import com.dtt.organization.service.iface.RestService;
import com.dtt.organization.util.ValidationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
public class RestServiceImplementation implements RestService {

    @Value("${idp.clientId}")
    private String clientId;

    @Value("${idp.clintSecret}")
    private String clientSecret;

    @Value("${idp.redirectUri}")
    private String redirectUri;
    @Value("${idp.openid}")
    private boolean openId;
    @Value("${jwt.clientassertiontype}")
    private String clientassertiontype;

    @Value("${idp.tokenUrl}")
    private String tokenUrl;

    @Value("${idp.idpjwkSetURL}")
    private String idpjwkSetURL;
    @Value("${idp.userInfoUrl}")
    private String userInfoUrl;
    @Value("${idp.aud}")
    private String aud;
    @Value("${idp.scope}")
    private String scope;

    RestTemplate restTemplate = new RestTemplate();


    @Value("${privateKey}")
    private String privateKeyPath;

    @Value("${idp.authorizationHeaderName}")
    private String authorizationHeaderName;

    @Value("${url.login.user.photo}")
    private String loginUserPhoto;

    private PrivateKey privateKeyObject;


    private static final long VALIDITY_IN_MILLISECONDS = 24L * 60 * 60 * 1000;

    private static Logger logger = LoggerFactory.getLogger(RestServiceImplementation.class);


    private static final String CLASS = "RestServiceImplementation";

    private static final  String TOKEN_EXPIRY = "tokenExpiry" ;
    private static final  String NONCE = "nonce" ;

    public String generateJWTWithRsa(Boolean isAuthorizedUrl, String state, String nonce) {
        try {

            String id = UUID.randomUUID().toString();

            Map<String, Object> claims = new HashMap<>( );
            claims.put("iss", clientId);
            claims.put("sub", clientId);
            claims.put("iat", new Date(System.currentTimeMillis()));

            if (Boolean.TRUE.equals(isAuthorizedUrl)) {
                claims.put("redirect_uri",redirectUri);
                claims.put("aud", aud);
                claims.put("scope",scope);
                claims.put("state",state);
                claims.put(NONCE,nonce);
            }else {
                claims.put("aud", tokenUrl);
            }

            return generateJwtToken(privateKeyObject,
                    VALIDITY_IN_MILLISECONDS,
                    id,
                    claims);
        } catch (Exception e) {
            logger.error("Error generating JWT with RSA: {}", e.getMessage(), e);
            return null;
        }
    }
    @PostConstruct
    public void init() {
        this.privateKeyObject = getPrivateKey(privateKeyPath);
    }

    public String generateJwtToken(PrivateKey privateKey, long expirationInMillis, String id, Map<String, Object> claims) {
        try {
            JwtBuilder builder = Jwts.builder()
                    .setId(id)
                    .setClaims(claims)
                    .setHeaderParam("typ", "JWT")
                    .setNotBefore(new Date(System.currentTimeMillis()))
                    .setExpiration(new Date(System.currentTimeMillis()))
                    .signWith(SignatureAlgorithm.RS256, privateKey);

            if (expirationInMillis >= 0) {
                long expMillis = System.currentTimeMillis() + expirationInMillis;
                Date exp = new Date(expMillis);
                builder.setExpiration(exp);
            }


            return builder.compact();
        } catch (Exception e) {
            logger.error("Error while generating JWT token", e);
            return null;
        }
    }

private PrivateKey getPrivateKey(String keyPath) {

    try {

        PKCS8EncodedKeySpec keySpec =
                new PKCS8EncodedKeySpec(Base64.getDecoder().decode(keyPath));

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        return keyFactory.generatePrivate(keySpec);

    }  catch (Exception e) {

        logger.error("Failed to load RSA private key from path: {}", keyPath, e);
        return null;
    }
}

    public String generateApplicationUid() {
        UUID ouid = UUID.randomUUID();
        return ouid.toString();
    }




    @Override
    public UserInfo getUserInfo(String code, HttpServletRequest request) {

        try {

            ResponseEntity<String> response = exchangeToken(code);

            if (!isResponseOk(response)) {
                return null;
            }

            JsonNode root = parseJson(response.getBody());

            if (hasError(root)) {
                logTokenError(response);
                return null;
            }

            String accessToken = getAccessToken(root);
            long expiresIn = getExpiresIn(root);

            if (openId) {
                return handleOpenId(root, expiresIn, request);
            }

            return handleOAuthUserInfo(accessToken, expiresIn, request);

        } catch (Exception e) {

            logger.error("{} getUserInfo() failed | code={}", CLASS, code, e);
            return null;
        }
    }

    private ResponseEntity<String> exchangeToken(String code) {

        HttpHeaders headers = buildTokenHeaders();

        MultiValueMap<String, String> body = buildTokenRequestBody(code);

        HttpEntity<MultiValueMap<String, String>> entity =
                new HttpEntity<>(body, headers);

        return restTemplate.exchange(
                tokenUrl,
                HttpMethod.POST,
                entity,
                String.class
        );
    }

    private HttpHeaders buildTokenHeaders() {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String auth = clientId + ":" + clientSecret;
        String encoded = Base64.getEncoder().encodeToString(auth.getBytes());

        headers.set(authorizationHeaderName, "Basic " + encoded);

        return headers;
    }
    private MultiValueMap<String, String> buildTokenRequestBody(String code) {

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();

        map.add("grant_type", "authorization_code");
        map.add("client_id", clientId);
        map.add("redirect_uri", redirectUri);
        map.add("code", code);

        if (openId) {
            map.add("client_assertion",
                    generateJWTWithRsa(false, null, null));
            map.add("client_assertion_type", clientassertiontype);
        }

        return map;
    }
    private JsonNode parseJson(String body) throws JsonProcessingException {

        ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree(body);
    }

    private boolean hasError(JsonNode root) {
        return root.has("error");
    }

    private void logTokenError(ResponseEntity<String> response) {
        logger.info("{} token endpoint error: {}", CLASS, response.getBody());
    }

    private String getAccessToken(JsonNode root) {
        return root.path("access_token").asText();
    }

    private long getExpiresIn(JsonNode root) {
        return root.path("expires_in").asLong();
    }

    private UserInfo handleOpenId(JsonNode root,
                                  long expiresIn,
                                  HttpServletRequest request) {

        String idToken = root.path("id_token").asText();

        return handleOpenIdFlow(idToken, expiresIn, request);
    }
    private UserInfo handleOAuthUserInfo(String accessToken,
                                         long expiresIn,
                                         HttpServletRequest request)
            throws JsonProcessingException {

        ResponseEntity<String> userResponse = fetchUserInfo(accessToken);

        if (!isResponseOk(userResponse)) {
            return null;
        }

        JsonNode root = parseJson(userResponse.getBody());

        if (hasError(root)) {
            return null;
        }

        ObjectMapper mapper = new ObjectMapper();
        UserInfo userInfo = mapper.readValue(root.toString(), UserInfo.class);

        userInfo.setApplicationID(generateApplicationUid());
        userInfo.setAccessToken(accessToken);

        setSessionExpiry(expiresIn, request);

        return userInfo;
    }
    private ResponseEntity<String> fetchUserInfo(String accessToken) {

        HttpHeaders headers = new HttpHeaders();
        headers.set(authorizationHeaderName, "Bearer " + accessToken);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        return restTemplate.exchange(
                userInfoUrl,
                HttpMethod.GET,
                entity,
                String.class
        );
    }

    private void setSessionExpiry(long expiresIn,
                                  HttpServletRequest request) {

        if (expiresIn <= 0) {
            return;
        }

        Instant expiryTime = Instant.now().plusSeconds(expiresIn);

        request.getSession().setAttribute(TOKEN_EXPIRY, expiryTime);

        Duration duration = Duration.between(Instant.now(), expiryTime);
        request.getSession().setMaxInactiveInterval((int) duration.getSeconds());
    }
    private UserInfo handleOpenIdFlow(String token,
                                      long expiresIn,
                                      HttpServletRequest request) {
        try {

            DecodedJWT decodedIdToken = JWT.decode(token);

            String tokenNonce = decodedIdToken.getClaim(NONCE).asString();
            String nonce = (String) request.getSession().getAttribute(NONCE);

            if (!Objects.equals(tokenNonce, nonce)) {
                throw new ValidationException("Invalid nonce");
            }

            ResponseEntity<String> restResponse =
                    restTemplate.getForEntity(idpjwkSetURL, String.class);

            if (restResponse.getBody() == null) {
                throw new IllegalStateException("JWK response body is null");
            }

            JSONObject jwkUrlResponse = new JSONObject(restResponse.getBody());
            JSONArray arrKeys = jwkUrlResponse.optJSONArray("keys");

            JwkNew jwkNew = new JwkNew();

            if (arrKeys != null) {

                for (int i = 0; i < arrKeys.length(); i++) {

                    JSONObject keyObj = arrKeys.optJSONObject(i);

                    if (keyObj != null &&
                            Objects.equals(keyObj.optString("kid"),
                                    decodedIdToken.getKeyId())) {

                        jwkNew.setAlgorithm(keyObj.optString("alg"));
                        jwkNew.setE(keyObj.optString("e"));
                        jwkNew.setId(keyObj.optString("kid"));
                        jwkNew.setN(keyObj.optString("n"));
                        jwkNew.setType(keyObj.optString("kty"));
                        jwkNew.setUsage(keyObj.optString("use"));

                        break;
                    }
                }
            }

            Claim claim = decodedIdToken.getClaim("daes_claims");

            UserInfo userInfo = claim.as(UserInfo.class);

            if (userInfo == null) {
                throw new IllegalStateException("UserInfo claim is null");
            }

            userInfo.setIdToken(token);
            userInfo.setApplicationID(generateApplicationUid());

            if (expiresIn > 0) {

                Instant expiryTime = Instant.now().plusSeconds(expiresIn);

                request.getSession().setAttribute(TOKEN_EXPIRY, expiryTime);

                request.getSession().setMaxInactiveInterval(
                        (int) Duration.between(Instant.now(), expiryTime).getSeconds()
                );
            }

            return userInfo;

        } catch (JSONException ex) {

            logger.error("JSON parsing error while handling OpenID flow", ex);
            return null;

        } catch (Exception ex) {

            logger.error("Error in handleOpenIdFlow", ex);
            return null;
        }
    }
    private boolean isResponseOk(ResponseEntity<String> response) {

        if (response == null) {
            return false;
        }

        if (response.getStatusCode() != HttpStatus.OK) {
            return false;
        }

        String body = response.getBody();

        return body != null && !body.trim().isEmpty();
    }
}


