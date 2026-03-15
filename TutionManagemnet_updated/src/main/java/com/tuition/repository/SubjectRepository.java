package com.tuition.repository;

import com.tuition.model.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {
    List<Subject> findByIsActive(Boolean isActive);
    List<Subject> findByTutorId(Long tutorId);
}
