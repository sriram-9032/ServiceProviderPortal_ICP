package com.dtt.organization.controller;

import com.dtt.organization.dto.ApiResponses;
import com.dtt.organization.dto.LoginRequestDTO;
import com.dtt.organization.dto.TrustedUserDTO;
import com.dtt.organization.service.iface.TrustedUserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/public/spoc")
public class TrustedUserController {
    private static final String CLASS = "TrustedUserController";
    private static final Logger logger = LoggerFactory.getLogger(TrustedUserController.class);

    private final TrustedUserService trustedUserService;

    public TrustedUserController(TrustedUserService trustedUserService) {
        this.trustedUserService = trustedUserService;
    }

    @PostMapping("/login")
    public ApiResponses login(@RequestBody LoginRequestDTO dto) {
        logger.info("Login request by email={}", dto.getEmail());
        return trustedUserService.login(dto);
    }

    @PostMapping("/save")
    public ApiResponses saveTrustedUser(@Valid @RequestBody TrustedUserDTO dto) {
        logger.info("{} save trusted user",CLASS);
      return trustedUserService.saveTrustedUser(dto);
    }

    @GetMapping("/get/all")
    public ApiResponses getAll() {
        logger.info("{} get All trusted users ",CLASS);
        return trustedUserService.getAllTrustedUsers();
    }












}
