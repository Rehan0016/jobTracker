package com.rehan.jobtracker.repository;

import com.rehan.jobtracker.entity.ApplicationStatus;
import com.rehan.jobtracker.entity.JobApplication;
import com.rehan.jobtracker.entity.Role;
import com.rehan.jobtracker.entity.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("JobApplicationRepository DataJpa Slice Tests")
class JobApplicationRepositoryTest {

    @Autowired
    private JobApplicationRepository jobApplicationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private User userA;
    private User userB;

    private JobApplication appA1;
    private JobApplication appA2;
    private JobApplication appA3;
    private JobApplication appB1;

    @BeforeEach
    void setUp() {
        jobApplicationRepository.deleteAll();
        userRepository.deleteAll();

        // Seed User A
        userA = new User("Alice Developer", "alice.dev@example.com", "hashA123", Role.USER);
        userA = userRepository.save(userA);

        // Seed User B
        userB = new User("Bob Engineer", "bob.eng@example.com", "hashB123", Role.USER);
        userB = userRepository.save(userB);

        // User A Application 1: Google - Senior Backend Engineer (INTERVIEW)
        appA1 = new JobApplication(
                "Google",
                "Senior Backend Engineer",
                "Mountain View, CA",
                "https://careers.google.com/1",
                ApplicationStatus.INTERVIEW,
                LocalDate.of(2026, 2, 1),
                "Round 2 technical interview scheduled"
        );
        appA1.setUser(userA);
        appA1 = jobApplicationRepository.save(appA1);

        // User A Application 2: Microsoft - Cloud Software Engineer (APPLIED)
        appA2 = new JobApplication(
                "Microsoft",
                "Cloud Software Engineer",
                "Redmond, WA",
                "https://careers.microsoft.com/2",
                ApplicationStatus.APPLIED,
                LocalDate.of(2026, 2, 5),
                "Referred by senior engineer"
        );
        appA2.setUser(userA);
        appA2 = jobApplicationRepository.save(appA2);

        // User A Application 3: Amazon - SDE II (OFFER)
        appA3 = new JobApplication(
                "Amazon",
                "SDE II",
                "Seattle, WA",
                "https://amazon.jobs/3",
                ApplicationStatus.OFFER,
                LocalDate.of(2026, 1, 15),
                "Offer received pending negotiation"
        );
        appA3.setUser(userA);
        appA3 = jobApplicationRepository.save(appA3);

        // User B Application 1: Meta - Systems Engineer (INTERVIEW)
        appB1 = new JobApplication(
                "Meta",
                "Systems Engineer",
                "Menlo Park, CA",
                "https://metacareers.com/4",
                ApplicationStatus.INTERVIEW,
                LocalDate.of(2026, 2, 10),
                "Screening passed"
        );
        appB1.setUser(userB);
        appB1 = jobApplicationRepository.save(appB1);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("findByIdAndUser returns application when owned by the given user")
    void findByIdAndUser_successWhenOwner() {
        Optional<JobApplication> result = jobApplicationRepository.findByIdAndUser(appA1.getId(), userA);

        assertThat(result).isPresent();
        assertThat(result.get().getCompanyName()).isEqualTo("Google");
        assertThat(result.get().getUser().getId()).isEqualTo(userA.getId());
    }

    @Test
    @DisplayName("findByIdAndUser returns empty when requested by a different user (data isolation)")
    void findByIdAndUser_emptyWhenDifferentUser() {
        // User B attempts to access User A's application
        Optional<JobApplication> result = jobApplicationRepository.findByIdAndUser(appA1.getId(), userB);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByUser returns only applications belonging to that user")
    void findByUser_returnsOnlyUserApplications() {
        List<JobApplication> appsA = jobApplicationRepository.findByUser(userA);
        List<JobApplication> appsB = jobApplicationRepository.findByUser(userB);

        assertThat(appsA).hasSize(3)
                .extracting(JobApplication::getCompanyName)
                .containsExactlyInAnyOrder("Google", "Microsoft", "Amazon");

        assertThat(appsB).hasSize(1)
                .extracting(JobApplication::getCompanyName)
                .containsExactly("Meta");
    }

    @Test
    @DisplayName("findByUserAndStatus filters user applications by status")
    void findByUserAndStatus_filtersCorrectly() {
        List<JobApplication> interviewsA = jobApplicationRepository.findByUserAndStatus(userA, ApplicationStatus.INTERVIEW);
        List<JobApplication> appliedA = jobApplicationRepository.findByUserAndStatus(userA, ApplicationStatus.APPLIED);
        List<JobApplication> rejectedA = jobApplicationRepository.findByUserAndStatus(userA, ApplicationStatus.REJECTED);

        assertThat(interviewsA).hasSize(1);
        assertThat(interviewsA.get(0).getCompanyName()).isEqualTo("Google");

        assertThat(appliedA).hasSize(1);
        assertThat(appliedA.get(0).getCompanyName()).isEqualTo("Microsoft");

        assertThat(rejectedA).isEmpty();
    }

    @Test
    @DisplayName("findByUserAndCompanyNameContainingIgnoreCase performs case-insensitive match")
    void findByCompanyName_caseInsensitive() {
        List<JobApplication> result = jobApplicationRepository
                .findByUserAndCompanyNameContainingIgnoreCase(userA, "goog");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCompanyName()).isEqualTo("Google");

        List<JobApplication> resultUpper = jobApplicationRepository
                .findByUserAndCompanyNameContainingIgnoreCase(userA, "MICRO");

        assertThat(resultUpper).hasSize(1);
        assertThat(resultUpper.get(0).getCompanyName()).isEqualTo("Microsoft");
    }

    @Test
    @DisplayName("findByUserAndJobTitleContainingIgnoreCase performs case-insensitive match")
    void findByJobTitle_caseInsensitive() {
        List<JobApplication> result = jobApplicationRepository
                .findByUserAndJobTitleContainingIgnoreCase(userA, "backend");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getJobTitle()).isEqualTo("Senior Backend Engineer");
    }

    @Test
    @DisplayName("searchByUserAndQuery matches across company name or job title")
    void searchByUserAndQuery_matchesCompanyOrTitle() {
        // Query matching company
        List<JobApplication> byCompany = jobApplicationRepository.searchByUserAndQuery(userA, "amaz");
        assertThat(byCompany).hasSize(1);
        assertThat(byCompany.get(0).getCompanyName()).isEqualTo("Amazon");

        // Query matching title
        List<JobApplication> byTitle = jobApplicationRepository.searchByUserAndQuery(userA, "Cloud");
        assertThat(byTitle).hasSize(1);
        assertThat(byTitle.get(0).getCompanyName()).isEqualTo("Microsoft");

        // Query does not match User B's Meta application even if keyword matches
        List<JobApplication> userASearchMeta = jobApplicationRepository.searchByUserAndQuery(userA, "Meta");
        assertThat(userASearchMeta).isEmpty();
    }

    @Test
    @DisplayName("findByUser with Pageable supports pagination and sorting")
    void findByUser_withPageable_supportsPaginationAndSorting() {
        Pageable pageable = PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "applicationDate"));
        Page<JobApplication> page = jobApplicationRepository.findByUser(userA, pageable);

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.getContent()).hasSize(2);
        // Descending by date: Microsoft (Feb 5) then Google (Feb 1)
        assertThat(page.getContent().get(0).getCompanyName()).isEqualTo("Microsoft");
        assertThat(page.getContent().get(1).getCompanyName()).isEqualTo("Google");
    }

    @Test
    @DisplayName("searchAndFilterByUser combines query, status filter, and pagination")
    void searchAndFilterByUser_withSearchAndStatus() {
        Pageable pageable = PageRequest.of(0, 10);

        // Search for "Engineer" with status INTERVIEW: only Google should match for User A
        Page<JobApplication> page = jobApplicationRepository.searchAndFilterByUser(
                userA, "Engineer", ApplicationStatus.INTERVIEW, pageable);

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getCompanyName()).isEqualTo("Google");

        // Search for "Engineer" with null status: Google and Microsoft should match for User A
        Page<JobApplication> pageAllStatus = jobApplicationRepository.searchAndFilterByUser(
                userA, "Engineer", null, pageable);

        assertThat(pageAllStatus.getTotalElements()).isEqualTo(2);
        assertThat(pageAllStatus.getContent())
                .extracting(JobApplication::getCompanyName)
                .containsExactlyInAnyOrder("Google", "Microsoft");
    }

    @Test
    @DisplayName("searchAndFilterByUser with empty search and null status returns all user applications")
    void searchAndFilterByUser_nullFilters_returnsAll() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<JobApplication> page = jobApplicationRepository.searchAndFilterByUser(
                userA, null, null, pageable);

        assertThat(page.getTotalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("searchByCriteriaAndUser filters by specific company, title, and status")
    void searchByCriteriaAndUser_filtersAccurately() {
        Pageable pageable = PageRequest.of(0, 10);

        Page<JobApplication> page = jobApplicationRepository.searchByCriteriaAndUser(
                userA, "Google", "Backend", ApplicationStatus.INTERVIEW, pageable);

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getCompanyName()).isEqualTo("Google");

        // Mismatched status should yield 0 results
        Page<JobApplication> mismatch = jobApplicationRepository.searchByCriteriaAndUser(
                userA, "Google", "Backend", ApplicationStatus.REJECTED, pageable);

        assertThat(mismatch.getTotalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("countByUser and countByUserAndStatus compute isolated metrics accurately")
    void countByUser_and_countByStatus() {
        long totalA = jobApplicationRepository.countByUser(userA);
        long interviewsA = jobApplicationRepository.countByUserAndStatus(userA, ApplicationStatus.INTERVIEW);
        long appliedA = jobApplicationRepository.countByUserAndStatus(userA, ApplicationStatus.APPLIED);
        long offersA = jobApplicationRepository.countByUserAndStatus(userA, ApplicationStatus.OFFER);
        long rejectedA = jobApplicationRepository.countByUserAndStatus(userA, ApplicationStatus.REJECTED);

        assertThat(totalA).isEqualTo(3);
        assertThat(interviewsA).isEqualTo(1);
        assertThat(appliedA).isEqualTo(1);
        assertThat(offersA).isEqualTo(1);
        assertThat(rejectedA).isEqualTo(0);

        long totalB = jobApplicationRepository.countByUser(userB);
        long interviewsB = jobApplicationRepository.countByUserAndStatus(userB, ApplicationStatus.INTERVIEW);

        assertThat(totalB).isEqualTo(1);
        assertThat(interviewsB).isEqualTo(1);
    }

    @Test
    @DisplayName("Deleting application removes record while preserving associated user")
    void delete_removesApplication_preservesUser() {
        Long appId = appA1.getId();
        jobApplicationRepository.deleteById(appId);

        entityManager.flush();
        entityManager.clear();

        assertThat(jobApplicationRepository.findById(appId)).isEmpty();
        assertThat(userRepository.findById(userA.getId())).isPresent();
    }
}
