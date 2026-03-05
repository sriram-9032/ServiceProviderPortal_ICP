package com.dtt.organization.service.impl;

import com.dtt.organization.dto.ApiResponses;
import com.dtt.organization.dto.WalletCertResponseDto;
import com.dtt.organization.dto.WalletIssueDTO;
import com.dtt.organization.model.OrganizationEntity;
import com.dtt.organization.model.SpocEntity;
import com.dtt.organization.model.WalletCertApprovalEntity;
import com.dtt.organization.repository.OrganizationRepository;
import com.dtt.organization.repository.SpocRepository;
import com.dtt.organization.repository.WalletCertRequestsRepo;
import com.dtt.organization.service.iface.WalletIface;
import com.dtt.organization.util.APIRequestHandler;
import com.dtt.organization.util.AppUtil;
import com.dtt.organization.util.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.Optional;


@Service
public class WalletImpl implements WalletIface {

    private static final String CLASS = "WalletImpl";
    private static final Logger logger = LoggerFactory.getLogger(WalletImpl.class);

    private static final String APPROVED  = "APPROVED";
    private static final String REJECTED  = "REJECTED";
    private static final String PENDING = "PENDING";
    private static final String NOT_FOUND = "NOT_FOUND" ;
    private static final String ORG_NOT_FOUND = "api.error.organization.not.found";






    @Value("${url.org.wallet}")
    String walletUrl;

    @Value("${wallet.cert.payment}")
    boolean walletCertPayment;

    @Value("${url.find.duplicate.ref.no}")
    String walletDuplicatePayment;

    @Value("${wallet.cert.Admin.approval}")
    private boolean walletCertAdminApproval;

    @Value("${url.get.wallet.by.ouid}")
    private String walletGetByOuidUrl;
    private final APIRequestHandler apiRequestHandler;
    private final OrganizationRepository organizationRepository;
    private final SpocRepository spocRepository;
    private final WalletCertRequestsRepo walletCertRequestsRepo;
    private final MessageSource messageSource;

    public WalletImpl(
            APIRequestHandler apiRequestHandler,
            OrganizationRepository organizationRepository,
            SpocRepository spocRepository,
            WalletCertRequestsRepo walletCertRequestsRepo,
            MessageSource messageSource) {

        this.apiRequestHandler = apiRequestHandler;
        this.organizationRepository = organizationRepository;
        this.spocRepository = spocRepository;
        this.walletCertRequestsRepo = walletCertRequestsRepo;
        this.messageSource = messageSource;
    }


    @Override
    public ApiResponses generateWalletCert(Long id,String paymentReferenceId) {
        try{
            OrganizationEntity organizationEntity = organizationRepository.findById(id).orElseThrow(() ->
                    new ValidationException(
                            messageSource.getMessage(
                                    ORG_NOT_FOUND,
                                    null,
                                     LocaleContextHolder.getLocale()
                            )
                    )
            );

            ApiResponses response = callOrgWalletCert(organizationEntity.getOuid(),paymentReferenceId);
            if(!response.isSuccess()){
                return response;
            }
            return response;

        }catch (Exception e){
            logger.error("{} Error while generating wallet cert | orgId: {}",
                    CLASS, id, e);
            return AppUtil.createApiResponses(
                    false,
                    messageSource.getMessage(
                            "api.error.organization.wallet.cert.failed",
                            null,
                             LocaleContextHolder.getLocale()
                    ),
                    null
            );
        }
    }


    @Override
    public ApiResponses saveWalletCertRequest(Long id, String paymentReferenceId) {
        try {
            if(!walletCertPayment){
                return saveWalletCertReqIntoTable(id,paymentReferenceId);
            }
            else{
                OrganizationEntity organizationEntity = organizationRepository.findById(id).orElseThrow(() ->
                        new ValidationException(
                                messageSource.getMessage(
                                        ORG_NOT_FOUND,
                                        null,
                                         LocaleContextHolder.getLocale()
                                )
                        )
                );


                WalletIssueDTO walletIssueDTO = new WalletIssueDTO();
                walletIssueDTO.setOrganizationUid(organizationEntity.getOuid());
                walletIssueDTO.setTransactionReferenceId(paymentReferenceId);

                HttpHeaders headers = new HttpHeaders();

                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Object> reqEntity = new HttpEntity<>(walletIssueDTO,headers);

                String url = walletDuplicatePayment;

                ApiResponses res = apiRequestHandler.handleApiRequest(url, HttpMethod.POST, reqEntity);
                logger.info("{} Response wallet url::: {} " ,CLASS, res);

                if(!res.isSuccess()){
                    return AppUtil.createApiResponses(false,res.getMessage(),null);
                }

                return saveWalletCertReqIntoTable(id,paymentReferenceId);

            }

        }catch (Exception e){
            logger.error("{} Error while saving wallet cert request | orgId: {}",
                    CLASS, id, e);
            return AppUtil.createApiResponses(
                    false,
                    messageSource.getMessage(
                            "api.error.wallet.request.failed",
                            null,
                             LocaleContextHolder.getLocale()
                    ),
                    null
            );
        }
    }


    @Override
    public ApiResponses changeStatusOfWalletCert(Long id, String status) {

        WalletCertApprovalEntity walletCertApproval =
                walletCertRequestsRepo.findById(id)
                        .orElseThrow(() ->
                                new ValidationException(
                                        messageSource.getMessage(
                                                "api.error.wallet.cert.not.found",
                                                null,
                                                LocaleContextHolder.getLocale()
                                        )
                                )
                        );

        OrganizationEntity organizationEntity =
                organizationRepository.findById(walletCertApproval.getOrgDetailsId())
                        .orElseThrow(() ->
                                new ValidationException(
                                        messageSource.getMessage(
                                                ORG_NOT_FOUND,
                                                null,
                                                LocaleContextHolder.getLocale()
                                        )
                                )
                        );

        if (APPROVED .equals(status)) {
            return approveWalletCert(
                    id,
                    organizationEntity.getOuid(),
                    walletCertApproval.getPaymentTransactionNo()
            );
        } else if (REJECTED .equals(status)) {
            return rejectWalletCert(id);
        } else {
            return AppUtil.createApiResponses(
                    false,
                    messageSource.getMessage(
                            "api.error.wallet.invalid.status",
                            null,
                            LocaleContextHolder.getLocale()
                    ),
                    null
            );
        }
    }


    @Override
    public ApiResponses getAllWalletReqs() {
        try{
            return AppUtil.createApiResponses(
                    true,
                    messageSource.getMessage(
                            "api.success.wallet.requests.fetched",
                            null,
                             LocaleContextHolder.getLocale()
                    ),
                    walletCertRequestsRepo.findAll()
            );
        }catch (Exception e){
            logger.error("{} Error while fetching wallet requests",
                    CLASS, e);
            return AppUtil.createApiResponses(
                    false,
                    messageSource.getMessage(
                            "api.error.wallet.requests.fetch.failed",
                            null,
                             LocaleContextHolder.getLocale()
                    ),
                    null
            );
        }
    }

    public ApiResponses callOrgWalletCert(String ouid, String paymentReferenceId){
        try{

            WalletIssueDTO walletIssueDTO = new WalletIssueDTO();
            walletIssueDTO.setOrganizationUid(ouid);
            walletIssueDTO.setTransactionReferenceId(paymentReferenceId);

            HttpHeaders headers = new HttpHeaders();

            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Object> reqEntity = new HttpEntity<>(walletIssueDTO,headers);

            String url = walletUrl;

            ApiResponses res = apiRequestHandler.handleApiRequest(url, HttpMethod.POST, reqEntity);
            logger.info("{} Response wallet url::: {} " ,CLASS, res);

            if(!res.isSuccess()){
                return AppUtil.createApiResponses(false,res.getMessage(),null);
            }

            return AppUtil.createApiResponses(res.isSuccess(),res.getMessage(),res.getResult());


        }catch(Exception e){
            logger.error("{} Error while calling wallet API | ouid: {}",
                    CLASS, ouid, e);
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

    public ApiResponses rejectWalletCert(Long id){
        Optional<WalletCertApprovalEntity> walletCertApprovalEntity = walletCertRequestsRepo.findById(id);
        if (walletCertApprovalEntity.isEmpty()) {
            return AppUtil.createApiResponses(
                    false,
                    messageSource.getMessage(
                            "api.error.wallet.request.not.found",
                            new Object[]{id},
                             LocaleContextHolder.getLocale()
                    ),
                    null
            );
        }
        WalletCertApprovalEntity entity = walletCertApprovalEntity.get();
        entity.setStatus(REJECTED );
        entity.setUpdatedOn(AppUtil.getDate());
        return AppUtil.createApiResponses(
                true,
                messageSource.getMessage(
                        "api.success.wallet.request.rejected",
                        null,
                         LocaleContextHolder.getLocale()
                ),
                null
        );

    }



    public ApiResponses approveWalletCert(Long id,String ouid, String paymentReferenceId){
        Optional<WalletCertApprovalEntity> walletCertApprovalEntity = walletCertRequestsRepo.findById(id);
        if (walletCertApprovalEntity.isEmpty()) {
            return AppUtil.createApiResponses(
                    false,
                    messageSource.getMessage(
                            "api.error.wallet.request.not.found",
                            new Object[]{id},
                             LocaleContextHolder.getLocale()
                    ),
                    null
            );
        }

        ApiResponses response = callOrgWalletCert(ouid,paymentReferenceId);
        if(!response.isSuccess()){
            return response;
        }
        WalletCertApprovalEntity entity = walletCertApprovalEntity.get();
        entity.setStatus(APPROVED );
        entity.setUpdatedOn(AppUtil.getDate());
        walletCertRequestsRepo.save(entity);
        return AppUtil.createApiResponses(
                true,
                messageSource.getMessage(
                        "api.success.wallet.request.approved",
                        null,
                         LocaleContextHolder.getLocale()
                ),
                null
        );

    }


    public ApiResponses saveWalletCertReqIntoTable(Long id,String paymentReferenceId){
        OrganizationEntity organizationEntity = organizationRepository.findById(id).orElseThrow(() ->
                new ValidationException(
                        messageSource.getMessage(
                                ORG_NOT_FOUND,
                                null,
                                 LocaleContextHolder.getLocale()
                        )
                )
        );
        SpocEntity spocEntity = spocRepository.findByOrgDetailsId(id)
                .orElseThrow(() ->
                        new ValidationException(
                                messageSource.getMessage(
                                        "api.error.spoc.not.found",
                                        null,
                                        LocaleContextHolder.getLocale()
                                )
                        )
                );
        WalletCertApprovalEntity walletCertApproval = walletCertRequestsRepo.findByOrgDetailsID(id);

        if(walletCertApproval!=null){
            walletCertApproval.setStatus(PENDING);
            walletCertApproval.setUpdatedOn(AppUtil.getDate());
            walletCertRequestsRepo.save(walletCertApproval);
            return AppUtil.createApiResponses(
                    true,
                    messageSource.getMessage(
                            "api.success.wallet.request.renewed",
                            null,
                             LocaleContextHolder.getLocale()
                    ),
                    null
            );
        }else {


            WalletCertApprovalEntity walletCertApprovalEntity = new WalletCertApprovalEntity();
            walletCertApprovalEntity.setOrgName(organizationEntity.getOrgName());
            walletCertApprovalEntity.setOrgDetailsId(organizationEntity.getId());
            walletCertApprovalEntity.setPaymentTransactionNo(paymentReferenceId);
            walletCertApprovalEntity.setSpocName(spocEntity.getSpocName());
            walletCertApprovalEntity.setStatus(PENDING);
            walletCertApprovalEntity.setCreatedOn(AppUtil.getDate());
            walletCertApprovalEntity.setUpdatedOn(AppUtil.getDate());
            walletCertRequestsRepo.save(walletCertApprovalEntity);
            return AppUtil.createApiResponses(
                    true,
                    messageSource.getMessage(
                            "api.success.wallet.request.saved",
                            null,
                             LocaleContextHolder.getLocale()
                    ),
                    null
            );
        }
    }

    @Override
    public ApiResponses walletDetails(long orgDetailsId) {
        try {

            OrganizationEntity organization = organizationRepository
                    .findById(orgDetailsId)
                    .orElseThrow(() ->new ValidationException(
                                    messageSource.getMessage(
                                            ORG_NOT_FOUND,
                                            null,
                                             LocaleContextHolder.getLocale()
                                    )
                            )
                    );


            if (walletCertAdminApproval) {

                Optional<WalletCertApprovalEntity> approvalOpt =
                        walletCertRequestsRepo.findTopByOrgDetailsIdOrderByCreatedOnDesc(orgDetailsId);

                if (approvalOpt.isEmpty()) {
                    WalletCertResponseDto dto = new WalletCertResponseDto();
                    dto.setStatus(NOT_FOUND);
                    return AppUtil.createApiResponses(
                            true,
                            messageSource.getMessage(
                                    "api.success.wallet.no.request.found",
                                    null,
                                     LocaleContextHolder.getLocale()
                            ),
                            dto
                    );
                }

                WalletCertApprovalEntity approval = approvalOpt.get();
                String status = approval.getStatus();

                if (PENDING.equalsIgnoreCase(status)) {
                    WalletCertResponseDto dto = new WalletCertResponseDto();
                    dto.setStatus(PENDING);
                    return AppUtil.createApiResponses(
                            true,
                            messageSource.getMessage(
                                    "api.success.wallet.pending",
                                    null,
                                     LocaleContextHolder.getLocale()
                            ),
                            dto
                    );
                }

                if (REJECTED .equalsIgnoreCase(status)) {
                    WalletCertResponseDto dto = new WalletCertResponseDto();
                    dto.setStatus(REJECTED );
                    return AppUtil.createApiResponses(
                            true,
                            messageSource.getMessage(
                                    "api.success.wallet.rejected",
                                    null,
                                     LocaleContextHolder.getLocale()
                            ),
                            dto
                    );
                }

                if (APPROVED .equalsIgnoreCase(status)) {
                    return fetchWalletCertificateByOuid(organization.getOuid());
                }
            }


            return fetchWalletCertificateByOuid(organization.getOuid());

        } catch (Exception e) {
            logger.error("{} Error while fetching wallet details | orgDetailsId: {}",
                    CLASS, orgDetailsId, e);

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

    private ApiResponses fetchWalletCertificateByOuid(String ouid) {

        WalletCertResponseDto dto = new WalletCertResponseDto();

        String url = walletGetByOuidUrl + "/" + ouid;

        HttpHeaders headers = new HttpHeaders();

        HttpEntity<Object> requestEntity = new HttpEntity<>(headers);

        ApiResponses res = apiRequestHandler.handleApiRequest(
                url,
                HttpMethod.GET,
                requestEntity
        );

        logger.info("{} Response wallet cert by ouid ::: {}", CLASS, res);




        if (!res.isSuccess() || res.getResult() == null) {
            dto.setStatus(NOT_FOUND);
        }

        else if(res.isSuccess() && res.getResult() == null){
            dto.setStatus(NOT_FOUND);
        }
        else{
            Object resultObj = res.getResult();

            if (!(resultObj instanceof Map)) {
                return AppUtil.createApiResponses(
                        false,
                        messageSource.getMessage(
                                "api.error.wallet.invalid.response",
                                null,
                                 LocaleContextHolder.getLocale()
                        ),
                        null
                );
            }

            Map<?, ?> result = (Map<?, ?>) resultObj;
            dto.setStatus((String) result.get("certificateStatus"));
            dto.setCertificateIssueDate(
                    trimDate((String) result.get("certificateStartDate"))
            );
            dto.setCertificateExpiryDate(
                    trimDate((String) result.get("certificateEndDate"))
            );
        }


        return AppUtil.createApiResponses(
                true,
                messageSource.getMessage(
                        "api.success.wallet.fetched",
                        null,
                         LocaleContextHolder.getLocale()
                ),
                dto
        );
    }




    private String trimDate(String dateTime) {
        if (dateTime == null || dateTime.isBlank()) {
            return null;
        }
        return dateTime.split("[T ]")[0];
    }






}