package com.exam.system.repository;

import com.exam.system.entity.WrongQuestionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface WrongQuestionRecordRepository extends JpaRepository<WrongQuestionRecord, Long> {
    Optional<WrongQuestionRecord> findByUserIdAndQuestionIdAndIsRemovedFalse(Long userId, Long questionId);
}
