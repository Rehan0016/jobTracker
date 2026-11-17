package com.rehan.jobtracker.security;

import com.rehan.jobtracker.entity.ApplicationStatus;
import com.rehan.jobtracker.entity.JobApplication;
import com.rehan.jobtracker.entity.Role;
import com.rehan.jobtracker.entity.User;
import com.rehan.jobtracker.repository.JobApplicationRepository;
import com.rehan.jobtracker.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Strict User Ownership and Authorization Integration Tests")
class UserOwnershipIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobApplicationRepository jobApplicationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private User userA;
    private User userB;
    private String tokenA;
    private String tokenB;
    private JobApplication appA;
    private JobApplication appB;

    @BeforeEach
    void setUp() {
        jobApplicationRepository.deleteAll();
        userRepository.deleteAll();

        // Create User A
        userA = new User("Alice Developer", "alice@example.com", passwordEncoder.encode("passwordA"), Role.USER);
        userA = userRepository.save(userA);
        tokenA = jwtService.generateToken(userA);

        // Create User B
        userB = new User("Bob Engineer", "bob@example.com", passwordEncoder.encode("passwordB"), Role.USER);
        userB = userRepository.save(userB);
        tokenB = jwtService.generateToken(userB);

        // Application belonging to User A (Status: APPLIED)
        appA = new JobApplication(
                "Google",
                "Senior Software Engineer",
                "Mountain View, CA",
                "https://careers.google.com/123",
                ApplicationStatus.APPLIED,
                LocalDate.of(2026, 1, 10),
                "User A application notes"
        );
        appA.setUser(userA);
        appA = jobApplicationRepository.save(appA);

        // Application belonging to User B (Status: INTERVIEW)
        appB = new JobApplication(
                "Meta",
                "Frontend Engineer",
                "Menlo Park, CA",
                "https://metacareers.com/456",
                ApplicationStatus.INTERVIEW,
                LocalDate.of(2026, 1, 15),
                "User B application notes"
        );
        appB.setUser(userB);
        appB = jobApplicationRepository.save(appB);
    }

    @Test
    @DisplayName("User A cannot read User B's application - returns HTTP 404 Not Found")
    void userACannotReadUserBApplication_Returns404() throws Exception {
        mockMvc.perform(get("/api/applications/" + appB.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")))
                .andExpect(jsonPath("$.message", containsString("JobApplication not found")))
                .andExpect(jsonPath("$.message", containsString(String.valueOf(appB.getId()))));
    }

    @Test
    @DisplayName("User A cannot update User B's application - returns HTTP 404 Not Found")
    void userACannotUpdateUserBApplication_Returns404() throws Exception {
        String updatePayload = """
                {
                    "companyName": "Hacked Meta",
                    "jobTitle": "Hacked Title",
                    "status": "REJECTED"
                }
                """;

        mockMvc.perform(put("/api/applications/" + appB.getId())
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")));

        // Verify in database that User B's application was NOT modified
        JobApplication unmodifiedAppB = jobApplicationRepository.findById(appB.getId()).orElseThrow();
        assertThat(unmodifiedAppB.getCompanyName()).isEqualTo("Meta");
        assertThat(unmodifiedAppB.getJobTitle()).isEqualTo("Frontend Engineer");
        assertThat(unmodifiedAppB.getStatus()).isEqualTo(ApplicationStatus.INTERVIEW);
    }

    @Test
    @DisplayName("User A cannot delete User B's application - returns HTTP 404 Not Found")
    void userACannotDeleteUserBApplication_Returns404() throws Exception {
        mockMvc.perform(delete("/api/applications/" + appB.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")));

        // Verify in database that User B's application was NOT deleted
        assertThat(jobApplicationRepository.existsById(appB.getId())).isTrue();
    }

    @Test
    @DisplayName("User A cannot see User B's applications in list, search, or pagination results")
    void userACannotSeeUserBApplicationsInSearchOrListing() throws Exception {
        // User A list all: should contain only Google, not Meta
        mockMvc.perform(get("/api/applications")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].companyName", is("Google")))
                .andExpect(jsonPath("$[0].id", is(appA.getId().intValue())));

        // User A searches for "Meta": should return 0 results
        mockMvc.perform(get("/api/applications/search")
                        .param("query", "Meta")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // User A paginated query searching for "Meta": should return 0 totalElements
        mockMvc.perform(get("/api/applications/paged")
                        .param("search", "Meta")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(0)))
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    @DisplayName("User A and User B receive isolated dashboard statistics")
    void userStatsAreStrictlyIsolated() throws Exception {
        // User A has 1 APPLIED, 0 INTERVIEWS
        mockMvc.perform(get("/api/applications/stats")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalApplications", is(1)))
                .andExpect(jsonPath("$.applied", is(1)))
                .andExpect(jsonPath("$.interviews", is(0)))
                .andExpect(jsonPath("$.rejected", is(0)));

        // User B has 1 INTERVIEW, 0 APPLIED
        mockMvc.perform(get("/api/applications/stats")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalApplications", is(1)))
                .andExpect(jsonPath("$.applied", is(0)))
                .andExpect(jsonPath("$.interviews", is(1)))
                .andExpect(jsonPath("$.rejected", is(0)));
    }

    @Test
    @DisplayName("User B can successfully access, update, and delete their own application")
    void userBCanAccessOwnApplication() throws Exception {
        // User B reads appB
        mockMvc.perform(get("/api/applications/" + appB.getId())
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(appB.getId().intValue())))
                .andExpect(jsonPath("$.companyName", is("Meta")));

        // User B updates appB
        String updatePayload = """
                {
                    "companyName": "Meta Platforms, Inc.",
                    "jobTitle": "Senior Frontend Engineer",
                    "status": "OFFER"
                }
                """;

        mockMvc.perform(put("/api/applications/" + appB.getId())
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyName", is("Meta Platforms, Inc.")))
                .andExpect(jsonPath("$.status", is("OFFER")));

        // User B deletes appB
        mockMvc.perform(delete("/api/applications/" + appB.getId())
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNoContent());

        // Verify deleted in DB
        assertThat(jobApplicationRepository.existsById(appB.getId())).isFalse();
    }

    @Test
    @DisplayName("Create application automatically associates with authenticated user and preserves CRUD functionality")
    void createApplication_AutomaticallyAssociatesWithAuthenticatedUser() throws Exception {
        String newAppPayload = """
                {
                    "companyName": "Netflix",
                    "jobTitle": "Distributed Systems Engineer",
                    "location": "Los Gatos, CA",
                    "status": "APPLIED"
                }
                """;

        String responseBody = mockMvc.perform(post("/api/applications")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newAppPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.companyName", is("Netflix")))
                .andExpect(jsonPath("$.jobTitle", is("Distributed Systems Engineer")))
                .andExpect(jsonPath("$.status", is("APPLIED")))
                .andReturn().getResponse().getContentAsString();

        // Verify in database that ownership was automatically assigned to userA
        JobApplication createdApp = jobApplicationRepository.findByCompanyNameContainingIgnoreCase("Netflix")
                .stream().findFirst().orElseThrow();
        assertThat(createdApp.getUser()).isNotNull();
        assertThat(createdApp.getUser().getId()).isEqualTo(userA.getId());

        // Ensure userB cannot access Netflix application
        mockMvc.perform(get("/api/applications/" + createdApp.getId())
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Unauthenticated and invalid JWT requests are rejected with 401 Unauthorized")
    void missingOrInvalidJwt_Returns401() throws Exception {
        // Missing token
        mockMvc.perform(get("/api/applications"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));

        // Invalid token
        mockMvc.perform(get("/api/applications")
                        .header("Authorization", "Bearer completely.invalid.jwt.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));
    }
}
