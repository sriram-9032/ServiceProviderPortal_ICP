package com.dtt.organization.service.iface;


import com.dtt.organization.dto.ApiResponses;
import com.dtt.organization.dto.OrganizationOnboardingDTO;
import com.dtt.organization.dto.SpocOrganizationResponseDTO;
import org.springframework.core.io.Resource;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface OrganizationService {


    ApiResponses save(OrganizationOnboardingDTO dto, Map<String, List<MultipartFile>> documents);


    public ApiResponses approveOrRejectOrg(String status,Long id);

    ApiResponses getAllOrganizations();


    ApiResponses getOrganizationsBySpocEmail(String email, int page, int size);
    ApiResponses getOrganizationDetailsById(Long id);

    ApiResponses getDashboardDetails(String spocEmail);


    ApiResponses getAllOrganizationApprovalDetails();

    ApiResponses getOrgCategoryandidByOrgid(String orgId);

    ApiResponses getRecentOrganizationBySpocEmail(String spocEmail);


    ResponseEntity<Resource> downloadDocument(Long orgDetailsId, Long documentId, String documentType);

    ApiResponses syncDataFromAdmin(String email);
}
