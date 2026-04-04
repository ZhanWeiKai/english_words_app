package com.englishword.controller;

import com.alibaba.fastjson2.JSONObject;
import com.englishword.dto.response.ApiResponse;
import com.englishword.entity.Sentence;
import com.englishword.service.SentenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 句子管理控制器
 *
 * 提供完整的 CRUD API：
 * - POST /api/sentences - 添加句子
 * - GET /api/sentences - 获取句子列表（分页、搜索）
 * - GET /api/sentences/{id} - 获取句子详情
 * - PUT /api/sentences/{id} - 更新句子
 * - DELETE /api/sentences/{id} - 删除句子
 * - GET /api/sentences/page - 获取分页信息
 * - GET /api/sentences/statistics - 获取统计信息
 */
@Tag(name = "句子管理", description = "句子的增删改查操作")
@RestController
@RequestMapping("/sentences")
@RequiredArgsConstructor
public class SentenceController {

    private final SentenceService sentenceService;

    /**
     * 添加句子
     *
     * @param sentence 句子对象
     * @param request HTTP请求
     * @return 添加结果
     */
    @Operation(summary = "添加句子", description = "创建新的句子记录（也可通过 AI Chat 调用 add_sentence 工具）")
    @PostMapping
    public ApiResponse<Sentence> addSentence(
            @RequestBody Sentence sentence,
            HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        return sentenceService.addSentence(
                userId,
                sentence.getEnglishText(),
                sentence.getChineseText(),
                sentence.getMarkedWords(),
                sentence.getSourceConversationId()
        );
    }

    /**
     * 获取句子列表
     *
     * @param keyword 搜索关键词（可选）
     * @param page 页码（从0开始，默认0）
     * @param size 每页大小（默认20）
     * @param request HTTP请求
     * @return 句子列表
     */
    @Operation(summary = "获取句子列表", description = "获取当前用户的句子列表，支持分页和搜索")
    @GetMapping
    public ApiResponse<List<Sentence>> getUserSentences(
            @Parameter(description = "搜索关键词（英文/中文）") @RequestParam(required = false) String keyword,
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        return sentenceService.getUserSentences(userId, keyword, page, size);
    }

    /**
     * 获取句子详情
     *
     * @param sentenceId 句子ID
     * @param request HTTP请求
     * @return 句子详情
     */
    @Operation(summary = "获取句子详情", description = "根据ID获取句子的详细信息")
    @GetMapping("/{sentenceId}")
    public ApiResponse<Sentence> getSentenceById(
            @Parameter(description = "句子ID") @PathVariable String sentenceId,
            HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        return sentenceService.getSentenceById(sentenceId, userId);
    }

    /**
     * 更新句子
     *
     * @param sentenceId 句子ID
     * @param sentence 更新的句子信息
     * @param request HTTP请求
     * @return 更新结果
     */
    @Operation(summary = "更新句子", description = "更新句子的内容（英文、中文、标记词）")
    @PutMapping("/{sentenceId}")
    public ApiResponse<Sentence> updateSentence(
            @Parameter(description = "句子ID") @PathVariable String sentenceId,
            @RequestBody Sentence sentence,
            HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        return sentenceService.updateSentence(sentenceId, userId, sentence);
    }

    /**
     * 删除句子
     *
     * @param sentenceId 句子ID
     * @param request HTTP请求
     * @return 删除结果
     */
    @Operation(summary = "删除句子", description = "删除指定的句子")
    @DeleteMapping("/{sentenceId}")
    public ApiResponse<String> deleteSentence(
            @Parameter(description = "句子ID") @PathVariable String sentenceId,
            HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        return sentenceService.deleteSentence(sentenceId, userId);
    }

    /**
     * 获取分页信息（包含总数）
     *
     * @param keyword 搜索关键词（可选）
     * @param page 页码
     * @param size 每页大小
     * @param request HTTP请求
     * @return 分页结果
     */
    @Operation(summary = "获取句子分页", description = "获取分页数据（包含总元素数、总页数）")
    @GetMapping("/page")
    public ApiResponse<Page<Sentence>> getUserSentencesPage(
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        Page<Sentence> pageResult = sentenceService.getUserSentencesPage(userId, keyword, page, size);
        return ApiResponse.success(pageResult);
    }

    /**
     * 获取统计信息
     *
     * @param request HTTP请求
     * @return 统计信息
     */
    @Operation(summary = "获取句子统计", description = "获取当前用户的句子总数")
    @GetMapping("/statistics")
    public JSONObject getSentenceStatistics(HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        return sentenceService.getSentenceStatistics(userId);
    }
}
