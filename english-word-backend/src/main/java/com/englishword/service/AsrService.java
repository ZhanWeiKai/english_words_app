package com.englishword.service;

import com.alibaba.dashscope.audio.asr.transcription.*;
import com.alibaba.dashscope.utils.Constants;
import com.google.gson.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.UUID;

/**
 * 语音识别服务
 * 接收 Android 上传的音频文件，调用 DashScope Fun-ASR 进行识别
 */
@Slf4j
@Service
public class AsrService {

    @Value("${dashscope.api-key:sk-f064b98676d8401fb6139d9a5e2cf78b}")
    private String apiKey;

    @Value("${dashscope.asr.model:fun-asr-mtl}")
    private String model;

    @Value("${asr.upload-dir:./uploads/asr}")
    private String uploadDir;

    @Value("${app.base-url:http://119.91.206.195:8885}")
    private String serverBaseUrl;

    @PostConstruct
    public void init() {
        Constants.baseHttpApiUrl = "https://dashscope.aliyuncs.com/api/v1";
        try {
            Files.createDirectories(Paths.get(uploadDir));
            log.info("ASR上传目录已创建: {}", uploadDir);
        } catch (IOException e) {
            log.error("创建ASR上传目录失败", e);
        }
    }

    /**
     * 识别音频文件
     *
     * @param file     音频文件
     * @param language 语言提示（可选：en, zh, ja 等）
     * @return 识别出的文本
     */
    public String recognize(MultipartFile file, String language) throws Exception {
        // 1. 确保上传目录存在
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath();
        Files.createDirectories(uploadPath);

        // 2. 保存上传文件到本地
        String originalFilename = file.getOriginalFilename();
        String ext = getFileExtension(originalFilename);
        String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path filePath = uploadPath.resolve(filename);
        file.transferTo(filePath.toFile());
        log.info("音频文件已保存: {}", filePath);

        try {
            // 2. 生成公网可访问的URL
            String fileUrl = serverBaseUrl + "/api/asr/files/" + filename;
            log.info("文件访问URL: {}", fileUrl);

            // 3. 调用 DashScope ASR
            String text = callDashScopeAsr(fileUrl, language);
            log.info("识别结果: {}", text);

            return text;
        } finally {
            // 4. 识别完成后删除临时文件
            try {
                Files.deleteIfExists(filePath);
                log.info("临时文件已删除: {}", filename);
            } catch (IOException e) {
                log.warn("删除临时文件失败: {}", filename, e);
            }
        }
    }

    /**
     * 调用 DashScope Fun-ASR 录音文件识别
     */
    private String callDashScopeAsr(String fileUrl, String language) throws Exception {
        TranscriptionParam.TranscriptionParamBuilder paramBuilder = TranscriptionParam.builder()
                .apiKey(apiKey)
                .model(model)
                .fileUrls(Arrays.asList(fileUrl));

        // 设置语言提示
        if (language != null && !language.isEmpty()) {
            paramBuilder.parameter("language_hints", new String[]{language});
        }

        TranscriptionParam param = paramBuilder.build();
        Transcription transcription = new Transcription();

        // 提交任务
        TranscriptionResult result = transcription.asyncCall(param);
        log.info("ASR任务已提交, taskId: {}", result.getTaskId());

        // 阻塞等待结果
        result = transcription.wait(
                TranscriptionQueryParam.FromTranscriptionParam(param, result.getTaskId()));

        // 检查任务状态
        if (result.getResults() == null || result.getResults().isEmpty()) {
            throw new RuntimeException("ASR识别失败: 无结果返回");
        }

        TranscriptionTaskResult taskResult = result.getResults().get(0);
        if (taskResult.getSubTaskStatus() != null
                && taskResult.getSubTaskStatus().name().equals("FAILED")) {
            throw new RuntimeException("ASR识别失败: " + taskResult.getMessage());
        }

        // 获取识别结果URL并解析文本
        String transcriptionUrl = taskResult.getTranscriptionUrl();
        return fetchTranscriptionText(transcriptionUrl);
    }

    /**
     * 从转录结果URL读取识别文本
     */
    private String fetchTranscriptionText(String transcriptionUrl) throws Exception {
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection)
                new java.net.URL(transcriptionUrl).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);

        try (java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(conn.getInputStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            JsonObject json = JsonParser.parseString(sb.toString()).getAsJsonObject();
            JsonArray transcripts = json.getAsJsonArray("transcripts");

            StringBuilder texts = new StringBuilder();
            for (int i = 0; i < transcripts.size(); i++) {
                if (i > 0) texts.append(" ");
                texts.append(transcripts.get(i).getAsJsonObject().get("text").getAsString());
            }
            return texts.toString();
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "wav";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
}
