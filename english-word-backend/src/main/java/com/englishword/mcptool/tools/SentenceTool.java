package com.englishword.mcptool.tools;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.englishword.context.UserContext;
import com.englishword.dto.response.ApiResponse;
import com.englishword.entity.Sentence;
import com.englishword.mcptool.annotation.McpParam;
import com.englishword.mcptool.annotation.McpTool;
import com.englishword.service.SentenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 句子工具类
 *
 * 提供句子相关的工具方法，供 AI 调用
 * 自动使用当前登录用户，无需传递用户ID
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SentenceTool {

    private final SentenceService sentenceService;

    /**
     * 获取当前用户ID，如果未登录则返回错误信息
     */
    private String getCurrentUserId() {
        String userId = UserContext.getUserId();
        if (userId == null) {
            throw new IllegalStateException("用户未登录");
        }
        return userId;
    }

    /**
     * 构建错误响应
     */
    private String errorResponse(String message) {
        JSONObject result = new JSONObject();
        result.put("success", false);
        result.put("message", message);
        return result.toJSONString();
    }

    /**
     * 添加句子
     */
    @McpTool(name = "add_sentence", description = """
            保存一个有价值的英语句子到用户句库。

            【重要】调用此工具前，AI 必须提供以下信息：
            - englishText: 英文句子原文
            - chineseText: 中文翻译（AI 必须翻译）
            - markedWords: 句子中的重点词汇（可选，逗号分隔）

            使用场景：用户说"收藏这个句子"、"保存这句话"时调用。
            """)
    public String addSentence(
            @McpParam(name = "englishText", description = "英文句子") String englishText,
            @McpParam(name = "chineseText", description = "中文翻译（AI必须提供）") String chineseText,
            @McpParam(name = "markedWords", description = "重点词汇，逗号分隔（可选）", required = false) String markedWords
    ) {
        String userId;
        try {
            userId = getCurrentUserId();
        } catch (IllegalStateException e) {
            return errorResponse(e.getMessage());
        }

        log.info("[MCP-SentenceTool] 添加句子: userId={}, englishText={}",
                 userId, englishText.substring(0, Math.min(50, englishText.length())));

        ApiResponse<Sentence> response = sentenceService.addSentence(
                userId, englishText, chineseText, markedWords, null);

        JSONObject result = new JSONObject();
        result.put("success", response.getCode() == 200);
        result.put("message", response.getMessage());

        if (response.getCode() == 200 && response.getData() != null) {
            result.put("sentence", sentenceToJson(response.getData()));
        }

        return result.toJSONString();
    }

    /**
     * 列出句子
     */
    @McpTool(name = "list_sentences", description = "获取当前用户的句子列表，支持分页和搜索")
    public String listSentences(
            @McpParam(name = "keyword", description = "搜索关键词（英文/中文/标记词）", required = false) String keyword,
            @McpParam(name = "page", description = "页码，从0开始", required = false) Integer page,
            @McpParam(name = "size", description = "每页数量，默认20", required = false) Integer size
    ) {
        String userId;
        try {
            userId = getCurrentUserId();
        } catch (IllegalStateException e) {
            return errorResponse(e.getMessage());
        }

        log.info("[MCP-SentenceTool] 获取句子列表: userId={}, keyword={}, page={}, size={}",
                 userId, keyword, page, size);

        int pageNum = (page != null && page >= 0) ? page : 0;
        int pageSize = (size != null && size > 0 && size <= 100) ? size : 20;

        Page<Sentence> sentencesPage = sentenceService.getUserSentencesPage(userId, keyword, pageNum, pageSize);

        JSONObject result = new JSONObject();
        result.put("success", true);
        result.put("totalElements", sentencesPage.getTotalElements());
        result.put("totalPages", sentencesPage.getTotalPages());
        result.put("currentPage", pageNum);

        JSONArray sentencesArray = new JSONArray();
        for (Sentence s : sentencesPage.getContent()) {
            sentencesArray.add(sentenceToJson(s));
        }
        result.put("sentences", sentencesArray);

        return result.toJSONString();
    }

    /**
     * 删除句子
     */
    @McpTool(name = "delete_sentence", description = "从用户句库中删除一个句子")
    public String deleteSentence(
            @McpParam(name = "sentenceId", description = "句子ID") String sentenceId
    ) {
        String userId;
        try {
            userId = getCurrentUserId();
        } catch (IllegalStateException e) {
            return errorResponse(e.getMessage());
        }

        log.info("[MCP-SentenceTool] 删除句子: userId={}, sentenceId={}", userId, sentenceId);

        ApiResponse<String> response = sentenceService.deleteSentence(sentenceId, userId);

        JSONObject result = new JSONObject();
        result.put("success", response.getCode() == 200);
        result.put("message", response.getMessage());

        return result.toJSONString();
    }

    /**
     * 获取句子统计
     */
    @McpTool(name = "get_sentence_statistics", description = "获取当前用户句库的统计信息")
    public String getSentenceStatistics() {
        String userId;
        try {
            userId = getCurrentUserId();
        } catch (IllegalStateException e) {
            return errorResponse(e.getMessage());
        }

        log.info("[MCP-SentenceTool] 获取句子统计: userId={}", userId);

        JSONObject stats = sentenceService.getSentenceStatistics(userId);

        JSONObject result = new JSONObject();
        result.put("success", true);
        result.put("totalSentences", stats.getLong("totalSentences"));

        return result.toJSONString();
    }

    /**
     * 将 Sentence 对象转换为 JSON
     */
    private JSONObject sentenceToJson(Sentence s) {
        JSONObject json = new JSONObject();
        json.put("sentenceId", s.getId());
        json.put("englishText", s.getEnglishText());
        json.put("chineseText", s.getChineseText());
        json.put("markedWords", parseMarkedWords(s.getMarkedWords()));
        json.put("createdAt", s.getCreatedAt() != null ? s.getCreatedAt().toString() : null);
        return json;
    }

    /**
     * 解析 markedWords JSON 字符串为 JSONArray
     */
    private JSONArray parseMarkedWords(String markedWordsJson) {
        if (markedWordsJson == null || markedWordsJson.isEmpty()) {
            return new JSONArray();
        }
        try {
            return JSONArray.parseArray(markedWordsJson);
        } catch (Exception e) {
            log.warn("[MCP-SentenceTool] 解析 markedWords 失败: {}", e.getMessage());
            return new JSONArray();
        }
    }
}
