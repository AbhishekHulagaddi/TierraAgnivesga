# EduPulse — Tuition Management System
## Java Spring Boot + MySQL Full Stack Application

---

## 🚀 Features

### 👨‍🎓 Student Portal
- **Self Registration & Login** (JWT-secured)
- **Subject Enrollment** — browse and enroll in subjects
- **Online Classes** — view scheduled classes with join links
- **Take MCQ Tests** — timed practice/weekly tests with live timer
- **Instant Results** — score, percentage, pass/fail with answer review
- **Download Notes/PDFs** — subject-wise study material
- **Announcements** — stay updated with portal news

### 👨‍🏫 Tutor Portal
- **Question Bank** — add MCQ questions per subject with difficulty & marks
- **Create Tests** — compose tests from question bank, set duration, publish
- **Schedule Classes** — add Zoom/Meet links, set timings
- **Upload PDF Notes** — upload study material per subject/chapter
- **View Test Results** — see student performance

### 🔧 Admin Portal
- **User Management** — manage students, tutors; activate/deactivate
- **Subject Management** — create/assign subjects to tutors
- **Announcements** — post priority-based announcements
- **Full Dashboard** — system-wide stats
- **Complete Oversight** — all tutor features + admin controls

---

## 🛠️ Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Java 17 + Spring Boot 3.2 |
| Security | Spring Security + JWT |
| ORM | Spring Data JPA + Hibernate |
| Database | MySQL 8.0 |
| Frontend | HTML5 + CSS3 + Vanilla JavaScript |
| Build | Maven |
| Fonts | Syne + DM Sans (Google Fonts) |

---

## ⚙️ Setup Instructions

### Prerequisites
- Java 17+
- MySQL 8.0+
- Maven 3.8+

### Step 1: Database Setup
```sql
-- Login to MySQL
mysql -u root -p

-- Create database
CREATE DATABASE tuition_db;

-- Run the schema file
USE tuition_db;
SOURCE src/main/resources/schema.sql;
```

### Step 2: Configure Database
Edit `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/tuition_db
spring.datasource.username=root
spring.datasource.password=YOUR_MYSQL_PASSWORD
```

### Step 3: Build & Run
```bash
cd tuition-mgmt
mvn clean install -DskipTests
mvn spring-boot:run
```

### Step 4: Access the Application
Open browser: **http://localhost:8080**

---

## 🔐 Default Login Credentials

| Role | Username | Password |
|------|----------|----------|
| Admin | `admin` | `admin123` |
| Tutor (Math) | `tutor_math` | `tutor123` |
| Tutor (Science) | `tutor_sci` | `tutor123` |
| Tutor (English) | `tutor_eng` | `tutor123` |
| Student 1 | `student1` | `student123` |
| Student 2 | `student2` | `student123` |

---

## 📁 Project Structure

```
tuition-mgmt/
├── src/main/
│   ├── java/com/tuition/
│   │   ├── TuitionManagementApplication.java
│   │   ├── config/
│   │   │   ├── SecurityConfig.java
│   │   │   ├── JwtUtil.java
│   │   │   └── JwtRequestFilter.java
│   │   ├── model/
│   │   │   ├── User.java
│   │   │   ├── Subject.java
│   │   │   ├── Question.java
│   │   │   ├── Test.java
│   │   │   ├── TestAttempt.java
│   │   │   ├── StudentAnswer.java
│   │   │   ├── Notes.java
│   │   │   ├── OnlineClass.java
│   │   │   ├── Enrollment.java
│   │   │   └── Announcement.java
│   │   ├── repository/
│   │   │   ├── UserRepository.java
│   │   │   └── AllRepositories.java
│   │   ├── service/
│   │   │   └── CustomUserDetailsService.java
│   │   └── controller/
│   │       ├── AuthController.java
│   │       ├── MainController.java
│   │       └── FileController.java
│   └── resources/
│       ├── application.properties
│       ├── schema.sql
│       └── static/
│           └── index.html     ← Complete SPA Frontend
├── uploads/notes/             ← PDF uploads stored here
└── pom.xml
```

---

## 🗄️ Database Schema

| Table | Description |
|-------|-------------|
| `users` | All users (admin, tutor, student) |
| `subjects` | Subject catalog with tutor assignment |
| `enrollments` | Student-subject enrollment mapping |
| `questions` | MCQ question bank per subject |
| `tests` | Practice/weekly tests |
| `test_questions` | Test-question many-to-many mapping |
| `test_attempts` | Student test attempt records |
| `student_answers` | Per-question answer tracking |
| `classes` | Online class schedule |
| `notes` | Uploaded PDF notes metadata |
| `announcements` | Portal announcements |

---

## 🔌 REST API Endpoints

### Auth
- `POST /api/auth/login` — Login, returns JWT
- `POST /api/auth/register` — Student self-registration
- `GET /api/auth/me` — Get current user info

### Subjects
- `GET /api/subjects` — List all active subjects
- `POST /api/admin/subjects` — Create subject (ADMIN)
- `DELETE /api/admin/subjects/{id}` — Delete subject (ADMIN)

### Questions
- `GET /api/questions/subject/{id}` — Questions by subject
- `POST /api/tutor/questions` — Add question (TUTOR/ADMIN)
- `DELETE /api/tutor/questions/{id}` — Delete question

### Tests
- `GET /api/tests` — All published tests
- `GET /api/tests/{id}` — Test with questions
- `POST /api/tutor/tests` — Create test
- `DELETE /api/tutor/tests/{id}` — Delete test

### Test Taking
- `POST /api/student/tests/{testId}/start` — Start attempt
- `POST /api/student/attempts/{attemptId}/submit` — Submit answers
- `GET /api/student/results/{studentId}` — All results
- `GET /api/student/results/{studentId}/attempt/{attemptId}` — Attempt detail

### Classes
- `GET /api/classes` — All classes
- `POST /api/tutor/classes` — Schedule class

### Notes
- `GET /api/notes` — All notes
- `POST /api/tutor/notes/upload` — Upload PDF (multipart)
- `GET /api/tutor/notes/download/{id}` — Download PDF

### Enrollments
- `GET /api/student/enrollments/{studentId}` — Student's courses
- `POST /api/student/enroll` — Enroll in subject

### Admin
- `GET /api/admin/users` — All users
- `PUT /api/admin/users/{id}/toggle` — Toggle active status
- `DELETE /api/admin/users/{id}` — Delete user
- `GET /api/admin/stats` — Dashboard statistics
- `POST /api/admin/announcements` — Post announcement

---

## 📈 Future Enhancements
- Email notifications for class reminders
- Video conferencing integration (WebRTC)
- Progress tracking with charts
- Certificate generation on test completion
- Mobile app (React Native)
- Real-time chat between tutor and student
