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
 */
@Repository
public interface WordRepository extends JpaRepository<Word, Long> {

    /**
     * 根据单词查找
     */
    Word findByWord(String word);

    /**
     * 搜索单词（模糊查询）
     */
    Page<Word> findByWordContainingIgnoreCase(String keyword, Pageable pageable);

    /**
     * 全文搜索单词
     */
    @Query("SELECT w FROM Word w WHERE w.word LIKE %:keyword% OR w.definition LIKE %:keyword%")
    Page<Word> searchWords(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 随机获取单词
     */
    @Query(value = "SELECT * FROM words ORDER BY RAND() LIMIT :limit", nativeQuery = true)
    List<Word> findRandomWords(@Param("limit") int limit);
}
