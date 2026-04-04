package com.englishword.repository;

import com.englishword.entity.Sentence;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 句子数据访问接口
 *
 * 对应实体：Sentence
 * 对应表：sentence
 */
@Repository
public interface SentenceRepository extends JpaRepository<Sentence, String> {

    /**
     * 根据用户ID查询句子列表（分页）
     *
     * @param userId 用户ID
     * @param pageable 分页对象
     * @return 句子分页列表
     */
    Page<Sentence> findByUserId(String userId, Pageable pageable);

    /**
     * 根据用户ID搜索句子（模糊查询英文、中文）
     *
     * @param userId 用户ID
     * @param keyword 关键词
     * @param pageable 分页对象
     * @return 句子分页列表
     */
    @Query("SELECT s FROM Sentence s WHERE s.userId = :userId AND " +
           "(s.englishText LIKE %:keyword% OR s.chineseText LIKE %:keyword%)")
    Page<Sentence> searchByUserId(@Param("userId") String userId,
                                  @Param("keyword") String keyword,
                                  Pageable pageable);

    /**
     * 统计用户句子数量
     *
     * @param userId 用户ID
     * @return 句子数量
     */
    long countByUserId(String userId);
}
