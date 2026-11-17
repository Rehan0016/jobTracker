package com.rehan.jobtracker.controller;

import com.rehan.jobtracker.dto.DashboardStatsResponse;
import com.rehan.jobtracker.dto.JobApplicationRequest;
import com.rehan.jobtracker.dto.JobApplicationResponse;
import com.rehan.jobtracker.dto.PagedResponse;
import com.rehan.jobtracker.entity.ApplicationStatus;
import com.rehan.jobtracker.exception.GlobalExceptionHandler;
import com.rehan.jobtracker.exception.ResourceNotFoundException;
import com.rehan.jobtracker.service.JobApplicationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import com.rehan.jobtracker.security.CustomUserDetailsService;
import com.rehan.jobtracker.security.JwtService;
import org.springframework.security.test.context.support.WithMockUser;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(JobApplicationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@WithMockUser
class JobApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JobApplicationService jobApplicationService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private JobApplicationResponse sampleResponse;

    @BeforeEach
    void setUp() {
        sampleResponse = new JobApplicationResponse(
                1L,
                "Google",
                "Software Engineer",
                "Mountain View, CA",
                "https://careers.google.com/jobs/123",
                ApplicationStatus.APPLIED,
                LocalDate.of(2026, 2, 1),
                "Applied through referral"
        );
    }

    // -------------------------------------------------------------
    // POST /api/applications tests
    // -------------------------------------------------------------

    @Test
    @DisplayName("POST /api/applications with valid request returns 201 Created and response DTO")
    void createApplication_validRequest_returns201() throws Exception {
        when(jobApplicationService.createApplication(any(JobApplicationRequest.class)))
                .thenReturn(sampleResponse);

        String validJson = """
                {
                    "companyName": "Google",
                    "jobTitle": "Software Engineer",
                    "location": "Mountain View, CA",
                    "jobUrl": "https://careers.google.com/jobs/123",
                    "status": "APPLIED",
                    "applicationDate": "2026-02-01",
                    "notes": "Applied through referral"
                }
                """;

        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.companyName").value("Google"))
                .andExpect(jsonPath("$.jobTitle").value("Software Engineer"))
                .andExpect(jsonPath("$.status").value("APPLIED"))
                .andExpect(jsonPath("$.jobUrl").value("https://careers.google.com/jobs/123"))
                .andExpect(jsonPath("$.applicationDate").value("2026-02-01"));

        verify(jobApplicationService, times(1)).createApplication(any(JobApplicationRequest.class));
    }

    @Test
    @DisplayName("POST /api/applications with blank companyName returns 400 Bad Request with validation errors")
    void createApplication_blankCompanyName_returns400() throws Exception {
        String invalidJson = """
                {
                    "companyName": "",
                    "jobTitle": "Software Engineer",
                    "status": "APPLIED"
                }
                """;

        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.validationErrors.companyName").value("Company name is required"));

        verify(jobApplicationService, never()).createApplication(any());
    }

    @Test
    @DisplayName("POST /api/applications with blank jobTitle returns 400 Bad Request")
    void createApplication_blankJobTitle_returns400() throws Exception {
        String invalidJson = """
                {
                    "companyName": "Google",
                    "jobTitle": "   ",
                    "status": "APPLIED"
                }
                """;

        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.jobTitle").value("Job title is required"));

        verify(jobApplicationService, never()).createApplication(any());
    }

    @Test
    @DisplayName("POST /api/applications with invalid jobUrl returns 400 Bad Request")
    void createApplication_invalidJobUrl_returns400() throws Exception {
        String invalidJson = """
                {
                    "companyName": "Google",
                    "jobTitle": "Software Engineer",
                    "jobUrl": "invalid-url-format",
                    "status": "APPLIED"
                }
                """;

        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.jobUrl").value("Job URL must be a valid URL"));

        verify(jobApplicationService, never()).createApplication(any());
    }

    @Test
    @DisplayName("POST /api/applications with future applicationDate returns 400 Bad Request")
    void createApplication_futureDate_returns400() throws Exception {
        String futureDate = LocalDate.now().plusDays(5).toString();
        String invalidJson = """
                {
                    "companyName": "Google",
                    "jobTitle": "Software Engineer",
                    "status": "APPLIED",
                    "applicationDate": "%s"
                }
                """.formatted(futureDate);

        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.applicationDate").value("Application date must not be in the future"));

        verify(jobApplicationService, never()).createApplication(any());
    }

    @Test
    @DisplayName("POST /api/applications with null status returns 400 Bad Request")
    void createApplication_nullStatus_returns400() throws Exception {
        String invalidJson = """
                {
                    "companyName": "Google",
                    "jobTitle": "Software Engineer",
                    "status": null
                }
                """;

        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.status").value("Application status is required"));

        verify(jobApplicationService, never()).createApplication(any());
    }

    @Test
    @DisplayName("POST /api/applications with invalid enum status string returns 400 Bad Request")
    void createApplication_invalidStatusEnum_returns400() throws Exception {
        String invalidJson = """
                {
                    "companyName": "Google",
                    "jobTitle": "Software Engineer",
                    "status": "NON_EXISTENT_STATUS"
                }
                """;

        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));

        verify(jobApplicationService, never()).createApplication(any());
    }

    // -------------------------------------------------------------
    // GET /api/applications tests
    // -------------------------------------------------------------

    @Test
    @DisplayName("GET /api/applications returns 200 OK and list of applications")
    void getAllApplications_returns200() throws Exception {
        when(jobApplicationService.getAllApplications()).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/applications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].companyName").value("Google"))
                .andExpect(jsonPath("$[0].jobTitle").value("Software Engineer"));

        verify(jobApplicationService, times(1)).getAllApplications();
    }

    @Test
    @DisplayName("GET /api/applications?status=APPLIED filters applications by status")
    void getApplicationsByStatus_returns200() throws Exception {
        when(jobApplicationService.getApplicationsByStatus(ApplicationStatus.APPLIED))
                .thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/applications").param("status", "APPLIED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status").value("APPLIED"));

        verify(jobApplicationService, times(1)).getApplicationsByStatus(ApplicationStatus.APPLIED);
    }

    // -------------------------------------------------------------
    // GET /api/applications/{id} tests
    // -------------------------------------------------------------

    @Test
    @DisplayName("GET /api/applications/{id} with existing ID returns 200 OK")
    void getApplicationById_existingId_returns200() throws Exception {
        when(jobApplicationService.getApplicationById(1L)).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/applications/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.companyName").value("Google"));

        verify(jobApplicationService, times(1)).getApplicationById(1L);
    }

    @Test
    @DisplayName("GET /api/applications/{id} with non-existent ID returns 404 Not Found")
    void getApplicationById_nonExistentId_returns404() throws Exception {
        when(jobApplicationService.getApplicationById(999L))
                .thenThrow(new ResourceNotFoundException("JobApplication", "id", 999L));

        mockMvc.perform(get("/api/applications/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value(containsString("JobApplication not found with id : '999'")));

        verify(jobApplicationService, times(1)).getApplicationById(999L);
    }

    // -------------------------------------------------------------
    // PUT /api/applications/{id} tests
    // -------------------------------------------------------------

    @Test
    @DisplayName("PUT /api/applications/{id} with valid request returns 200 OK")
    void updateApplication_validRequest_returns200() throws Exception {
        when(jobApplicationService.updateApplication(eq(1L), any(JobApplicationRequest.class)))
                .thenReturn(sampleResponse);

        String validJson = """
                {
                    "companyName": "Google",
                    "jobTitle": "Software Engineer",
                    "location": "Mountain View, CA",
                    "jobUrl": "https://careers.google.com/jobs/123",
                    "status": "APPLIED",
                    "applicationDate": "2026-02-01",
                    "notes": "Applied through referral"
                }
                """;

        mockMvc.perform(put("/api/applications/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.companyName").value("Google"));

        verify(jobApplicationService, times(1)).updateApplication(eq(1L), any(JobApplicationRequest.class));
    }

    @Test
    @DisplayName("PUT /api/applications/{id} with invalid request returns 400 Bad Request")
    void updateApplication_invalidRequest_returns400() throws Exception {
        String invalidJson = """
                {
                    "companyName": "",
                    "jobTitle": "Software Engineer",
                    "status": "APPLIED"
                }
                """;

        mockMvc.perform(put("/api/applications/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.companyName").value("Company name is required"));

        verify(jobApplicationService, never()).updateApplication(any(), any());
    }

    @Test
    @DisplayName("PUT /api/applications/{id} when non-existent returns 404 Not Found")
    void updateApplication_nonExistentId_returns404() throws Exception {
        when(jobApplicationService.updateApplication(eq(999L), any(JobApplicationRequest.class)))
                .thenThrow(new ResourceNotFoundException("JobApplication", "id", 999L));

        String validJson = """
                {
                    "companyName": "Google",
                    "jobTitle": "Software Engineer",
                    "status": "APPLIED"
                }
                """;

        mockMvc.perform(put("/api/applications/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        verify(jobApplicationService, times(1)).updateApplication(eq(999L), any(JobApplicationRequest.class));
    }

    // -------------------------------------------------------------
    // DELETE /api/applications/{id} tests
    // -------------------------------------------------------------

    @Test
    @DisplayName("DELETE /api/applications/{id} returns 204 No Content")
    void deleteApplication_existingId_returns204() throws Exception {
        doNothing().when(jobApplicationService).deleteApplication(1L);

        mockMvc.perform(delete("/api/applications/1"))
                .andExpect(status().isNoContent());

        verify(jobApplicationService, times(1)).deleteApplication(1L);
    }

    @Test
    @DisplayName("DELETE /api/applications/{id} when non-existent returns 404 Not Found")
    void deleteApplication_nonExistentId_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("JobApplication", "id", 999L))
                .when(jobApplicationService).deleteApplication(999L);

        mockMvc.perform(delete("/api/applications/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        verify(jobApplicationService, times(1)).deleteApplication(999L);
    }

    // -------------------------------------------------------------
    // Dashboard Statistics tests
    // -------------------------------------------------------------

    @Test
    @DisplayName("GET /api/applications/stats returns 200 OK and dashboard statistics")
    void getDashboardStats_returns200() throws Exception {
        DashboardStatsResponse stats = new DashboardStatsResponse(20, 8, 5, 4, 1, 2);
        when(jobApplicationService.getDashboardStats()).thenReturn(stats);

        mockMvc.perform(get("/api/applications/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalApplications").value(20))
                .andExpect(jsonPath("$.applied").value(8))
                .andExpect(jsonPath("$.interviews").value(5))
                .andExpect(jsonPath("$.rejected").value(4))
                .andExpect(jsonPath("$.selected").value(1))
                .andExpect(jsonPath("$.offers").value(2));

        verify(jobApplicationService, times(1)).getDashboardStats();
    }

    @Test
    @DisplayName("GET /api/applications/dashboard-stats returns 200 OK")
    void getDashboardStatsAlias_returns200() throws Exception {
        DashboardStatsResponse stats = new DashboardStatsResponse(10, 5, 2, 1, 1, 1);
        when(jobApplicationService.getDashboardStats()).thenReturn(stats);

        mockMvc.perform(get("/api/applications/dashboard-stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalApplications").value(10))
                .andExpect(jsonPath("$.applied").value(5));

        verify(jobApplicationService, times(1)).getDashboardStats();
    }

    // -------------------------------------------------------------
    // Pagination and Sorting tests
    // -------------------------------------------------------------

    @Test
    @DisplayName("GET /api/applications with page parameter returns 200 OK and PagedResponse of results")
    void getApplicationsPaged_withPageParam_returns200() throws Exception {
        PagedResponse<JobApplicationResponse> pagedResponse = PagedResponse.from(
                new PageImpl<>(List.of(sampleResponse), PageRequest.of(0, 10), 1));
        when(jobApplicationService.getApplicationsPaged(isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(pagedResponse);

        mockMvc.perform(get("/api/applications")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "companyName,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].companyName").value("Google"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.currentPage").value(0))
                .andExpect(jsonPath("$.pageSize").value(10))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(10));

        verify(jobApplicationService, times(1)).getApplicationsPaged(isNull(), isNull(), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /api/applications/paged returns 200 OK and PagedResponse with search, filter, pagination, and sorting")
    void getApplicationsPaged_dedicatedEndpoint_withSearchFilterSort() throws Exception {
        PagedResponse<JobApplicationResponse> pagedResponse = PagedResponse.from(
                new PageImpl<>(List.of(sampleResponse), PageRequest.of(0, 5), 1));
        when(jobApplicationService.getApplicationsPaged(eq("Google"), isNull(), isNull(), eq(ApplicationStatus.APPLIED), any(Pageable.class)))
                .thenReturn(pagedResponse);

        mockMvc.perform(get("/api/applications/paged")
                        .param("page", "0")
                        .param("size", "5")
                        .param("search", "Google")
                        .param("status", "APPLIED")
                        .param("sort", "applicationDate,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].companyName").value("Google"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.currentPage").value(0))
                .andExpect(jsonPath("$.pageSize").value(5))
                .andExpect(jsonPath("$.first").value(true));

        verify(jobApplicationService, times(1))
                .getApplicationsPaged(eq("Google"), isNull(), isNull(), eq(ApplicationStatus.APPLIED), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /api/applications/paged sorting by jobTitle works properly")
    void getApplicationsPaged_sortingByJobTitle() throws Exception {
        PagedResponse<JobApplicationResponse> pagedResponse = PagedResponse.from(
                new PageImpl<>(List.of(sampleResponse), PageRequest.of(0, 10), 1));
        when(jobApplicationService.getApplicationsPaged(isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(pagedResponse);

        mockMvc.perform(get("/api/applications/paged")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "jobTitle,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.currentPage").value(0))
                .andExpect(jsonPath("$.pageSize").value(10));

        verify(jobApplicationService, times(1))
                .getApplicationsPaged(isNull(), isNull(), isNull(), isNull(), any(Pageable.class));
    }

    // -------------------------------------------------------------
    // Search tests
    // -------------------------------------------------------------

    @Test
    @DisplayName("GET /api/applications/search by company returns 200 OK and matching list")
    void searchApplications_byCompany_returns200() throws Exception {
        when(jobApplicationService.searchByCompanyName("Google")).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/applications/search").param("company", "Google"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].companyName").value("Google"));

        verify(jobApplicationService, times(1)).searchByCompanyName("Google");
    }

    @Test
    @DisplayName("GET /api/applications/search by title returns 200 OK and matching list")
    void searchApplications_byTitle_returns200() throws Exception {
        when(jobApplicationService.searchByJobTitle("Engineer")).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/applications/search").param("title", "Engineer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].jobTitle").value("Software Engineer"));

        verify(jobApplicationService, times(1)).searchByJobTitle("Engineer");
    }

    @Test
    @DisplayName("GET /api/applications/search by query keyword returns 200 OK")
    void searchApplications_byQuery_returns200() throws Exception {
        when(jobApplicationService.searchApplications("Google")).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/applications/search").param("query", "Google"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].companyName").value("Google"));

        verify(jobApplicationService, times(1)).searchApplications("Google");
    }

    // -------------------------------------------------------------
    // Exception Handling tests for invalid enum in query param
    // -------------------------------------------------------------

    @Test
    @DisplayName("GET /api/applications with invalid status enum param returns 400 Bad Request with consistent ErrorResponse")
    void getApplications_invalidEnumQueryParam_returns400() throws Exception {
        mockMvc.perform(get("/api/applications").param("status", "INVALID_ENUM_VALUE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message", containsString("could not be converted to required type")))
                .andExpect(jsonPath("$.path").value("/api/applications"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }
}
