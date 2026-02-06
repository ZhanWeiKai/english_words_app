package com.englishword.repository;

import com.englishword.entity.AIConversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * AI对话记录数据访问接口
 *
 * 对应实体：AIConversation
 * 对应表：ai_conversation
 */
@Repository
public interface AIConversationRepository extends JpaRepository<AIConversation, String> {

    /**
     * 根据用户ID查询对话列表
     *
     * @param userId 用户ID
     * @param pageable 分页对象
     * @return 对话记录分页列表
     */
    Page<AIConversation> findByUserId(String userId, Pageable pageable);

    /**
     * 根据用户ID查询最近的对话（按创建时间倒序）
     *
     * @param userId 用户ID
     * @param pageable 分页对象
     * @return 对话记录分页列表
     */
    Page<AIConversation> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    /**
     * 根据用户ID查询最近的N条对话
     *
     * @param userId 用户ID
     * @param pageable 分页对象
     * @return 对话记录列表
     */
    List<AIConversation> findTop10ByUserIdOrderByCreatedAtDesc(String userId);

    /**
     * 根据上下文单词ID查询对话
     *
     * @param contextWordId 关联的单词ID
     * @param pageable 分页对象
     * @return 对话记录分页列表
     */
    Page<AIConversation> findByContextWordId(String contextWordId, Pageable pageable);
}
