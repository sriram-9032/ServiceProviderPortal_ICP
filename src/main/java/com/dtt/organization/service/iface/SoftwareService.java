package com.dtt.organization.service.iface;

import com.dtt.organization.dto.ApiResponses;
import com.dtt.organization.dto.SoftwareWithLicenseDTO;
import com.dtt.organization.dto.UploadSofwareDTO;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface SoftwareService {

    ApiResponses uploadSoftware(UploadSofwareDTO dto, MultipartFile softwareZip);

    ApiResponses publishOrUnpublishSoftware(Long softwareId, String action);

    ApiResponses getAllSoftwares();

    List<SoftwareWithLicenseDTO> getSoftwareLicenseCards(Long orgDetailsId);

    ResponseEntity<Resource> downloadSoftware(Long softwareId);

    ApiResponses getSoftwareNameWithValues();
}
