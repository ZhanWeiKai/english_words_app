package com.englishword.repository;

import com.englishword.entity.Word;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 单词数据访问接口
 *
 * 对应实体：Word
 * 对应表：word
 */
@Repository
public interface WordRepository extends JpaRepository<Word, String> {

    /**
     * 根据用户ID和状态查询单词列表
     *
     * @param userId 用户ID
     * @param status 状态（LEARNING/MASTERED）
     * @param pageable 分页对象
     * @return 单词分页列表
     */
    Page<Word> findByUserIdAndStatus(String userId, String status, Pageable pageable);

    /**
     * 根据用户ID查询单词列表（所有状态）
     *
     * @param userId 用户ID
     * @param pageable 分页对象
     * @return 单词分页列表
     */
    Page<Word> findByUserId(String userId, Pageable pageable);

    /**
     * 根据用户ID和掌握程度查询
     *
     * @param userId 用户ID
     * @param masteryLevel 掌握程度（1-5）
     * @param pageable 分页对象
     * @return 单词分页列表
     */
    Page<Word> findByUserIdAndMasteryLevel(String userId, Integer masteryLevel, Pageable pageable);

    /**
     * 根据用户ID搜索单词（模糊查询）
     *
     * @param userId 用户ID
     * @param keyword 关键词
     * @param pageable 分页对象
     * @return 单词分页列表
     */
    @Query("SELECT w FROM Word w WHERE w.userId = :userId AND " +
           "(w.word LIKE %:keyword% OR w.definition LIKE %:keyword%)")
    Page<Word> searchByUserId(@Param("userId") String userId,
                              @Param("keyword") String keyword,
                              Pageable pageable);

    /**
     * 统计用户单词数量（按状态）
     *
     * @param userId 用户ID
     * @param status 状态
     * @return 单词数量
     */
    long countByUserIdAndStatus(String userId, String status);
}
