package com.dtt.organization.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class LoginProfile implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("Email")
    private String email;
    @JsonProperty("OrgnizationId")
    private String organizationId;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    @Override
    public String toString() {
        return "LoginProfile{" +
                "email='" + email + '\'' +
                ", organizationId='" + organizationId + '\'' +
                '}';
    }
}



