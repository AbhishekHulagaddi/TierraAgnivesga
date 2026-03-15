package com.tuition.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "tests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Test {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    /**
     * For SUBJECT_MCQ tests, subject is mandatory.
     * For WEEKLY tests, subject may be null (combined across subjects).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tutor_id", nullable = false)
    private User tutor;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "test_type")
    private TestType testType = TestType.SUBJECT_MCQ;

    /** Duration in minutes — set by admin */
    @Column(name = "duration_minutes")
    private Integer durationMinutes = 30;

    /** Total marks — auto-calculated from questions or set by admin */
    @Column(name = "total_marks")
    private Integer totalMarks = 0;

    @Column(name = "passing_marks")
    private Integer passingMarks = 0;

    /**
     * For WEEKLY tests: how many questions to randomly pick from the pool.
     * For SUBJECT_MCQ: equals questions.size().
     */
    @Column(name = "number_of_questions")
    private Integer numberOfQuestions;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "is_published")
    private Boolean isPublished = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /**
     * Question pool.
     * SUBJECT_MCQ: these are the actual test questions.
     * WEEKLY: random N are sampled from this pool at attempt start.
     */
    @ManyToMany
    @JoinTable(
        name = "test_questions",
        joinColumns = @JoinColumn(name = "test_id"),
        inverseJoinColumns = @JoinColumn(name = "question_id")
    )
    private List<Question> questions;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum TestType {
        SUBJECT_MCQ,
        WEEKLY,
        PRACTICE,
        CHAPTER
    }
}
