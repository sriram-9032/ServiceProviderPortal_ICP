package com.dtt.organization.service.iface;

import com.dtt.organization.dto.ApiResponses;

public interface WalletIface {

    ApiResponses generateWalletCert(Long id, String paymentReferenceId);

    ApiResponses saveWalletCertRequest(Long id,String paymentReferenceId);

    ApiResponses changeStatusOfWalletCert(Long id,String status);

    ApiResponses getAllWalletReqs();

    ApiResponses walletDetails(long id);



}
