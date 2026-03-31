package com.englishword.mcptool.tools;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.englishword.entity.Word;
import com.englishword.mcptool.annotation.McpParam;
import com.englishword.mcptool.annotation.McpTool;
import com.englishword.repository.WordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/**
 * 单词工具类
 *
 * 提供单词相关的工具方法，供 AI 调用
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WordTool {

    private final WordRepository wordRepository;

    /**
     * 获取用户词库列表
     */
    @McpTool(name = "list_user_words", description = "获取用户词库中的所有单词列表，支持分页")
    public String listUserWords(
            @McpParam(name = "userId", description = "用户ID") String userId,
            @McpParam(name = "page", description = "页码，从0开始", required = false) Integer page,
            @McpParam(name = "size", description = "每页数量，默认20", required = false) Integer size
    ) {
        log.info("[MCP-WordTool] 获取用户词库: userId={}, page={}, size={}", userId, page, size);

        int pageNum = (page != null && page >= 0) ? page : 0;
        int pageSize = (size != null && size > 0 && size <= 100) ? size : 20;

        Page<Word> wordPage = wordRepository.findByUserId(userId, PageRequest.of(pageNum, pageSize));

        JSONObject result = new JSONObject();
        result.put("totalElements", wordPage.getTotalElements());
        result.put("totalPages", wordPage.getTotalPages());
        result.put("currentPage", pageNum);
        result.put("pageSize", pageSize);

        JSONArray wordsArray = new JSONArray();
        for (Word w : wordPage.getContent()) {
            JSONObject wordJson = new JSONObject();
            wordJson.put("wordId", w.getWordId());
            wordJson.put("word", w.getWord());
            wordJson.put("definition", w.getDefinition());
            wordJson.put("translation", w.getTranslation());
            wordJson.put("pronunciation", w.getPronunciation());
            wordJson.put("partOfSpeech", w.getPartOfSpeech());
            wordJson.put("exampleSentence", w.getExampleSentence());
            wordJson.put("masteryLevel", w.getMasteryLevel());
            wordJson.put("status", w.getStatus());
            wordsArray.add(wordJson);
        }

        result.put("words", wordsArray);

        return result.toJSONString();
    }

    /**
     * 搜索单词
     */
    @McpTool(name = "search_words", description = "在用户词库中搜索单词（模糊匹配）")
    public String searchWords(
            @McpParam(name = "userId", description = "用户ID") String userId,
            @McpParam(name = "keyword", description = "搜索关键词") String keyword,
            @McpParam(name = "page", description = "页码，从0开始", required = false) Integer page,
            @McpParam(name = "size", description = "每页数量", required = false) Integer size
    ) {
        log.info("[MCP-WordTool] 搜索单词: userId={}, keyword={}", userId, keyword);

        int pageNum = (page != null && page >= 0) ? page : 0;
        int pageSize = (size != null && size > 0 && size <= 100) ? size : 20;

        Page<Word> wordPage = wordRepository.searchByUserId(userId, keyword, PageRequest.of(pageNum, pageSize));

        JSONObject result = new JSONObject();
        result.put("keyword", keyword);
        result.put("totalElements", wordPage.getTotalElements());
        result.put("totalPages", wordPage.getTotalPages());

        JSONArray wordsArray = new JSONArray();
        for (Word w : wordPage.getContent()) {
            JSONObject wordJson = new JSONObject();
            wordJson.put("wordId", w.getWordId());
            wordJson.put("word", w.getWord());
            wordJson.put("definition", w.getDefinition());
            wordJson.put("translation", w.getTranslation());
            wordsArray.add(wordJson);
        }

        result.put("words", wordsArray);

        return result.toJSONString();
    }

    /**
     * 获取用户单词统计
     */
    @McpTool(name = "get_word_statistics", description = "获取用户词库的统计信息")
    public String getWordStatistics(
            @McpParam(name = "userId", description = "用户ID") String userId
    ) {
        log.info("[MCP-WordTool] 获取单词统计: userId={}", userId);

        long learningCount = wordRepository.countByUserIdAndStatus(userId, "LEARNING");
        long masteredCount = wordRepository.countByUserIdAndStatus(userId, "MASTERED");
        long totalCount = learningCount + masteredCount;

        JSONObject result = new JSONObject();
        result.put("userId", userId);
        result.put("totalWords", totalCount);
        result.put("learningWords", learningCount);
        result.put("masteredWords", masteredCount);

        return result.toJSONString();
    }
}
