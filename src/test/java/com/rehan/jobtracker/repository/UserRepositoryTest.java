package com.rehan.jobtracker.repository;

import com.rehan.jobtracker.entity.ApplicationStatus;
import com.rehan.jobtracker.entity.JobApplication;
import com.rehan.jobtracker.entity.Role;
import com.rehan.jobtracker.entity.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobApplicationRepository jobApplicationRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("Save user and findByEmail returns persisted user")
    void saveAndFindByEmail_success() {
        User user = new User("Jane Doe", "jane@example.com", "hashedPassword123", Role.USER);
        userRepository.save(user);

        Optional<User> found = userRepository.findByEmail("jane@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isNotNull();
        assertThat(found.get().getName()).isEqualTo("Jane Doe");
        assertThat(found.get().getRole()).isEqualTo(Role.USER);
        assertThat(found.get().getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("findByEmail returns empty when email does not exist")
    void findByEmail_notFound_returnsEmpty() {
        Optional<User> found = userRepository.findByEmail("nonexistent@example.com");

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("existsByEmail returns true when user exists and false otherwise")
    void existsByEmail_checksPresenceCorrectly() {
        User user = new User("Admin User", "admin@example.com", "adminPass123", Role.ADMIN);
        userRepository.save(user);

        assertThat(userRepository.existsByEmail("admin@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("other@example.com")).isFalse();
    }

    @Test
    @DisplayName("Email unique constraint prevents duplicate emails")
    void emailUniqueConstraint_throwsException() {
        User user1 = new User("User One", "duplicate@example.com", "pass1", Role.USER);
        userRepository.saveAndFlush(user1);

        User user2 = new User("User Two", "duplicate@example.com", "pass2", Role.USER);

        assertThatThrownBy(() -> userRepository.saveAndFlush(user2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("JobApplication properly associates with User via @ManyToOne foreign key")
    void jobApplication_userRelationship_persisted() {
        User user = new User("Job Seeker", "seeker@example.com", "secureHash", Role.USER);
        User savedUser = userRepository.save(user);

        JobApplication application = new JobApplication(
                "Netflix",
                "Senior Software Engineer",
                "Los Gatos, CA",
                "https://jobs.netflix.com/123",
                ApplicationStatus.APPLIED,
                LocalDate.now(),
                "Referral from alumni"
        );
        application.setUser(savedUser);

        JobApplication savedApplication = jobApplicationRepository.save(application);
        entityManager.flush();
        entityManager.clear();

        JobApplication retrieved = jobApplicationRepository.findById(savedApplication.getId()).orElseThrow();

        assertThat(retrieved.getUser()).isNotNull();
        assertThat(retrieved.getUser().getId()).isEqualTo(savedUser.getId());
        assertThat(retrieved.getUser().getEmail()).isEqualTo("seeker@example.com");
    }
}
