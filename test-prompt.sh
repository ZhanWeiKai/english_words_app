#!/bin/bash
# Test ZhipuAI training prompt directly

API_KEY="cea9d940b7b7498d916e1c924ba3b6ca.zwaG7aTXwBW60Dr4"
API_URL="https://open.bigmodel.cn/api/paas/v4/chat/completions"

TRAINING_WORDS="allocate / inevitable / sustainable"

SYSTEM_PROMPT="你是一位专业的雅思口语考官，正在帮助用户练习使用目标单词。

## 本轮训练考词
${TRAINING_WORDS}

## 【重要】你必须严格遵守以下输出格式

每次提问时，你必须按以下格式输出，缺一不可：

\`\`\`
👨‍🏫 Examiner: [英文问题]
中文：[中文翻译]

（必须使用考词：[单词] /[音标]/ ＝ [中文含义]
常见搭配：[搭配1] / [搭配2]）

你来回答。
\`\`\`

## 格式说明（必须遵守）
1. 👨‍🏫 Examiner: 后面必须是完整的英文问题
2. 中文：后面必须是该问题的完整中文翻译
3. 括号内必须包含：单词、音标、中文含义、常见搭配
4. 每个问题只针对一个考词
5. 不评分、不总结

请开始第一轮训练！用第一个考词提问。"

curl -s "$API_URL" \
  -H "Authorization: Bearer $API_KEY" \
  -H "Content-Type: application/json" \
  -d "{
    \"model\": \"glm-4-flash\",
    \"messages\": [
      {\"role\": \"system\", \"content\": \"$SYSTEM_PROMPT\"},
      {\"role\": \"user\", \"content\": \"Let's start!\"}
    ],
    \"temperature\": 0.7,
    \"max_tokens\": 1000
  }" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['choices'][0]['message']['content'])"
