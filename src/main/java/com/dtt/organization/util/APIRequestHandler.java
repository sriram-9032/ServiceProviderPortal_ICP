package com.dtt.organization.util;

import com.dtt.organization.dto.ApiResponses;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Configuration

public class APIRequestHandler {

    private final RestTemplate restTemplate;

    public APIRequestHandler(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public ApiResponses handleApiRequest(String url,
                                        HttpMethod method,
                                        HttpEntity<Object> requestEntity) {

        try {

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

