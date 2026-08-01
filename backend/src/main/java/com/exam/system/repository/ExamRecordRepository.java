package com.exam.system.repository;

import com.exam.system.entity.ExamRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamRecordRepository extends JpaRepository<ExamRecord, Long> {
}
