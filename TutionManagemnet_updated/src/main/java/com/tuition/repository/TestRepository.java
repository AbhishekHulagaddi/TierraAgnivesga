package com.tuition.repository;

import com.tuition.model.Test;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TestRepository extends JpaRepository<Test, Long> {
    List<Test> findBySubjectId(Long subjectId);
    List<Test> findByTutorId(Long tutorId);
    List<Test> findByIsPublished(Boolean published);
    List<Test> findBySubjectIdAndIsPublished(Long subjectId, Boolean published);
    List<Test> findByTestType(Test.TestType testType);
    List<Test> findByTestTypeAndIsPublished(Test.TestType testType, Boolean published);
    List<Test> findBySubjectIdAndTestTypeAndIsPublished(Long subjectId, Test.TestType testType, Boolean published);

    List<Test> findByIsPublishedAndSubjectId(boolean isPublished, Long subjectId);

    List<Test> findByIsPublishedAndTitleContaining(boolean isPublished, String title);

    List<Test> findByIsPublishedAndSubjectIdAndTitleContaining(
            boolean isPublished,
            Long subjectId,
            String title);

}
