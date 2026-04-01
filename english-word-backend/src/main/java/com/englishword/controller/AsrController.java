package com.englishword.controller;

import com.englishword.service.AsrService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 语音识别接口
 * Android 录音后上传音频文件，后端调用 DashScope Fun-ASR 识别
 */
@Slf4j
@RestController
@RequestMapping("/asr")
@RequiredArgsConstructor
public class AsrController {

    private final AsrService asrService;

    /**
     * 语音识别
     *
     * POST /api/asr/recognize
     * Content-Type: multipart/form-data
     *
     * @param file     音频文件（m4a/wav/mp3等）
     * @param language 语言提示（可选: en, zh, ja），不传则自动识别
     * @return 识别出的文本
     */
    @PostMapping("/recognize")
    public ResponseEntity<?> recognize(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "language", required = false) String language) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "音频文件不能为空"));
        }

        log.info("收到语音识别请求, 文件名: {}, 大小: {} bytes, 语言: {}",
                file.getOriginalFilename(), file.getSize(), language);

        try {
            String text = asrService.recognize(file, language);
            return ResponseEntity.ok(Map.of(
                    "text", text,
                    "language", language != null ? language : "auto"
            ));
        } catch (Exception e) {
            log.error("语音识别失败", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "语音识别失败: " + e.getMessage()));
        }
    }
}
