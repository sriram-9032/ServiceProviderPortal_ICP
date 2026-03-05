package com.dtt.organization.controller;

import com.dtt.organization.service.iface.OrganisationFormsFieldsConfigureIface;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.web.bind.annotation.*;


@RequestMapping("/api/public")
@RestController
public class OrganisationCategoryMappingController {


    private static final String CLASS = "OrganisationCategoryMappingController";
    private static final Logger logger = LoggerFactory.getLogger(OrganisationCategoryMappingController.class);

    private final OrganisationFormsFieldsConfigureIface organisationFormsFieldsConfigureIface;

    public OrganisationCategoryMappingController(OrganisationFormsFieldsConfigureIface organisationFormsFieldsConfigureIface) {
        this.organisationFormsFieldsConfigureIface = organisationFormsFieldsConfigureIface;
    }


    @GetMapping("/get/all/categories")
    public ApiResponses getCategories(){
        logger.info("{} get all Categories ",CLASS);
        return organisationFormsFieldsConfigureIface.getAllCategories();
    }
    @PutMapping("/update/category/label")
    public ApiResponses updateCategoryLabel(
            @RequestParam(required = false) Integer id,
            @RequestParam(required = false) String labelName) {

        return organisationFormsFieldsConfigureIface
                .updateCategoryLabel(id, labelName);
    }



}
