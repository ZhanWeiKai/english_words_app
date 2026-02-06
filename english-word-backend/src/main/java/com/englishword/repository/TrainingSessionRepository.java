package com.englishword.repository;

import com.englishword.entity.TrainingSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 训练会话数据访问接口
 *
 * 对应实体：TrainingSession
 * 对应表：training_session
 */
@Repository
public interface TrainingSessionRepository extends JpaRepository<TrainingSession, String> {

    /**
     * 根据用户ID查询训练会话列表
     *
     * @param userId 用户ID
     * @param pageable 分页对象
     * @return 训练会话分页列表
     */
    Page<TrainingSession> findByUserId(String userId, Pageable pageable);

    /**
     * 根据用户ID查询最近的N条训练记录
     *
     * @param userId 用户ID
     * @param pageable 分页对象
     * @return 训练会话列表
     */
    Page<TrainingSession> findByUserIdOrderByStartTimeDesc(String userId, Pageable pageable);

    /**
     * 根据用户ID和时间范围查询
     *
     * @param userId 用户ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 训练会话列表
     */
    List<TrainingSession> findByUserIdAndStartTimeBetween(
        String userId,
        LocalDateTime startTime,
        LocalDateTime endTime
    );
}
