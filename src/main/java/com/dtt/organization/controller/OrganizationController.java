package com.dtt.organization.controller;


import com.dtt.organization.dto.ApiResponses;
import com.dtt.organization.dto.OrganizationOnboardingDTO;
import com.dtt.organization.model.MetaDocumentEntity;
import com.dtt.organization.repository.MetaDocumentRepository;
import com.dtt.organization.service.iface.OrganizationService;

import com.dtt.organization.util.AppUtil;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api")
public class OrganizationController {



    @Value("${portal.url}")
    private String portalUrl;



    private final OrganizationService organizationService;
    private final MetaDocumentRepository metaDocumentRepository;

    public OrganizationController(

            OrganizationService organizationService,
            MetaDocumentRepository metaDocumentRepository) {


        this.organizationService = organizationService;
        this.metaDocumentRepository = metaDocumentRepository;
    }


@PostMapping(
        value = "/save",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE
)
public ApiResponses save(
        @Valid @RequestPart("data") OrganizationOnboardingDTO dto,
        MultipartHttpServletRequest request
) {
    return organizationService.save(dto, request.getMultiFileMap());
}

    @GetMapping("/meta-documents")
    public ApiResponses getAllMetaDocuments() {
        try {
            List<MetaDocumentEntity> list = metaDocumentRepository.findAll();

            return AppUtil.createApiResponses(true, "Fetched successfully", list);

        } catch (Exception e) {
            return AppUtil.createApiResponses(false, "Failed to fetch meta documents", e.getMessage());
        }
    }

    @GetMapping("/recent/by-spoc")
    public ApiResponses getRecentOrgBySpoc(
            @RequestParam String email) {
        return organizationService.getRecentOrganizationBySpocEmail(email);
    }




}