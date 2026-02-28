-- =============================================
-- V2__seed_admin_user.sql
-- Poodle LMS - Seed default admin user
-- Password: Admin@123 (BCrypt encoded)
-- =============================================

INSERT INTO employees (first_name, last_name, email, password, role, deleted)
VALUES (
    'System',
    'Admin',
    'admin@poodle.com',
    '$2a$12$bfkBupSCvp8oevJ27DYBD.yii5a9NFG/sb/aPZvK5d7nReSCF3ZNu',
    'ROLE_ADMIN',
    FALSE
);
