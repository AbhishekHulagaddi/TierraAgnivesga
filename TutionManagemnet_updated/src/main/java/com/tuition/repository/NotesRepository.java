package com.tuition.repository;

import com.tuition.model.Notes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NotesRepository extends JpaRepository<Notes, Long> {
    List<Notes> findBySubjectId(Long subjectId);
    List<Notes> findByTutorId(Long tutorId);
    List<Notes> findBySubjectIdAndIsActive(Long subjectId, Boolean active);
}
