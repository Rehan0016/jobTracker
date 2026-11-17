package com.rehan.jobtracker.service;

import com.rehan.jobtracker.dto.DashboardStatsResponse;
import com.rehan.jobtracker.dto.JobApplicationRequest;
import com.rehan.jobtracker.dto.JobApplicationResponse;
import com.rehan.jobtracker.dto.PagedResponse;
import com.rehan.jobtracker.entity.ApplicationStatus;
import com.rehan.jobtracker.entity.JobApplication;
import com.rehan.jobtracker.entity.User;
import com.rehan.jobtracker.exception.ResourceNotFoundException;
import com.rehan.jobtracker.repository.JobApplicationRepository;
import com.rehan.jobtracker.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class JobApplicationService {

    private final JobApplicationRepository jobApplicationRepository;
    private final UserRepository userRepository;

    // Constructor-based dependency injection
    public JobApplicationService(JobApplicationRepository jobApplicationRepository,
                                 UserRepository userRepository) {
        this.jobApplicationRepository = jobApplicationRepository;
        this.userRepository = userRepository;
    }

    /**
     * Helper to obtain the currently authenticated user from SecurityContextHolder.
     */
    public User getCurrentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new BadCredentialsException("User is not authenticated");
        }

        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    /**
     * Create a new job application, automatically associating it with the authenticated user.
     */
    public JobApplicationResponse createApplication(JobApplicationRequest request) {
        User currentUser = getCurrentAuthenticatedUser();

        JobApplication application = new JobApplication();
        application.setCompanyName(request.getCompanyName());
        application.setJobTitle(request.getJobTitle());
        application.setLocation(request.getLocation());
        application.setJobUrl(request.getJobUrl());
        application.setStatus(request.getStatus());
        application.setApplicationDate(request.getApplicationDate() != null ? request.getApplicationDate() : LocalDate.now());
        application.setNotes(request.getNotes());
        application.setJobDescription(request.getJobDescription());
        application.setDeadline(request.getDeadline());
        application.setUser(currentUser);

        JobApplication savedApplication = jobApplicationRepository.save(application);
        return JobApplicationResponse.fromEntity(savedApplication);
    }

    /**
     * Retrieve all job applications belonging to the authenticated user.
     */
    @Transactional(readOnly = true)
    public List<JobApplicationResponse> getAllApplications() {
        User currentUser = getCurrentAuthenticatedUser();
        return jobApplicationRepository.findByUser(currentUser)
                .stream()
                .map(JobApplicationResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Retrieve a single job application by its ID, scoped strictly to the authenticated user.
     */
    @Transactional(readOnly = true)
    public JobApplicationResponse getApplicationById(Long id) {
        User currentUser = getCurrentAuthenticatedUser();
        JobApplication application = findEntityByIdAndUser(id, currentUser);
        return JobApplicationResponse.fromEntity(application);
    }

    /**
     * Update an existing job application by its ID, only if owned by the authenticated user.
     */
    public JobApplicationResponse updateApplication(Long id, JobApplicationRequest request) {
        User currentUser = getCurrentAuthenticatedUser();
        JobApplication existingApplication = findEntityByIdAndUser(id, currentUser);

        existingApplication.setCompanyName(request.getCompanyName());
        existingApplication.setJobTitle(request.getJobTitle());
        existingApplication.setLocation(request.getLocation());
        existingApplication.setJobUrl(request.getJobUrl());
        existingApplication.setStatus(request.getStatus());
        if (request.getApplicationDate() != null) {
            existingApplication.setApplicationDate(request.getApplicationDate());
        }
        existingApplication.setNotes(request.getNotes());
        existingApplication.setJobDescription(request.getJobDescription());
        existingApplication.setDeadline(request.getDeadline());

        JobApplication updated = jobApplicationRepository.save(existingApplication);
        return JobApplicationResponse.fromEntity(updated);
    }

    /**
     * Delete a job application by its ID, only if owned by the authenticated user.
     */
    public void deleteApplication(Long id) {
        User currentUser = getCurrentAuthenticatedUser();
        JobApplication existingApplication = findEntityByIdAndUser(id, currentUser);
        jobApplicationRepository.delete(existingApplication);
    }

    /**
     * Retrieve authenticated user's applications filtered by status.
     */
    @Transactional(readOnly = true)
    public List<JobApplicationResponse> getApplicationsByStatus(ApplicationStatus status) {
        User currentUser = getCurrentAuthenticatedUser();
        return jobApplicationRepository.findByUserAndStatus(currentUser, status)
                .stream()
                .map(JobApplicationResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Retrieve authenticated user's applications with pagination, sorting, search, and status filtering.
     */
    @Transactional(readOnly = true)
    public PagedResponse<JobApplicationResponse> getApplicationsPaged(
            String search,
            String company,
            String title,
            ApplicationStatus status,
            Pageable pageable) {

        User currentUser = getCurrentAuthenticatedUser();
        Page<JobApplication> page;
        if ((company != null && !company.isBlank()) || (title != null && !title.isBlank())) {
            page = jobApplicationRepository.searchByCriteriaAndUser(currentUser, company, title, status, pageable);
        } else if (search != null && !search.isBlank()) {
            page = jobApplicationRepository.searchAndFilterByUser(currentUser, search, status, pageable);
        } else if (status != null) {
            page = jobApplicationRepository.findByUserAndStatus(currentUser, status, pageable);
        } else {
            page = jobApplicationRepository.findByUser(currentUser, pageable);
        }

        return PagedResponse.from(page.map(JobApplicationResponse::fromEntity));
    }

    /**
     * Search authenticated user's applications by company name (case-insensitive).
     */
    @Transactional(readOnly = true)
    public List<JobApplicationResponse> searchByCompanyName(String companyName) {
        User currentUser = getCurrentAuthenticatedUser();
        return jobApplicationRepository.findByUserAndCompanyNameContainingIgnoreCase(currentUser, companyName)
                .stream()
                .map(JobApplicationResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Search authenticated user's applications by job title (case-insensitive).
     */
    @Transactional(readOnly = true)
    public List<JobApplicationResponse> searchByJobTitle(String jobTitle) {
        User currentUser = getCurrentAuthenticatedUser();
        return jobApplicationRepository.findByUserAndJobTitleContainingIgnoreCase(currentUser, jobTitle)
                .stream()
                .map(JobApplicationResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Search authenticated user's applications across company name and job title (case-insensitive).
     */
    @Transactional(readOnly = true)
    public List<JobApplicationResponse> searchApplications(String query) {
        User currentUser = getCurrentAuthenticatedUser();
        return jobApplicationRepository.searchByUserAndQuery(currentUser, query)
                .stream()
                .map(JobApplicationResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Compute dashboard statistics strictly scoped to the authenticated user's applications.
     */
    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats() {
        User currentUser = getCurrentAuthenticatedUser();
        long total = jobApplicationRepository.countByUser(currentUser);
        long applied = jobApplicationRepository.countByUserAndStatus(currentUser, ApplicationStatus.APPLIED);
        long interviews = jobApplicationRepository.countByUserAndStatus(currentUser, ApplicationStatus.INTERVIEW);
        long rejected = jobApplicationRepository.countByUserAndStatus(currentUser, ApplicationStatus.REJECTED);
        long selected = jobApplicationRepository.countByUserAndStatus(currentUser, ApplicationStatus.SELECTED);
        long offers = jobApplicationRepository.countByUserAndStatus(currentUser, ApplicationStatus.OFFER);
        long assessment = jobApplicationRepository.countByUserAndStatus(currentUser, ApplicationStatus.ASSESSMENT);
        long withdrawn = jobApplicationRepository.countByUserAndStatus(currentUser, ApplicationStatus.WITHDRAWN);
        double successRate = total > 0 ? Math.round(((double) (offers + selected) / total * 100.0) * 10.0) / 10.0 : 0.0;

        return new DashboardStatsResponse(total, applied, interviews, rejected, selected, offers, assessment, withdrawn, successRate);
    }

    /**
     * Retrieve upcoming interviews and active evaluation applications for the authenticated user.
     */
    @Transactional(readOnly = true)
    public List<JobApplicationResponse> getUpcomingInterviews() {
        User currentUser = getCurrentAuthenticatedUser();
        return jobApplicationRepository.findByUserAndStatus(currentUser, ApplicationStatus.INTERVIEW)
                .stream()
                .map(JobApplicationResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Internal helper method to find the entity scoped to user or throw ResourceNotFoundException (404).
     */
    private JobApplication findEntityByIdAndUser(Long id, User user) {
        return jobApplicationRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("JobApplication", "id", id));
    }
}
