package com.tuition.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "test_attempts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_id", nullable = false)
    private Test test;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    private Integer score = 0;

    @Column(name = "total_marks")
    private Integer totalMarks = 0;

    private java.math.BigDecimal percentage = java.math.BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    private Status status = Status.IN_PROGRESS;
    
    @Enumerated(EnumType.STRING)
    private Result result = Result.PENDING;

    @PrePersist
    protected void onCreate() {
        startedAt = LocalDateTime.now();
    }

    public enum Status { IN_PROGRESS, SUBMITTED, TIMED_OUT }
    
    public enum Result { PENDING, GOOD, AVERAGE, EXCELLENT, POOR }
}
