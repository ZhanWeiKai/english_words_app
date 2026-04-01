package com.englishword.test;

import com.alibaba.dashscope.audio.asr.transcription.*;
import com.alibaba.dashscope.utils.Constants;
import com.google.gson.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;

/**
 * Fun-ASR 录音文件识别 Demo
 * 使用 DashScope Java SDK 调用 Fun-ASR 模型识别语音文件
 *
 * API Key: sk-f064b98676d8401fb6139d9a5e2cf78b
 * 模型: fun-asr-mtl (多语言)
 * 文档: https://help.aliyun.com/zh/model-studio/fun-asr-recorded-speech-recognition-java-sdk
 *
 * 注意: 录音文件识别不支持本地文件，需要提供公网可访问的 URL
 */
public class DemoFunAsr {

    private static final String API_KEY = "sk-f064b98676d8401fb6139d9a5e2cf78b";
    private static final String MODEL = "fun-asr-mtl"; // 多语言模型

    // 阿里云提供的示例音频文件
    private static final String SAMPLE_AUDIO_FEMALE =
            "https://dashscope.oss-cn-beijing.aliyuncs.com/samples/audio/paraformer/hello_world_female2.wav";
    private static final String SAMPLE_AUDIO_MALE =
            "https://dashscope.oss-cn-beijing.aliyuncs.com/samples/audio/paraformer/hello_world_male2.wav";

    /**
     * 方式1: 异步提交 + 同步等待（阻塞直到任务完成）
     * 适合单个文件识别，简单直接
     */
    public static void demoAsyncWait() {
        System.out.println("=== Demo 1: 异步提交 + 同步等待 ===");

        TranscriptionParam param = TranscriptionParam.builder()
                .apiKey(API_KEY)
                .model(MODEL)
                .fileUrls(Arrays.asList(SAMPLE_AUDIO_FEMALE))
                .build();

        try {
            Transcription transcription = new Transcription();

            // 1. 异步提交任务
            TranscriptionResult result = transcription.asyncCall(param);
            System.out.println("  任务已提交, requestId: " + result.getRequestId());
            System.out.println("  taskId: " + result.getTaskId());

            // 2. 阻塞等待任务完成
            result = transcription.wait(
                    TranscriptionQueryParam.FromTranscriptionParam(param, result.getTaskId()));

            // 3. 打印任务状态
            System.out.println("\n  === 任务状态 ===");
            System.out.println(new GsonBuilder().setPrettyPrinting().create().toJson(result.getOutput()));

            // 4. 获取并打印实际识别文本
            if (result.getResults() != null) {
                System.out.println("\n  === 识别文本 ===");
                for (TranscriptionTaskResult taskResult : result.getResults()) {
                    if (taskResult.getTranscriptionUrl() != null) {
                        String text = fetchTranscriptionText(taskResult.getTranscriptionUrl());
                        System.out.println("  文件: " + taskResult.getFileUrl());
                        System.out.println("  识别文本: " + text);
                    }
                }
            }

        } catch (Exception e) {
            System.out.println("[Error] " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 从转录结果URL读取实际识别文本
     */
    private static String fetchTranscriptionText(String transcriptionUrl) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(transcriptionUrl).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();

            // 解析JSON提取text字段
            JsonObject json = JsonParser.parseString(sb.toString()).getAsJsonObject();
            JsonArray transcripts = json.getAsJsonArray("transcripts");
            StringBuilder texts = new StringBuilder();
            for (int i = 0; i < transcripts.size(); i++) {
                if (i > 0) texts.append(" | ");
                texts.append(transcripts.get(i).getAsJsonObject().get("text").getAsString());
            }
            return texts.toString();
        } catch (Exception e) {
            return "[读取失败: " + e.getMessage() + "]";
        }
    }

    /**
     * 方式2: 异步提交 + 轮询查询（非阻塞）
     * 适合批量文件识别，可以自行控制轮询间隔
     */
    public static void demoAsyncPolling() {
        System.out.println("\n=== Demo 2: 异步提交 + 轮询查询 (多个文件) ===");

        TranscriptionParam param = TranscriptionParam.builder()
                .apiKey(API_KEY)
                .model(MODEL)
                .fileUrls(Arrays.asList(SAMPLE_AUDIO_FEMALE, SAMPLE_AUDIO_MALE))
                .build();

        try {
            Transcription transcription = new Transcription();

            // 1. 提交任务
            TranscriptionResult result = transcription.asyncCall(param);
            System.out.println("  任务已提交, requestId: " + result.getRequestId());
            System.out.println("  taskId: " + result.getTaskId());

            // 2. 轮询查询结果
            int pollCount = 0;
            while (true) {
                pollCount++;
                result = transcription.fetch(
                        TranscriptionQueryParam.FromTranscriptionParam(param, result.getTaskId()));

                System.out.println("  第 " + pollCount + " 次查询, 状态: " + result.getTaskStatus());

                if (result.getTaskStatus() != null
                        && (result.getTaskStatus().name().equals("SUCCEEDED")
                        || result.getTaskStatus().name().equals("FAILED"))) {
                    break;
                }

                Thread.sleep(2000); // 每2秒查询一次
            }

            // 3. 打印识别结果
            System.out.println("\n  === 识别结果 ===");
            System.out.println(new GsonBuilder().setPrettyPrinting().create().toJson(result.getOutput()));

            // 4. 打印每个子任务的结果链接
            if (result.getResults() != null) {
                System.out.println("\n  === 子任务结果 ===");
                for (TranscriptionTaskResult taskResult : result.getResults()) {
                    System.out.println("  文件: " + taskResult.getFileUrl());
                    System.out.println("  状态: " + taskResult.getSubTaskStatus());
                    System.out.println("  结果URL: " + taskResult.getTranscriptionUrl());
                    System.out.println();
                }
            }

        } catch (Exception e) {
            System.out.println("[Error] " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 方式3: 指定语言识别
     * fun-asr-mtl 支持: zh(中文), en(英文), ja(日语), ko(韩语), vi(越南语) 等31种语言
     */
    public static void demoWithLanguageHint() {
        System.out.println("\n=== Demo 3: 指定语言 (英文) ===");

        TranscriptionParam param = TranscriptionParam.builder()
                .apiKey(API_KEY)
                .model(MODEL)
                .fileUrls(Arrays.asList(SAMPLE_AUDIO_FEMALE))
                .parameter("language_hints", new String[]{"en"}) // 指定英文
                .build();

        try {
            Transcription transcription = new Transcription();
            TranscriptionResult result = transcription.asyncCall(param);
            System.out.println("  任务已提交, taskId: " + result.getTaskId());

            result = transcription.wait(
                    TranscriptionQueryParam.FromTranscriptionParam(param, result.getTaskId()));

            System.out.println("\n  === 识别结果 ===");
            System.out.println(new GsonBuilder().setPrettyPrinting().create().toJson(result.getOutput()));

        } catch (Exception e) {
            System.out.println("[Error] " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // 设置 HTTP API 地址（北京地域）
        Constants.baseHttpApiUrl = "https://dashscope.aliyuncs.com/api/v1";

        System.out.println("===========================================");
        System.out.println("Fun-ASR 录音文件识别 Demo");
        System.out.println("模型: " + MODEL);
        System.out.println("===========================================\n");

        // Demo 1: 同步等待（最简单，推荐先用这个测试）
        demoAsyncWait();

        // Demo 2: 轮询查询（批量文件）
        // demoAsyncPolling();

        // Demo 3: 指定语言
        // demoWithLanguageHint();

        System.out.println("\n=== 全部 Demo 完成 ===");
        System.exit(0);
    }
}
