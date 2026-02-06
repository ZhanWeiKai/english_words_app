package com.englishword.repository;

import com.englishword.entity.LearningRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 学习记录数据访问接口
 */
@Repository
public interface LearningRecordRepository extends JpaRepository<LearningRecord, Long> {

    /**
     * 查找用户的所有学习记录
     */
    List<LearningRecord> findByUserId(Long userId);

    /**
     * 查找用户对某个单词的学习记录
     */
    Optional<LearningRecord> findByUserIdAndWordId(Long userId, Long wordId);

    /**
     * 查找用户需要复习的单词
     */
    List<LearningRecord> findByUserIdAndNextReviewDateBefore(Long userId, java.time.LocalDateTime date);
}
