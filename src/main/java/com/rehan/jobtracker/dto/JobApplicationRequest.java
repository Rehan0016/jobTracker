package com.rehan.jobtracker.dto;

import com.rehan.jobtracker.entity.ApplicationStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import org.hibernate.validator.constraints.URL;
import java.time.LocalDate;

public class JobApplicationRequest {

    @NotBlank(message = "Company name is required")
    private String companyName;

    @NotBlank(message = "Job title is required")
    private String jobTitle;

    private String location;

    @URL(message = "Job URL must be a valid URL")
    private String jobUrl;

    @NotNull(message = "Application status is required")
    private ApplicationStatus status;

    @PastOrPresent(message = "Application date must not be in the future")
    private LocalDate applicationDate;

    private String notes;

    @jakarta.validation.constraints.Size(max = 4000, message = "Job description cannot exceed 4000 characters")
    private String jobDescription;

    private LocalDate deadline;

    public JobApplicationRequest() {
    }

    public JobApplicationRequest(String companyName, String jobTitle, String location, String jobUrl,
                                 ApplicationStatus status, LocalDate applicationDate, String notes) {
        this.companyName = companyName;
        this.jobTitle = jobTitle;
        this.location = location;
        this.jobUrl = jobUrl;
        this.status = status;
        this.applicationDate = applicationDate;
        this.notes = notes;
    }

    public JobApplicationRequest(String companyName, String jobTitle, String location, String jobUrl,
                                 ApplicationStatus status, LocalDate applicationDate, String notes,
                                 String jobDescription, LocalDate deadline) {
        this.companyName = companyName;
        this.jobTitle = jobTitle;
        this.location = location;
        this.jobUrl = jobUrl;
        this.status = status;
        this.applicationDate = applicationDate;
        this.notes = notes;
        this.jobDescription = jobDescription;
        this.deadline = deadline;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getJobUrl() {
        return jobUrl;
    }

    public void setJobUrl(String jobUrl) {
        this.jobUrl = jobUrl;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }

    public LocalDate getApplicationDate() {
        return applicationDate;
    }

    public void setApplicationDate(LocalDate applicationDate) {
        this.applicationDate = applicationDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getJobDescription() {
        return jobDescription;
    }

    public void setJobDescription(String jobDescription) {
        this.jobDescription = jobDescription;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }
}
