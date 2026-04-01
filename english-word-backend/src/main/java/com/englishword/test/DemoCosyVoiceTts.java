package com.englishword.test;

import com.alibaba.dashscope.audio.tts.SpeechSynthesisResult;
import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesisParam;
import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesizer;
import com.alibaba.dashscope.common.ResultCallback;
import com.alibaba.dashscope.utils.Constants;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;

/**
 * CosyVoice TTS (语音合成) Demo
 * 使用 DashScope Java SDK 调用 CosyVoice 模型将文本转为语音
 *
 * API Key: sk-f064b98676d8401fb6139d9a5e2cf78b
 * 模型: qwen3-tts-vd-2026-01-26
 * 文档: https://help.aliyun.com/zh/model-studio/cosyvoice-java-sdk
 */
public class DemoCosyVoiceTts {

    private static final String API_KEY = "sk-f064b98676d8401fb6139d9a5e2cf78b";
    private static final String MODEL = "cosyvoice-v3-flash"; // 可选: cosyvoice-v3-flash, cosyvoice-v3-plus, qwen3-tts-flash
    private static final String VOICE = "longanyang"; // 龙昂扬音色

    /**
     * 方式1: 非流式调用（同步阻塞，适合短文本）
     * 一次性发送完整文本，直接返回完整音频
     */
    public static void demoSync() {
        System.out.println("=== Demo 1: 非流式调用 (同步) ===");

        SpeechSynthesisParam param = SpeechSynthesisParam.builder()
                .apiKey(API_KEY)
                .model(MODEL)
                .voice(VOICE)
                .build();

        // 第二个参数 null 表示同步模式
        SpeechSynthesizer synthesizer = new SpeechSynthesizer(param, null);

        try {
            // 阻塞直到音频返回
            ByteBuffer audio = synthesizer.call("Hello! Welcome to the English Word App. 今天天气真不错！");

            if (audio != null) {
                // 保存到文件
                File file = new File("tts_output_sync.mp3");
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    fos.write(audio.array());
                }
                System.out.println("[Success] 音频已保存到: " + file.getAbsolutePath());
                System.out.println("[Metric] requestId: " + synthesizer.getLastRequestId()
                        + ", 首包延迟: " + synthesizer.getFirstPackageDelay() + "ms");
            } else {
                System.out.println("[Error] 未收到音频数据");
            }
        } catch (Exception e) {
            System.out.println("[Error] " + e.getMessage());
            e.printStackTrace();
        } finally {
            synthesizer.getDuplexApi().close(1000, "bye");
        }
    }

    /**
     * 方式2: 单向流式调用（异步回调，适合需要实时播放的场景）
     * 一次性发送文本，通过回调分片接收音频数据
     */
    public static void demoStreaming() {
        System.out.println("\n=== Demo 2: 单向流式调用 (异步回调) ===");

        CountDownLatch latch = new CountDownLatch(1);
        StringBuilder allAudio = new StringBuilder();

        ResultCallback<SpeechSynthesisResult> callback = new ResultCallback<SpeechSynthesisResult>() {
            @Override
            public void onEvent(SpeechSynthesisResult result) {
                if (result.getAudioFrame() != null) {
                    allAudio.append("片段+");
                    System.out.println("  [收到音频片段] 大小: " + result.getAudioFrame().remaining() + " bytes");
                }
            }

            @Override
            public void onComplete() {
                System.out.println("  [完成] 语音合成结束");
                latch.countDown();
            }

            @Override
            public void onError(Exception e) {
                System.out.println("  [错误] " + e.getMessage());
                latch.countDown();
            }
        };

        SpeechSynthesisParam param = SpeechSynthesisParam.builder()
                .apiKey(API_KEY)
                .model(MODEL)
                .voice(VOICE)
                .build();

        SpeechSynthesizer synthesizer = new SpeechSynthesizer(param, callback);

        try {
            synthesizer.call("This is a streaming text-to-speech demo. 这是流式语音合成的演示。");
            latch.await(); // 等待合成完成
            System.out.println("[Metric] requestId: " + synthesizer.getLastRequestId()
                    + ", 首包延迟: " + synthesizer.getFirstPackageDelay() + "ms");
        } catch (Exception e) {
            System.out.println("[Error] " + e.getMessage());
            e.printStackTrace();
        } finally {
            synthesizer.getDuplexApi().close(1000, "bye");
        }
    }

    /**
     * 方式3: 双向流式调用（可分多次发送文本，适合配合 LLM 流式输出）
     * 多次调用 streamingCall 发送文本片段，通过回调实时接收音频
     */
    public static void demoBidirectionalStreaming() {
        System.out.println("\n=== Demo 3: 双向流式调用 (分片输入) ===");

        CountDownLatch latch = new CountDownLatch(1);

        ResultCallback<SpeechSynthesisResult> callback = new ResultCallback<SpeechSynthesisResult>() {
            @Override
            public void onEvent(SpeechSynthesisResult result) {
                if (result.getAudioFrame() != null) {
                    System.out.println("  [收到音频片段] 大小: " + result.getAudioFrame().remaining() + " bytes");
                }
            }

            @Override
            public void onComplete() {
                System.out.println("  [完成] 双向流式语音合成结束");
                latch.countDown();
            }

            @Override
            public void onError(Exception e) {
                System.out.println("  [错误] " + e.getMessage());
                latch.countDown();
            }
        };

        SpeechSynthesisParam param = SpeechSynthesisParam.builder()
                .apiKey(API_KEY)
                .model(MODEL)
                .voice(VOICE)
                .build();

        SpeechSynthesizer synthesizer = new SpeechSynthesizer(param, callback);

        try {
            // 模拟 LLM 流式输出的文本片段
            String[] textChunks = {
                    "Hello, ",
                    "this is a bidirectional ",
                    "streaming TTS demo. ",
                    "You can send text ",
                    "in multiple chunks."
            };

            for (String chunk : textChunks) {
                System.out.println("  [发送文本] " + chunk);
                synthesizer.streamingCall(chunk);
                Thread.sleep(500); // 模拟 LLM 生成延迟
            }

            // 必须调用 streamingComplete 结束流式合成
            synthesizer.streamingComplete();
            latch.await();

            System.out.println("[Metric] requestId: " + synthesizer.getLastRequestId()
                    + ", 首包延迟: " + synthesizer.getFirstPackageDelay() + "ms");
        } catch (Exception e) {
            System.out.println("[Error] " + e.getMessage());
            e.printStackTrace();
        } finally {
            synthesizer.getDuplexApi().close(1000, "bye");
        }
    }

    public static void main(String[] args) {
        // 设置 WebSocket API 地址（北京地域）
        Constants.baseWebsocketApiUrl = "wss://dashscope.aliyuncs.com/api-ws/v1/inference";

        System.out.println("===========================================");
        System.out.println("CosyVoice TTS Demo");
        System.out.println("模型: " + MODEL);
        System.out.println("音色: " + VOICE);
        System.out.println("===========================================\n");

        // Demo 1: 非流式（最简单，推荐先用这个测试）
        demoSync();

        // Demo 2: 单向流式
        // demoStreaming();

        // Demo 3: 双向流式（配合 LLM 使用）
        // demoBidirectionalStreaming();

        System.out.println("\n=== 全部 Demo 完成 ===");
        System.exit(0);
    }
}
