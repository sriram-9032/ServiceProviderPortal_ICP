package com.dtt.organization.util;

import com.dtt.organization.dto.ApiResponses;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

@Configuration

public class APIRequestHandler {

    private final RestTemplate restTemplate;

    public APIRequestHandler(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private static final List<String> ALLOWED_HOSTS =
            List.of("email-service.company.com");

    private void validateUrl(String url) {
        try {
            URI uri = new URI(url);
            String host = uri.getHost();

            if (host == null || !ALLOWED_HOSTS.contains(host)) {
                throw new IllegalArgumentException("Invalid URL host");
            }

        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URL format", e);
        }
    }

    public ApiResponses handleApiRequest(String url,
                                        HttpMethod method,
                                        HttpEntity<Object> requestEntity) {

        try {

            validateUrl(url);
            ResponseEntity<ApiResponses> response =
                    restTemplate.exchange(url, method, requestEntity, ApiResponses.class);

            if (response.getStatusCode().is2xxSuccessful()) {

                ApiResponses body = response.getBody();

                if (body != null) {

                    return AppUtil.createApiResponses(
                            body.isSuccess(),
                            body.getMessage(),
                            body.getResult()
                    );
                }

                return AppUtil.createApiResponses(false, "Empty response body", null);
            }

            if (response.getStatusCode() == HttpStatus.BAD_REQUEST) {
                return AppUtil.createApiResponses(false, "Bad Request", null);
            }

            if (response.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR) {
                return AppUtil.createApiResponses(false, "Internal Server Error", null);
            }

            return AppUtil.createApiResponses(false, "Unexpected Error", null);

        } catch (RestClientException ex) {

            return AppUtil.createApiResponses(
                    false,
                    "Error in API request: " + ex.getMessage(),
                    null
            );
        }
    }
}
