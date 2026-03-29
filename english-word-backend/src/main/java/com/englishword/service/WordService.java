package com.englishword.service;

import com.englishword.dto.response.ApiResponse;
import com.englishword.entity.Word;
import com.englishword.repository.WordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 单词服务
 *
 * 功能：
 * - 单词CRUD操作
 * - 按用户查询单词列表
 * - 搜索单词
 * - 更新掌握程度
 */
@Service
@RequiredArgsConstructor
public class WordService {

    private final WordRepository wordRepository;

    /**
     * 添加单词
     *
     * @param userId 用户ID
     * @param word 单词对象
     * @return 添加结果
     */
    @Transactional
    public ApiResponse<Word> addWord(String userId, Word word) {
        // 设置用户ID
        word.setUserId(userId);

        // 保存单词
        Word savedWord = wordRepository.save(word);

        return ApiResponse.success(savedWord, "添加成功");
    }

    /**
     * 根据ID获取单词详情
     *
     * @param wordId 单词ID
     * @param userId 用户ID（用于验证权限）
     * @return 单词详情
     */
    public ApiResponse<Word> getWordById(String wordId, String userId) {
        Optional<Word> wordOptional = wordRepository.findById(wordId);

        if (wordOptional.isEmpty()) {
            return ApiResponse.error(404, "单词不存在");
        }

        Word word = wordOptional.get();

        // 验证权限
        if (!word.getUserId().equals(userId)) {
            return ApiResponse.error(403, "无权访问此单词");
        }

        return ApiResponse.success(word);
    }

    /**
     * 更新单词信息
     *
     * @param wordId 单词ID
     * @param userId 用户ID
     * @param updatedWord 更新的单词信息
     * @return 更新结果
     */
    @Transactional
    public ApiResponse<Word> updateWord(String wordId, String userId, Word updatedWord) {
        // 查找原单词
        Optional<Word> wordOptional = wordRepository.findById(wordId);
        if (wordOptional.isEmpty()) {
            return ApiResponse.error(404, "单词不存在");
        }

        Word word = wordOptional.get();

        // 验证权限
        if (!word.getUserId().equals(userId)) {
            return ApiResponse.error(403, "无权修改此单词");
        }

        // 更新字段（保留wordId和userId）
        updatedWord.setWordId(word.getWordId());
        updatedWord.setUserId(word.getUserId());

        Word savedWord = wordRepository.save(updatedWord);
        return ApiResponse.success(savedWord, "更新成功");
    }

    /**
     * 删除单词
     *
     * @param wordId 单词ID
     * @param userId 用户ID
     * @return 删除结果
     */
    @Transactional
    public ApiResponse<String> deleteWord(String wordId, String userId) {
        // 查找单词
        Optional<Word> wordOptional = wordRepository.findById(wordId);
        if (wordOptional.isEmpty()) {
            return ApiResponse.error(404, "单词不存在");
        }

        Word word = wordOptional.get();

        // 验证权限
        if (!word.getUserId().equals(userId)) {
            return ApiResponse.error(403, "无权删除此单词");
        }

        // 删除单词
        wordRepository.deleteById(wordId);

        return ApiResponse.success(null, "删除成功");
    }

    /**
     * 获取用户的单词列表
     *
     * @param userId 用户ID
     * @param status 状态（LEARNING/MASTERED，null表示全部）
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @return 单词列表
     */
    public ApiResponse<List<Word>> getUserWords(String userId, String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Word> wordsPage;
        if (status == null || status.isEmpty()) {
            wordsPage = wordRepository.findByUserId(userId, pageable);
        } else {
            wordsPage = wordRepository.findByUserIdAndStatus(userId, status, pageable);
        }

        return ApiResponse.success(wordsPage.getContent());
    }

    /**
     * 搜索单词（模糊查询）
     *
     * @param userId 用户ID
     * @param keyword 关键词
     * @param page 页码
     * @param size 每页大小
     * @return 搜素结果
     */
    public ApiResponse<List<Word>> searchWords(String userId, String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Word> wordsPage = wordRepository.searchByUserId(userId, keyword, pageable);

        return ApiResponse.success(wordsPage.getContent());
    }

    /**
     * 更新掌握程度
     *
     * @param wordId 单词ID
     * @param userId 用户ID
     * @param masteryLevel 掌握程度（1-5）
     * @return 更新结果
     */
    @Transactional
    public ApiResponse<Word> updateMasteryLevel(String wordId, String userId, Integer masteryLevel) {
        // 验证掌握程度范围
        if (masteryLevel < 1 || masteryLevel > 5) {
            return ApiResponse.error(400, "掌握程度必须在1-5之间");
        }

        // 查找单词
        Optional<Word> wordOptional = wordRepository.findById(wordId);
        if (wordOptional.isEmpty()) {
            return ApiResponse.error(404, "单词不存在");
        }

        Word word = wordOptional.get();

        // 验证权限
        if (!word.getUserId().equals(userId)) {
            return ApiResponse.error(403, "无权修改此单词");
        }

        // 更新掌握程度
        word.setMasteryLevel(masteryLevel);

        // 如果达到5星，自动标记为已掌握
        if (masteryLevel >= 5) {
            word.setStatus("MASTERED");
        }

        Word savedWord = wordRepository.save(word);
        return ApiResponse.success(savedWord, "掌握程度更新成功");
    }

    /**
     * 统计用户单词数量（按状态）
     *
     * @param userId 用户ID
     * @param status 状态（LEARNING/MASTERED）
     * @return 单词数量
     */
    public ApiResponse<Long> countByStatus(String userId, String status) {
        long count = wordRepository.countByUserIdAndStatus(userId, status);

        return ApiResponse.success(count);
    }
}
