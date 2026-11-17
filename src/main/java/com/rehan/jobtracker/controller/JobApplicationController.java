package com.rehan.jobtracker.controller;

import com.rehan.jobtracker.dto.DashboardStatsResponse;
import com.rehan.jobtracker.dto.JobApplicationRequest;
import com.rehan.jobtracker.dto.JobApplicationResponse;
import com.rehan.jobtracker.dto.PagedResponse;
import com.rehan.jobtracker.entity.ApplicationStatus;
import com.rehan.jobtracker.service.JobApplicationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
public class JobApplicationController {

    private final JobApplicationService jobApplicationService;

    // Constructor-based dependency injection
    public JobApplicationController(JobApplicationService jobApplicationService) {
        this.jobApplicationService = jobApplicationService;
    }

    /**
     * POST /api/applications - Create a new job application
     */
    @PostMapping
    public ResponseEntity<JobApplicationResponse> createApplication(
            @Valid @RequestBody JobApplicationRequest request) {

        JobApplicationResponse created = jobApplicationService.createApplication(request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    /**
     * GET /api/applications - Retrieve all job applications (optionally filtered by status or searched)
     */
    @GetMapping
    public ResponseEntity<List<JobApplicationResponse>> getAllApplications(
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(required = false) String company,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String search) {

        List<JobApplicationResponse> applications;
        if (company != null && !company.isBlank()) {
            applications = jobApplicationService.searchByCompanyName(company);
        } else if (title != null && !title.isBlank()) {
            applications = jobApplicationService.searchByJobTitle(title);
        } else if (search != null && !search.isBlank()) {
            applications = jobApplicationService.searchApplications(search);
        } else if (status != null) {
            applications = jobApplicationService.getApplicationsByStatus(status);
        } else {
            applications = jobApplicationService.getAllApplications();
        }
        return ResponseEntity.ok(applications);
    }

    /**
     * GET /api/applications?page=0&size=10 - Retrieve paginated and sorted applications
     */
    @GetMapping(params = "page")
    public ResponseEntity<PagedResponse<JobApplicationResponse>> getApplicationsWithPageParam(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String company,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) ApplicationStatus status,
            Pageable pageable) {

        PagedResponse<JobApplicationResponse> pagedApplications = jobApplicationService.getApplicationsPaged(
                search, company, title, status, pageable);
        return ResponseEntity.ok(pagedApplications);
    }

    /**
     * GET /api/applications/paged - Dedicated endpoint for paginated and sorted applications
     */
    @GetMapping("/paged")
    public ResponseEntity<PagedResponse<JobApplicationResponse>> getApplicationsPaged(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String company,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) ApplicationStatus status,
            Pageable pageable) {

        PagedResponse<JobApplicationResponse> pagedApplications = jobApplicationService.getApplicationsPaged(
                search, company, title, status, pageable);
        return ResponseEntity.ok(pagedApplications);
    }

    /**
     * GET /api/applications/search - Search applications by company name or job title
     */
    @GetMapping("/search")
    public ResponseEntity<List<JobApplicationResponse>> searchApplications(
            @RequestParam(required = false) String company,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String query) {

        List<JobApplicationResponse> results;
        if (company != null && !company.isBlank()) {
            results = jobApplicationService.searchByCompanyName(company);
        } else if (title != null && !title.isBlank()) {
            results = jobApplicationService.searchByJobTitle(title);
        } else if (query != null && !query.isBlank()) {
            results = jobApplicationService.searchApplications(query);
        } else {
            results = jobApplicationService.getAllApplications();
        }
        return ResponseEntity.ok(results);
    }

    /**
     * GET /api/applications/stats - Retrieve dashboard statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsResponse> getDashboardStats() {
        DashboardStatsResponse stats = jobApplicationService.getDashboardStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * GET /api/applications/dashboard-stats - Alias for dashboard statistics
     */
    @GetMapping("/dashboard-stats")
    public ResponseEntity<DashboardStatsResponse> getDashboardStatsAlias() {
        DashboardStatsResponse stats = jobApplicationService.getDashboardStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * GET /api/applications/upcoming-interviews - Retrieve interviews for dashboard
     */
    @GetMapping("/upcoming-interviews")
    public ResponseEntity<List<JobApplicationResponse>> getUpcomingInterviews() {
        List<JobApplicationResponse> interviews = jobApplicationService.getUpcomingInterviews();
        return ResponseEntity.ok(interviews);
    }

    /**
     * GET /api/applications/{id} - Retrieve a specific application by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<JobApplicationResponse> getApplicationById(@PathVariable Long id) {
        JobApplicationResponse application = jobApplicationService.getApplicationById(id);
        return ResponseEntity.ok(application);
    }

    /**
     * PUT /api/applications/{id} - Update an existing application
     */
    @PutMapping("/{id}")
    public ResponseEntity<JobApplicationResponse> updateApplication(
            @PathVariable Long id,
            @Valid @RequestBody JobApplicationRequest request) {

        JobApplicationResponse updated = jobApplicationService.updateApplication(id, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/applications/{id} - Delete an application by ID
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteApplication(@PathVariable Long id) {
        jobApplicationService.deleteApplication(id);
        return ResponseEntity.noContent().build();
    }
}
