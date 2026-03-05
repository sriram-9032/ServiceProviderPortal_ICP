package com.dtt.organization.util;

import com.dtt.organization.dto.ApiResponses;
import com.dtt.organization.dto.EmailDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Value("${send.email.url}")
    private String sendEmailUrl;

    @Value("${url.getEmaillist}")
    private String getEmailListUrl;

    @Value("${max.adminEmails}")
    private int maxAdminEmailCount;

    private final RestTemplate restTemplate = new RestTemplate();


    public ApiResponses sendEmail(String spocEmail, String emailBody, String emailSubject) {

        EmailDto emailDto = new EmailDto();
        emailDto.setEmailBody(emailBody);
        emailDto.setRecipients(Collections.singletonList(spocEmail));
        emailDto.setSubject(emailSubject);

        return executeEmailRequest(emailDto);
    }


    public ApiResponses sendEmailForAdmin(List<String> spocEmail,
                                         String emailBody,
                                         String emailSubject) {

        EmailDto emailDto = new EmailDto();
        emailDto.setEmailBody(emailBody);
        emailDto.setRecipients(spocEmail);
        emailDto.setSubject(emailSubject);
        emailDto.setSendMailToAdmin(true);

        return executeEmailRequest(emailDto);
    }


    private ApiResponses executeEmailRequest(EmailDto emailDto) {

        try {

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<EmailDto> requestEntity =
                    new HttpEntity<>(emailDto, headers);

            ResponseEntity<ApiResponses> response =
                    restTemplate.exchange(
                            sendEmailUrl,
                            HttpMethod.POST,
                            requestEntity,
                            ApiResponses.class
                    );

            if (!response.getStatusCode().is2xxSuccessful()) {
                logger.warn("Email API returned non-success status: {}",
                        response.getStatusCode());
                return AppUtil.createApiResponses(false,
                        "Email sending failed",
                        null);
            }

            ApiResponses body = Optional.ofNullable(response.getBody())
                    .orElse(AppUtil.createApiResponses(false,
                            "Empty response body",
                            null));

            return AppUtil.createApiResponses(
                    true,
                    body.getMessage(),
                    body.getResult()
            );

        } catch (ResourceAccessException e) {
            logger.error("Email service unreachable", e);
            return AppUtil.createApiResponses(false,
                    "Service is unavailable or connection refused",
                    null);

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            logger.error("Email API HTTP error: {}", e.getStatusCode(), e);
            return AppUtil.createApiResponses(false,
                    e.getStatusCode().toString(),
                    null);

        } catch (Exception e) {
            logger.error("Unexpected error while sending email", e);
            return AppUtil.createApiResponses(false,
                    "Unexpected error occurred",
                    null);
        }
    }


    public ApiResponses getAdminEmailList() {

        try {

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Void> requestEntity =
                    new HttpEntity<>(headers);

            ResponseEntity<ApiResponses> response =
                    restTemplate.exchange(
                            getEmailListUrl,
                            HttpMethod.GET,
                            requestEntity,
                            ApiResponses.class
                    );

            if (!response.getStatusCode().is2xxSuccessful()) {
                logger.warn("Get email list API returned status: {}",
                        response.getStatusCode());
                return AppUtil.createApiResponses(false,
                        "Error Fetching Emails",
                        null);
            }

            ApiResponses body = response.getBody();

            if (body == null || body.getResult() == null) {
                return AppUtil.createApiResponses(true,
                        "No Emails Found",
                        Collections.emptyList());
            }

            List<String> emailList = (List<String>) body.getResult();

            if (emailList.isEmpty()) {
                return AppUtil.createApiResponses(true,
                        "No Emails Found",
                        Collections.emptyList());
            }

            if (maxAdminEmailCount == 0 ||
                    emailList.size() <= maxAdminEmailCount) {

                return AppUtil.createApiResponses(true,
                        "Email List Fetched Successfully",
                        emailList);
            }

            return AppUtil.createApiResponses(true,
                    "Partial Email List Fetched Successfully",
                    emailList.subList(0, maxAdminEmailCount));

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            logger.error("HTTP error while fetching admin emails", e);
            return AppUtil.createApiResponses(false,
                    "Error Fetching Emails",
                    e.getMessage());

        } catch (Exception e) {
            logger.error("Unexpected error while fetching admin emails", e);
            return AppUtil.createApiResponses(false,
                    "Unexpected Error Occurred",
                    null);
        }
    }
}