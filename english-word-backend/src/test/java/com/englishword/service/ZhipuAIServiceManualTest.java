package com.englishword.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 手动测试类 - 在IDEA中右键运行此测试
 *
 * 运行方式：
 * 1. 在IDEA中打开此文件
 * 2. 右键点击类名或方法名
 * 3. 选择 "Run 'ZhipuAIServiceManualTest'"
 */
@Slf4j
@SpringBootTest
public class ZhipuAIServiceManualTest {

    @Autowired
    private ZhipuAIService zhipuAIService;

    /**
     * 快速测试 - 单词讲解
     * 在IDEA中右键此方法选择运行
     */
    @Test
    public void quickTest() {
        log.info("\n╔════════════════════════════════════════════════════════════╗");
        log.info("║          ZhipuAI 快速测试 - 单词讲解                        ║");
        log.info("╚════════════════════════════════════════════════════════════╝");

        String word = "serendipity";
        log.info("\n📝 测试单词: {}", word);
        log.info("⏳ 正在调用智谱AI API...\n");

        long startTime = System.currentTimeMillis();
        String response = zhipuAIService.explainWord(word, null);
        long endTime = System.currentTimeMillis();

        log.info("✅ API调用成功!");
        log.info("⏱️  响应时间: {} ms", (endTime - startTime));
        log.info("\n═══════════════════════════════════════════════════════════");
        log.info("🤖 AI回复内容:");
        log.info("═══════════════════════════════════════════════════════════");
        log.info("\n{}\n", response);
        log.info("═══════════════════════════════════════════════════════════");
        log.info("📊 统计信息:");
        log.info("   - 响应长度: {} 字符", response.length());
        log.info("   - 响应时间: {} ms", (endTime - startTime));
        log.info("═══════════════════════════════════════════════════════════");

        assert response != null && !response.trim().isEmpty() : "AI响应为空！";
        log.info("\n✅ 测试通过！AI能够正常返回内容。");
    }

    /**
     * 测试场景训练模式
     */
    @Test
    public void testTrainingMode() {
        log.info("\n╔════════════════════════════════════════════════════════════╗");
        log.info("║          ZhipuAI 测试 - 场景训练模式                        ║");
        log.info("╚════════════════════════════════════════════════════════════╝");

        String word = "ephemeral";
        String scenario = "在咖啡店谈论社交媒体趋势";

        log.info("\n🎯 目标单词: {}", word);
        log.info("🎬 场景描述: {}", scenario);
        log.info("⏳ 正在调用智谱AI API...\n");

        long startTime = System.currentTimeMillis();
        String response = zhipuAIService.practiceInScenario(word, scenario, null);
        long endTime = System.currentTimeMillis();

        log.info("✅ API调用成功!");
        log.info("⏱️  响应时间: {} ms", (endTime - startTime));
        log.info("\n═══════════════════════════════════════════════════════════");
        log.info("🤖 AI回复内容:");
        log.info("═══════════════════════════════════════════════════════════");
        log.info("\n{}\n", response);
        log.info("═══════════════════════════════════════════════════════════");

        assert response != null && !response.trim().isEmpty() : "AI响应为空！";
        log.info("\n✅ 测试通过！训练模式AI能够正常返回内容。");
    }
}
