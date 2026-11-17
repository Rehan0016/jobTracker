package com.rehan.jobtracker.dto;

import com.rehan.jobtracker.entity.ApplicationStatus;
import com.rehan.jobtracker.entity.JobApplication;
import java.time.LocalDate;

public class JobApplicationResponse {

    private Long id;
    private String companyName;
    private String jobTitle;
    private String location;
    private String jobUrl;
    private ApplicationStatus status;
    private LocalDate applicationDate;
    private String notes;
    private String jobDescription;
    private LocalDate deadline;

    public JobApplicationResponse() {
    }

    public JobApplicationResponse(Long id, String companyName, String jobTitle, String location,
                                  String jobUrl, ApplicationStatus status, LocalDate applicationDate, String notes) {
        this.id = id;
        this.companyName = companyName;
        this.jobTitle = jobTitle;
        this.location = location;
        this.jobUrl = jobUrl;
        this.status = status;
        this.applicationDate = applicationDate;
        this.notes = notes;
    }

    public JobApplicationResponse(Long id, String companyName, String jobTitle, String location,
                                  String jobUrl, ApplicationStatus status, LocalDate applicationDate, String notes,
                                  String jobDescription, LocalDate deadline) {
        this.id = id;
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

    // Factory method to map from Entity to DTO
    public static JobApplicationResponse fromEntity(JobApplication entity) {
        if (entity == null) {
            return null;
        }
        return new JobApplicationResponse(
                entity.getId(),
                entity.getCompanyName(),
                entity.getJobTitle(),
                entity.getLocation(),
                entity.getJobUrl(),
                entity.getStatus(),
                entity.getApplicationDate(),
                entity.getNotes(),
                entity.getJobDescription(),
                entity.getDeadline()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
