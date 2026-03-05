package com.dtt.organization.service.impl;

import com.dtt.organization.dto.ApiResponses;
import com.dtt.organization.dto.ApplyLicenseDTO;
import com.dtt.organization.model.OrganizationEntity;
import com.dtt.organization.repository.OrganizationRepository;
import com.dtt.organization.service.iface.LicenseService;
import com.dtt.organization.util.APIRequestHandler;
import com.dtt.organization.util.AppUtil;
import com.dtt.organization.util.ValidationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.util.Optional;


@Service
public class LicenseServiceImpl implements LicenseService {


    private static final String CLASS = "LicenseServiceImpl";
    private static final Logger logger = LoggerFactory.getLogger(LicenseServiceImpl.class);


    @Value("${generate.license}")
    String generateLicense;

    @Value("${download.license}")
    String downloadLicense;

    private final OrganizationRepository organizationRepository;
    private final APIRequestHandler apiRequestHandler;
    private final MessageSource messageSource;
    private final ObjectMapper objectMapper;

    public LicenseServiceImpl(OrganizationRepository organizationRepository,
                              APIRequestHandler apiRequestHandler,
                              MessageSource messageSource,
                              ObjectMapper objectMapper) {

        this.organizationRepository = organizationRepository;
        this.apiRequestHandler = apiRequestHandler;
        this.messageSource = messageSource;
        this.objectMapper = objectMapper;
    }

    @Override
    public ResponseEntity<Resource> downloadLicense(String ouid,String type) {

        try {
            logger.info("{} downloadLicense() for ouid :: {}",CLASS,ouid);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String allclienttUrl = downloadLicense + ouid + "/" + type;
            logger.info("{} sending GET request to external API | url={}", CLASS, allclienttUrl);
            HttpEntity<Object> requestEntity = new HttpEntity<>(headers);
            ApiResponses res = apiRequestHandler.handleApiRequest(allclienttUrl, HttpMethod.GET, requestEntity);
            logger.info("{} API response received | ouid={} | success={}", CLASS, ouid, res.isSuccess());



            if (!res.isSuccess()) {
                logger.error("{} downloadLicense failed | ouid={} | reason=API response unsuccessful", CLASS, ouid);

                throw new ValidationException(
                        messageSource.getMessage(
                                "api.error.license.download.invalid.response",
                                null,
                                LocaleContextHolder.getLocale()
                        )
                );
            }

            String content = (String) res.getResult();

            byte[] data = content.getBytes(StandardCharsets.UTF_8);
            Resource resource = new ByteArrayResource(data);

            String fileName = "license.txt";
            logger.info("{} downloadLicense successful | ouid={} | fileName={}", CLASS, ouid, fileName);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + fileName + "\"")
                    .contentType(MediaType.TEXT_PLAIN)
                    .contentLength(data.length)
                    .body(resource);

        } catch (Exception e) {
            logger.error("{} downloadLicense failed | ouid={} | exception={}",
                    CLASS, ouid, e.getMessage(), e);

            return ResponseEntity
                    .internalServerError()
                    .header("X-Error-Message",
                            messageSource.getMessage(
                                    "license.download.failed",
                                    null,
                                    LocaleContextHolder.getLocale()))
                    .build();
        }
    }


    @Override
    public ApiResponses applyLicense(Long orgId) {
        logger.info("{} applyLicense() for orgId={}", CLASS, orgId);
        try {
            Optional<OrganizationEntity> org = organizationRepository.findById(orgId);

               if(org.isEmpty()){
                   logger.warn("{} applyLicense() Organization not found | orgId={}", CLASS, orgId);
                   return AppUtil.createApiResponses(false,
                           messageSource.getMessage("api.error.organization.not.found", null, LocaleContextHolder.getLocale()),
                           null);


               }




            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ApplyLicenseDTO downloadLicenseDTO = new ApplyLicenseDTO();
            downloadLicenseDTO.setOuid(org.get().getOuid());
            downloadLicenseDTO.setLicenseType("COMMERCIAL");

            String applicationType = ("ENTERPRISE_GATEWAY" + "_" + org.get().getId()).replace(" ", "_");

            downloadLicenseDTO.setApplicationType(applicationType);
            logger.info("{}  applyLicense API request for orgId={} | ouid={} | applicationType={}",
                    CLASS, orgId, org.get().getOuid(), applicationType);

            logger.info("{} DownloadLicenseDTO {}", CLASS, downloadLicenseDTO);


            HttpEntity<Object> reqEntity = new HttpEntity<>(downloadLicenseDTO, headers);

            String url = generateLicense;

            ApiResponses res = apiRequestHandler.handleApiRequest(url, HttpMethod.POST, reqEntity);

            if (!res.isSuccess()) {
                logger.error("{} applyLicense API failed | orgId={} | message={}", CLASS, orgId, res.getMessage());
                return AppUtil.createApiResponses(false, res.getMessage(), null);
            }


            String admin = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(res.getResult());
            logger.debug("applyLicense response result: {}", res.getResult());
            if(admin != null){
                logger.info("{} applyLicense successful | orgId={}", CLASS, orgId);
                return AppUtil.createApiResponses(true, res.getMessage(), null);
            }else{


                logger.info("{} applyLicense submitted for approval | orgId={}", CLASS, orgId);

                return AppUtil.createApiResponses(
                        true,
                        messageSource.getMessage(
                                "license.request.submitted",
                                null,
                                LocaleContextHolder.getLocale()
                        ),
                        null
                );
            }


        } catch (Exception e) {
            logger.error("{} applyLicense() failed | orgId={} | exception={}", CLASS, orgId, e.getMessage(), e);
            return AppUtil.createApiResponses(
                    false,
                    messageSource.getMessage(
                            "api.error.something.went.wrong",
                            null,
                            LocaleContextHolder.getLocale()
                    ),
                    null
            );
        }

    }
}

