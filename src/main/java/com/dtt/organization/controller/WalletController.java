package com.dtt.organization.controller;


import com.dtt.organization.dto.ApiResponses;
import com.dtt.organization.service.iface.WalletIface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    private final WalletIface walletIface;

    public WalletController(WalletIface walletIface) {
        this.walletIface = walletIface;
    }
    private static final String CLASS = "WalletController";
    private static final Logger logger = LoggerFactory.getLogger(WalletController.class);

    @PostMapping("/save/wallet")
    public ApiResponses saveWallet(@RequestParam Long orgDetailsId,@RequestParam(required = false) String paymentId) {
        logger.info("{} saveWallet",CLASS);
        return walletIface.generateWalletCert(orgDetailsId,paymentId);
    }

    @PostMapping("/save/wallet/req")
    public ApiResponses saveWalletReq(@RequestParam Long orgDetailsId, @RequestParam(required = false) String paymentId) {
        logger.info("{} saveWalletReq",CLASS);
        return walletIface.saveWalletCertRequest(orgDetailsId,paymentId);
    }



}
