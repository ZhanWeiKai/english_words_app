package com.englishword.controller;

import com.englishword.dto.response.ApiResponse;
import com.englishword.entity.Word;
import com.englishword.service.WordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 单词管理控制器
 *
 * 提供的API：
 * - POST /api/words - 添加单词
 * - GET /api/words - 获取单词列表（分页）
 * - GET /api/words/{id} - 获取单词详情
 * - PUT /api/words/{id} - 更新单词
 * - DELETE /api/words/{id} - 删除单词
 * - GET /api/words/search - 搜索单词
 * - PUT /api/words/{id}/mastery - 更新掌握程度
 * - GET /api/words/count - 统计单词数量
 */
@Tag(name = "单词管理", description = "单词的增删改查操作")
@RestController
@RequestMapping("/words")
@RequiredArgsConstructor
public class WordController {

    private final WordService wordService;

    /**
     * 添加单词
     *
     * @param word 单词对象
     * @param request HTTP请求（用于获取用户ID）
     * @return 添加结果
     */
    @Operation(summary = "添加单词", description = "创建新的单词记录")
    @PostMapping
    public ApiResponse<Word> addWord(
            @RequestBody Word word,
            HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        return wordService.addWord(userId, word);
    }

    /**
     * 获取单词列表
     *
     * @param status 状态（LEARNING/MASTERED，可选）
     * @param page 页码（从0开始，默认0）
     * @param size 每页大小（默认20）
     * @param request HTTP请求
     * @return 单词列表
     */
    @Operation(summary = "获取单词列表", description = "获取当前用户的单词列表")
    @GetMapping
    public ApiResponse<List<Word>> getUserWords(
            @Parameter(description = "状态（LEARNING/MASTERED）") @RequestParam(required = false) String status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "100") int size,
            HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        return wordService.getUserWords(userId, status, page, size);
    }

    /**
     * 获取单词详情
     *
     * @param wordId 单词ID
     * @param request HTTP请求
     * @return 单词详情
     */
    @Operation(summary = "获取单词详情", description = "根据ID获取单词的详细信息")
    @GetMapping("/{wordId}")
    public ApiResponse<Word> getWordById(
            @Parameter(description = "单词ID") @PathVariable String wordId,
            HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        return wordService.getWordById(wordId, userId);
    }

    /**
     * 更新单词
     *
     * @param wordId 单词ID
     * @param updatedWord 更新的单词信息
     * @param request HTTP请求
     * @return 更新结果
     */
    @Operation(summary = "更新单词", description = "更新单词的信息（释义、例句等）")
    @PutMapping("/{wordId}")
    public ApiResponse<Word> updateWord(
            @Parameter(description = "单词ID") @PathVariable String wordId,
            @RequestBody Word updatedWord,
            HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        return wordService.updateWord(wordId, userId, updatedWord);
    }

    /**
     * 删除单词
     *
     * @param wordId 单词ID
     * @param request HTTP请求
     * @return 删除结果
     */
    @Operation(summary = "删除单词", description = "删除指定的单词")
    @DeleteMapping("/{wordId}")
    public ApiResponse<String> deleteWord(
            @Parameter(description = "单词ID") @PathVariable String wordId,
            HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        return wordService.deleteWord(wordId, userId);
    }

    /**
     * 搜索单词
     *
     * @param keyword 关键词
     * @param page 页码
     * @param size 每页大小
     * @param request HTTP请求
     * @return 搜索结果
     */
    @Operation(summary = "搜索单词", description = "根据关键词搜索单词（模糊匹配单词或释义）")
    @GetMapping("/search")
    public ApiResponse<List<Word>> searchWords(
            @Parameter(description = "关键词") @RequestParam String keyword,
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "100") int size,
            HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        return wordService.searchWords(userId, keyword, page, size);
    }

    /**
     * 更新掌握程度
     *
     * @param wordId 单词ID
     * @param masteryLevel 掌握程度（1-5）
     * @param request HTTP请求
     * @return 更新结果
     */
    @Operation(summary = "更新掌握程度", description = "更新单词的掌握等级（1-5星）")
    @PutMapping("/{wordId}/mastery")
    public ApiResponse<Word> updateMasteryLevel(
            @Parameter(description = "单词ID") @PathVariable String wordId,
            @Parameter(description = "掌握程度（1-5）") @RequestParam Integer masteryLevel,
            HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        return wordService.updateMasteryLevel(wordId, userId, masteryLevel);
    }

    /**
     * 统计单词数量
     *
     * @param status 状态（LEARNING/MASTERED）
     * @param request HTTP请求
     * @return 单词数量
     */
    @Operation(summary = "统计单词数量", description = "统计指定状态的单词数量")
    @GetMapping("/count")
    public ApiResponse<Long> countByStatus(
            @Parameter(description = "状态（LEARNING/MASTERED）") @RequestParam String status,
            HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        return wordService.countByStatus(userId, status);
    }
}

