package com.tuition.repository;

import com.tuition.model.OnlineClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OnlineClassRepository extends JpaRepository<OnlineClass, Long> {
    List<OnlineClass> findBySubjectId(Long subjectId);
    List<OnlineClass> findByTutorId(Long tutorId);
    List<OnlineClass> findByStatus(OnlineClass.ClassStatus status);
    List<OnlineClass> findBySubjectIdOrderByScheduledAtAsc(Long subjectId);
    List<OnlineClass> findAllByOrderByScheduledAtAsc();
}
