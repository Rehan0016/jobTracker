package com.rehan.jobtracker.dto;

import com.rehan.jobtracker.entity.ApplicationStatus;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class JobApplicationValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private JobApplicationRequest createValidRequest() {
        return new JobApplicationRequest(
                "Google",
                "Software Engineer",
                "Mountain View, CA",
                "https://careers.google.com/jobs/results/123",
                ApplicationStatus.APPLIED,
                LocalDate.now(),
                "Referral application"
        );
    }

    @Test
    @DisplayName("Valid request should have no constraint violations")
    void validRequest_hasNoViolations() {
        JobApplicationRequest request = createValidRequest();

        Set<ConstraintViolation<JobApplicationRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Blank companyName should fail validation")
    void blankCompanyName_hasViolation() {
        JobApplicationRequest request = createValidRequest();
        request.setCompanyName("   ");

        Set<ConstraintViolation<JobApplicationRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Company name is required");
    }

    @Test
    @DisplayName("Null companyName should fail validation")
    void nullCompanyName_hasViolation() {
        JobApplicationRequest request = createValidRequest();
        request.setCompanyName(null);

        Set<ConstraintViolation<JobApplicationRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Company name is required");
    }

    @Test
    @DisplayName("Blank jobTitle should fail validation")
    void blankJobTitle_hasViolation() {
        JobApplicationRequest request = createValidRequest();
        request.setJobTitle("");

        Set<ConstraintViolation<JobApplicationRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Job title is required");
    }

    @Test
    @DisplayName("Null status should fail validation")
    void nullStatus_hasViolation() {
        JobApplicationRequest request = createValidRequest();
        request.setStatus(null);

        Set<ConstraintViolation<JobApplicationRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Application status is required");
    }

    @Test
    @DisplayName("Invalid URL format for jobUrl should fail validation")
    void invalidJobUrl_hasViolation() {
        JobApplicationRequest request = createValidRequest();
        request.setJobUrl("invalid-url-format");

        Set<ConstraintViolation<JobApplicationRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("jobUrl");
    }

    @Test
    @DisplayName("Null or empty jobUrl is optional and should pass validation")
    void nullOrEmptyJobUrl_hasNoViolations() {
        JobApplicationRequest requestWithNull = createValidRequest();
        requestWithNull.setJobUrl(null);
        assertThat(validator.validate(requestWithNull)).isEmpty();

        JobApplicationRequest requestWithEmpty = createValidRequest();
        requestWithEmpty.setJobUrl("");
        assertThat(validator.validate(requestWithEmpty)).isEmpty();
    }

    @Test
    @DisplayName("Future applicationDate should fail validation")
    void futureApplicationDate_hasViolation() {
        JobApplicationRequest request = createValidRequest();
        request.setApplicationDate(LocalDate.now().plusDays(1));

        Set<ConstraintViolation<JobApplicationRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("applicationDate");
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Application date must not be in the future");
    }

    @Test
    @DisplayName("Past or today applicationDate should pass validation")
    void pastOrTodayApplicationDate_hasNoViolations() {
        JobApplicationRequest requestToday = createValidRequest();
        requestToday.setApplicationDate(LocalDate.now());
        assertThat(validator.validate(requestToday)).isEmpty();

        JobApplicationRequest requestPast = createValidRequest();
        requestPast.setApplicationDate(LocalDate.now().minusWeeks(2));
        assertThat(validator.validate(requestPast)).isEmpty();

        JobApplicationRequest requestNullDate = createValidRequest();
        requestNullDate.setApplicationDate(null);
        assertThat(validator.validate(requestNullDate)).isEmpty();
    }

    @Test
    @DisplayName("Optional location and notes fields can be null without violations")
    void optionalFieldsCanBeNull() {
        JobApplicationRequest request = createValidRequest();
        request.setLocation(null);
        request.setNotes(null);

        Set<ConstraintViolation<JobApplicationRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Assessment and Withdrawn statuses should be valid")
    void newStatuses_areValid() {
        JobApplicationRequest req1 = createValidRequest();
        req1.setStatus(ApplicationStatus.ASSESSMENT);
        assertThat(validator.validate(req1)).isEmpty();

        JobApplicationRequest req2 = createValidRequest();
        req2.setStatus(ApplicationStatus.WITHDRAWN);
        assertThat(validator.validate(req2)).isEmpty();
    }

    @Test
    @DisplayName("Job description and deadline fields pass validation when valid")
    void jobDescriptionAndDeadline_valid() {
        JobApplicationRequest request = createValidRequest();
        request.setJobDescription("Full-stack Java and React role working on microservices.");
        request.setDeadline(LocalDate.now().plusMonths(1));

        Set<ConstraintViolation<JobApplicationRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Job description exceeding 4000 characters fails validation")
    void jobDescription_exceedsMaxLength_fails() {
        JobApplicationRequest request = createValidRequest();
        request.setJobDescription("A".repeat(4001));

        Set<ConstraintViolation<JobApplicationRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Job description cannot exceed 4000 characters");
    }
}
