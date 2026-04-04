package com.englishword.service;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.englishword.dto.response.ApiResponse;
import com.englishword.entity.Sentence;
import com.englishword.repository.SentenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 句子服务
 *
 * 功能：
 * - 句子CRUD操作
 * - 按用户查询句子列表
 * - 搜索句子
 * - 标记词管理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SentenceService {

    private final SentenceRepository sentenceRepository;

    /**
     * 添加句子
     *
     * @param userId 用户ID
     * @param englishText 英文句子
     * @param chineseText 中文翻译
     * @param markedWords 标记词（逗号分隔，可选）
     * @param sourceConversationId 来源对话ID（可选）
     * @return 添加结果
     */
    @Transactional
    public ApiResponse<Sentence> addSentence(String userId, String englishText, String chineseText,
                                             String markedWords, String sourceConversationId) {
        Sentence sentence = new Sentence();
        sentence.setUserId(userId);
        sentence.setEnglishText(englishText);
        sentence.setChineseText(chineseText);
        sentence.setSourceConversationId(sourceConversationId);

        // 处理标记词
        if (markedWords != null && !markedWords.trim().isEmpty()) {
            String markedWordsJson = parseMarkedWords(markedWords);
            sentence.setMarkedWords(markedWordsJson);
        }

        Sentence savedSentence = sentenceRepository.save(sentence);
        log.info("[SentenceService] 句子已保存: id={}, userId={}, englishText={}",
                 savedSentence.getId(), userId, englishText.substring(0, Math.min(50, englishText.length())));

        return ApiResponse.success(savedSentence, "句子已保存");
    }

    /**
     * 将逗号分隔的标记词转换为JSON格式
     *
     * @param markedWords 逗号分隔的单词
     * @return JSON字符串
     */
    private String parseMarkedWords(String markedWords) {
        JSONArray wordsArray = new JSONArray();
        String[] words = markedWords.split(",");

        for (String word : words) {
            String trimmedWord = word.trim();
            if (!trimmedWord.isEmpty()) {
                JSONObject wordObj = new JSONObject();
                wordObj.put("word", trimmedWord);
                wordObj.put("wordId", null);  // 初始时没有关联词库ID
                wordsArray.add(wordObj);
            }
        }

        return wordsArray.toJSONString();
    }

    /**
     * 根据ID获取句子详情
     *
     * @param sentenceId 句子ID
     * @param userId 用户ID（用于验证权限）
     * @return 句子详情
     */
    public ApiResponse<Sentence> getSentenceById(String sentenceId, String userId) {
        Optional<Sentence> sentenceOptional = sentenceRepository.findById(sentenceId);

        if (sentenceOptional.isEmpty()) {
            return ApiResponse.error(404, "句子不存在");
        }

        Sentence sentence = sentenceOptional.get();

        // 验证权限
        if (!sentence.getUserId().equals(userId)) {
            return ApiResponse.error(403, "无权访问此句子");
        }

        return ApiResponse.success(sentence, "查询成功");
    }

    /**
     * 删除句子
     *
     * @param sentenceId 句子ID
     * @param userId 用户ID
     * @return 删除结果
     */
    @Transactional
    public ApiResponse<String> deleteSentence(String sentenceId, String userId) {
        Optional<Sentence> sentenceOptional = sentenceRepository.findById(sentenceId);

        if (sentenceOptional.isEmpty()) {
            return ApiResponse.error(404, "句子不存在");
        }

        Sentence sentence = sentenceOptional.get();

        // 验证权限
        if (!sentence.getUserId().equals(userId)) {
            return ApiResponse.error(403, "无权删除此句子");
        }

        sentenceRepository.deleteById(sentenceId);
        log.info("[SentenceService] 句子已删除: id={}, userId={}", sentenceId, userId);

        return ApiResponse.success(null, "删除成功");
    }

    /**
     * 获取用户的句子列表
     *
     * @param userId 用户ID
     * @param keyword 搜索关键词（可选）
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @return 句子列表
     */
    public ApiResponse<List<Sentence>> getUserSentences(String userId, String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Sentence> sentencesPage;
        if (keyword == null || keyword.trim().isEmpty()) {
            sentencesPage = sentenceRepository.findByUserId(userId, pageable);
        } else {
            sentencesPage = sentenceRepository.searchByUserId(userId, keyword.trim(), pageable);
        }

        return ApiResponse.success(sentencesPage.getContent());
    }

    /**
     * 获取分页信息
     *
     * @param userId 用户ID
     * @param keyword 搜索关键词（可选）
     * @param page 页码
     * @param size 每页大小
     * @return 分页信息（总元素数、总页数、当前页）
     */
    public Page<Sentence> getUserSentencesPage(String userId, String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        if (keyword == null || keyword.trim().isEmpty()) {
            return sentenceRepository.findByUserId(userId, pageable);
        } else {
            return sentenceRepository.searchByUserId(userId, keyword.trim(), pageable);
        }
    }

    /**
     * 更新句子
     *
     * @param sentenceId 句子ID
     * @param userId 用户ID
     * @param updatedSentence 更新的句子信息
     * @return 更新结果
     */
    @Transactional
    public ApiResponse<Sentence> updateSentence(String sentenceId, String userId, Sentence updatedSentence) {
        Optional<Sentence> sentenceOptional = sentenceRepository.findById(sentenceId);

        if (sentenceOptional.isEmpty()) {
            return ApiResponse.error(404, "句子不存在");
        }

        Sentence sentence = sentenceOptional.get();

        // 验证权限
        if (!sentence.getUserId().equals(userId)) {
            return ApiResponse.error(403, "无权修改此句子");
        }

        // 更新字段（只更新非空字段）
        if (updatedSentence.getEnglishText() != null) {
            sentence.setEnglishText(updatedSentence.getEnglishText());
        }
        if (updatedSentence.getChineseText() != null) {
            sentence.setChineseText(updatedSentence.getChineseText());
        }
        if (updatedSentence.getMarkedWords() != null) {
            sentence.setMarkedWords(updatedSentence.getMarkedWords());
        }

        Sentence savedSentence = sentenceRepository.save(sentence);
        log.info("[SentenceService] 句子已更新: id={}, userId={}", sentenceId, userId);

        return ApiResponse.success(savedSentence, "更新成功");
    }

    /**
     * 获取用户句子统计
     *
     * @param userId 用户ID
     * @return 统计信息
     */
    public JSONObject getSentenceStatistics(String userId) {
        long totalSentences = sentenceRepository.countByUserId(userId);

        JSONObject stats = new JSONObject();
        stats.put("totalSentences", totalSentences);

        return stats;
    }
}
