package com.englishword.service;

import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesisParam;
import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesizer;
import com.alibaba.dashscope.utils.Constants;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * 语音合成服务
 * 调用 DashScope CosyVoice 将文本转为语音，保存为 MP3 文件
 */
@Slf4j
@Service
public class TtsService {

    @Value("${dashscope.api-key:sk-f064b98676d8401fb6139d9a5e2cf78b}")
    private String apiKey;

    @Value("${dashscope.tts.model:cosyvoice-v3-flash}")
    private String model;

    @Value("${dashscope.tts.voice:longanyang}")
    private String defaultVoice;

    @Value("${tts.output-dir:./uploads/tts}")
    private String outputDir;

    @Value("${app.base-url:http://localhost:8885}")
    private String serverBaseUrl;

    @PostConstruct
    public void init() {
        Constants.baseWebsocketApiUrl = "wss://dashscope.aliyuncs.com/api-ws/v1/inference";
        try {
            Files.createDirectories(Paths.get(outputDir));
            log.info("[TTS] 初始化完成: model={}, voice={}, outputDir={}", model, defaultVoice, outputDir);
        } catch (Exception e) {
            log.error("[TTS] 创建输出目录失败", e);
        }
    }

    /**
     * 文本转语音
     *
     * @param text  要合成的文本
     * @param voice 音色（可选）
     * @return MP3 文件的访问 URL
     */
    public String synthesize(String text, String voice) throws Exception {
        String useVoice = (voice != null && !voice.isEmpty()) ? voice : defaultVoice;

        log.info("[TTS] 合成: text='{}', voice={}", text, useVoice);

        SpeechSynthesisParam param = SpeechSynthesisParam.builder()
                .apiKey(apiKey)
                .model(model)
                .voice(useVoice)
                .build();

        SpeechSynthesizer synthesizer = new SpeechSynthesizer(param, null);

        try {
            ByteBuffer audio = synthesizer.call(text);

            if (audio == null) {
                throw new RuntimeException("未收到音频数据");
            }

            byte[] audioBytes = new byte[audio.remaining()];
            audio.get(audioBytes);

            // 保存 MP3 到本地目录
            String filename = UUID.randomUUID().toString().replace("-", "") + ".mp3";
            try (FileOutputStream fos = new FileOutputStream(
                    Paths.get(outputDir).resolve(filename).toFile())) {
                fos.write(audioBytes);
            }

            String audioUrl = serverBaseUrl + "/api/tts/files/" + filename;

            log.info("[TTS] 完成: file={}, size={}, url={}", filename, audioBytes.length, audioUrl);

            return audioUrl;

        } finally {
            synthesizer.getDuplexApi().close(1000, "bye");
        }
    }
}
