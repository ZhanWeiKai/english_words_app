package com.englishword.ui.screens.sentence

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.englishword.data.model.MarkedWord
import com.englishword.data.model.Sentence

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SentenceCard(
    sentence: Sentence,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 英文句子
            Text(
                text = sentence.englishText ?: "",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1F2937),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 中文翻译
            Text(
                text = sentence.chineseText ?: "",
                fontSize = 14.sp,
                color = Color(0xFF6B7280),
                modifier = Modifier.fillMaxWidth()
            )

            // 标记词
            val markedWordsList = sentence.parseMarkedWords()
            if (markedWordsList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📝",
                        fontSize = 14.sp,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    markedWordsList.forEach { markedWord ->
                        MarkedWordChip(markedWord = markedWord)
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 底部：日期和删除按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 创建日期
                Text(
                    text = formatDate(sentence.createdAt),
                    fontSize = 12.sp,
                    color = Color(0xFF9CA3AF)
                )

                // 删除按钮
                IconButton(
                    onClick = {
                        sentence.id?.let { onDelete(it) }
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MarkedWordChip(markedWord: MarkedWord) {
    val isLinked = !markedWord.wordId.isNullOrBlank()

    AssistChip(
        onClick = { },
        label = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = markedWord.word ?: "",
                    fontSize = 12.sp,
                    color = if (isLinked) Color(0xFF1565C0) else Color(0xFF4B5563)
                )
                if (isLinked) {
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = "已关联词库",
                        modifier = Modifier.size(12.dp),
                        tint = Color(0xFF1565C0)
                    )
                }
            }
        },
        modifier = Modifier.height(28.dp),
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (isLinked) Color(0xFFE3F2FD) else Color(0xFFF3F4F6)
        ),
        border = null
    )
}

private fun formatDate(dateString: String?): String {
    if (dateString.isNullOrBlank()) return ""

    return try {
        // 假设格式是 "2026-04-02T10:30:00" 或类似
        val datePart = dateString.substringBefore("T")
        val parts = datePart.split("-")
        if (parts.size == 3) {
            "${parts[0]}-${parts[1]}-${parts[2]}"
        } else {
            dateString
        }
    } catch (e: Exception) {
        dateString
    }
}
