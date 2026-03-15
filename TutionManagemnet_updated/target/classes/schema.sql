-- ================================================
-- TUITION MANAGEMENT SYSTEM - MySQL Schema
-- ================================================

CREATE DATABASE IF NOT EXISTS tuition_db;
USE tuition_db;

-- Users table (base for all roles)
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    phone VARCHAR(15),
    role ENUM('ADMIN', 'TUTOR', 'STUDENT') NOT NULL DEFAULT 'STUDENT',
    profile_image VARCHAR(255),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Subjects
CREATE TABLE IF NOT EXISTS subjects (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(20) UNIQUE NOT NULL,
    description TEXT,
    grade_level VARCHAR(20),
    thumbnail VARCHAR(255),
    tutor_id BIGINT,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (tutor_id) REFERENCES users(id) ON DELETE SET NULL
);

-- Student-Subject Enrollment
CREATE TABLE IF NOT EXISTS enrollments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    subject_id BIGINT NOT NULL,
    enrolled_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT true,
    FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE,
    UNIQUE KEY unique_enrollment (student_id, subject_id)
);

-- Online Classes / Sessions
CREATE TABLE IF NOT EXISTS classes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    subject_id BIGINT NOT NULL,
    tutor_id BIGINT NOT NULL,
    description TEXT,
    meeting_link VARCHAR(500),
    scheduled_at DATETIME NOT NULL,
    duration_minutes INT DEFAULT 60,
    status ENUM('SCHEDULED', 'LIVE', 'COMPLETED', 'CANCELLED') DEFAULT 'SCHEDULED',
    recording_url VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE,
    FOREIGN KEY (tutor_id) REFERENCES users(id) ON DELETE CASCADE
);

-- MCQ Questions Bank
CREATE TABLE IF NOT EXISTS questions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    subject_id BIGINT NOT NULL,
    tutor_id BIGINT NOT NULL,
    question_text TEXT NOT NULL,
    option_a VARCHAR(500) NOT NULL,
    option_b VARCHAR(500) NOT NULL,
    option_c VARCHAR(500) NOT NULL,
    option_d VARCHAR(500) NOT NULL,
    correct_answer ENUM('A','B','C','D') NOT NULL,
    explanation TEXT,
    difficulty ENUM('EASY','MEDIUM','HARD') DEFAULT 'MEDIUM',
    marks INT DEFAULT 1,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE,
    FOREIGN KEY (tutor_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Practice Tests / Weekly Tests
CREATE TABLE IF NOT EXISTS tests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    subject_id BIGINT NOT NULL,
    tutor_id BIGINT NOT NULL,
    description TEXT,
    test_type ENUM('PRACTICE','WEEKLY','CHAPTER') DEFAULT 'PRACTICE',
    duration_minutes INT DEFAULT 30,
    total_marks INT DEFAULT 0,
    passing_marks INT DEFAULT 0,
    start_time DATETIME,
    end_time DATETIME,
    is_published BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE,
    FOREIGN KEY (tutor_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Test-Question mapping
CREATE TABLE IF NOT EXISTS test_questions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    test_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    question_order INT,
    FOREIGN KEY (test_id) REFERENCES tests(id) ON DELETE CASCADE,
    FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE,
    UNIQUE KEY unique_test_question (test_id, question_id)
);

-- Student Test Attempts
CREATE TABLE IF NOT EXISTS test_attempts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    test_id BIGINT NOT NULL,
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    submitted_at TIMESTAMP NULL,
    score INT DEFAULT 0,
    total_marks INT DEFAULT 0,
    percentage DECIMAL(5,2) DEFAULT 0,
    status ENUM('IN_PROGRESS','SUBMITTED','TIMED_OUT') DEFAULT 'IN_PROGRESS',
    FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (test_id) REFERENCES tests(id) ON DELETE CASCADE
);

-- Student Answers
CREATE TABLE IF NOT EXISTS student_answers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    attempt_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    selected_answer ENUM('A','B','C','D'),
    is_correct BOOLEAN DEFAULT false,
    marks_obtained INT DEFAULT 0,
    FOREIGN KEY (attempt_id) REFERENCES test_attempts(id) ON DELETE CASCADE,
    FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE
);

-- Notes / PDF Materials
CREATE TABLE IF NOT EXISTS notes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    subject_id BIGINT NOT NULL,
    tutor_id BIGINT NOT NULL,
    description TEXT,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT,
    chapter VARCHAR(100),
    is_active BOOLEAN DEFAULT true,
    download_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE,
    FOREIGN KEY (tutor_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Announcements
CREATE TABLE IF NOT EXISTS announcements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    author_id BIGINT NOT NULL,
    subject_id BIGINT NULL,
    priority ENUM('LOW','NORMAL','HIGH','URGENT') DEFAULT 'NORMAL',
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE SET NULL
);

-- ================================================
-- SAMPLE DATA
-- ================================================

-- Admin user (password: admin123)
INSERT INTO users (username, email, password, full_name, phone, role) VALUES
('admin', 'admin@tuition.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh7y', 'System Admin', '9999999999', 'ADMIN');

-- Tutors (password: tutor123)
INSERT INTO users (username, email, password, full_name, phone, role) VALUES
('tutor_math', 'math@tuition.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh7y', 'Dr. Priya Sharma', '9876543210', 'TUTOR'),
('tutor_sci', 'science@tuition.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh7y', 'Prof. Rahul Verma', '9876543211', 'TUTOR'),
('tutor_eng', 'english@tuition.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh7y', 'Mrs. Anita Singh', '9876543212', 'TUTOR');

-- Students (password: student123)
INSERT INTO users (username, email, password, full_name, phone, role) VALUES
('student1', 'student1@gmail.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh7y', 'Arjun Kumar', '9123456789', 'STUDENT'),
('student2', 'student2@gmail.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh7y', 'Priya Patel', '9123456790', 'STUDENT'),
('student3', 'student3@gmail.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh7y', 'Rohit Gupta', '9123456791', 'STUDENT');

-- Subjects
INSERT INTO subjects (name, code, description, grade_level, tutor_id) VALUES
('Mathematics', 'MATH10', 'Class 10 Mathematics covering Algebra, Geometry, and Statistics', 'Class 10', 2),
('Science', 'SCI10', 'Class 10 Science - Physics, Chemistry & Biology', 'Class 10', 3),
('English', 'ENG10', 'English Literature and Grammar for Class 10', 'Class 10', 4),
('Mathematics', 'MATH12', 'Class 12 Mathematics - Calculus, Vectors, 3D Geometry', 'Class 12', 2),
('Physics', 'PHY12', 'Class 12 Physics - Mechanics, Electrostatics, Optics', 'Class 12', 3);

-- Enrollments
INSERT INTO enrollments (student_id, subject_id) VALUES
(5, 1), (5, 2), (5, 3),
(6, 1), (6, 4),
(7, 2), (7, 3), (7, 5);

-- Sample Classes
INSERT INTO classes (title, subject_id, tutor_id, description, meeting_link, scheduled_at, duration_minutes, status) VALUES
('Quadratic Equations - Chapter 4', 1, 2, 'Learn to solve quadratic equations using factorization', 'https://meet.google.com/abc-defg-hij', '2025-03-15 10:00:00', 60, 'SCHEDULED'),
('Acids, Bases and Salts', 2, 3, 'Chemistry chapter on acids bases and salts with experiments', 'https://zoom.us/j/123456789', '2025-03-14 14:00:00', 90, 'COMPLETED'),
('The Making of the Global World', 2, 3, 'History chapter discussion', 'https://meet.google.com/xyz-abcd-efg', '2025-03-16 11:00:00', 60, 'SCHEDULED');

-- Sample Questions
INSERT INTO questions (subject_id, tutor_id, question_text, option_a, option_b, option_c, option_d, correct_answer, explanation, difficulty, marks) VALUES
(1, 2, 'What is the value of x in the equation 2x + 5 = 15?', '3', '4', '5', '6', 'C', 'Solving: 2x = 15 - 5 = 10, so x = 5', 'EASY', 1),
(1, 2, 'The product of the roots of ax² + bx + c = 0 is:', 'b/a', '-b/a', 'c/a', '-c/a', 'C', 'By Vieta\'s formulas, product of roots = c/a', 'MEDIUM', 1),
(1, 2, 'If HCF(306, 657) = 9, what is LCM(306, 657)?', '22338', '22339', '22340', '22341', 'A', 'LCM × HCF = Product, so LCM = (306×657)/9 = 22338', 'MEDIUM', 2),
(2, 3, 'Which of the following is NOT a characteristic of acids?', 'Sour taste', 'Red litmus to blue', 'pH less than 7', 'React with metals', 'B', 'Acids turn blue litmus to red, not the other way. That is a property of bases.', 'EASY', 1),
(2, 3, 'The atomic number of Carbon is:', '5', '6', '7', '8', 'B', 'Carbon has atomic number 6 with 6 protons in its nucleus', 'EASY', 1),
(2, 3, 'Which gas is produced when zinc reacts with dilute H2SO4?', 'CO2', 'SO2', 'H2', 'O2', 'C', 'Zn + H2SO4 → ZnSO4 + H2 (Hydrogen gas is produced)', 'MEDIUM', 1),
(3, 4, 'Which literary device is used in "The stars danced playfully"?', 'Simile', 'Metaphor', 'Personification', 'Alliteration', 'C', 'Attributing human quality (dancing) to stars is Personification', 'EASY', 1),
(3, 4, 'The word "benevolent" means:', 'Cruel', 'Well-meaning', 'Indifferent', 'Hostile', 'B', 'Benevolent means well-meaning and kindly', 'EASY', 1);

-- Sample Tests
INSERT INTO tests (title, subject_id, tutor_id, description, test_type, duration_minutes, total_marks, passing_marks, is_published) VALUES
('Math Weekly Test - Week 1', 1, 2, 'Chapter 1-3 revision test covering Real Numbers and Polynomials', 'WEEKLY', 30, 4, 2, true),
('Science Practice Quiz', 2, 3, 'Quick practice on Acids Bases and Atoms chapter', 'PRACTICE', 20, 3, 2, true),
('English Grammar Quiz', 3, 4, 'Literary devices and vocabulary practice', 'PRACTICE', 15, 2, 1, true);

-- Test Questions mapping
INSERT INTO test_questions (test_id, question_id, question_order) VALUES
(1, 1, 1), (1, 2, 2), (1, 3, 3),
(2, 4, 1), (2, 5, 2), (2, 6, 3),
(3, 7, 1), (3, 8, 2);

-- Sample Announcements
INSERT INTO announcements (title, content, author_id, priority) VALUES
('Welcome to New Semester!', 'Dear students, welcome to the new semester. Classes begin from Monday. Please check your schedules.', 1, 'HIGH'),
('Weekend Practice Tests', 'Weekend practice tests are now live. Please complete before Sunday midnight for evaluation.', 1, 'NORMAL'),
('Math Olympiad Registration Open', 'Registration for Math Olympiad 2025 is now open. Interested students contact admin.', 2, 'HIGH');

COMMIT;

-- ================================================
-- NEW: Subscription & updated columns
-- ================================================

-- Add is_subscribed to users (run once on existing DB)
ALTER TABLE users ADD COLUMN IF NOT EXISTS is_subscribed BOOLEAN DEFAULT false;
ALTER TABLE users ADD COLUMN IF NOT EXISTS city VARCHAR(100) NOT NULL DEFAULT '';
ALTER TABLE users ADD COLUMN IF NOT EXISTS college VARCHAR(100) NOT NULL DEFAULT '';

-- Add number_of_questions to tests (for weekly random pick)
ALTER TABLE tests ADD COLUMN IF NOT EXISTS number_of_questions INT NULL;

-- Update test_type enum to include new types
ALTER TABLE tests MODIFY COLUMN test_type ENUM('PRACTICE','WEEKLY','CHAPTER','SUBJECT_MCQ') DEFAULT 'SUBJECT_MCQ';

-- Student Subscriptions
CREATE TABLE IF NOT EXISTS subscriptions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    plan_name VARCHAR(200) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status ENUM('ACTIVE','EXPIRED','CANCELLED') NOT NULL DEFAULT 'ACTIVE',
    amount_paid DOUBLE,
    payment_reference VARCHAR(200),
    activated_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (activated_by) REFERENCES users(id) ON DELETE SET NULL
);

-- Email OTP table (if not already created by Hibernate)
CREATE TABLE IF NOT EXISTS email_otp (
    id VARCHAR(36) PRIMARY KEY,
    email VARCHAR(255),
    otp VARCHAR(10),
    expiry_time DATETIME,
    verified BOOLEAN DEFAULT false
);
