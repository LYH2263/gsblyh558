package com.exam.system.repository;

import com.exam.system.entity.PracticeRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PracticeRecordRepository extends JpaRepository<PracticeRecord, Long> {
}
