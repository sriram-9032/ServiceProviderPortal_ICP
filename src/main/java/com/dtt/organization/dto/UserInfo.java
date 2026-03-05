package com.dtt.organization.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UserInfo {

    String username;
    String email;
    String gender;
    String name;
    String suid;
    String birthdate;
    String phone;
    @JsonProperty("id_document_type")
    String idDocumentType;

    @JsonProperty("id_document_number")
    String idDocumentNumber;
    String loa;
    String country;
    String idToken;
    String applicationID;
    @JsonProperty("profile_image")
    String profileImage;
    @JsonProperty("smart_phone_user")
    boolean smartPhoneUser;
    String sub;
    @JsonProperty("login_type")
    String loginType;

    @JsonProperty("login_profile")
    List<LoginProfile> loginProfile;

    String accessToken;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSuid() {
        return suid;
    }

    public void setSuid(String suid) {
        this.suid = suid;
    }

    public String getBirthdate() {
        return birthdate;
    }

    public void setBirthdate(String birthdate) {
        this.birthdate = birthdate;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getIdDocumentType() {
        return idDocumentType;
    }

    public void setIdDocumentType(String idDocumentType) {
        this.idDocumentType = idDocumentType;
    }

    public String getIdDocumentNumber() {
        return idDocumentNumber;
    }

    public void setIdDocumentNumber(String idDocumentNumber) {
        this.idDocumentNumber = idDocumentNumber;
    }

    public String getLoa() {
        return loa;
    }

    public void setLoa(String loa) {
        this.loa = loa;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getIdToken() {
        return idToken;
    }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }

    public String getApplicationID() {
        return applicationID;
    }

    public void setApplicationID(String applicationID) {
        this.applicationID = applicationID;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public boolean isSmartPhoneUser() {
        return smartPhoneUser;
    }

    public void setSmartPhoneUser(boolean smartPhoneUser) {
        this.smartPhoneUser = smartPhoneUser;
    }

    public String getSub() {
        return sub;
    }

    public void setSub(String sub) {
        this.sub = sub;
    }

    public String getLoginType() {
        return loginType;
    }

    public void setLoginType(String loginType) {
        this.loginType = loginType;
    }

    public List<LoginProfile> getLoginProfile() {
        return loginProfile;
    }

    public void setLoginProfile(List<LoginProfile> loginProfile) {
        this.loginProfile = loginProfile;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    @Override
    public String toString() {
        return "UserInfo{" +
                "username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", gender='" + gender + '\'' +
                ", name='" + name + '\'' +
                ", suid='" + suid + '\'' +
                ", birthdate='" + birthdate + '\'' +
                ", phone='" + phone + '\'' +
                ", idDocumentType='" + idDocumentType + '\'' +
                ", idDocumentNumber='" + idDocumentNumber + '\'' +
                ", loa='" + loa + '\'' +
                ", country='" + country + '\'' +
                ", idToken='" + idToken + '\'' +
                ", applicationID='" + applicationID + '\'' +
                ", profileImage='" + profileImage + '\'' +
                ", smartPhoneUser=" + smartPhoneUser +
                ", sub='" + sub + '\'' +
                ", loginType='" + loginType + '\'' +
                ", loginProfile=" + loginProfile +
                ", accessToken='" + accessToken + '\'' +
                '}';
    }
}

