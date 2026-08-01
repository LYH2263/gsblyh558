package com.exam.system.repository;

import com.exam.system.entity.ExamFinishType;
import com.exam.system.entity.ExamResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExamResultRepository extends JpaRepository<ExamResult, Long> {
    List<ExamResult> findByUserId(Long userId);
    List<ExamResult> findByExamId(Long examId);

    /** 某场次按交卷类型统计的作答份数。 */
    long countBySessionIdAndFinishType(Long sessionId, ExamFinishType finishType);

    /** 某场次全部作答份数。 */
    long countBySessionId(Long sessionId);

    /** 某场次平均用时（分钟）。无数据返回 null。 */
    @Query("SELECT AVG(r.usedMinutes) FROM ExamResult r WHERE r.session.id = :sessionId AND r.usedMinutes IS NOT NULL")
    Double findAverageUsedMinutesBySessionId(@Param("sessionId") Long sessionId);
}
