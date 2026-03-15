package com.tuition.repository;

import com.tuition.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    List<Subscription> findByStudentIdOrderByCreatedAtDesc(Long studentId);

    List<Subscription> findByStatus(Subscription.SubscriptionStatus status);

    /** Returns the latest active subscription for a student, if any. */
    @Query("SELECT s FROM Subscription s WHERE s.student.id = :studentId " +
           "AND s.status = 'ACTIVE' AND s.startDate <= :today AND s.endDate >= :today " +
           "ORDER BY s.endDate DESC")
    Optional<Subscription> findActiveSubscription(@Param("studentId") Long studentId,
                                                   @Param("today") LocalDate today);

    boolean existsByStudentIdAndStatus(Long studentId, Subscription.SubscriptionStatus status);
}
