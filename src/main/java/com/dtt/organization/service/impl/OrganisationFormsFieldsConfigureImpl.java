package com.dtt.organization.service.impl;

import com.dtt.organization.dto.ApiResponses;
import com.dtt.organization.model.OrganisationCategories;
import com.dtt.organization.repository.OrganisationCategoryRepo;
import com.dtt.organization.service.iface.OrganisationFormsFieldsConfigureIface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrganisationFormsFieldsConfigureImpl
        implements OrganisationFormsFieldsConfigureIface {
    private static final String CLASS = "OrganisationFormsFieldsConfigureImpl";
    private static final Logger logger = LoggerFactory.getLogger(OrganisationFormsFieldsConfigureImpl.class);


    private final OrganisationCategoryRepo organisationCategoryRepo;
    private final MessageSource messageSource;

    public OrganisationFormsFieldsConfigureImpl(
            OrganisationCategoryRepo organisationCategoryRepo,
            MessageSource messageSource) {

        this.organisationCategoryRepo = organisationCategoryRepo;
        this.messageSource = messageSource;
    }

    @Override
    public ApiResponses getAllCategories() {
        logger.info("{} getAllCategories() request received", CLASS);
        try {

            List<OrganisationCategories> categories =
                    organisationCategoryRepo.findAll();
            logger.info("{} getAllCategories() successful | totalCategories={}",
                    CLASS,
                    categories.size());
            return AppUtil.createApiResponses(
                    true,
                    messageSource.getMessage(
                            "api.success.categories.fetched",
                            null,
                            LocaleContextHolder.getLocale()
                    ),
                    categories
            );

        } catch (Exception e) {
            logger.error("{} getAllCategories() failed", CLASS, e);

            return AppUtil.createApiResponses(
                    false,
                    messageSource.getMessage(
                            "api.error.something.went.wrong",
                            null,
                            LocaleContextHolder.getLocale()
                    ),
                    null
            );
        }
    }

    @Override
    public ApiResponses updateCategoryLabel(Integer id, String labelName) {
        logger.info("{} updateCategoryLabel() | id={}", CLASS, id);
        try {



            if (id == null || id <= 0) {
                return AppUtil.createApiResponses(
                        false,
                        messageSource.getMessage(
                                "api.error.category.id.invalid",
                                null,
                                LocaleContextHolder.getLocale()
                        ),
                        null
                );
            }

            if (labelName == null || labelName.trim().isEmpty()) {
                return AppUtil.createApiResponses(
                        false,
                        messageSource.getMessage(
                                "api.error.category.label.empty",
                                null,
                                LocaleContextHolder.getLocale()
                        ),
                        null
                );
            }

            String updatedOn = LocalDateTime.now().toString();

            int updatedRows = organisationCategoryRepo
                    .updateLabelNameById(id, labelName.trim(), updatedOn);

            if (updatedRows == 0) {
                return AppUtil.createApiResponses(
                        false,
                        messageSource.getMessage(
                                "api.error.category.not.found",
                                null,
                                LocaleContextHolder.getLocale()
                        ),
                        null
                );
            }

            return AppUtil.createApiResponses(
                    true,
                    messageSource.getMessage(
                            "api.success.category.label.updated",
                            null,
                            LocaleContextHolder.getLocale()
                    ),
                    null
            );
        }
        catch (org.springframework.dao.DataIntegrityViolationException ex) {
            return AppUtil.createApiResponses(
                    false,
                    messageSource.getMessage(
                            "api.error.category.label.exists",
                            null,
                            LocaleContextHolder.getLocale()
                    ),
                    null
            );
        }
        catch (Exception e) {
            logger.error("Error updating category label", e);
            return AppUtil.createApiResponses(
                    false,
                    messageSource.getMessage(
                            "api.error.something.went.wrong",
                            null,
                            LocaleContextHolder.getLocale()
                    ),
                    null
            );
        }
    }
}
