# ASR 语音识别后端接口实施方案

## 概述

在 Spring Boot 后端新增 ASR（语音识别）接口，Android 端上传录音文件，后端调用阿里云 DashScope Fun-ASR 模型识别语音内容并返回文本。

## 核心问题

DashScope ASR **不支持本地文件直传**，必须传公网 URL。所以后端需要：

```
Android 上传音频 → 后端保存文件 → 生成公网URL → 调DashScope → 返回识别文本
```

## 接口设计

### 请求

```
POST /api/asr/recognize
Content-Type: multipart/form-data
Authorization: Bearer <token>

参数:
- file: 音频文件 (m4a/wav/mp3)
- language: 可选，"en" | "zh"，默认自动识别
```

### 响应

```json
{
  "text": "hello world",
  "language": "en",
  "duration": 3.8
}
```

## 需要新增的文件

| 文件 | 作用 |
|---|---|
| `AsrController.java` | 接收音频文件上传 |
| `AsrService.java` | 保存文件 + 调用 DashScope ASR |
| Spring WebMvc 配置 | 暴露文件目录为静态资源，让 DashScope 能下载 |

## 文件怎么变成公网 URL

服务器 `119.91.206.195` 本身就是公网的，最简单的方式：

```
上传文件 → 保存到 ./uploads/asr/xxx.m4a
       → 生成URL: http://119.91.206.195:8885/api/asr/files/xxx.m4a
       → DashScope 通过这个URL下载音频并识别
       → 识别完成后删除临时文件
```

不需要额外接入 OSS，服务器本身就是公网可访问的。

## 调用时序

```
Android                Backend                    DashScope
  |                       |                          |
  |--- POST 音频文件 ---->|                          |
  |                       |--- 保存到本地            |
  |                       |--- 生成公网URL --------->|
  |                       |       (提交识别任务)      |
  |                       |<--- taskId --------------|
  |                       |--- 轮询结果 ----------->|
  |                       |<--- 识别文本 ------------|
  |                       |--- 删除临时文件          |
  |<-- 返回识别文本 ------|                          |
```

## 技术选型

- **模型**: `fun-asr-mtl`（多语言，支持中文、英文、日语等31种语言）
- **SDK**: DashScope Java SDK 2.21.9（已在 pom.xml 中配置）
- **文件格式**: Android 默认录音格式 M4A（AAC 编码），DashScope 支持
- **预估耗时**: 总共约 3-5 秒

## Android 端录音说明

Android 使用 `MediaRecorder` 录音，默认生成 M4A 格式（AAC 编码）：

```kotlin
val recorder = MediaRecorder().apply {
    setAudioSource(MediaRecorder.AudioSource.MIC)
    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)  // M4A
    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
    setOutputFile(outputFile.absolutePath)
    prepare()
    start()
}
```

DashScope Fun-ASR 支持的格式：aac, amr, flac, m4a, mp3, ogg, opus, wav, webm 等
