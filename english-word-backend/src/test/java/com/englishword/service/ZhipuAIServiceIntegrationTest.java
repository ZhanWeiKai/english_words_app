package com.englishword.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ZhipuAIService集成测试
 *
 * 测试智谱AI API是否能正常调用并返回结果
 */
@Slf4j
@SpringBootTest
class ZhipuAIServiceIntegrationTest {

    @Autowired
    private ZhipuAIService zhipuAIService;

    @BeforeEach
    void setUp() {
        log.info("========================================");
        log.info("开始 ZhipuAI 集成测试");
        log.info("========================================");
    }

    @Test
    void testCallZhipuAI_Directly() {
        log.info("\n【测试1】直接调用 callZhipuAI 方法");

        String systemPrompt = "你是一个友好的英语学习助手。";
        String userMessage = "Hello, how are you?";
        String conversationHistory = null;

        log.info("系统提示词: {}", systemPrompt);
        log.info("用户消息: {}", userMessage);
        log.info("对话历史: {}", conversationHistory);

        try {
            // 这里我们需要通过反射或者创建一个公共的包装方法来测试
            // 由于callZhipuAI是private的，我们测试公共方法
            log.info("callZhipuAI 是私有方法，将通过公共方法测试");
        } catch (Exception e) {
            log.error("测试失败", e);
            fail("测试失败: " + e.getMessage());
        }
    }

    @Test
    void testExplainWord() {
        log.info("\n【测试2】Word Inquiry模式 - 单词讲解");

        String word = "serendipity";
        String conversationHistory = null;

        log.info("测试单词: {}", word);
        log.info("开始调用智谱AI...");

        String response = zhipuAIService.explainWord(word, conversationHistory);

        log.info("========================================");
        log.info("AI回复内容:");
        log.info("========================================");
        log.info(response);
        log.info("========================================");

        // 验证响应不为空
        assertNotNull(response, "AI响应不应为null");
        assertFalse(response.trim().isEmpty(), "AI响应不应为空字符串");

        // 验证响应包含关键内容
        assertTrue(
            response.toLowerCase().contains(word) ||
            response.contains("单词") ||
            response.contains("meaning") ||
            response.length() > 50,
            "AI响应应该包含相关内容或足够详细"
        );

        log.info("✅ 测试通过 - AI成功返回单词讲解");
    }

    @Test
    void testPracticeInScenario() {
        log.info("\n【测试3】Word Training模式 - 场景训练");

        String targetWord = "ephemeral";
        String scenario = "You're at a coffee shop talking about social media trends.";
        String conversationHistory = null;

        log.info("目标单词: {}", targetWord);
        log.info("场景描述: {}", scenario);
        log.info("开始调用智谱AI...");

        String response = zhipuAIService.practiceInScenario(targetWord, scenario, conversationHistory);

        log.info("========================================");
        log.info("AI回复内容:");
        log.info("========================================");
        log.info(response);
        log.info("========================================");

        // 验证响应不为空
        assertNotNull(response, "AI响应不应为null");
        assertFalse(response.trim().isEmpty(), "AI响应不应为空字符串");

        // 验证响应内容合理
        assertTrue(
            response.length() > 20,
            "AI响应应该有足够的内容"
        );

        log.info("✅ 测试通过 - AI成功返回场景训练对话");
    }

    @Test
    void testChat() {
        log.info("\n【测试4】通用对话模式");

        String message = "Can you explain the difference between 'affect' and 'effect'?";
        String conversationHistory = null;
        String systemPrompt = "你是一个专业的英语老师，请用简洁明了的方式回答问题。";

        log.info("用户消息: {}", message);
        log.info("系统提示: {}", systemPrompt);
        log.info("开始调用智谱AI...");

        String response = zhipuAIService.chat(message, conversationHistory, systemPrompt);

        log.info("========================================");
        log.info("AI回复内容:");
        log.info("========================================");
        log.info(response);
        log.info("========================================");

        // 验证响应不为空
        assertNotNull(response, "AI响应不应为null");
        assertFalse(response.trim().isEmpty(), "AI响应不应为空字符串");

        // 验证响应长度合理
        assertTrue(
            response.length() > 30,
            "AI响应应该有足够的解释内容"
        );

        log.info("✅ 测试通过 - AI成功返回对话回复");
    }

    @Test
    void testExplainWord_MultipleWords() {
        log.info("\n【测试5】批量测试单词讲解");

        String[] testWords = {
            "epiphany",
            "ubiquitous",
            "serendipity"
        };

        for (String word : testWords) {
            log.info("\n--- 测试单词: {} ---", word);

            String response = zhipuAIService.explainWord(word, null);

            log.info("AI回复摘要 (前200字符): {}",
                response.length() > 200 ? response.substring(0, 200) + "..." : response);

            assertNotNull(response, "AI响应不应为null");
            assertFalse(response.trim().isEmpty(), "AI响应不应为空");
            assertTrue(response.length() > 50, "AI响应应该足够详细");

            log.info("✅ 单词 {} 测试通过", word);
        }

        log.info("\n✅ 批量测试完成 - 所有单词都成功获取AI回复");
    }

    @Test
    void testExplainWord_WithConversationHistory() {
        log.info("\n【测试6】带对话历史的单词讲解");

        String word = "practice";
        String conversationHistory = """
            [
                {"role": "user", "content": "What does practice mean?"},
                {"role": "assistant", "content": "Practice means doing something regularly to improve."}
            ]
            """;

        log.info("测试单词: {}", word);
        log.info("对话历史: {}", conversationHistory);
        log.info("开始调用智谱AI...");

        String response = zhipuAIService.explainWord(word, conversationHistory);

        log.info("========================================");
        log.info("AI回复内容:");
        log.info("========================================");
        log.info(response);
        log.info("========================================");

        assertNotNull(response, "AI响应不应为null");
        assertFalse(response.trim().isEmpty(), "AI响应不应为空字符串");

        log.info("✅ 测试通过 - AI成功处理带历史记录的对话");
    }
}
