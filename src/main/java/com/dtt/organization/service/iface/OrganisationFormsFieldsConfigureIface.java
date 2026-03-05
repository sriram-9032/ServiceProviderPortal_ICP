package com.dtt.organization.service.iface;


import com.dtt.organization.dto.ApiResponses;

public interface OrganisationFormsFieldsConfigureIface {


    ApiResponses getAllCategories();
    ApiResponses updateCategoryLabel(Integer id, String labelName);
}