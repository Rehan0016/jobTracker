package com.rehan.jobtracker.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import org.hibernate.validator.constraints.URL;
import java.time.LocalDate;

@Entity
@Table(name = "job_applications")
public class JobApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_name", nullable = false)
    @NotBlank(message = "Company name is required")
    private String companyName;

    @Column(name = "job_title", nullable = false)
    @NotBlank(message = "Job title is required")
    private String jobTitle;

    @Column(name = "location")
    private String location;

    @Column(name = "job_url")
    @URL(message = "Job URL must be a valid URL")
    private String jobUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @NotNull(message = "Application status is required")
    private ApplicationStatus status;

    @Column(name = "application_date")
    @PastOrPresent(message = "Application date must not be in the future")
    private LocalDate applicationDate;

    @Column(name = "notes", length = 2000)
    private String notes;

    @Column(name = "job_description", length = 4000)
    private String jobDescription;

    @Column(name = "deadline")
    private LocalDate deadline;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private User user;

    // Default no-argument constructor required by JPA/Hibernate
    public JobApplication() {
    }

    // Parameterized constructor for convenience and testing
    public JobApplication(String companyName, String jobTitle, String location, String jobUrl,
                          ApplicationStatus status, LocalDate applicationDate, String notes) {
        this.companyName = companyName;
        this.jobTitle = jobTitle;
        this.location = location;
        this.jobUrl = jobUrl;
        this.status = status;
        this.applicationDate = applicationDate;
        this.notes = notes;
    }

    // Full constructor including ID
    public JobApplication(Long id, String companyName, String jobTitle, String location, String jobUrl,
                          ApplicationStatus status, LocalDate applicationDate, String notes) {
        this.id = id;
        this.companyName = companyName;
        this.jobTitle = jobTitle;
        this.location = location;
        this.jobUrl = jobUrl;
        this.status = status;
        this.applicationDate = applicationDate;
        this.notes = notes;
    }

    // Getters and Setters

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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @Override
    public String toString() {
        return "JobApplication{" +
                "id=" + id +
                ", companyName='" + companyName + '\'' +
                ", jobTitle='" + jobTitle + '\'' +
                ", location='" + location + '\'' +
                ", jobUrl='" + jobUrl + '\'' +
                ", status=" + status +
                ", applicationDate=" + applicationDate +
                ", notes='" + notes + '\'' +
                '}';
    }
}
