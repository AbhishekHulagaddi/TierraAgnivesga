package com.tuition.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "subscriptions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    /** Admin-managed plan label, e.g. "AIAPGET Full Course 2025" */
    @Column(name = "plan_name", nullable = false, length = 200)
    private String planName;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionStatus status;

    /** Amount paid in INR paise or smallest unit */
    @Column(name = "amount_paid")
    private Double amountPaid;

    /** Any payment reference / transaction id */
    @Column(name = "payment_reference", length = 200)
    private String paymentReference;

    /** Who activated / created this subscription (admin) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activated_by")
    private User activatedBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) status = SubscriptionStatus.ACTIVE;
    }

    public boolean isCurrentlyActive() {
        LocalDate today = LocalDate.now();
        return status == SubscriptionStatus.ACTIVE
                && !startDate.isAfter(today)
                && !endDate.isBefore(today);
    }

    public enum SubscriptionStatus {
        ACTIVE, EXPIRED, CANCELLED
    }
}
