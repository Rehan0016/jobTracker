package com.rehan.jobtracker.service;

import com.rehan.jobtracker.dto.DashboardStatsResponse;
import com.rehan.jobtracker.dto.JobApplicationRequest;
import com.rehan.jobtracker.dto.JobApplicationResponse;
import com.rehan.jobtracker.dto.PagedResponse;
import com.rehan.jobtracker.entity.ApplicationStatus;
import com.rehan.jobtracker.entity.JobApplication;
import com.rehan.jobtracker.entity.Role;
import com.rehan.jobtracker.entity.User;
import com.rehan.jobtracker.exception.ResourceNotFoundException;
import com.rehan.jobtracker.repository.JobApplicationRepository;
import com.rehan.jobtracker.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobApplicationServiceTest {

    @Mock
    private JobApplicationRepository jobApplicationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private JobApplicationService jobApplicationService;

    private User testUser;
    private JobApplication sampleApplication;
    private JobApplicationRequest sampleRequest;

    @BeforeEach
    void setUp() {
        testUser = new User("Alice Developer", "alice@example.com", "encodedPassword", Role.USER);
        testUser.setId(1L);

        // Configure Spring Security context with authenticated user
        Authentication auth = new UsernamePasswordAuthenticationToken(testUser, null, testUser.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);

        lenient().when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(testUser));

        sampleApplication = new JobApplication(
                1L,
                "Amazon",
                "Backend Engineer",
                "Seattle, WA",
                "https://amazon.jobs/123",
                ApplicationStatus.APPLIED,
                LocalDate.of(2026, 1, 15),
                "Applied via career portal"
        );
        sampleApplication.setUser(testUser);

        sampleRequest = new JobApplicationRequest(
                "Amazon",
                "Backend Engineer",
                "Seattle, WA",
                "https://amazon.jobs/123",
                ApplicationStatus.APPLIED,
                LocalDate.of(2026, 1, 15),
                "Applied via career portal"
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("createApplication automatically associates entity with authenticated user and returns response")
    void createApplication_success() {
        when(jobApplicationRepository.save(any(JobApplication.class))).thenReturn(sampleApplication);

        JobApplicationResponse response = jobApplicationService.createApplication(sampleRequest);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getCompanyName()).isEqualTo("Amazon");
        assertThat(response.getJobTitle()).isEqualTo("Backend Engineer");
        assertThat(response.getStatus()).isEqualTo(ApplicationStatus.APPLIED);
        assertThat(response.getApplicationDate()).isEqualTo(LocalDate.of(2026, 1, 15));

        ArgumentCaptor<JobApplication> captor = ArgumentCaptor.forClass(JobApplication.class);
        verify(jobApplicationRepository, times(1)).save(captor.capture());
        JobApplication saved = captor.getValue();
        assertThat(saved.getCompanyName()).isEqualTo("Amazon");
        assertThat(saved.getJobTitle()).isEqualTo("Backend Engineer");
        assertThat(saved.getUser()).isEqualTo(testUser);
    }

    @Test
    @DisplayName("createApplication defaults applicationDate to today when null in request")
    void createApplication_defaultsDateToTodayWhenNull() {
        sampleRequest.setApplicationDate(null);
        when(jobApplicationRepository.save(any(JobApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));

        JobApplicationResponse response = jobApplicationService.createApplication(sampleRequest);

        assertThat(response.getApplicationDate()).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("getAllApplications returns user's mapped list of JobApplicationResponse")
    void getAllApplications_returnsList() {
        when(jobApplicationRepository.findByUser(testUser)).thenReturn(List.of(sampleApplication));

        List<JobApplicationResponse> results = jobApplicationService.getAllApplications();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getCompanyName()).isEqualTo("Amazon");
        verify(jobApplicationRepository, times(1)).findByUser(testUser);
    }

    @Test
    @DisplayName("getApplicationById returns response DTO when application belongs to authenticated user")
    void getApplicationById_found() {
        when(jobApplicationRepository.findByIdAndUser(1L, testUser)).thenReturn(Optional.of(sampleApplication));

        JobApplicationResponse response = jobApplicationService.getApplicationById(1L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getCompanyName()).isEqualTo("Amazon");
    }

    @Test
    @DisplayName("getApplicationById throws ResourceNotFoundException (404) when not found or owned by another user")
    void getApplicationById_notFound() {
        when(jobApplicationRepository.findByIdAndUser(999L, testUser)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobApplicationService.getApplicationById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("JobApplication")
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("updateApplication updates existing entity when owned by user and returns updated response DTO")
    void updateApplication_success() {
        when(jobApplicationRepository.findByIdAndUser(1L, testUser)).thenReturn(Optional.of(sampleApplication));
        when(jobApplicationRepository.save(any(JobApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));

        JobApplicationRequest updateRequest = new JobApplicationRequest(
                "Amazon AWS",
                "Senior Backend Engineer",
                "Remote",
                "https://amazon.jobs/456",
                ApplicationStatus.INTERVIEW,
                LocalDate.of(2026, 1, 20),
                "First round scheduled"
        );

        JobApplicationResponse response = jobApplicationService.updateApplication(1L, updateRequest);

        assertThat(response.getCompanyName()).isEqualTo("Amazon AWS");
        assertThat(response.getJobTitle()).isEqualTo("Senior Backend Engineer");
        assertThat(response.getLocation()).isEqualTo("Remote");
        assertThat(response.getStatus()).isEqualTo(ApplicationStatus.INTERVIEW);
        assertThat(response.getApplicationDate()).isEqualTo(LocalDate.of(2026, 1, 20));
        assertThat(response.getNotes()).isEqualTo("First round scheduled");
    }

    @Test
    @DisplayName("updateApplication throws ResourceNotFoundException (404) when ID does not exist or belongs to another user")
    void updateApplication_notFound() {
        when(jobApplicationRepository.findByIdAndUser(999L, testUser)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobApplicationService.updateApplication(999L, sampleRequest))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(jobApplicationRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteApplication deletes entity when owned by user")
    void deleteApplication_success() {
        when(jobApplicationRepository.findByIdAndUser(1L, testUser)).thenReturn(Optional.of(sampleApplication));

        jobApplicationService.deleteApplication(1L);

        verify(jobApplicationRepository, times(1)).delete(sampleApplication);
    }

    @Test
    @DisplayName("deleteApplication throws ResourceNotFoundException (404) when ID does not exist or belongs to another user")
    void deleteApplication_notFound() {
        when(jobApplicationRepository.findByIdAndUser(999L, testUser)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobApplicationService.deleteApplication(999L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(jobApplicationRepository, never()).delete(any(JobApplication.class));
    }

    @Test
    @DisplayName("getApplicationsByStatus returns matching applications for current user")
    void getApplicationsByStatus_returnsFilteredList() {
        when(jobApplicationRepository.findByUserAndStatus(testUser, ApplicationStatus.APPLIED))
                .thenReturn(List.of(sampleApplication));

        List<JobApplicationResponse> results = jobApplicationService.getApplicationsByStatus(ApplicationStatus.APPLIED);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStatus()).isEqualTo(ApplicationStatus.APPLIED);
    }

    @Test
    @DisplayName("getDashboardStats returns aggregated status counts for current user only")
    void getDashboardStats_returnsCorrectCounts() {
        when(jobApplicationRepository.countByUser(testUser)).thenReturn(15L);
        when(jobApplicationRepository.countByUserAndStatus(testUser, ApplicationStatus.APPLIED)).thenReturn(5L);
        when(jobApplicationRepository.countByUserAndStatus(testUser, ApplicationStatus.INTERVIEW)).thenReturn(4L);
        when(jobApplicationRepository.countByUserAndStatus(testUser, ApplicationStatus.REJECTED)).thenReturn(3L);
        when(jobApplicationRepository.countByUserAndStatus(testUser, ApplicationStatus.SELECTED)).thenReturn(1L);
        when(jobApplicationRepository.countByUserAndStatus(testUser, ApplicationStatus.OFFER)).thenReturn(2L);
        when(jobApplicationRepository.countByUserAndStatus(testUser, ApplicationStatus.ASSESSMENT)).thenReturn(0L);
        when(jobApplicationRepository.countByUserAndStatus(testUser, ApplicationStatus.WITHDRAWN)).thenReturn(0L);

        DashboardStatsResponse stats = jobApplicationService.getDashboardStats();

        assertThat(stats.getTotalApplications()).isEqualTo(15L);
        assertThat(stats.getApplied()).isEqualTo(5L);
        assertThat(stats.getInterviews()).isEqualTo(4L);
        assertThat(stats.getRejected()).isEqualTo(3L);
        assertThat(stats.getSelected()).isEqualTo(1L);
        assertThat(stats.getOffers()).isEqualTo(2L);
        assertThat(stats.getSuccessRate()).isEqualTo(20.0);
    }

    @Test
    @DisplayName("getUpcomingInterviews returns interviews scoped to current user")
    void getUpcomingInterviews_returnsCurrentUsersInterviews() {
        JobApplication interviewApp = new JobApplication("TechCorp", "Lead Dev", "Remote", null, ApplicationStatus.INTERVIEW, LocalDate.now(), "Round 2");
        interviewApp.setId(50L);
        when(jobApplicationRepository.findByUserAndStatus(testUser, ApplicationStatus.INTERVIEW))
                .thenReturn(List.of(interviewApp));

        List<JobApplicationResponse> interviews = jobApplicationService.getUpcomingInterviews();

        assertThat(interviews).hasSize(1);
        assertThat(interviews.get(0).getCompanyName()).isEqualTo("TechCorp");
        assertThat(interviews.get(0).getStatus()).isEqualTo(ApplicationStatus.INTERVIEW);
    }

    @Test
    @DisplayName("getApplicationsPaged with search and status delegates to searchAndFilterByUser")
    void getApplicationsPaged_withSearchAndStatus() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<JobApplication> page = new PageImpl<>(List.of(sampleApplication), pageable, 1);

        when(jobApplicationRepository.searchAndFilterByUser(testUser, "Amazon", ApplicationStatus.APPLIED, pageable))
                .thenReturn(page);

        PagedResponse<JobApplicationResponse> result = jobApplicationService.getApplicationsPaged("Amazon", null, null, ApplicationStatus.APPLIED, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getTotalPages()).isEqualTo(1);
        assertThat(result.getCurrentPage()).isEqualTo(0);
        assertThat(result.getPageSize()).isEqualTo(10);
        verify(jobApplicationRepository).searchAndFilterByUser(testUser, "Amazon", ApplicationStatus.APPLIED, pageable);
    }

    @Test
    @DisplayName("getApplicationsPaged with company or title delegates to searchByCriteriaAndUser")
    void getApplicationsPaged_withCompanyOrTitle() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<JobApplication> page = new PageImpl<>(List.of(sampleApplication), pageable, 1);

        when(jobApplicationRepository.searchByCriteriaAndUser(testUser, "Amazon", "Engineer", ApplicationStatus.APPLIED, pageable))
                .thenReturn(page);

        PagedResponse<JobApplicationResponse> result = jobApplicationService.getApplicationsPaged(null, "Amazon", "Engineer", ApplicationStatus.APPLIED, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getTotalPages()).isEqualTo(1);
        assertThat(result.getCurrentPage()).isEqualTo(0);
        assertThat(result.getPageSize()).isEqualTo(10);
        verify(jobApplicationRepository).searchByCriteriaAndUser(testUser, "Amazon", "Engineer", ApplicationStatus.APPLIED, pageable);
    }

    @Test
    @DisplayName("getApplicationsPaged with null filters delegates to findByUser")
    void getApplicationsPaged_withNullFilters() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<JobApplication> page = new PageImpl<>(List.of(sampleApplication), pageable, 1);

        when(jobApplicationRepository.findByUser(testUser, pageable)).thenReturn(page);

        PagedResponse<JobApplicationResponse> result = jobApplicationService.getApplicationsPaged(null, null, null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getTotalPages()).isEqualTo(1);
        assertThat(result.getCurrentPage()).isEqualTo(0);
        assertThat(result.getPageSize()).isEqualTo(10);
        verify(jobApplicationRepository).findByUser(testUser, pageable);
    }

    @Test
    @DisplayName("searchByCompanyName returns matching applications for current user")
    void searchByCompanyName_returnsMatches() {
        when(jobApplicationRepository.findByUserAndCompanyNameContainingIgnoreCase(testUser, "Amazon"))
                .thenReturn(List.of(sampleApplication));

        List<JobApplicationResponse> results = jobApplicationService.searchByCompanyName("Amazon");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getCompanyName()).isEqualTo("Amazon");
    }

    @Test
    @DisplayName("searchByJobTitle returns matching applications for current user")
    void searchByJobTitle_returnsMatches() {
        when(jobApplicationRepository.findByUserAndJobTitleContainingIgnoreCase(testUser, "Backend"))
                .thenReturn(List.of(sampleApplication));

        List<JobApplicationResponse> results = jobApplicationService.searchByJobTitle("Backend");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getJobTitle()).isEqualTo("Backend Engineer");
    }

    @Test
    @DisplayName("searchApplications returns matching applications across company and title for current user")
    void searchApplications_returnsMatches() {
        when(jobApplicationRepository.searchByUserAndQuery(testUser, "keyword"))
                .thenReturn(List.of(sampleApplication));

        List<JobApplicationResponse> results = jobApplicationService.searchApplications("keyword");

        assertThat(results).hasSize(1);
    }

    @Test
    @DisplayName("getCurrentAuthenticatedUser throws BadCredentialsException when unauthenticated")
    void getCurrentAuthenticatedUser_unauthenticated_throwsException() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> jobApplicationService.getAllApplications())
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("User is not authenticated");
    }
}
