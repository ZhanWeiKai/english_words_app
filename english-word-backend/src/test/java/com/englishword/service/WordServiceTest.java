package com.englishword.service;

import com.englishword.entity.Word;
import com.englishword.repository.WordRepository;
import com.englishword.dto.response.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * WordService单元测试
 */
@ExtendWith(MockitoExtension.class)
class WordServiceTest {

    @Mock
    private WordRepository wordRepository;

    @InjectMocks
    private WordService wordService;

    private final String testUserId = "user_test_123";
    private Word testWord;

    @BeforeEach
    void setUp() {
        // 准备测试数据
        testWord = new Word();
        testWord.setWordId("word_test_001");
        testWord.setUserId(testUserId);
        testWord.setWord("ephemeral");
        testWord.setPronunciation("/ɪˈfemərəl/");
        testWord.setPartOfSpeech("adj.");
        testWord.setDefinition("短暂的；瞬息的");
        testWord.setMasteryLevel(2);
        testWord.setStatus("LEARNING");
    }

    @Test
    void testAddWord_Success() {
        // Mock repository行为
        when(wordRepository.save(any(Word.class))).thenReturn(testWord);

        // 调用服务
        ApiResponse<Word> response = wordService.addWord(testUserId, testWord);

        // 验证结果
        assertTrue(response.getCode() == 200);
        assertNotNull(response.getData());
        assertEquals("ephemeral", response.getData().getWord());
        assertEquals(testUserId, response.getData().getUserId());

        // 验证repository调用
        verify(wordRepository, times(1)).save(any(Word.class));
    }

    @Test
    void testGetWordById_Found() {
        // Mock repository行为
        when(wordRepository.findById("word_test_001")).thenReturn(Optional.of(testWord));

        // 调用服务
        ApiResponse<Word> response = wordService.getWordById("word_test_001", testUserId);

        // 验证结果
        assertTrue(response.getCode() == 200);
        assertNotNull(response.getData());
        assertEquals("ephemeral", response.getData().getWord());

        // 验证repository调用
        verify(wordRepository, times(1)).findById("word_test_001");
    }

    @Test
    void testGetWordById_NotFound() {
        // Mock repository行为
        when(wordRepository.findById("word_not_exist")).thenReturn(Optional.empty());

        // 调用服务
        ApiResponse<Word> response = wordService.getWordById("word_not_exist", testUserId);

        // 验证结果
        assertEquals(404, response.getCode());
        assertEquals("单词不存在", response.getMessage());
    }

    @Test
    void testGetWordById_NoPermission() {
        // 创建另一个用户的单词
        Word otherUserWord = new Word();
        otherUserWord.setWordId("word_test_002");
        otherUserWord.setUserId("other_user_id");

        // Mock repository行为
        when(wordRepository.findById("word_test_002")).thenReturn(Optional.of(otherUserWord));

        // 调用服务
        ApiResponse<Word> response = wordService.getWordById("word_test_002", testUserId);

        // 验证结果
        assertEquals(403, response.getCode());
        assertEquals("无权访问此单词", response.getMessage());
    }

    @Test
    void testUpdateMasteryLevel_Success() {
        // Mock repository行为
        when(wordRepository.findById("word_test_001")).thenReturn(Optional.of(testWord));
        when(wordRepository.save(any(Word.class))).thenReturn(testWord);

        // 调用服务
        ApiResponse<Word> response = wordService.updateMasteryLevel("word_test_001", testUserId, 5);

        // 验证结果
        assertTrue(response.getCode() == 200);
        assertEquals("MASTERED", response.getData().getStatus());

        // 验证repository调用
        verify(wordRepository, times(1)).findById("word_test_001");
        verify(wordRepository, times(1)).save(any(Word.class));
    }

    @Test
    void testUpdateMasteryLevel_InvalidLevel() {
        // 调用服务（掌握程度超出范围）
        ApiResponse<Word> response = wordService.updateMasteryLevel("word_test_001", testUserId, 6);

        // 验证结果
        assertEquals(400, response.getCode());
        assertEquals("掌握程度必须在1-5之间", response.getMessage());
    }

    @Test
    void testDeleteWord_Success() {
        // Mock repository行为
        when(wordRepository.findById("word_test_001")).thenReturn(Optional.of(testWord));
        doNothing().when(wordRepository).deleteById("word_test_001");

        // 调用服务
        ApiResponse<String> response = wordService.deleteWord("word_test_001", testUserId);

        // 验证结果
        assertTrue(response.getCode() == 200);
        assertEquals("删除成功", response.getMessage());

        // 验证repository调用
        verify(wordRepository, times(1)).findById("word_test_001");
        verify(wordRepository, times(1)).deleteById("word_test_001");
    }

    @Test
    void testCountByStatus() {
        // Mock repository行为
        when(wordRepository.countByUserIdAndStatus(testUserId, "LEARNING")).thenReturn(10L);

        // 调用服务
        ApiResponse<Long> response = wordService.countByStatus(testUserId, "LEARNING");

        // 验证结果
        assertTrue(response.getCode() == 200);
        assertEquals(10L, response.getData());

        // 验证repository调用
        verify(wordRepository, times(1)).countByUserIdAndStatus(testUserId, "LEARNING");
    }
}
