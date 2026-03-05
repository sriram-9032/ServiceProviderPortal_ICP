package com.dtt.organization.service.iface;

import com.dtt.organization.dto.ApiResponses;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

public interface LicenseService {

    ApiResponses applyLicense(Long orgId);

    ResponseEntity<Resource> downloadLicense(String ouid,String type);






}
