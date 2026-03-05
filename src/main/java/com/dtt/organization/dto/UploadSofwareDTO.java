package com.dtt.organization.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.web.multipart.MultipartFile;

public class UploadSofwareDTO {
    @JsonProperty("SoftwareId")
    private int softwareId;
    private MultipartFile zipFile;
    private String softwareVersion;
    private String fileName;
    private String downloadLink;
    private String installManual;
    private String softwareName;
    private String createdOn;

    private String updatedOn;

    private String publishedOn;
    @JsonProperty("Status")
    private String status;

    private String sizeOfManual;

    private String sizeOfSoftware;

    public int getSoftwareId() {
        return softwareId;
    }

    public void setSoftwareId(int softwareId) {
        this.softwareId = softwareId;
    }

    public MultipartFile getZipFile() {
        return zipFile;
    }

    public void setZipFile(MultipartFile zipFile) {
        this.zipFile = zipFile;
    }

    public String getSoftwareVersion() {
        return softwareVersion;
    }

    public void setSoftwareVersion(String softwareVersion) {
        this.softwareVersion = softwareVersion;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getDownloadLink() {
        return downloadLink;
    }

    public void setDownloadLink(String downloadLink) {
        this.downloadLink = downloadLink;
    }

    public String getInstallManual() {
        return installManual;
    }

    public void setInstallManual(String installManual) {
        this.installManual = installManual;
    }

    public String getSoftwareName() {
        return softwareName;
    }

    public void setSoftwareName(String softwareName) {
        this.softwareName = softwareName;
    }

    public String getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(String createdOn) {
        this.createdOn = createdOn;
    }

    public String getUpdatedOn() {
        return updatedOn;
    }

    public void setUpdatedOn(String updatedOn) {
        this.updatedOn = updatedOn;
    }

    public String getPublishedOn() {
        return publishedOn;
    }

    public void setPublishedOn(String publishedOn) {
        this.publishedOn = publishedOn;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSizeOfManual() {
        return sizeOfManual;
    }

    public void setSizeOfManual(String sizeOfManual) {
        this.sizeOfManual = sizeOfManual;
    }

    public String getSizeOfSoftware() {
        return sizeOfSoftware;
    }

    public void setSizeOfSoftware(String sizeOfSoftware) {
        this.sizeOfSoftware = sizeOfSoftware;
    }

    @Override
    public String toString() {
        return "UploadSofwareDTO{" +
                "softwareId=" + softwareId +
                ", zipFile=" + zipFile +
                ", softwareVersion='" + softwareVersion + '\'' +
                ", fileName='" + fileName + '\'' +
                ", downloadLink='" + downloadLink + '\'' +
                ", installManual='" + installManual + '\'' +
                ", softwareName='" + softwareName + '\'' +
                ", createdOn='" + createdOn + '\'' +
                ", updatedOn='" + updatedOn + '\'' +
                ", publishedOn='" + publishedOn + '\'' +
                ", status='" + status + '\'' +
                ", sizeOfManual='" + sizeOfManual + '\'' +
                ", sizeOfSoftware='" + sizeOfSoftware + '\'' +
                '}';
    }
}
