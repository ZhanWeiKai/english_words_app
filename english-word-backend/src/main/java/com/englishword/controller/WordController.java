package com.englishword.controller;

import com.englishword.dto.response.ApiResponse;
import com.englishword.entity.Word;
import com.englishword.service.WordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 单词控制器
 */
@Tag(name = "单词管理", description = "单词相关接口")
@RestController
@RequestMapping("/words")
@RequiredArgsConstructor
public class WordController {

    private final WordService wordService;

    @Operation(summary = "根据ID获取单词")
    @GetMapping("/{id}")
    public ApiResponse<Word> getWordById(
            @Parameter(description = "单词ID") @PathVariable Long id) {
        return wordService.getWordById(id);
    }

    @Operation(summary = "根据单词查询")
    @GetMapping("/word/{word}")
    public ApiResponse<Word> getWordByWord(
            @Parameter(description = "单词") @PathVariable String word) {
        return wordService.getWordByWord(word);
    }

    @Operation(summary = "搜索单词")
    @GetMapping("/search")
    public ApiResponse<Page<Word>> searchWords(
            @Parameter(description = "关键词") @RequestParam String keyword,
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size) {
        return wordService.searchWords(keyword, page, size);
    }

    @Operation(summary = "随机获取单词")
    @GetMapping("/random")
    public ApiResponse<List<Word>> getRandomWords(
            @Parameter(description = "数量") @RequestParam(defaultValue = "10") int limit) {
        return wordService.getRandomWords(limit);
    }

    @Operation(summary = "添加单词")
    @PostMapping
    public ApiResponse<Word> addWord(@RequestBody Word word) {
        return wordService.addWord(word);
    }
}
