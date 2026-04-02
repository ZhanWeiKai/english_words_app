package com.englishword.controller;

import com.englishword.service.TtsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 语音合成接口
 * 接收文本，保存为 MP3 文件，返回文件访问 URL
 */
@Slf4j
@RestController
@RequestMapping("/tts")
@Tag(name = "语音合成", description = "TTS 文本转语音接口")
@RequiredArgsConstructor
public class TtsController {

    private final TtsService ttsService;

    /**
     * 文本转语音
     *
     * POST /api/tts/synthesize
     *
     * @param text  要合成的文本
     * @param voice 音色（可选，默认 longanyang）
     * @return 音频文件 URL
     */
    @Operation(summary = "文本转语音", description = "将文本合成为 MP3 音频，返回文件 URL")
    @PostMapping("/synthesize")
    public ResponseEntity<?> synthesize(
            @Parameter(description = "要合成的文本") @RequestParam("text") String text,
            @Parameter(description = "音色（可选）") @RequestParam(value = "voice", required = false) String voice) {

        if (text == null || text.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "文本不能为空"));
        }

        log.info("收到TTS请求: text='{}', voice={}", text, voice);

        try {
            String audioUrl = ttsService.synthesize(text, voice);
            return ResponseEntity.ok(Map.of(
                    "audioUrl", audioUrl,
                    "text", text
            ));
        } catch (Exception e) {
            log.error("TTS合成失败", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "TTS合成失败: " + e.getMessage()));
        }
    }
}
