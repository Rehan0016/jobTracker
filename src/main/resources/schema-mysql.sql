-- ===================================================================
-- JobTracker Database Schema Initialization / Migration
-- Idempotent DDL: safe for both fresh setups and existing databases
-- Preserves all existing records and volumes
-- ===================================================================

-- 1. Users Table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role ENUM('USER', 'ADMIN') NOT NULL,
    created_at DATETIME(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Job Applications Table
CREATE TABLE IF NOT EXISTS job_applications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_name VARCHAR(255) NOT NULL,
    job_title VARCHAR(255) NOT NULL,
    location VARCHAR(255) DEFAULT NULL,
    job_url VARCHAR(255) DEFAULT NULL,
    status ENUM('APPLIED', 'ASSESSMENT', 'INTERVIEW', 'OFFER', 'REJECTED', 'SELECTED', 'WITHDRAWN') NOT NULL,
    application_date DATE DEFAULT NULL,
    notes VARCHAR(2000) DEFAULT NULL,
    job_description VARCHAR(4000) DEFAULT NULL,
    deadline DATE DEFAULT NULL,
    user_id BIGINT DEFAULT NULL,
    CONSTRAINT fk_job_applications_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    INDEX idx_job_applications_status (status),
    INDEX idx_job_applications_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
