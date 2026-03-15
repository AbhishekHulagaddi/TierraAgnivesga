package com.tuition.controller;

import com.tuition.model.*;
import com.tuition.model.TestAttempt.Result;
import com.tuition.repository.*;
import com.tuition.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MainController {

    private final SubjectRepository subjectRepository;
    private final QuestionRepository questionRepository;
    private final TestRepository testRepository;
    private final NotesRepository notesRepository;
    private final OnlineClassRepository classRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AnnouncementRepository announcementRepository;
    private final TestAttemptRepository testAttemptRepository;
    private final StudentAnswerRepository studentAnswerRepository;
    private final UserRepository userRepository;
    private final SubscriptionService subscriptionService;

    // ================================================================
    // HELPER — subscription guard
    // ================================================================
    private ResponseEntity<?> subscriptionRequired() {
        return ResponseEntity.status(403).body(Map.of(
            "error", "Subscription required",
            "code",  "SUBSCRIPTION_REQUIRED",
            "message", "Please subscribe to access classes, tests, and notes."
        ));
    }

    // ================================================================
    // SUBJECTS
    // ================================================================
    @GetMapping("/subjects")
    public ResponseEntity<?> getAllSubjects() {
        List<Subject> subjects = subjectRepository.findByIsActive(true);
        return ResponseEntity.ok(subjects.stream().map(s -> Map.of(
            "id",          s.getId(),
            "name",        s.getName(),
            "code",        s.getCode(),
            "description", s.getDescription() != null ? s.getDescription() : "",
            "gradeLevel",  s.getGradeLevel() != null ? s.getGradeLevel() : "",
            "tutorName",   s.getTutor() != null ? s.getTutor().getFullName() : "N/A",
            "tutorId",     s.getTutor() != null ? s.getTutor().getId() : 0
        )).collect(Collectors.toList()));
    }

    @PostMapping("/admin/subjects")
    public ResponseEntity<?> createSubject(@RequestBody Map<String, Object> req) {
        Long tutorId = Long.parseLong(req.get("tutorId").toString());
        User tutor = userRepository.findById(tutorId).orElse(null);
        Subject subject = Subject.builder()
            .name(req.get("name").toString())
            .code(req.get("code").toString())
            .description(req.getOrDefault("description", "").toString())
            .gradeLevel(req.getOrDefault("gradeLevel", "").toString())
            .tutor(tutor)
            .isActive(true)
            .build();
        return ResponseEntity.ok(subjectRepository.save(subject));
    }

    @DeleteMapping("/admin/subjects/{id}")
    public ResponseEntity<?> deleteSubject(@PathVariable Long id) {
        subjectRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Subject deleted"));
    }

    // ================================================================
    // QUESTIONS — tutor can add/delete subject-wise
    // ================================================================
    @GetMapping("/questions/subject/{subjectId}")
    public ResponseEntity<?> getQuestionsBySubject(@PathVariable Long subjectId) {
        List<Question> questions = questionRepository.findBySubjectIdAndIsActive(subjectId, true);
        return ResponseEntity.ok(questions.stream().map(q -> Map.of(
            "id",            q.getId(),
            "questionText",  q.getQuestionText(),
            "optionA",       q.getOptionA(),
            "optionB",       q.getOptionB(),
            "optionC",       q.getOptionC(),
            "optionD",       q.getOptionD(),
            "correctAnswer", q.getCorrectAnswer().name(),
            "explanation",   q.getExplanation() != null ? q.getExplanation() : "",
            "difficulty",    q.getDifficulty().name(),
            "marks",         q.getMarks()
        )).collect(Collectors.toList()));
    }

    @PostMapping("/tutor/questions")
    public ResponseEntity<?> addQuestion(@RequestBody Map<String, Object> req,
                                          @RequestHeader("Authorization") String auth) {
        Long subjectId = Long.parseLong(req.get("subjectId").toString());
        Long tutorId   = Long.parseLong(req.get("tutorId").toString());
        Subject subject = subjectRepository.findById(subjectId).orElseThrow();
        User tutor      = userRepository.findById(tutorId).orElseThrow();
        Question q = Question.builder()
            .subject(subject).tutor(tutor)
            .questionText(req.get("questionText").toString())
            .optionA(req.get("optionA").toString())
            .optionB(req.get("optionB").toString())
            .optionC(req.get("optionC").toString())
            .optionD(req.get("optionD").toString())
            .correctAnswer(Question.CorrectAnswer.valueOf(req.get("correctAnswer").toString()))
            .explanation(req.getOrDefault("explanation", "").toString())
            .difficulty(Question.Difficulty.valueOf(req.getOrDefault("difficulty", "MEDIUM").toString()))
            .marks(Integer.parseInt(req.getOrDefault("marks", "1").toString()))
            .isActive(true)
            .build();
        return ResponseEntity.ok(questionRepository.save(q));
    }

    @DeleteMapping("/tutor/questions/{id}")
    public ResponseEntity<?> deleteQuestion(@PathVariable Long id) {
        questionRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Question deleted"));
    }

    // ================================================================
    // TESTS — subject-wise MCQ + weekly combined
    // ================================================================

    /** All published tests — subscription required for students */
    @GetMapping("/tests")
    public ResponseEntity<?> getAllTests(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Integer section) {

        if (studentId != null && !subscriptionService.isSubscribed(studentId)) {
            return subscriptionRequired();
        }

        List<Test> tests;

        if (subjectId != null && section != null) {
            tests = testRepository.findByIsPublishedAndSubjectIdAndTitleContaining(
                    true, subjectId, "Section " + section);
        } 
        else if (subjectId != null) {
            tests = testRepository.findByIsPublishedAndSubjectId(true, subjectId);
        } 
        else if (section != null) {
            tests = testRepository.findByIsPublishedAndTitleContaining(true, "Section " + section);
        } 
        else {
            tests = testRepository.findByIsPublished(true);
        }

        return ResponseEntity.ok(
                tests.stream().map(this::mapTest).collect(Collectors.toList())
        );
    }


    /** Subject-wise MCQ tests — subscription required */
    @GetMapping("/tests/subject/{subjectId}")
    public ResponseEntity<?> getTestsBySubject(@PathVariable Long subjectId,
                                                @RequestParam(required = false) Long studentId) {
        if (studentId != null && !subscriptionService.isSubscribed(studentId)) {
            return subscriptionRequired();
        }
        return ResponseEntity.ok(
            testRepository.findBySubjectIdAndTestTypeAndIsPublished(
                    subjectId, Test.TestType.SUBJECT_MCQ, true)
                .stream().map(this::mapTest).collect(Collectors.toList()));
    }

    /** All published weekly tests */
    @GetMapping("/tests/weekly")
    public ResponseEntity<?> getWeeklyTests(@RequestParam(required = false) Long studentId) {
        if (studentId != null && !subscriptionService.isSubscribed(studentId)) {
            return subscriptionRequired();
        }
        return ResponseEntity.ok(
            testRepository.findByTestTypeAndIsPublished(Test.TestType.WEEKLY, true)
                .stream().map(this::mapTest).collect(Collectors.toList()));
    }

    /** Single test detail with questions (no correct answers exposed) — subscription required */
    @GetMapping("/tests/{id}")
    public ResponseEntity<?> getTestById(@PathVariable Long id,
                                          @RequestParam(required = false) Long studentId) {
        if (studentId != null && !subscriptionService.isSubscribed(studentId)) {
            return subscriptionRequired();
        }
        Test t = testRepository.findById(id).orElseThrow();
        Map<String, Object> result = mapTest(t);
        List<Map<String, Object>> questions = t.getQuestions().stream().map(q -> {
            Map<String, Object> qm = new HashMap<>();
            qm.put("id",           q.getId());
            qm.put("questionText", q.getQuestionText());
            qm.put("optionA",      q.getOptionA());
            qm.put("optionB",      q.getOptionB());
            qm.put("optionC",      q.getOptionC());
            qm.put("optionD",      q.getOptionD());
            qm.put("marks",        q.getMarks());
            return qm;
        }).collect(Collectors.toList());
        result.put("questions", questions);
        return ResponseEntity.ok(result);
    }

    private Map<String, Object> mapTest(Test t) {
        Map<String, Object> m = new HashMap<>();
        m.put("id",              t.getId());
        m.put("title",           t.getTitle());
        m.put("subjectId",       t.getSubject() != null ? t.getSubject().getId() : null);
        m.put("subjectName",     t.getSubject() != null ? t.getSubject().getName() : "Combined");
        m.put("tutorName",       t.getTutor().getFullName());
        m.put("description",     t.getDescription() != null ? t.getDescription() : "");
        m.put("testType",        t.getTestType().name());
        m.put("durationMinutes", t.getDurationMinutes());
        m.put("totalMarks",      t.getTotalMarks());
        m.put("passingMarks",    t.getPassingMarks());
        m.put("numberOfQuestions", t.getNumberOfQuestions() != null
                ? t.getNumberOfQuestions() : (t.getQuestions() != null ? t.getQuestions().size() : 0));
        m.put("isPublished",     t.getIsPublished());
        m.put("questionCount",   t.getQuestions() != null ? t.getQuestions().size() : 0);
        return m;
    }

    /**
     * TUTOR: Create subject-wise MCQ test.
     * Admin configures: numberOfQuestions, durationMinutes, marks via passingMarks.
     */
    @PostMapping("/tutor/tests/subject-mcq")
    public ResponseEntity<?> createSubjectMcqTest(@RequestBody Map<String, Object> req) {
        Long subjectId = Long.parseLong(req.get("subjectId").toString());
        Long tutorId   = Long.parseLong(req.get("tutorId").toString());
        Subject subject = subjectRepository.findById(subjectId).orElseThrow();
        User tutor      = userRepository.findById(tutorId).orElseThrow();

        @SuppressWarnings("unchecked")
        List<Long> questionIds = ((List<Object>) req.get("questionIds")).stream()
            .map(o -> Long.parseLong(o.toString())).collect(Collectors.toList());
        List<Question> questions = questionRepository.findAllById(questionIds);
        int totalMarks = questions.stream().mapToInt(Question::getMarks).sum();

        Test test = Test.builder()
            .title(req.get("title").toString())
            .subject(subject).tutor(tutor)
            .description(req.getOrDefault("description", "").toString())
            .testType(Test.TestType.SUBJECT_MCQ)
            .durationMinutes(Integer.parseInt(req.getOrDefault("durationMinutes", "30").toString()))
            .totalMarks(totalMarks)
            .passingMarks(Integer.parseInt(req.getOrDefault("passingMarks", "0").toString()))
            .numberOfQuestions(questions.size())
            .isPublished(Boolean.parseBoolean(req.getOrDefault("isPublished", "false").toString()))
            .questions(questions)
            .build();
        return ResponseEntity.ok(testRepository.save(test));
    }

    /**
     * ADMIN: Create weekly combined test.
     * Admin specifies: numberOfQuestions (how many to randomly pick), durationMinutes,
     * passingMarks, and a pool of questionIds from any subject.
     */
    @PostMapping("/admin/tests/weekly")
    public ResponseEntity<?> createWeeklyTest(@RequestBody Map<String, Object> req) {
        Long tutorId = Long.parseLong(req.get("tutorId").toString());
        User tutor   = userRepository.findById(tutorId).orElseThrow();

        @SuppressWarnings("unchecked")
        List<Long> questionIds = ((List<Object>) req.get("questionIds")).stream()
            .map(o -> Long.parseLong(o.toString())).collect(Collectors.toList());
        List<Question> questionPool = questionRepository.findAllById(questionIds);

        int numberOfQuestions = Integer.parseInt(req.getOrDefault("numberOfQuestions",
                String.valueOf(questionPool.size())).toString());
        // Total marks is calculated assuming each randomly selected question has avg marks
        // Admin can override via totalMarks field
        int totalMarks = questionPool.stream().mapToInt(Question::getMarks).sum();
        if (req.containsKey("totalMarks")) {
            totalMarks = Integer.parseInt(req.get("totalMarks").toString());
        }

        Test test = Test.builder()
            .title(req.get("title").toString())
            .subject(null) // Weekly tests span all subjects
            .tutor(tutor)
            .description(req.getOrDefault("description", "").toString())
            .testType(Test.TestType.WEEKLY)
            .durationMinutes(Integer.parseInt(req.getOrDefault("durationMinutes", "60").toString()))
            .totalMarks(totalMarks)
            .passingMarks(Integer.parseInt(req.getOrDefault("passingMarks", "0").toString()))
            .numberOfQuestions(numberOfQuestions)
            .isPublished(Boolean.parseBoolean(req.getOrDefault("isPublished", "false").toString()))
            .questions(questionPool)
            .build();
        return ResponseEntity.ok(testRepository.save(test));
    }

    /** Legacy endpoint — kept for backward compatibility (maps to SUBJECT_MCQ) */
    @PostMapping("/tutor/tests")
    public ResponseEntity<?> createTest(@RequestBody Map<String, Object> req) {
        return createSubjectMcqTest(req);
    }

    @DeleteMapping("/tutor/tests/{id}")
    public ResponseEntity<?> deleteTest(@PathVariable Long id) {
        testRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Test deleted"));
    }

    @PutMapping("/admin/tests/{id}/publish")
    public ResponseEntity<?> publishTest(@PathVariable Long id,
                                          @RequestBody Map<String, Object> req) {
        Test test = testRepository.findById(id).orElseThrow();
        test.setIsPublished(Boolean.parseBoolean(req.getOrDefault("isPublished", "true").toString()));
        testRepository.save(test);
        return ResponseEntity.ok(Map.of("message", "Test publish status updated"));
    }

    // ================================================================
    // NOTES — subscription required for students
    // ================================================================
    @GetMapping("/notes/subject/{subjectId}")
    public ResponseEntity<?> getNotesBySubject(@PathVariable Long subjectId,
                                                @RequestParam(required = false) Long studentId) {
        if (studentId != null && !subscriptionService.isSubscribed(studentId)) {
            return subscriptionRequired();
        }
        return ResponseEntity.ok(notesRepository.findBySubjectIdAndIsActive(subjectId, true)
            .stream().map(n -> Map.of(
                "id",            n.getId(),
                "title",         n.getTitle(),
                "description",   n.getDescription() != null ? n.getDescription() : "",
                "chapter",       n.getChapter() != null ? n.getChapter() : "",
                "fileName",      n.getFileName(),
                "fileSize",      n.getFileSize() != null ? n.getFileSize() : 0,
                "downloadCount", n.getDownloadCount(),
                "tutorName",     n.getTutor().getFullName(),
                "createdAt",     n.getCreatedAt()
            )).collect(Collectors.toList()));
    }

    @GetMapping("/notes")
    public ResponseEntity<?> getAllNotes(@RequestParam(required = false) Long studentId) {
        if (studentId != null && !subscriptionService.isSubscribed(studentId)) {
            return subscriptionRequired();
        }
        return ResponseEntity.ok(notesRepository.findAll().stream().map(n -> Map.of(
            "id",            n.getId(),
            "title",         n.getTitle(),
            "subjectName",   n.getSubject().getName(),
            "subjectId",     n.getSubject().getId(),
            "description",   n.getDescription() != null ? n.getDescription() : "",
            "chapter",       n.getChapter() != null ? n.getChapter() : "",
            "fileName",      n.getFileName(),
            "downloadCount", n.getDownloadCount(),
            "tutorName",     n.getTutor().getFullName(),
            "createdAt",     n.getCreatedAt()
        )).collect(Collectors.toList()));
    }

    // ================================================================
    // ONLINE CLASSES — subscription required for students
    // ================================================================
    @GetMapping("/classes")
    public ResponseEntity<?> getAllClasses(@RequestParam(required = false) Long studentId) {
        if (studentId != null && !subscriptionService.isSubscribed(studentId)) {
            return subscriptionRequired();
        }
        return ResponseEntity.ok(classRepository.findAllByOrderByScheduledAtAsc()
            .stream().map(this::mapClass).collect(Collectors.toList()));
    }

    @GetMapping("/classes/subject/{subjectId}")
    public ResponseEntity<?> getClassesBySubject(@PathVariable Long subjectId,
                                                   @RequestParam(required = false) Long studentId) {
        if (studentId != null && !subscriptionService.isSubscribed(studentId)) {
            return subscriptionRequired();
        }
        return ResponseEntity.ok(classRepository.findBySubjectIdOrderByScheduledAtAsc(subjectId)
            .stream().map(this::mapClass).collect(Collectors.toList()));
    }

    @PostMapping("/tutor/classes")
    public ResponseEntity<?> createClass(@RequestBody Map<String, Object> req) {
        Long subjectId = Long.parseLong(req.get("subjectId").toString());
        Long tutorId   = Long.parseLong(req.get("tutorId").toString());
        Subject subject = subjectRepository.findById(subjectId).orElseThrow();
        User tutor      = userRepository.findById(tutorId).orElseThrow();
        OnlineClass cls = OnlineClass.builder()
            .title(req.get("title").toString())
            .subject(subject).tutor(tutor)
            .description(req.getOrDefault("description", "").toString())
            .meetingLink(req.getOrDefault("meetingLink", "").toString())
            .scheduledAt(java.time.LocalDateTime.parse(req.get("scheduledAt").toString()))
            .durationMinutes(Integer.parseInt(req.getOrDefault("durationMinutes", "60").toString()))
            .status(OnlineClass.ClassStatus.SCHEDULED)
            .build();
        return ResponseEntity.ok(classRepository.save(cls));
    }

    private Map<String, Object> mapClass(OnlineClass c) {
        Map<String, Object> m = new HashMap<>();
        m.put("id",              c.getId());
        m.put("title",           c.getTitle());
        m.put("subjectId",       c.getSubject().getId());
        m.put("subjectName",     c.getSubject().getName());
        m.put("tutorName",       c.getTutor().getFullName());
        m.put("description",     c.getDescription() != null ? c.getDescription() : "");
        m.put("meetingLink",     c.getMeetingLink() != null ? c.getMeetingLink() : "");
        m.put("scheduledAt",     c.getScheduledAt());
        m.put("durationMinutes", c.getDurationMinutes());
        m.put("status",          c.getStatus().name());
        m.put("recordingUrl",    c.getRecordingUrl() != null ? c.getRecordingUrl() : "");
        return m;
    }

    // ================================================================
    // ENROLLMENTS
    // ================================================================
    @GetMapping("/student/enrollments/{studentId}")
    public ResponseEntity<?> getEnrollments(@PathVariable Long studentId) {
        return ResponseEntity.ok(enrollmentRepository.findByStudentId(studentId).stream().map(e -> Map.of(
            "id",          e.getId(),
            "subjectId",   e.getSubject().getId(),
            "subjectName", e.getSubject().getName(),
            "subjectCode", e.getSubject().getCode(),
            "tutorName",   e.getSubject().getTutor() != null ? e.getSubject().getTutor().getFullName() : "",
            "enrolledAt",  e.getEnrolledAt()
        )).collect(Collectors.toList()));
    }

    @PostMapping("/student/enroll")
    public ResponseEntity<?> enroll(@RequestBody Map<String, Object> req) {
        Long studentId = Long.parseLong(req.get("studentId").toString());
        Long subjectId = Long.parseLong(req.get("subjectId").toString());
        if (!subscriptionService.isSubscribed(studentId)) {
            return subscriptionRequired();
        }
        if (enrollmentRepository.existsByStudentIdAndSubjectId(studentId, subjectId)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Already enrolled"));
        }
        User student = userRepository.findById(studentId).orElseThrow();
        Subject subject = subjectRepository.findById(subjectId).orElseThrow();
        Enrollment e = Enrollment.builder().student(student).subject(subject).isActive(true).build();
        enrollmentRepository.save(e);
        return ResponseEntity.ok(Map.of("message", "Enrolled successfully"));
    }

    // ================================================================
    // TEST ATTEMPTS — subscription required
    // ================================================================
    @PostMapping("/student/tests/{testId}/start")
    public ResponseEntity<?> startTest(@PathVariable Long testId,
                                        @RequestBody Map<String, Object> req) {
        Long studentId = Long.parseLong(req.get("studentId").toString());

        // Subscription guard
        if (!subscriptionService.isSubscribed(studentId)) {
            return subscriptionRequired();
        }

        User student = userRepository.findById(studentId).orElseThrow();
        Test test    = testRepository.findById(testId).orElseThrow();

        Optional<TestAttempt> existing = testAttemptRepository
            .findByStudentIdAndTestIdAndStatus(studentId, testId, TestAttempt.Status.IN_PROGRESS);
        if (existing.isPresent()) {
            return ResponseEntity.ok(Map.of("attemptId", existing.get().getId()));
        }

        // For WEEKLY tests, randomly sample numberOfQuestions from the pool
        List<Question> attemptQuestions;
        if (test.getTestType() == Test.TestType.WEEKLY && test.getNumberOfQuestions() != null
                && test.getNumberOfQuestions() < test.getQuestions().size()) {
            List<Question> pool = new ArrayList<>(test.getQuestions());
            Collections.shuffle(pool);
            attemptQuestions = pool.subList(0, test.getNumberOfQuestions());
        } else {
            attemptQuestions = test.getQuestions();
        }

        int totalMarks = attemptQuestions.stream().mapToInt(Question::getMarks).sum();

        TestAttempt attempt = TestAttempt.builder()
            .student(student).test(test)
            .totalMarks(totalMarks)
            .status(TestAttempt.Status.IN_PROGRESS)
            .build();
        TestAttempt saved = testAttemptRepository.save(attempt);

        // Return the sampled questions (without correct answers)
        List<Map<String, Object>> questionList = attemptQuestions.stream().map(q -> {
            Map<String, Object> qm = new HashMap<>();
            qm.put("id",           q.getId());
            qm.put("questionText", q.getQuestionText());
            qm.put("optionA",      q.getOptionA());
            qm.put("optionB",      q.getOptionB());
            qm.put("optionC",      q.getOptionC());
            qm.put("optionD",      q.getOptionD());
            qm.put("marks",        q.getMarks());
            return qm;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
            "attemptId",       saved.getId(),
            "durationMinutes", test.getDurationMinutes(),
            "totalMarks",      totalMarks,
            "questions",       questionList
        ));
    }

    @PostMapping("/student/attempts/{attemptId}/submit")
    public ResponseEntity<?> submitTest(@PathVariable Long attemptId,
                                         @RequestBody Map<String, Object> req) {
        TestAttempt attempt = testAttemptRepository.findById(attemptId).orElseThrow();

        // Guard: check subscription still valid
        if (!subscriptionService.isSubscribed(attempt.getStudent().getId())) {
            return subscriptionRequired();
        }

        Test test = attempt.getTest();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> answers = (List<Map<String, Object>>) req.get("answers");

        int totalScore = 0;
        for (Map<String, Object> ans : answers) {
            Long questionId  = Long.parseLong(ans.get("questionId").toString());
            String selectedAns = ans.get("selectedAnswer").toString();
            Question question = questionRepository.findById(questionId).orElseThrow();
            boolean isCorrect = question.getCorrectAnswer().name().equals(selectedAns);
            int marks = isCorrect ? question.getMarks() : 0;
            totalScore += marks;

            StudentAnswer sa = StudentAnswer.builder()
                .attempt(attempt).question(question)
                .selectedAnswer(Question.CorrectAnswer.valueOf(selectedAns))
                .isCorrect(isCorrect)
                .marksObtained(marks)
                .build();
            studentAnswerRepository.save(sa);
        }

        double percentage = attempt.getTotalMarks() > 0
            ? (double) totalScore / attempt.getTotalMarks() * 100 : 0;
        attempt.setScore(totalScore);
        attempt.setPercentage(java.math.BigDecimal.valueOf(percentage));
        attempt.setStatus(TestAttempt.Status.SUBMITTED);
        attempt.setSubmittedAt(java.time.LocalDateTime.now());
        Result result;

        if (percentage < 40) {
            result = Result.POOR;
        } else if (percentage < 60) {
            result = Result.AVERAGE;
        } else if (percentage < 80) {
            result = Result.GOOD;
        } else {
            result = Result.EXCELLENT;
        }

        attempt.setResult(result);
        testAttemptRepository.save(attempt);

        return ResponseEntity.ok(Map.of(
            "score",      totalScore,
            "totalMarks", attempt.getTotalMarks(),
            "percentage", String.format("%.1f", percentage),
            "passed",     attempt.getResult()
        ));
    }
    
    @GetMapping("/student/attempts/{attemptId}/review")
    public ResponseEntity<?> reviewAttempt(@PathVariable Long attemptId) {
        TestAttempt attempt = testAttemptRepository.findById(attemptId).orElseThrow();
        
        if (!subscriptionService.isSubscribed(attempt.getStudent().getId())) {
            return subscriptionRequired();
        }
        
        List<StudentAnswer> studentAnswers = studentAnswerRepository.findByAttemptId(attemptId);
        
        // Map studentAnswers by questionId for quick lookup
        Map<Long, StudentAnswer> answerMap = studentAnswers.stream()
            .collect(java.util.stream.Collectors.toMap(
                sa -> sa.getQuestion().getId(), sa -> sa
            ));
        
        // Get all questions for this test attempt
        List<Map<String, Object>> questions = attempt.getTest().getQuestions().stream()
            .map(q -> {
                StudentAnswer sa = answerMap.get(q.getId());
                Map<String, Object> qMap = new java.util.LinkedHashMap<>();
                qMap.put("questionId",    q.getId());
                qMap.put("questionText",  q.getQuestionText());
                qMap.put("optionA",       q.getOptionA());
                qMap.put("optionB",       q.getOptionB());
                qMap.put("optionC",       q.getOptionC());
                qMap.put("optionD",       q.getOptionD());
                qMap.put("correctAnswer", q.getCorrectAnswer().name());
                qMap.put("explanation",   q.getExplanation());
                qMap.put("marks",         q.getMarks());
                qMap.put("selectedAnswer", sa != null ? sa.getSelectedAnswer().name() : null);
                qMap.put("isCorrect",      sa != null && sa.getIsCorrect());
                qMap.put("marksObtained",  sa != null ? sa.getMarksObtained() : 0);
                return qMap;
            })
            .collect(java.util.stream.Collectors.toList());
        
        return ResponseEntity.ok(Map.of(
            "attemptId",  attemptId,
            "testTitle",  attempt.getTest().getTitle(),
            "subjectName",attempt.getTest().getSubject() != null 
                            ? attempt.getTest().getSubject().getName() : "All Subjects",
            "score",      attempt.getScore(),
            "totalMarks", attempt.getTotalMarks(),
            "percentage", attempt.getPercentage().toString(),
            "passed",     attempt.getScore() >= attempt.getTest().getPassingMarks(),
            "questions",  questions
        ));
    }

    @GetMapping("/student/results/{studentId}")
    public ResponseEntity<?> getStudentResults(@PathVariable Long studentId) {
        List<TestAttempt> attempts = testAttemptRepository.findByStudentIdOrderByStartedAtDesc(studentId);
        return ResponseEntity.ok(attempts.stream().map(a -> {
            Map<String, Object> m = new HashMap<>();
            m.put("attemptId",   a.getId());
            m.put("testTitle",   a.getTest().getTitle());
            m.put("subjectName", a.getTest().getSubject() != null
                    ? a.getTest().getSubject().getName() : "Weekly Combined");
            m.put("testType",    a.getTest().getTestType().name());
            m.put("score",       a.getScore());
            m.put("totalMarks",  a.getTotalMarks());
            m.put("percentage",  a.getPercentage());
            m.put("status",      a.getStatus().name());
            m.put("startedAt",   a.getStartedAt());
            m.put("submittedAt", a.getSubmittedAt() != null ? a.getSubmittedAt() : "");
            return m;
        }).collect(Collectors.toList()));
    }

    @GetMapping("/student/results/{studentId}/attempt/{attemptId}")
    public ResponseEntity<?> getAttemptDetail(@PathVariable Long studentId,
                                               @PathVariable Long attemptId) {
        List<StudentAnswer> answers = studentAnswerRepository.findByAttemptId(attemptId);
        return ResponseEntity.ok(answers.stream().map(a -> Map.of(
            "questionText",  a.getQuestion().getQuestionText(),
            "optionA",       a.getQuestion().getOptionA(),
            "optionB",       a.getQuestion().getOptionB(),
            "optionC",       a.getQuestion().getOptionC(),
            "optionD",       a.getQuestion().getOptionD(),
            "correctAnswer", a.getQuestion().getCorrectAnswer().name(),
            "selectedAnswer", a.getSelectedAnswer() != null ? a.getSelectedAnswer().name() : "N/A",
            "isCorrect",     a.getIsCorrect(),
            "marksObtained", a.getMarksObtained(),
            "explanation",   a.getQuestion().getExplanation() != null ? a.getQuestion().getExplanation() : ""
        )).collect(Collectors.toList()));
    }

    // ================================================================
    // ANNOUNCEMENTS
    // ================================================================
    @GetMapping("/announcements")
    public ResponseEntity<?> getAnnouncements() {
        return ResponseEntity.ok(announcementRepository.findByIsActiveTrueOrderByCreatedAtDesc()
            .stream().map(a -> Map.of(
                "id",         a.getId(),
                "title",      a.getTitle(),
                "content",    a.getContent(),
                "authorName", a.getAuthor().getFullName(),
                "priority",   a.getPriority().name(),
                "createdAt",  a.getCreatedAt()
            )).collect(Collectors.toList()));
    }

    @PostMapping("/admin/announcements")
    public ResponseEntity<?> createAnnouncement(@RequestBody Map<String, Object> req) {
        Long authorId = Long.parseLong(req.get("authorId").toString());
        User author = userRepository.findById(authorId).orElseThrow();
        Announcement a = Announcement.builder()
            .title(req.get("title").toString())
            .content(req.get("content").toString())
            .author(author)
            .priority(Announcement.Priority.valueOf(req.getOrDefault("priority", "NORMAL").toString()))
            .isActive(true)
            .build();
        announcementRepository.save(a);
        return ResponseEntity.ok(Map.of("message", "Announcement created"));
    }

    // ================================================================
    // ADMIN: USERS
    // ================================================================
    @GetMapping("/admin/users")
    public ResponseEntity<?> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll().stream().map(u -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id",           u.getId());
            m.put("username",     u.getUsername());
            m.put("fullName",     u.getFullName());
            m.put("email",        u.getEmail());
            m.put("role",         u.getRole().name());
            m.put("phone",        u.getPhone() != null ? u.getPhone() : "");
            m.put("isActive",     u.getIsActive());
            m.put("isSubscribed", u.getIsSubscribed());
            m.put("createdAt",    u.getCreatedAt());
            m.put("collegeName", u.getCollege());
            m.put("city",    u.getCity());
            return m;
        }).collect(Collectors.toList()));
    }

    @PutMapping("/admin/users/{id}/toggle")
    public ResponseEntity<?> toggleUser(@PathVariable Long id) {
        User user = userRepository.findById(id).orElseThrow();
        user.setIsActive(!user.getIsActive());
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("isActive", user.getIsActive()));
    }

    @DeleteMapping("/admin/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        userRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "User deleted"));
    }

    // ================================================================
    // DASHBOARD STATS
    // ================================================================
    @GetMapping("/admin/stats")
    public ResponseEntity<?> getAdminStats() {
        long totalStudents       = userRepository.findByRole(User.Role.STUDENT).size();
        long subscribedStudents  = userRepository.findByRole(User.Role.STUDENT)
                                       .stream().filter(u -> Boolean.TRUE.equals(u.getIsSubscribed())).count();
        long totalTutors         = userRepository.findByRole(User.Role.TUTOR).size();
        long totalSubjects       = subjectRepository.count();
        long totalTests          = testRepository.count();
        long subjectMcqTests     = testRepository.findByTestType(Test.TestType.SUBJECT_MCQ).size();
        long weeklyTests         = testRepository.findByTestType(Test.TestType.WEEKLY).size();
        long totalQuestions      = questionRepository.count();
        long totalNotes          = notesRepository.count();
        return ResponseEntity.ok(Map.of(
            "totalStudents",      totalStudents,
            "subscribedStudents", subscribedStudents,
            "totalTutors",        totalTutors,
            "totalSubjects",      totalSubjects,
            "totalTests",         totalTests,
            "subjectMcqTests",    subjectMcqTests,
            "weeklyTests",        weeklyTests,
            "totalQuestions",     totalQuestions,
            "totalNotes",         totalNotes
        ));
    }

    @GetMapping("/student/stats/{studentId}")
    public ResponseEntity<?> getStudentStats(@PathVariable Long studentId) {
        long enrolledSubjects = enrollmentRepository.findByStudentId(studentId).size();
        long completedTests   = testAttemptRepository.countCompletedByStudent(studentId);
        long weeklyCompleted  = testAttemptRepository.findByStudentIdOrderByStartedAtDesc(studentId)
            .stream().filter(a -> a.getTest().getTestType() == Test.TestType.WEEKLY
                    && a.getStatus() == TestAttempt.Status.SUBMITTED).count();
        Double avgPercentage  = testAttemptRepository.avgPercentageByStudent(studentId);
        boolean isSubscribed  = subscriptionService.isSubscribed(studentId);
        return ResponseEntity.ok(Map.of(
            "enrolledSubjects", enrolledSubjects,
            "completedTests",   completedTests,
            "weeklyCompleted",  weeklyCompleted,
            "avgPercentage",    avgPercentage != null ? String.format("%.1f", avgPercentage) : "0.0",
            "isSubscribed",     isSubscribed
        ));
    }
}
