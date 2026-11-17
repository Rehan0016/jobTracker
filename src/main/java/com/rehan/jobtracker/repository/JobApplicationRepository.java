package com.rehan.jobtracker.repository;

import com.rehan.jobtracker.entity.ApplicationStatus;
import com.rehan.jobtracker.entity.JobApplication;
import com.rehan.jobtracker.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {

    // User-scoped single entity lookup (enforcing ownership at database level)
    Optional<JobApplication> findByIdAndUser(Long id, User user);

    // User-scoped list retrieval
    List<JobApplication> findByUser(User user);

    List<JobApplication> findByUserAndStatus(User user, ApplicationStatus status);

    List<JobApplication> findByUserAndCompanyNameContainingIgnoreCase(User user, String companyName);

    List<JobApplication> findByUserAndJobTitleContainingIgnoreCase(User user, String jobTitle);

    @Query("SELECT j FROM JobApplication j WHERE j.user = :user AND " +
           "(LOWER(j.companyName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(j.jobTitle) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<JobApplication> searchByUserAndQuery(@Param("user") User user, @Param("query") String query);

    // User-scoped pagination methods
    Page<JobApplication> findByUser(User user, Pageable pageable);

    Page<JobApplication> findByUserAndStatus(User user, ApplicationStatus status, Pageable pageable);

    Page<JobApplication> findByUserAndCompanyNameContainingIgnoreCase(User user, String companyName, Pageable pageable);

    Page<JobApplication> findByUserAndJobTitleContainingIgnoreCase(User user, String jobTitle, Pageable pageable);

    // User-scoped multi-field search across company and title, with optional status filter, pagination, and sorting
    @Query("SELECT j FROM JobApplication j WHERE j.user = :user AND " +
           "(:search IS NULL OR :search = '' OR " +
           " LOWER(j.companyName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(j.jobTitle) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:status IS NULL OR j.status = :status)")
    Page<JobApplication> searchAndFilterByUser(
            @Param("user") User user,
            @Param("search") String search,
            @Param("status") ApplicationStatus status,
            Pageable pageable);

    // User-scoped specific search by company and/or title, with optional status filter, pagination, and sorting
    @Query("SELECT j FROM JobApplication j WHERE j.user = :user AND " +
           "(:company IS NULL OR :company = '' OR LOWER(j.companyName) LIKE LOWER(CONCAT('%', :company, '%'))) AND " +
           "(:title IS NULL OR :title = '' OR LOWER(j.jobTitle) LIKE LOWER(CONCAT('%', :title, '%'))) AND " +
           "(:status IS NULL OR j.status = :status)")
    Page<JobApplication> searchByCriteriaAndUser(
            @Param("user") User user,
            @Param("company") String company,
            @Param("title") String title,
            @Param("status") ApplicationStatus status,
            Pageable pageable);

    // User-scoped dashboard statistics count queries
    long countByUser(User user);

    long countByUserAndStatus(User user, ApplicationStatus status);

    // Legacy un-scoped queries preserved for backwards compatibility
    List<JobApplication> findByStatus(ApplicationStatus status);

    List<JobApplication> findByCompanyNameContainingIgnoreCase(String companyName);

    List<JobApplication> findByJobTitleContainingIgnoreCase(String jobTitle);

    List<JobApplication> findByCompanyNameContainingIgnoreCaseOrJobTitleContainingIgnoreCase(String companyName, String jobTitle);

    Page<JobApplication> findByStatus(ApplicationStatus status, Pageable pageable);

    Page<JobApplication> findByCompanyNameContainingIgnoreCase(String companyName, Pageable pageable);

    Page<JobApplication> findByJobTitleContainingIgnoreCase(String jobTitle, Pageable pageable);

    @Query("SELECT j FROM JobApplication j WHERE " +
           "(:search IS NULL OR :search = '' OR " +
           " LOWER(j.companyName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(j.jobTitle) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:status IS NULL OR j.status = :status)")
    Page<JobApplication> searchAndFilter(
            @Param("search") String search,
            @Param("status") ApplicationStatus status,
            Pageable pageable);

    @Query("SELECT j FROM JobApplication j WHERE " +
           "(:company IS NULL OR :company = '' OR LOWER(j.companyName) LIKE LOWER(CONCAT('%', :company, '%'))) AND " +
           "(:title IS NULL OR :title = '' OR LOWER(j.jobTitle) LIKE LOWER(CONCAT('%', :title, '%'))) AND " +
           "(:status IS NULL OR j.status = :status)")
    Page<JobApplication> searchByCriteria(
            @Param("company") String company,
            @Param("title") String title,
            @Param("status") ApplicationStatus status,
            Pageable pageable);

    long countByStatus(ApplicationStatus status);
}
