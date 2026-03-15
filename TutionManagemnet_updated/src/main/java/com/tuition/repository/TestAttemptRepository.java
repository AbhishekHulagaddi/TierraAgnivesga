package com.tuition.repository;

import com.tuition.model.TestAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TestAttemptRepository extends JpaRepository<TestAttempt, Long> {
    List<TestAttempt> findByStudentId(Long studentId);
    List<TestAttempt> findByTestId(Long testId);
    Optional<TestAttempt> findByStudentIdAndTestIdAndStatus(Long studentId, Long testId, TestAttempt.Status status);
    List<TestAttempt> findByStudentIdOrderByStartedAtDesc(Long studentId);

    @Query("SELECT AVG(ta.percentage) FROM TestAttempt ta WHERE ta.student.id = :studentId AND ta.status = 'SUBMITTED'")
    Double avgPercentageByStudent(@Param("studentId") Long studentId);

    @Query("SELECT COUNT(ta) FROM TestAttempt ta WHERE ta.student.id = :studentId AND ta.status = 'SUBMITTED'")
    long countCompletedByStudent(@Param("studentId") Long studentId);
}
