package com.dtt.organization.service.impl;

import com.dtt.organization.dto.*;
import com.dtt.organization.model.*;
import com.dtt.organization.repository.*;
import com.dtt.organization.util.*;

import com.dtt.organization.service.iface.OrganizationService;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.io.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;



@Service
    @Transactional
    public class OrganizationServiceImpl implements OrganizationService {

    private static final String CLASS = "OrganizationServiceImpl";
    private static final Logger logger = LoggerFactory.getLogger(OrganizationServiceImpl.class);


    @Value("${enable.postpaid}")
    boolean enablePostPaid;

    @Value("${register.organisation}")
    String registerOrganisation;


    @Value("${portal.url}")
    private String portalUrl;

    @Value("${portal.name}")
    private String portalName;


    private final OrganizationService organizationService;
    private final OrganizationRepository organizationRepository;
    private final AuditorRepository auditorRepository;
    private final SpocRepository spocRepository;
    private final EmailService emailService;
    private final OrganisationViewIface organisationViewIface;
    private final OrganizationDocumentRepository organizationDocumentRepository;
    private final APIRequestHandler apiRequestHandler;
    private final OrganisationCategoryRepo organisationCategoryRepo;
    private final MetaDocumentRepository metaDocumentRepository;
    private final MessageSource messageSource;
    private final ObjectMapper objectMapper;

    public OrganizationServiceImpl(
            APIRequestHandler apiRequestHandler,
            OrganisationCategoryRepo organisationCategoryRepo,
            MetaDocumentRepository metaDocumentRepository,
            MessageSource messageSource,
            ObjectMapper objectMapper,
            OrganizationRepository organizationRepository,
            AuditorRepository auditorRepository,
            SpocRepository spocRepository,
            EmailService emailService,
            OrganisationViewIface organisationViewIface,
            OrganizationDocumentRepository organizationDocumentRepository,
            OrganizationService organizationService) {

        this.apiRequestHandler = apiRequestHandler;
        this.organisationCategoryRepo = organisationCategoryRepo;
        this.metaDocumentRepository = metaDocumentRepository;
        this.messageSource = messageSource;
        this.objectMapper = objectMapper;
        this.organizationRepository = organizationRepository;
        this.auditorRepository = auditorRepository;
        this.spocRepository = spocRepository;
        this.emailService = emailService;
        this.organisationViewIface = organisationViewIface;
        this.organizationDocumentRepository = organizationDocumentRepository;
        this.organizationService = organizationService;
    }
    private static final String REJECTED = "REJECTED";
    private static final String SOMETHING_WENT_WRONG = "api.error.something.went.wrong";
    private static final String ACTIVE = "ACTIVE";

    @Value("${organisation.exists}")
    String organisationExists;



    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponses save(OrganizationOnboardingDTO dto,
                             Map<String, List<MultipartFile>> documents) {

        logger.info("{} save() organization onboarding", CLASS);

        try {

            validateUniqueness(dto);

            OrganizationEntity organizationEntity = createAndSaveOrganization(dto);

            processDocuments(organizationEntity.getId(), documents);

            saveSpocAndAuditor(organizationEntity.getId(), dto);

            notifyAdmin(organizationEntity.getOrgName());

            return successResponse();

        } catch (ValidationException ve) {
            return validationErrorResponse(ve);

        } catch (Exception e) {
            return genericErrorResponse(e);
        }
    }

    private void validateUniqueness(OrganizationOnboardingDTO dto) {

        String regNo = dto.getOrganization().getRegNo();
        String taxNumber = dto.getOrganization().getTaxNumber();
        String orgName = dto.getOrganization().getOrgName();

        ApiResponses uniqueCheck = checkUniqueness(orgName, regNo, taxNumber);

        if (uniqueCheck != null) {
            throw new ValidationException(uniqueCheck.getMessage());
        }
    }

    private OrganizationEntity createAndSaveOrganization(OrganizationOnboardingDTO dto) {

        OrganizationEntity entity = new OrganizationEntity();

        entity.setOrgName(dto.getOrganization().getOrgName());
        entity.setRegNo(dto.getOrganization().getRegNo());
        entity.setOrgType(dto.getOrganization().getOrgType());
        entity.setTaxNumber(dto.getOrganization().getTaxNumber());
        entity.setAddress(dto.getOrganization().getAddress());
        entity.setOrgEmail(dto.getOrganization().getOrgEmail());
        entity.setStatus("PENDING");
        entity.setCreatedOn(AppUtil.getDate());
        entity.setUpdatedOn(AppUtil.getDate());

        return organizationRepository.save(entity);
    }
    private void processDocuments(Long orgId,
                                  Map<String, List<MultipartFile>> documents) throws IOException {

        List<MetaDocumentEntity> metaDocs = metaDocumentRepository.findAll();

        for (MetaDocumentEntity meta : metaDocs) {

            List<MultipartFile> files =
                    documents != null ? documents.get(meta.getDocumentLabel()) : null;

            validateMandatoryDocument(meta, files);

            saveFiles(orgId, meta, files);
        }
    }
    private void validateMandatoryDocument(MetaDocumentEntity meta,
                                           List<MultipartFile> files) {

        if (meta.isMandatory() &&
                (files == null || files.isEmpty() || files.get(0).isEmpty())) {

            throw new ValidationException(
                    messageSource.getMessage(
                            "api.error.document.mandatory",
                            new Object[]{meta.getDocumentLabel()},
                            LocaleContextHolder.getLocale()
                    )
            );
        }
    }
    private void saveFiles(Long orgId,
                           MetaDocumentEntity meta,
                           List<MultipartFile> files) throws IOException {

        if (files == null) return;

        for (MultipartFile file : files) {

            if (file != null && !file.isEmpty()) {
                validateDocument(file, meta);
                saveDocumentEntity(orgId, meta.getId(), file);
            }
        }
    }
    private void saveSpocAndAuditor(Long orgId,
                                    OrganizationOnboardingDTO dto) {

        saveSpoc(orgId, dto.getSpocDetails());

        if (dto.getAuditorDetails() != null) {
            saveAuditor(orgId, dto.getAuditorDetails());
        }
    }

    private ApiResponses successResponse() {
        return AppUtil.createApiResponses(
                true,
                messageSource.getMessage(
                        "api.success.organization.onboarding.submitted",
                        null,
                        LocaleContextHolder.getLocale()
                ),
                null
        );
    }

    private ApiResponses validationErrorResponse(ValidationException ve) {
        logger.warn("Validation error during onboarding: {}", ve.getMessage());
        return AppUtil.createApiResponses(false, ve.getMessage(), null);
    }

    private ApiResponses genericErrorResponse(Exception e) {
        logger.error("Error while onboarding organization", e);
        return AppUtil.createApiResponses(
                false,
                messageSource.getMessage(
                        "api.error.organization.onboarding.failed",
                        null,
                        LocaleContextHolder.getLocale()
                ),
                null
        );
    }

    private void validateDocument(MultipartFile file, MetaDocumentEntity meta) {

        long maxSizeInBytes = meta.getDocumentSizeKb() * 1024;
        if (file.getSize() > maxSizeInBytes) {
            throw new ValidationException(
                    messageSource.getMessage(
                            "api.error.document.size.exceeded",
                            new Object[]{meta.getDocumentLabel(), meta.getDocumentSizeKb()},
                             LocaleContextHolder.getLocale()
                    )
            );
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.contains(".")) {
            throw new ValidationException(
                    messageSource.getMessage(
                            "api.error.document.invalid.filename",
                            new Object[]{meta.getDocumentLabel()},
                             LocaleContextHolder.getLocale()
                    )
            );
        }

        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        String allowedTypes = meta.getDocumentType().toLowerCase();

        if (!allowedTypes.contains(extension)) {
            throw new ValidationException(
                    messageSource.getMessage(
                            "api.error.document.invalid.type",
                            new Object[]{meta.getDocumentLabel(), meta.getDocumentType()},
                             LocaleContextHolder.getLocale()
                    )
            );
        }
    }


    private void saveDocumentEntity(Long orgId, Long metaDocumentId, MultipartFile file) throws IOException {
        OrganizationDocumentEntity doc = new OrganizationDocumentEntity();
        doc.setOrgId(orgId);
        doc.setMetaDocumentId(metaDocumentId);
        doc.setDocumentName(file.getOriginalFilename());
        doc.setContentType(file.getContentType());
        doc.setDocumentData(Base64.getEncoder().encodeToString(file.getBytes()));
        doc.setCreatedOn(AppUtil.getDate());
        organizationDocumentRepository.save(doc);
    }

    private ApiResponses checkUniqueness(String orgName, String regNo, String taxNumber) {

        String url = organisationExists + orgName;
        ApiResponses res = apiRequestHandler.handleApiRequest(url, HttpMethod.GET, null);
        if (res != null && res.isSuccess()) {
            return AppUtil.createApiResponses(
                    false,
                    messageSource.getMessage(
                            "api.error.organization.name.exists",
                            null,
                             LocaleContextHolder.getLocale()
                    ),
                    null
            );
        }

        if (regNo != null && organizationRepository.existsByRegNo(regNo)) {
            return AppUtil.createApiResponses(
                    false,
                    messageSource.getMessage(
                            "api.error.organization.regno.exists",
                            null,
                             LocaleContextHolder.getLocale()
                    ),
                    null
            );
        }
        if (taxNumber != null && organizationRepository.existsByTaxNumber(taxNumber)) {
            return AppUtil.createApiResponses(
                    false,
                    messageSource.getMessage(
                            "api.error.organization.taxno.exists",
                            null,
                             LocaleContextHolder.getLocale()
                    ),
                    null
            );
        }
        if (organizationRepository.existsByOrgNameIgnoreCase(orgName)) {
            return AppUtil.createApiResponses(
                    false,
                    messageSource.getMessage(
                            "api.error.organization.name.already.exists",
                            null,
                             LocaleContextHolder.getLocale()
                    ),
                    null
            );
        }
        return null;
    }

    private void saveSpoc(Long orgId, SpocDetailsDTO spocDto) {
        SpocEntity spoc = new SpocEntity();
        spoc.setOrgDetailsId(orgId);
        spoc.setSpocName(spocDto.getSpocName());
        spoc.setSpocOfficalEmail(spocDto.getSpocOfficalEmail());
        spoc.setSpocDocumentNumber(spocDto.getSpocDocumentNumber());
        spoc.setCreatedOn(AppUtil.getDate());
        spoc.setUpdatedOn(AppUtil.getDate());
        spocRepository.save(spoc);
    }

    private void saveAuditor(Long orgId, AuditorDetailsDTO auditorDto) {
        AuditorEntity auditor = new AuditorEntity();
        auditor.setOrgDetailsId(orgId);
        auditor.setAuditorName(auditorDto.getAuditorName());
        auditor.setAuditorDocumentNumber(auditorDto.getAuditorDocumentNumber());
        auditor.setAuditorOfficialEmail(auditorDto.getAuditorOfficalEmail());
        auditor.setCreatedOn(AppUtil.getDate());
        auditor.setUpdatedOn(AppUtil.getDate());
        auditorRepository.save(auditor);
    }

    private void notifyAdmin(String orgName) {
        String subject = "Onboarding Approval Request submitted";

        String htmlContent =
                "<html>" +
                        "<body style='font-family: Arial, Helvetica, sans-serif; font-size: 14px; color: #000;'>" +

                        "<p>Greetings!</p>" +

                        "<p>" +
                        "Administrator,<br/>" +
                        "The SPOC has submitted an onboarding request for the organization " +
                        "<strong>" + orgName + "</strong>." +
                        "</p>" +

                        "<p>Kindly do the needful.</p>" +

                        "<p>Thanks!</p>" +

                        "</body>" +
                        "</html>";
        ApiResponses res = emailService.getAdminEmailList();

        if (res.isSuccess()) {
            List<String> adminEmails = (List<String>) res.getResult();
            if (!adminEmails.isEmpty()) {
                emailService.sendEmailForAdmin(adminEmails, htmlContent, subject);
            }
        }
    }

    @Override

    public ApiResponses approveOrRejectOrg(String status, Long id){

        logger.info("{}  approve Or Reject Org{} ", CLASS, id);
        try {

            OrganizationEntity organizationEntity = organizationRepository.findById(id).orElseThrow(() ->
                    new ValidationException(
                            messageSource.getMessage(
                                    "api.error.organization.not.found",
                                    null,
                                     LocaleContextHolder.getLocale()
                            )
                    ));
            SpocEntity spoc = spocRepository.findByOrgDetailsId(id).orElseThrow(() ->  new ValidationException(
                    messageSource.getMessage(
                            "api.error.organization.spoc.not.found",
                            null,
                             LocaleContextHolder.getLocale()
                    )
            ));



            if (status.equals("APPROVED")) {
                logger.info("{} org approved ::::" ,CLASS);
                if ("APPROVED".equalsIgnoreCase(organizationEntity.getStatus())) {
                    return AppUtil.createApiResponses(
                            false,
                            messageSource.getMessage(
                                    "api.error.organization.already.approved",
                                    null,
                                     LocaleContextHolder.getLocale()
                            ),
                            null
                    );
                }


                RegisterOrganizationDTO registerOrganizationDTO = new RegisterOrganizationDTO();

                HttpHeaders headers = new HttpHeaders();

                List<Integer> tempId = new ArrayList<>();
                List<String> checkBox = new ArrayList<>();
                tempId.add(1);
                tempId.add(5);
                registerOrganizationDTO.setOrganizationName(organizationEntity.getOrgName());
                registerOrganizationDTO.setSpocUgpassEmail(spoc.getSpocOfficalEmail());
                registerOrganizationDTO.setManageByAdmin(false);

                registerOrganizationDTO.setTaxNo(organizationEntity.getTaxNumber());
                registerOrganizationDTO.setCorporateOfficeAddress(organizationEntity.getAddress());
                registerOrganizationDTO.setTemplateId(tempId);
                registerOrganizationDTO.setDocumentListCheckbox(checkBox);


                registerOrganizationDTO.setOrganizationEmail(spoc.getSpocOfficalEmail());

                registerOrganizationDTO.setEnablePostPaidOption(enablePostPaid);



                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Object> reqEntity = new HttpEntity<>(registerOrganizationDTO, headers);





                String url = registerOrganisation;
                logger.info("{} URL CALLING ::::{}" ,CLASS,url);
                ApiResponses res = apiRequestHandler.handleApiRequest(url, HttpMethod.POST, reqEntity);


                if (!res.isSuccess()) {
                    return AppUtil.createApiResponses(false, res.getMessage(), null);
                }



                String response = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(res.getResult());

                RegisterOrganizationDTO responseDto = objectMapper.readValue(response, RegisterOrganizationDTO.class);


                organizationEntity.setStatus(ACTIVE);
                organizationEntity.setOuid(responseDto.getOrganizationUid());
                organizationEntity.setUpdatedOn(AppUtil.getDate());

                organizationRepository.save(organizationEntity);



                String body =
                        "<html>" +
                                "<body style='font-family: Arial, Helvetica, sans-serif; font-size: 14px; color: #000;'>" +

                                "<p>Dear " + spoc.getSpocName() + ",</p>" +

                                "<p>Your organization has been approved on the <strong>" + portalName + "</strong>.</p>" +

                                "<p>" +
                                "<strong>Organization Name:</strong> " + organizationEntity.getOrgName() +
                                "</p>" +

                                "<p>You can now access the <strong>" + portalName + "</strong> using your existing login credentials.</p>" +

                                "<p>" +
                                "<strong>Portal Link:</strong><br/>" +
                                "<a href='" + portalUrl + "' target='_blank'>" + portalUrl + "</a>" +
                                "</p>" +

                                "<p>Regards,<br/>" +
                                "<strong> Admin</strong></p>" +

                                "<p style='font-size: 10px; font-style: italic; color: gray;'>" +
                                "* This is an automated email from <strong>" + portalName + "</strong>. " +
                                "Please contact the administrator if you have any questions regarding this email." +
                                "</p>" +


                                "</body>" +
                                "</html>";


                emailService.sendEmail(spoc.getSpocOfficalEmail(),  body,"Your Organization is Approved");




                return AppUtil.createApiResponses(
                        true,
                        messageSource.getMessage(
                                "api.success.organization.approved",
                                null,
                                 LocaleContextHolder.getLocale()
                        ),
                        null
                );
            } else if (status.equals(REJECTED)) {
                logger.info("{} org rejected ::::" ,CLASS);

                if (REJECTED.equalsIgnoreCase(organizationEntity.getStatus())) {
                    return AppUtil.createApiResponses(
                            false,
                            messageSource.getMessage(
                                    "api.error.organization.already.rejected",
                                    null,
                                     LocaleContextHolder.getLocale()
                            ),
                            null
                    );
                }
                organizationEntity.setStatus(REJECTED);
                organizationEntity.setUpdatedOn(AppUtil.getDate());

                organizationRepository.save(organizationEntity);
                String body =
                        "<html>" +
                                "<body style='font-family: Arial, Helvetica, sans-serif; font-size: 14px; color: #000;'>" +

                                "<p>Dear " + spoc.getSpocName() + ",</p>" +

                                "<p>" +
                                "We regret to inform you that your organisation " +
                                "<strong>\"" + organizationEntity.getOrgName() + "\"</strong> " +
                                "has been rejected." +
                                "</p>" +

                                "<p>Regards,<br/>" +
                                "Admin</p>" +


                                "<p style='font-size: 10px; font-style: italic; color: gray;'>" +
                                "* This is an automated email from <strong>" + portalName + "</strong>. " +
                                "Please contact the administrator if you have any questions regarding this email." +
                                "</p>" +


                                "</body>" +
                                "</html>";


                emailService.sendEmail(spoc.getSpocOfficalEmail(),  body,"Organisation Rejected");


                return AppUtil.createApiResponses(
                        true,
                        messageSource.getMessage(
                                "api.success.organization.rejected",
                                null,
                                 LocaleContextHolder.getLocale()
                        ),
                        null
                );
            } else {
                return AppUtil.createApiResponses(
                        false,
                        messageSource.getMessage(
                                "api.error.organization.invalid.status",
                                null,
                                 LocaleContextHolder.getLocale()
                        ),
                        null
                );
            }
        }catch (Exception e){
            logger.error("Unexpected error occurred while processing request", e);
            return AppUtil.createApiResponses(
                    false,
                    messageSource.getMessage(
                            SOMETHING_WENT_WRONG,
                            null,
                             LocaleContextHolder.getLocale()
                    ),
                    null
            );

        }
    }


    @Override
    public ApiResponses  getAllOrganizations() {

        logger.info("{} get All Organizations",CLASS);
        try{

        List<OrganizationEntity> organizationEntities = organizationRepository.findAllOrgs();


        List<GetAllOrganizationtDTO> organizationtDTOS = new ArrayList<>();


        for (OrganizationEntity establishment : organizationEntities) {

            GetAllOrganizationtDTO establishmentDTO = new GetAllOrganizationtDTO();
            establishmentDTO.setOrgName(establishment.getOrgName());
            establishmentDTO.setOrgDetailsId(establishment.getId());
            establishmentDTO.setRegNo(establishment.getRegNo());
            establishmentDTO.setOrgType(establishment.getOrgType());
            establishmentDTO.setStatus(establishment.getStatus());
            establishmentDTO.setTaxNumber(establishment.getTaxNumber());
            establishmentDTO.setAddress(establishment.getAddress());
            establishmentDTO.setOrgEmail(establishment.getOrgEmail());
            Optional<SpocEntity> spocOpt =
                    spocRepository.findByOrgDetailsId(establishment.getId());

            if (spocOpt.isPresent()) {
                establishmentDTO.setSpocName(spocOpt.get().getSpocName());
                establishmentDTO.setSpocOfficalEmail(spocOpt.get().getSpocOfficalEmail());
            }

            organizationtDTOS.add(establishmentDTO);
        }


            return AppUtil.createApiResponses(
                    true,
                    messageSource.getMessage(
                            "api.success.organizations.fetched",
                            null,
                             LocaleContextHolder.getLocale()
                    ),
                    organizationtDTOS
            );
        }catch (Exception e){
            logger.error("Error while fetching organizations", e);
            return AppUtil.createApiResponses(
                    false,
                    messageSource.getMessage(
                            "api.error.organizations.fetch.failed",
                            null,
                             LocaleContextHolder.getLocale()
                    ),
                    null
            );

        }
    }


    @Override
    public ApiResponses getOrganizationDetailsById(Long id) {
        logger.info("{} get organization by id {}",CLASS,id);
        try {

            OrganizationEntity organizationEntity = organizationRepository.findById(id).orElse(null);

            if (organizationEntity == null) {
                return AppUtil.createApiResponses(
                        false,
                        messageSource.getMessage(
                                "api.error.organization.not.found",
                                null,
                                 LocaleContextHolder.getLocale()
                        ),
                        null
                );
            }


            Optional<SpocEntity> spocEntity = spocRepository.findByOrgDetailsId(organizationEntity.getId());

            logger.info("{} spoc details {}",CLASS,spocEntity);

            Optional<AuditorEntity> auditorEntity = auditorRepository.findByOrgDetailsId(organizationEntity.getId());

            OrganizationDetailsResponseDTO response = new OrganizationDetailsResponseDTO();


            response.setOuid(organizationEntity.getOuid());
            response.setOrgName(organizationEntity.getOrgName());
            response.setOrgNo(organizationEntity.getRegNo());

            response.setOrgType(organizationEntity.getOrgType());

            response.setStatus(organizationEntity.getStatus());
            response.setTaxNumber(organizationEntity.getTaxNumber());
            response.setOrgDetailsId(organizationEntity.getId());
            response.setCreatedOn(AppUtil.formatDate(organizationEntity.getUpdatedOn()));
            response.setAddress(organizationEntity.getAddress());
            response.setOrgEmail(organizationEntity.getOrgEmail());


            if (spocEntity.isPresent()) {
                response.setSpocName(spocEntity.get().getSpocName());
                response.setSpocOfficialEmail(spocEntity.get().getSpocOfficalEmail());
                response.setSpocDocumentNumber(spocEntity.get().getSpocDocumentNumber());
            }


            if (auditorEntity.isPresent()) {
                response.setAuditorName(auditorEntity.get().getAuditorName());
                response.setAuditorOfficialEmail(
                        auditorEntity.get().getAuditorOfficialEmail());
                response.setAuditorDocumentNumber(
                        auditorEntity.get().getAuditorDocumentNumber());
            }

            List<OrganizationDocumentEntity> orgDocuments =
                    organizationDocumentRepository.findByOrgId(organizationEntity.getId());

            List<DocumentResponseDTO> documentDtos = new ArrayList<>();

            for (OrganizationDocumentEntity orgDoc : orgDocuments) {

                Optional<MetaDocumentEntity> metaOpt = metaDocumentRepository.findById(orgDoc.getMetaDocumentId());

                if (metaOpt.isPresent()) {
                    MetaDocumentEntity meta = metaOpt.get();

                    DocumentResponseDTO docDto = new DocumentResponseDTO();
                    docDto.setDocumentName(meta.getDocumentName());
                    docDto.setDocumentType(meta.getDocumentType());


                    docDto.setDocumentData(portalUrl+"/api/public/download/document/by/id/"+organizationEntity.getId()+"/"+meta.getId()+"/" +meta.getDocumentType());

                    documentDtos.add(docDto);
                }
            }

            response.setDocuments(documentDtos);




            return AppUtil.createApiResponses(
                    true,
                    messageSource.getMessage(
                            "api.success.organization.details.fetched",
                            null,
                             LocaleContextHolder.getLocale()
                    ),
                    response
            );

        } catch (Exception e) {
            logger.error("{} Error while fetching organization details for id {}", CLASS, id, e);
            return AppUtil.createApiResponses(
                    false,
                    messageSource.getMessage(
                            "api.error.organization.details.fetch.failed",
                            null,
                             LocaleContextHolder.getLocale()
                    ),
                    null
            );
        }
    }


    @Transactional
    @Override
    public ApiResponses getOrganizationsBySpocEmail(String email, int page, int size)
    {

        try {
            ApiResponses res = organizationService.syncDataFromAdmin(email);
            if(!res.isSuccess()){
                return AppUtil.createApiResponses(
                        false,
                        messageSource.getMessage(
                                SOMETHING_WENT_WRONG,
                                null,
                                LocaleContextHolder.getLocale()
                        ),
                        null
                );
            }

            List<SpocEntity> spocList = spocRepository.findAllBySpocOfficalEmail(email);
            if (spocList.isEmpty()) {
                return AppUtil.createApiResponses(
                        false,
                        messageSource.getMessage(
                                "api.error.spoc.organizations.not.found",
                                null,
                                 LocaleContextHolder.getLocale()
                        ),
                        Page.empty()
                );
            }
            List<Long> orgIds = spocList.stream()
                    .map(SpocEntity::getOrgDetailsId)
                    .distinct()
                    .toList();

            Pageable pageable = PageRequest.of(
                    page,
                    size,
                    Sort.by("createdOn").descending()
            );

            Page<OrganizationEntity> orgPage = organizationRepository.findByIdIn(orgIds, pageable);

            Page<SpocOrganizationResponseDTO> dtoPage =
                    orgPage.map(org -> {
                        SpocOrganizationResponseDTO dto =
                                new SpocOrganizationResponseDTO();

                        dto.setOrganizationId(org.getId());
                        dto.setOrganizationName(org.getOrgName());
                        dto.setRegNo(org.getRegNo());
                        dto.setStatus(org.getStatus());
                        dto.setCreatedOn(AppUtil.formatDate(org.getCreatedOn()));
                        dto.setOrgType(org.getOrgType());

                        return dto;
                    });

            return AppUtil.createApiResponses(
                    true,
                    messageSource.getMessage(
                            "api.success.organizations.fetched",
                            null,
                             LocaleContextHolder.getLocale()
                    ),
                    dtoPage
            );

        } catch (Exception e) {
            logger.error("{} Error while fetching organizations for spoc email {}", CLASS, email, e);
            return AppUtil.createApiResponses(
                    false,
                    messageSource.getMessage(
                            SOMETHING_WENT_WRONG,
                            null,
                             LocaleContextHolder.getLocale()
                    ),
                    null
            );
        }
    }


@Override
public ApiResponses getDashboardDetails(String spocEmail) {

    try {

        ApiResponses res1 = organizationService.syncDataFromAdmin(spocEmail);
         if(!res1.isSuccess()){
             return AppUtil.createApiResponses(
                     false,
                     messageSource.getMessage(
                             SOMETHING_WENT_WRONG,
                             null,
                              LocaleContextHolder.getLocale()
                     ),
                     null
             );
         }



        DashboardResponseDto dto = new DashboardResponseDto();
        List<OrganizationEntity> orgs = organizationRepository.getAllOrgsBySpocEmail(spocEmail);

        dto.setOrgs(organizationRepository.countActiveOrganizationsBySpocEmail(spocEmail));
        dto.setTotalApplications(organizationRepository.countApplicationsBySpocEmail(spocEmail));
        long pendingApplications = 0;
        long approvedOrganizations = 0;

        for (OrganizationEntity org : orgs) {


            if ("PENDING".equalsIgnoreCase(org.getStatus())) {
                pendingApplications++;
            }
            else if (ACTIVE.equalsIgnoreCase(org.getStatus())) {
                approvedOrganizations++;
            }
        }

        dto.setPendingApplications(pendingApplications);
        dto.setTotalOrganizations(approvedOrganizations);

        return AppUtil.createApiResponses(
                true,
                messageSource.getMessage(
                        "api.success.dashboard.fetched",
                        null,
                         LocaleContextHolder.getLocale()
                ),
                dto
        );


    } catch (Exception e) {
        logger.error("{} Error while fetching dashboard details for spocEmail {}", CLASS, spocEmail, e);
        return AppUtil.createApiResponses(
                false,
                messageSource.getMessage(
                        "api.error.dashboard.fetch.failed",
                        null,
                         LocaleContextHolder.getLocale()
                ),
                null
        );
    }
}



    @Transactional
    public ApiResponses syncDataFromAdmin(String email) {
        try {
            if (email == null || email.isEmpty()) {
                return AppUtil.createApiResponses(
                        false,
                        messageSource.getMessage(
                                "api.error.email.null",
                                null,
                                 LocaleContextHolder.getLocale()
                        ),
                        null
                );
            }

            List<OrganizationEntity> formsInSSP = organizationRepository.organizationsBySpocEmail(email);
           
            List<OrganisationView> formsInView = organisationViewIface.getDetailsByEmail(email);
          

            updateChangedSpocs(formsInSSP, formsInView);
            createMissingOrganisations(email, formsInSSP, formsInView);




            return AppUtil.createApiResponses(
                    true,
                    messageSource.getMessage(
                            "api.success.sync.completed",
                            null,
                             LocaleContextHolder.getLocale()
                    ),
                    null
            );

        } catch (Exception e) {

            return AppUtil.createApiResponses(
                    false,
                    messageSource.getMessage(
                            "api.error.sync.failed",
                            null,
                             LocaleContextHolder.getLocale()
                    ),
                    null
            );
        }
    }


    private void updateChangedSpocs(List<OrganizationEntity> formsInSSP,
                                    List<OrganisationView> formsInView) {

        Map<String, String> orgUgPassEmailMap = formsInView.stream()
                .collect(Collectors.toMap(
                        OrganisationView::getOuid,
                        OrganisationView::getSpocUgpassEmail,
                        (e1, e2) -> e1));

        for (OrganizationEntity sspForm : formsInSSP) {

            String ouid = sspForm.getOuid();
            String viewEmail = orgUgPassEmailMap.get(ouid);

            Optional<SpocEntity> spocEntity =
                    spocRepository.findByOrgDetailsId(sspForm.getId());

            spocEntity.ifPresent(spoc -> {

                String sspEmail = spoc.getSpocOfficalEmail();

                if (!Objects.equals(sspEmail, viewEmail)) {
                    handleSpocMismatch(sspForm, ouid);
                }
            });
        }
    }
    private void handleSpocMismatch(OrganizationEntity sspForm, String ouid) {

        Optional<OrganizationEntity> form =
                organizationRepository.findById(sspForm.getId());

        if (form.isEmpty()) {
            return;
        }

        OrganisationView details =
                organisationViewIface.getESealDetails(ouid);

        updateSpocDetailsWithoutDevice(form, details);
    }

    private void updateSpocDetailsWithoutDevice(Optional<OrganizationEntity> form,
            OrganisationView details) {

        form.ifPresent(organization ->

                spocRepository.findByOrgDetailsId(organization.getId())
                        .ifPresent(spoc -> {

                            spoc.setSpocOfficalEmail(details.getSpocUgpassEmail());
                            spoc.setSpocName(null);
                            spoc.setSpocDocumentNumber(null);
                            spoc.setUpdatedOn(AppUtil.getDate());

                            spocRepository.save(spoc);
                        })
        );
    }




    private void createMissingOrganisations(String email,
                                            List<OrganizationEntity> formsInSSP,
                                            List<OrganisationView> formsInView) {



        for (OrganisationView viewForm : formsInView) {
            boolean exists = formsInSSP.stream().anyMatch(f -> f.getOuid().equals(viewForm.getOuid()));
            if (!exists) {
                createNewOrganisation(viewForm, email);
            }
        }
    }

    private void createNewOrganisation(OrganisationView viewForm,
                                       String email) {

        OrganizationEntity newForm = new OrganizationEntity();
        newForm.setOrgName(viewForm.getOrgName());
        newForm.setRegNo(viewForm.getUniqueRegdNo());
        newForm.setOuid(viewForm.getOuid());
        newForm.setTaxNumber(viewForm.getTaxNo());
        newForm.setAddress(viewForm.getCorporateOfficeAddress());
        newForm.setOrgEmail(viewForm.getOrganizationEmail());

        newForm.setOrgType("government");
        newForm.setStatus(ACTIVE);
        newForm.setOrgAddedByAdmin(true);
        newForm.setCreatedOn(viewForm.getCreatedDate());
        newForm.setUpdatedOn(viewForm.getUpdatedDate());


        OrganizationEntity saved = organizationRepository.save(newForm);

        AuditorEntity auditor = new AuditorEntity();
        auditor.setOrgDetailsId(saved.getId());
        auditorRepository.save(auditor);



        SpocEntity spoc = new SpocEntity();
        spoc.setOrgDetailsId(saved.getId());
        spoc.setSpocOfficalEmail(email);
        spoc.setSpocDocumentNumber(null);
        spoc.setSpocName(null);

        spoc.setCreatedOn(viewForm.getCreatedDate());
        spoc.setUpdatedOn(viewForm.getUpdatedDate());
        spocRepository.save(spoc);
    }


    @Override
    public ApiResponses getAllOrganizationApprovalDetails() {
        try {
            List<Object[]> results = organizationRepository.findAllApprovedOrganizations();

            List<Map<String, Object>> responseList = new ArrayList<>();

            for (Object[] row : results) {
                Map<String, Object> orgData = new HashMap<>();
                orgData.put("org_uid", row[0]);
                orgData.put("org_status", row[1]);
                orgData.put("org_category", row[2]);
                responseList.add(orgData);
            }
            return AppUtil.createApiResponses(
                    true,
                    messageSource.getMessage(
                            "api.success.organization.approval.details.fetched",
                            null,
                             LocaleContextHolder.getLocale()
                    ),
                    responseList
            );

        } catch (Exception e) {
            logger.error("{} Error while fetching organization approval details", CLASS, e);
            return AppUtil.createApiResponses(
                    false,
                    messageSource.getMessage(
                            "api.error.organization.approval.details.fetch.failed",
                            null,
                             LocaleContextHolder.getLocale()
                    ),
                    null
            );
        }
    }


    @Override
    public ApiResponses getOrgCategoryandidByOrgid(String orgId) {
        try{
            if(orgId == null || orgId.isEmpty()){
                return AppUtil.createApiResponses(
                        false,
                        messageSource.getMessage(
                                "api.error.org.id.null",
                                null,
                                 LocaleContextHolder.getLocale()
                        ),
                        null
                );
            }

            OrganizationEntity organisationOnboardingForms = organizationRepository.allFormByOrgUid(orgId);



            if(organisationOnboardingForms ==null){
                return AppUtil.createApiResponses(
                        false,
                        messageSource.getMessage(
                                "api.error.organization.details.not.found",
                                null,
                                 LocaleContextHolder.getLocale()
                        ),
                        null
                );
            }

            Optional<OrganisationCategories> organisationCategories = organisationCategoryRepo.findByCategoryName(organisationOnboardingForms.getOrgType());

            GetOrgIDByOuidResponseDto organisationOnboardingFormsDTO = new GetOrgIDByOuidResponseDto();
           organisationOnboardingFormsDTO.setOrgName(organisationOnboardingForms.getOrgName());
           organisationOnboardingFormsDTO.setOuid(organisationOnboardingForms.getOuid());
            organisationOnboardingFormsDTO.setCategoryName(organisationOnboardingForms.getOrgType());
            organisationOnboardingFormsDTO.setCategoryId(organisationCategories.get().getId());
            organisationOnboardingFormsDTO.setCategoryDisplayName(organisationCategories.get().getLabelName());




            return AppUtil.createApiResponses(
                    true,
                    messageSource.getMessage(
                            "api.success.organization.category.fetched",
                            null,
                             LocaleContextHolder.getLocale()
                    ),
                    organisationOnboardingFormsDTO
            );



        }catch(Exception e){
            logger.error("{} Error while fetching org category for orgId {}", CLASS, orgId, e);

            return AppUtil.createApiResponses(
                    false,
                    messageSource.getMessage(
                            "api.error.organization.category.fetch.failed",
                            null,
                             LocaleContextHolder.getLocale()
                    ),
                    null
            );
        }
    }


    @Override
    public ApiResponses getRecentOrganizationBySpocEmail(String spocEmail) {

        try {
            if (spocEmail == null || spocEmail.isEmpty()) {
                return AppUtil.createApiResponses(
                        false,
                        messageSource.getMessage(
                                "api.error.spoc.email.null",
                                null,
                                 LocaleContextHolder.getLocale()
                        ),
                        null
                );
            }

            Optional<SpocEntity> spocOpt =
                    spocRepository.findTopBySpocOfficalEmailOrderByCreatedOnDesc(spocEmail);

            if (!spocOpt.isPresent()) {
                return AppUtil.createApiResponses(
                        false,
                        messageSource.getMessage(
                                "api.error.spoc.details.not.found",
                                null,
                                 LocaleContextHolder.getLocale()
                        ),
                        null
                );
            }

            SpocEntity spoc = spocOpt.get();


            Optional<OrganizationEntity> orgOpt =
                    organizationRepository.findById(spoc.getOrgDetailsId());

            if (!orgOpt.isPresent()) {
                return AppUtil.createApiResponses(
                        false,
                        messageSource.getMessage(
                                "api.error.organization.recent.not.found",
                                null,
                                 LocaleContextHolder.getLocale()
                        ),
                        null
                );
            }

            OrganizationEntity org = orgOpt.get();


            OrganizationRecentDTO dto = new OrganizationRecentDTO();
            dto.setOrgName(org.getOrgName());
            String orgType = org.getOrgType();
            if (orgType != null && !orgType.isEmpty()) {
                dto.setOrgType(
                        orgType.substring(0, 1).toUpperCase() + orgType.substring(1).toLowerCase()
                );
            }
            dto.setStatus(org.getStatus());
            String createdOn = org.getCreatedOn();
            if (createdOn != null && createdOn.length() >= 10) {
                dto.setCreatedOn(createdOn.substring(0, 10));
            }
            dto.setTaxNumber(org.getTaxNumber());
            dto.setRegNo(org.getRegNo());

            return AppUtil.createApiResponses(
                    true,
                    messageSource.getMessage(
                            "api.success.organization.recent.fetched",
                            null,
                             LocaleContextHolder.getLocale()
                    ),
                    dto
            );


        } catch (Exception e) {
            logger.error("{} Error while fetching recent organization for spocEmail {}",
                    CLASS, spocEmail, e);
            return AppUtil.createApiResponses(
                    false,
                    messageSource.getMessage(
                            SOMETHING_WENT_WRONG,
                            null,
                             LocaleContextHolder.getLocale()
                    ),
                    null
            );
        }
    }



    @Override
    public ResponseEntity<Resource> downloadDocument(Long orgDetailsId, Long documentId, String documentType) {

        OrganizationDocumentEntity doc =
                organizationDocumentRepository.findByOrgIdAndMetaDocumentId(orgDetailsId, documentId);

        byte[] fileBytes = Base64.getDecoder().decode(doc.getDocumentData());

        ByteArrayResource resource = new ByteArrayResource(fileBytes);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(doc.getContentType()))
                .contentLength(fileBytes.length)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + doc.getDocumentName() + "\"")
                .body((Resource) resource);
    }



}
