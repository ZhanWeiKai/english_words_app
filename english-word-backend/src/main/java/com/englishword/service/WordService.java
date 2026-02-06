package com.englishword.service;

import com.englishword.dto.response.ApiResponse;
import com.englishword.entity.Word;
import com.englishword.repository.WordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 单词服务类
 */
@Service
@RequiredArgsConstructor
public class WordService {

    private final WordRepository wordRepository;

    /**
     * 根据ID获取单词
     */
    public ApiResponse<Word> getWordById(Long id) {
        return wordRepository.findById(id)
                .map(ApiResponse::success)
                .orElse(ApiResponse.error("单词不存在"));
    }

    /**
     * 根据单词查询
     */
    public ApiResponse<Word> getWordByWord(String word) {
        Word w = wordRepository.findByWord(word);
        if (w == null) {
            return ApiResponse.error("单词不存在");
        }
        return ApiResponse.success(w);
    }

    /**
     * 搜索单词
     */
    public ApiResponse<Page<Word>> searchWords(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Word> words = wordRepository.searchWords(keyword, pageable);
        return ApiResponse.success(words);
    }

    /**
     * 随机获取单词
     */
    public ApiResponse<List<Word>> getRandomWords(int limit) {
        List<Word> words = wordRepository.findRandomWords(limit);
        return ApiResponse.success(words);
    }

    /**
     * 添加单词
     */
    public ApiResponse<Word> addWord(Word word) {
        Word savedWord = wordRepository.save(word);
        return ApiResponse.success("添加成功", savedWord);
    }
}
