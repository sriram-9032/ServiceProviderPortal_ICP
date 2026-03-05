package com.dtt.organization.service.iface;

import com.dtt.organization.dto.ApiResponses;
import com.dtt.organization.dto.LoginRequestDTO;
import com.dtt.organization.dto.TrustedUserDTO;

public interface TrustedUserService {



    ApiResponses login(LoginRequestDTO dto);

    ApiResponses saveTrustedUser(TrustedUserDTO trustedUserDTO);
    ApiResponses getAllTrustedUsers();
}
