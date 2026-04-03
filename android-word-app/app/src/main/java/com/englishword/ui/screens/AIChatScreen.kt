package com.englishword.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.englishword.audio.AudioRecorder
import com.englishword.data.ASRService
import com.englishword.data.RetrofitClient
import com.englishword.data.TTSService
import com.englishword.data.model.ChatMessage
import com.englishword.data.model.Word
import com.englishword.data.model.WordResult
import com.englishword.ui.components.WordResultCard
import kotlin.math.absoluteValue
import kotlin.random.Random

// Colors matching xiaozhi-android style
private val InputBgColor = Color(0xFFF5F7F9)
private val HintColor = Color(0xFF9CA3AF)
private val TextColor = Color(0xFF1F2937)
private val IconColor = Color(0xFF6C6C70)
private val DisabledColor = Color(0xFFC4C9D2)
private val RecordingBgColor = Color(0xFFE3F2FD) // blue.shade50 equivalent
private val CancelBgColor = Color(0xFFFFEBEE) // red.shade50 equivalent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIChatScreen(
    onBack: () -> Unit,
    selectedWords: List<Word> = emptyList(),
    conversationId: String? = null
) {
    // Use ViewModel for state management
    val viewModel: AIChatViewModel = viewModel()
    val context = LocalContext.current
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val addedWords by viewModel.addedWords.collectAsState()
    val addingWords by viewModel.addingWords.collectAsState()
    val error by viewModel.error.collectAsState()

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Check if this is training mode
    val isTrainingMode = selectedWords.isNotEmpty()

    // Initialize with training message if in training mode, or load existing conversation
    LaunchedEffect(isTrainingMode, conversationId) {
        if (isTrainingMode) {
            viewModel.initTrainingMode(selectedWords)
        } else if (conversationId != null) {
            viewModel.loadExistingConversation(conversationId)
        }
    }

    // Scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Track added words silently
    var previousAddedCount by remember { mutableStateOf(0) }
    LaunchedEffect(addedWords.size) {
        previousAddedCount = addedWords.size
    }

    // Voice input state
    var isVoiceMode by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var isCancelling by remember { mutableStateOf(false) }

    val view = LocalView.current

    // Audio recorder
    val audioRecorder = remember { AudioRecorder(context) }
    var currentRecordingPath by remember { mutableStateOf<String?>(null) }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Permission result handled silently
    }

    // Check and request permission function
    fun checkAndRequestPermission(): Boolean {
        val permissionStatus = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        )
        return if (permissionStatus == PackageManager.PERMISSION_GRANTED) {
            true
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isTrainingMode) "Word Training" else "AI Chat") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Training words banner
            if (isTrainingMode) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    tonalElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "Training Words:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = selectedWords.mapNotNull { it.word }.joinToString(", "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Messages list
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { message ->
                    if (message.hasWordResults) {
                        // Render word search results
                        WordSearchResultMessage(
                            message = message,
                            addedWords = addedWords,
                            addingWords = addingWords,
                            onAddWord = { wordResult ->
                                // Check if word is null
                                if (wordResult.word == null) {
                                    return@WordSearchResultMessage
                                }

                                // Check if already added or adding
                                if (addedWords.contains(wordResult.word)) {
                                    return@WordSearchResultMessage
                                }
                                if (addingWords.contains(wordResult.word)) {
                                    return@WordSearchResultMessage
                                }

                                viewModel.addWord(wordResult)
                            }
                        )
                    } else {
                        // Regular message
                        ChatMessageItem(message = message)
                    }
                }

                // Show loading indicator when waiting for AI response
                if (isLoading) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.clip(MaterialTheme.shapes.medium)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "AI is thinking...",
                                        color = MaterialTheme.colorScheme.onSurface,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Input area
            if (isVoiceMode) {
                VoiceInputArea(
                    isRecording = isRecording,
                    isCancelling = isCancelling,
                    onRecordStart = {
                        // Check permission first
                        if (!checkAndRequestPermission()) {
                            return@VoiceInputArea
                        }
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        isRecording = true
                        isCancelling = false
                        // Start actual recording
                        val path = audioRecorder.startRecording()
                        if (path != null) {
                            currentRecordingPath = path
                        } else {
                            isRecording = false
                        }
                    },
                    onRecordEnd = { cancelled ->
                        if (cancelled) {
                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            // Cancel recording
                            audioRecorder.cancelRecording()
                            currentRecordingPath = null
                        } else {
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            // Stop recording and get file path
                            val savedPath = audioRecorder.stopRecording()
                            if (savedPath != null) {
                                // Call ASR API and send to AI
                                scope.launch {
                                    try {
                                        val text = recognizeSpeech(context, savedPath)
                                        if (text != null && text.isNotBlank()) {
                                            // Send recognized text to AI
                                            val mode = if (isTrainingMode) "word_training" else "word_search"
                                            val targetWord = if (isTrainingMode) selectedWords.firstOrNull()?.word else null
                                            viewModel.sendMessage(
                                                message = text,
                                                mode = mode,
                                                targetWord = targetWord
                                            )
                                        }
                                    } catch (e: Exception) {
                                        // Silent fail
                                    }
                                }
                            }
                            currentRecordingPath = null
                        }
                        isRecording = false
                        isCancelling = false
                    },
                    onCancelChange = { cancelling ->
                        if (cancelling != isCancelling) {
                            isCancelling = cancelling
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        }
                    },
                    onSwitchToText = {
                        // Cancel any ongoing recording
                        if (isRecording) {
                            audioRecorder.cancelRecording()
                            currentRecordingPath = null
                        }
                        isRecording = false
                        isCancelling = false
                        isVoiceMode = false
                    }
                )
            } else {
                TextInputArea(
                    inputText = inputText,
                    onInputChange = { inputText = it },
                    onSend = {
                        if (inputText.isNotBlank() && !isLoading) {
                            val mode = if (isTrainingMode) "word_training" else "word_search"
                            val targetWord = if (isTrainingMode) selectedWords.firstOrNull()?.word else null

                            viewModel.sendMessage(
                                message = inputText,
                                mode = mode,
                                targetWord = targetWord
                            )
                            inputText = ""

                            // Scroll to bottom after sending
                            scope.launch {
                                kotlinx.coroutines.delay(100)
                                if (messages.isNotEmpty()) {
                                    listState.animateScrollToItem(messages.size - 1)
                                }
                            }
                        }
                    },
                    onSwitchToVoice = { isVoiceMode = true },
                    isLoading = isLoading
                )
            }
        }
    }
}

@Composable
fun TextInputArea(
    inputText: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onSwitchToVoice: () -> Unit,
    isLoading: Boolean = false
) {
    val hasText = inputText.isNotBlank()

    // Container with shadow like xiaozhi-android
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .shadow(
                elevation = 10.dp,
                spotColor = Color.Black.copy(alpha = 0.08f),
                ambientColor = Color.Transparent
            )
            .padding(
                start = 16.dp,
                top = 16.dp,
                end = 16.dp,
                bottom = 16.dp
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Input container with rounded corners - 支持多行输入
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp, max = 120.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(InputBgColor)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                contentAlignment = Alignment.TopStart
            ) {
                BasicTextField(
                    value = inputText,
                    onValueChange = onInputChange,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    textStyle = TextStyle(
                        color = if (isLoading) DisabledColor else TextColor,
                        fontSize = 16.sp
                    ),
                    cursorBrush = SolidColor(Color(0xFF2196F3)), // 蓝色光标，与深灰色文本区分
                    decorationBox = { innerTextField ->
                        if (inputText.isEmpty()) {
                            Text(
                                text = if (isLoading) "Waiting for response..." else "输入消息...",
                                color = HintColor,
                                fontSize = 16.sp
                            )
                        }
                        innerTextField()
                    },
                    minLines = 1,
                    maxLines = 5
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Send or Voice button
            if (hasText || isLoading) {
                // Send button (disabled when loading)
                IconButton(
                    onClick = onSend,
                    modifier = Modifier.size(40.dp),
                    enabled = !isLoading
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (isLoading) DisabledColor else Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                }
            } else {
                // Voice button
                IconButton(
                    onClick = onSwitchToVoice,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = "Voice Input",
                        tint = IconColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun VoiceInputArea(
    isRecording: Boolean,
    isCancelling: Boolean,
    onRecordStart: () -> Unit,
    onRecordEnd: (cancelled: Boolean) -> Unit,
    onCancelChange: (cancelling: Boolean) -> Unit,
    onSwitchToText: () -> Unit
) {
    val viewConfiguration = LocalViewConfiguration.current

    // Wave animation state
    val waveHeights = remember { mutableStateListOf(*Array(16) { 0.5f }) }

    LaunchedEffect(isRecording) {
        if (isRecording && !isCancelling) {
            while (true) {
                waveHeights.forEachIndexed { index, _ ->
                    waveHeights[index] = 0.3f + Random.nextFloat() * 0.7f
                }
                kotlinx.coroutines.delay(100)
            }
        }
    }

    // Track start Y position for drag detection
    var startY by remember { mutableFloatStateOf(0f) }
    var wasCancelled by remember { mutableStateOf(false) }
    val cancelThreshold = 80f // pixels to drag up to cancel (reduced for easier trigger)

    // Container with shadow
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .shadow(
                elevation = 10.dp,
                spotColor = Color.Black.copy(alpha = 0.08f),
                ambientColor = Color.Transparent
            )
            .padding(
                start = 16.dp,
                top = 16.dp,
                end = 16.dp,
                bottom = 16.dp
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Voice input button - single pointerInput handles all gestures
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        when {
                            isRecording && isCancelling -> CancelBgColor
                            isRecording -> RecordingBgColor
                            else -> InputBgColor
                        }
                    )
                    .pointerInput(Unit) {
                        // Single gesture handler for: long press + drag + release
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            startY = down.position.y
                            wasCancelled = false

                            val pressStartTime = System.currentTimeMillis()
                            var longPressTriggered = false
                            var pointerReleased = false

                            // Loop to track drag and detect long press
                            while (!pointerReleased) {
                                // Check for long press timeout (use ViewConfiguration long press timeout)
                                if (!longPressTriggered &&
                                    System.currentTimeMillis() - pressStartTime > viewConfiguration.longPressTimeoutMillis &&
                                    !pointerReleased) {
                                    longPressTriggered = true
                                    onRecordStart()
                                }

                                // Use withTimeoutOrNull to poll events without blocking
                                val event = withTimeoutOrNull(16) {
                                    awaitPointerEvent()
                                }

                                if (event != null) {
                                    val change = event.changes.firstOrNull { it.id == down.id }

                                    if (change == null || !change.pressed) {
                                        pointerReleased = true
                                        if (longPressTriggered) {
                                            onRecordEnd(wasCancelled)
                                        }
                                    } else if (change.pressed && longPressTriggered) {
                                        // Track drag for cancel
                                        val currentY = change.position.y
                                        val dragDistance = startY - currentY
                                        val shouldCancel = dragDistance > cancelThreshold
                                        if (shouldCancel != isCancelling) {
                                            wasCancelled = shouldCancel
                                            onCancelChange(shouldCancel)
                                        }
                                    }
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isRecording && !isCancelling) {
                    // Wave animation
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        waveHeights.forEach { height ->
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height((20 * height).dp)
                                    .clip(RoundedCornerShape(1.5.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                            )
                        }
                    }
                } else {
                    Text(
                        text = when {
                            isRecording && isCancelling -> "松开手指，取消发送"
                            isRecording -> "松开发送，上滑取消"
                            else -> "按住说话"
                        },
                        color = when {
                            isRecording && isCancelling -> Color.Red
                            isRecording -> MaterialTheme.colorScheme.primary
                            else -> TextColor
                        },
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Keyboard button
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable { onSwitchToText() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Keyboard,
                    contentDescription = "Keyboard",
                    tint = IconColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun ChatMessageItem(message: ChatMessage) {
    val isUser = message.role == "user"
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // TTS播放状态
    var isPlaying by remember { mutableStateOf(false) }

    // 保存content到局部变量避免智能转换问题
    val messageContent = message.content

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(MaterialTheme.shapes.medium)
        ) {
            // 使用 SelectionContainer 包裹 Text 以支持文本选择（长按全选/复制）
            // 自定义文本选择颜色，使用蓝色而非主题橙色
            val customTextSelectionColors = TextSelectionColors(
                handleColor = Color(0xFF2196F3), // 蓝色手柄
                backgroundColor = Color(0xFF2196F3).copy(alpha = 0.2f) // 蓝色半透明背景
            )
            CompositionLocalProvider(LocalTextSelectionColors provides customTextSelectionColors) {
                SelectionContainer {
                    Text(
                        text = messageContent ?: "",
                        modifier = Modifier.padding(12.dp),
                        color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // TTS播放按钮（仅AI消息显示）
        if (!isUser && !messageContent.isNullOrBlank()) {
            IconButton(
                onClick = {
                    Log.d("tts_tag", "=== VolumeUp button clicked ===")
                    if (isPlaying) {
                        // 停止播放
                        Log.d("tts_tag", "Stopping playback...")
                        TTSService.stopPlaying()
                        isPlaying = false
                    } else {
                        // 开始播放
                        Log.d("tts_tag", "Starting playback...")
                        isPlaying = true
                        val textToSpeak = messageContent // 捕获到局部变量
                        Log.d("tts_tag", "Text to speak: ${textToSpeak.take(50)}...")
                        scope.launch {
                            try {
                                Log.d("tts_tag", "Getting apiService...")
                                val apiService = RetrofitClient.getApiService()
                                Log.d("tts_tag", "Calling TTSService.speak()...")
                                TTSService.speak(
                                    apiService,
                                    textToSpeak,
                                    onComplete = {
                                        // 播放完成回调
                                        Log.d("tts_tag", "Playback completed")
                                        isPlaying = false
                                    }
                                )
                            } catch (e: Exception) {
                                Log.e("tts_tag", "ERROR in onClick: ${e.message}", e)
                                isPlaying = false
                            }
                        }
                    }
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = if (isPlaying) "停止播放" else "播放语音",
                    tint = if (isPlaying) MaterialTheme.colorScheme.primary else IconColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun WordSearchResultMessage(
    message: ChatMessage,
    addedWords: Set<String>,
    addingWords: Set<String>,
    onAddWord: (WordResult) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        // Header text
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.clip(MaterialTheme.shapes.medium)
        ) {
            Text(
                text = "找到相关单词：",
                modifier = Modifier.padding(12.dp),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Word cards
        message.wordResults?.forEach { wordResult ->
            WordResultCard(
                wordResult = wordResult,
                isAdded = addedWords.contains(wordResult.word),
                isAdding = addingWords.contains(wordResult.word),
                onAddClick = { onAddWord(wordResult) }
            )
        }
    }
}

/**
 * Helper function to call ASR API
 */
private suspend fun recognizeSpeech(context: Context, audioFilePath: String): String? {
    return try {
        val apiService = RetrofitClient.getApiService()
        ASRService.recognizeSpeech(apiService, audioFilePath)
    } catch (e: Exception) {
        Log.e("AIChatScreen", "ASR call failed", e)
        null
    }
}
