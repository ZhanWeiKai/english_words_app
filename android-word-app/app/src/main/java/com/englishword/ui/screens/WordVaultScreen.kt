package com.englishword.ui.screens

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.englishword.data.model.Word
import com.englishword.ui.theme.*

private const val TAG = "english_words"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun WordVaultScreen(
    username: String,
    onLogout: () -> Unit,
    onAIAssistant: () -> Unit = {},
    onStartTraining: (List<Word>) -> Unit = {}
) {
    Log.d(TAG, "=== WordVaultScreen compose === username: $username")

    val viewModel: WordVaultViewModel = viewModel()
    val words by viewModel.words.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }

    // Multi-select state
    var isMultiSelectMode by remember { mutableStateOf(false) }
    var selectedWords by remember { mutableStateOf(setOf<Word>()) }

    // Snackbar state
    val snackbarHostState = remember { SnackbarHostState() }

    // Log state changes
    LaunchedEffect(words) {
        Log.d(TAG, "Words state changed: ${words.size} words")
    }
    LaunchedEffect(isLoading) {
        Log.d(TAG, "isLoading state changed: $isLoading")
    }
    LaunchedEffect(errorMessage) {
        Log.d(TAG, "errorMessage state changed: $errorMessage")
    }

    // Load words on init
    LaunchedEffect(Unit) {
        Log.d(TAG, "LaunchedEffect(Unit) - calling viewModel.loadWords()")
        viewModel.loadWords()
    }

    // Show error snackbar
    LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            Log.e(TAG, "Showing error snackbar: $message")
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(if (isMultiSelectMode) "已选择 ${selectedWords.size} 个单词"
                         else "Word Vault")
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isMultiSelectMode) MaterialTheme.colorScheme.primary
                                     else MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                ),
                actions = {
                    if (isMultiSelectMode) {
                        // Cancel button in multi-select mode
                        IconButton(onClick = {
                            isMultiSelectMode = false
                            selectedWords = emptySet()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.White)
                        }
                    } else {
                        // Refresh button
                        IconButton(onClick = { viewModel.loadWords() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White)
                        }
                        TextButton(onClick = onLogout) {
                            Text("Logout", color = Color.White)
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (isMultiSelectMode && selectedWords.isNotEmpty()) {
                // Start Training FAB
                FloatingActionButton(
                    onClick = {
                        onStartTraining(selectedWords.toList())
                        isMultiSelectMode = false
                        selectedWords = emptySet()
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Start Training")
                }
            } else if (!isMultiSelectMode) {
                // AI Assistant FAB (black)
                FloatingActionButton(
                    onClick = onAIAssistant,
                    containerColor = Color.Black,
                    contentColor = Color.White
                ) {
                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "AI Assistant")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Welcome message (only show when not in multi-select mode)
            if (!isMultiSelectMode) {
                Text(
                    text = "Welcome, $username!",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        // Debounced search - search when text changes
                        if (it.isNotEmpty()) {
                            viewModel.searchWords(it)
                        } else {
                            viewModel.loadWords()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search words...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = {
                                searchQuery = ""
                                viewModel.loadWords()
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Filter chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFilter == "All",
                        onClick = {
                            selectedFilter = "All"
                            viewModel.filterByStatus(null)
                        },
                        label = { Text("All") }
                    )
                    FilterChip(
                        selected = selectedFilter == "Learning",
                        onClick = {
                            selectedFilter = "Learning"
                            viewModel.filterByStatus("LEARNING")
                        },
                        label = { Text("Learning") }
                    )
                    FilterChip(
                        selected = selectedFilter == "Mastered",
                        onClick = {
                            selectedFilter = "Mastered"
                            viewModel.filterByStatus("MASTERED")
                        },
                        label = { Text("Mastered") }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Hint for multi-select
            if (!isMultiSelectMode) {
                Text(
                    text = "长按单词进入多选模式",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Word list
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (words.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No words yet. Add your first word!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(words) { word ->
                        WordCard(
                            word = word,
                            isSelected = selectedWords.contains(word),
                            isMultiSelectMode = isMultiSelectMode,
                            onLongClick = {
                                if (!isMultiSelectMode) {
                                    isMultiSelectMode = true
                                    selectedWords = setOf(word)
                                }
                            },
                            onClick = {
                                if (isMultiSelectMode) {
                                    selectedWords = if (selectedWords.contains(word)) {
                                        selectedWords - word
                                    } else {
                                        selectedWords + word
                                    }
                                    // Exit multi-select mode if no words selected
                                    if (selectedWords.isEmpty()) {
                                        isMultiSelectMode = false
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WordCard(
    word: Word,
    isSelected: Boolean = false,
    isMultiSelectMode: Boolean = false,
    onLongClick: () -> Unit = {},
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onLongClick = onLongClick,
                onClick = onClick
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                             else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Selection indicator
            if (isMultiSelectMode) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Word + Phonetic + Part of Speech
                    Column {
                        Text(
                            text = word.word ?: "",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        // Phonetic and Part of Speech
                        if (!word.phonetic.isNullOrBlank() || !word.partOfSpeech.isNullOrBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (!word.phonetic.isNullOrBlank()) {
                                    Text(
                                        text = word.phonetic ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                                if (!word.partOfSpeech.isNullOrBlank()) {
                                    Text(
                                        text = word.partOfSpeech ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }

                    // Mastery stars
                    Row {
                        repeat(5) { index ->
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = if (index < word.masteryLevelValue) {
                                    StarActive
                                } else {
                                    StarInactive
                                },
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Definition
                Text(
                    text = word.definition ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Translation
                Text(
                    text = word.translation ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Status badge
                Surface(
                    color = if (word.status == "MASTERED") Mastered else Learning,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = word.status ?: "",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }
        }
    }
}
