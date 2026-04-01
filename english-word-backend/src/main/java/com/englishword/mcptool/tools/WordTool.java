package com.englishword.mcptool.tools;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.englishword.context.UserContext;
import com.englishword.dto.response.ApiResponse;
import com.englishword.entity.Word;
import com.englishword.mcptool.annotation.McpParam;
import com.englishword.mcptool.annotation.McpTool;
import com.englishword.service.WordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 单词工具类
 *
 * 提供单词相关的工具方法，供 AI 调用
 * 自动使用当前登录用户，无需传递用户ID
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WordTool {

    private final WordService wordService;

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
     * 获取用户词库列表
     */
    @McpTool(name = "list_user_words", description = "获取当前用户词库中的所有单词列表，支持分页和按状态筛选")
    public String listUserWords(
            @McpParam(name = "status", description = "单词状态：LEARNING(学习中) 或 MASTERED(已掌握)，不传则返回全部", required = false) String status,
            @McpParam(name = "page", description = "页码，从0开始", required = false) Integer page,
            @McpParam(name = "size", description = "每页数量，默认20", required = false) Integer size
    ) {
        String userId;
        try {
            userId = getCurrentUserId();
        } catch (IllegalStateException e) {
            return errorResponse(e.getMessage());
        }

        log.info("[MCP-WordTool] 获取用户词库: userId={}, status={}, page={}, size={}", userId, status, page, size);

        int pageNum = (page != null && page >= 0) ? page : 0;
        int pageSize = (size != null && size > 0 && size <= 100) ? size : 20;

        ApiResponse<List<Word>> response = wordService.getUserWords(userId, status, pageNum, pageSize);

        JSONObject result = new JSONObject();
        result.put("success", response.getCode() == 200);
        result.put("message", response.getMessage());

        if (response.getCode() == 200 && response.getData() != null) {
            JSONArray wordsArray = new JSONArray();
            for (Word w : response.getData()) {
                wordsArray.add(wordToJson(w));
            }
            result.put("words", wordsArray);
        }

        return result.toJSONString();
    }

    /**
     * 搜索单词
     */
    @McpTool(name = "search_words", description = "在当前用户词库中搜索单词（模糊匹配单词、释义）")
    public String searchWords(
            @McpParam(name = "keyword", description = "搜索关键词") String keyword,
            @McpParam(name = "page", description = "页码，从0开始", required = false) Integer page,
            @McpParam(name = "size", description = "每页数量", required = false) Integer size
    ) {
        String userId;
        try {
            userId = getCurrentUserId();
        } catch (IllegalStateException e) {
            return errorResponse(e.getMessage());
        }

        log.info("[MCP-WordTool] 搜索单词: userId={}, keyword={}", userId, keyword);

        int pageNum = (page != null && page >= 0) ? page : 0;
        int pageSize = (size != null && size > 0 && size <= 100) ? size : 20;

        ApiResponse<List<Word>> response = wordService.searchWords(userId, keyword, pageNum, pageSize);

        JSONObject result = new JSONObject();
        result.put("success", response.getCode() == 200);
        result.put("message", response.getMessage());
        result.put("keyword", keyword);

        if (response.getCode() == 200 && response.getData() != null) {
            JSONArray wordsArray = new JSONArray();
            for (Word w : response.getData()) {
                wordsArray.add(wordToJson(w));
            }
            result.put("words", wordsArray);
        }

        return result.toJSONString();
    }

    /**
     * 获取单个单词详情
     */
    @McpTool(name = "get_word_detail", description = "根据单词ID获取单词的详细信息")
    public String getWordDetail(
            @McpParam(name = "wordId", description = "单词ID") String wordId
    ) {
        String userId;
        try {
            userId = getCurrentUserId();
        } catch (IllegalStateException e) {
            return errorResponse(e.getMessage());
        }

        log.info("[MCP-WordTool] 获取单词详情: userId={}, wordId={}", userId, wordId);

        ApiResponse<Word> response = wordService.getWordById(wordId, userId);

        JSONObject result = new JSONObject();
        result.put("success", response.getCode() == 200);
        result.put("message", response.getMessage());

        if (response.getCode() == 200 && response.getData() != null) {
            result.put("word", wordToJson(response.getData()));
        }

        return result.toJSONString();
    }

    /**
     * 添加单词
     */
    @McpTool(name = "add_word", description = """
            添加新单词到当前用户词库。

            【重要】调用此工具前，AI 必须使用自身知识库填充以下完整信息：
            - definition: 英文释义（用英文解释单词含义）
            - translation: 中文翻译
            - pronunciation: 国际音标（如 /ˈæpl/）
            - partOfSpeech: 词性（如 n./v./adj./adv.）
            - exampleSentence: 英文例句

            示例：添加单词 "apple" 时，AI 应提供：
            - definition: a round fruit with red, green, or yellow skin and firm white flesh
            - translation: 苹果
            - pronunciation: /ˈæpl/
            - partOfSpeech: n.
            - exampleSentence: I eat an apple every day for breakfast.
            """)
    public String addWord(
            @McpParam(name = "word", description = "英文单词") String word,
            @McpParam(name = "definition", description = "英文释义（AI必须提供）") String definition,
            @McpParam(name = "translation", description = "中文翻译（AI必须提供）") String translation,
            @McpParam(name = "pronunciation", description = "国际音标（AI必须提供，如 /ˈæpl/）") String pronunciation,
            @McpParam(name = "partOfSpeech", description = "词性（AI必须提供，如 n./v./adj./adv.）") String partOfSpeech,
            @McpParam(name = "exampleSentence", description = "英文例句（AI必须提供）") String exampleSentence
    ) {
        String userId;
        try {
            userId = getCurrentUserId();
        } catch (IllegalStateException e) {
            return errorResponse(e.getMessage());
        }

        log.info("[MCP-WordTool] 添加单词: userId={}, word={}", userId, word);

        // 创建单词对象
        Word newWord = new Word();
        newWord.setWord(word);
        newWord.setDefinition(definition);
        newWord.setTranslation(translation);
        newWord.setPronunciation(pronunciation);
        newWord.setPartOfSpeech(partOfSpeech);
        newWord.setExampleSentence(exampleSentence);

        // 调用服务添加单词
        ApiResponse<Word> response = wordService.addWord(userId, newWord);

        JSONObject result = new JSONObject();
        result.put("success", response.getCode() == 200);
        result.put("message", response.getMessage());

        if (response.getCode() == 200 && response.getData() != null) {
            result.put("word", wordToJson(response.getData()));
        }

        return result.toJSONString();
    }

    /**
     * 更新单词掌握程度
     */
    @McpTool(name = "update_mastery", description = "更新单词的掌握程度（1-5星）")
    public String updateMastery(
            @McpParam(name = "wordId", description = "单词ID") String wordId,
            @McpParam(name = "masteryLevel", description = "掌握程度（1-5）") Integer masteryLevel
    ) {
        String userId;
        try {
            userId = getCurrentUserId();
        } catch (IllegalStateException e) {
            return errorResponse(e.getMessage());
        }

        log.info("[MCP-WordTool] 更新掌握程度: userId={}, wordId={}, masteryLevel={}", userId, wordId, masteryLevel);

        ApiResponse<Word> response = wordService.updateMasteryLevel(wordId, userId, masteryLevel);

        JSONObject result = new JSONObject();
        result.put("success", response.getCode() == 200);
        result.put("message", response.getMessage());

        if (response.getCode() == 200 && response.getData() != null) {
            result.put("word", wordToJson(response.getData()));
        }

        return result.toJSONString();
    }

    /**
     * 删除单词
     */
    @McpTool(name = "delete_word", description = "从当前用户词库中删除单词")
    public String deleteWord(
            @McpParam(name = "wordId", description = "单词ID") String wordId
    ) {
        String userId;
        try {
            userId = getCurrentUserId();
        } catch (IllegalStateException e) {
            return errorResponse(e.getMessage());
        }

        log.info("[MCP-WordTool] 删除单词: userId={}, wordId={}", userId, wordId);

        ApiResponse<String> response = wordService.deleteWord(wordId, userId);

        JSONObject result = new JSONObject();
        result.put("success", response.getCode() == 200);
        result.put("message", response.getMessage());

        return result.toJSONString();
    }

    /**
     * 获取单词统计
     */
    @McpTool(name = "get_word_statistics", description = "获取当前用户词库的统计信息（学习中/已掌握数量）")
    public String getWordStatistics() {
        String userId;
        try {
            userId = getCurrentUserId();
        } catch (IllegalStateException e) {
            return errorResponse(e.getMessage());
        }

        log.info("[MCP-WordTool] 获取单词统计: userId={}", userId);

        ApiResponse<Long> learningResponse = wordService.countByStatus(userId, "LEARNING");
        ApiResponse<Long> masteredResponse = wordService.countByStatus(userId, "MASTERED");

        long learningCount = learningResponse.getCode() == 200 ? learningResponse.getData() : 0;
        long masteredCount = masteredResponse.getCode() == 200 ? masteredResponse.getData() : 0;

        JSONObject result = new JSONObject();
        result.put("totalWords", learningCount + masteredCount);
        result.put("learningWords", learningCount);
        result.put("masteredWords", masteredCount);

        return result.toJSONString();
    }

    /**
     * 将 Word 对象转换为 JSON
     */
    private JSONObject wordToJson(Word w) {
        JSONObject json = new JSONObject();
        json.put("wordId", w.getWordId());
        json.put("word", w.getWord());
        json.put("definition", w.getDefinition());
        json.put("translation", w.getTranslation());
        json.put("pronunciation", w.getPronunciation());
        json.put("partOfSpeech", w.getPartOfSpeech());
        json.put("exampleSentence", w.getExampleSentence());
        json.put("masteryLevel", w.getMasteryLevel());
        json.put("status", w.getStatus());
        return json;
    }
}
