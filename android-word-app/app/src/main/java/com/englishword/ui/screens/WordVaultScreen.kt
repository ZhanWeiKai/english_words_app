package com.englishword.ui.screens

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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.englishword.data.model.Word
import com.englishword.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun WordVaultScreen(
    username: String,
    onLogout: () -> Unit,
    onAIAssistant: () -> Unit = {},
    onStartTraining: (List<Word>) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    var words by remember { mutableStateOf(listOf<Word>()) }
    var isLoading by remember { mutableStateOf(true) }

    // Multi-select state
    var isMultiSelectMode by remember { mutableStateOf(false) }
    var selectedWords by remember { mutableStateOf(setOf<Word>()) }

    // Load words on init
    LaunchedEffect(Unit) {
        // TODO: Load words from API
        isLoading = false
        // Sample data for now
        words = listOf(
            Word().apply {
                this.word = "serendipity"
                this.definition = "The occurrence of events by chance in a happy way"
                this.translation = "意外发现珍奇事物的本领"
                this.masteryLevelValue = 3
                this.status = "LEARNING"
            },
            Word().apply {
                this.word = "ephemeral"
                this.definition = "Lasting for a very short time"
                this.translation = "短暂的"
                this.masteryLevelValue = 5
                this.status = "MASTERED"
            },
            Word().apply {
                this.word = "ubiquitous"
                this.definition = "Present, appearing, or found everywhere"
                this.translation = "无处不在的"
                this.masteryLevelValue = 2
                this.status = "LEARNING"
            },
            Word().apply {
                this.word = "pragmatic"
                this.definition = "Dealing with things sensibly and realistically"
                this.translation = "务实的"
                this.masteryLevelValue = 4
                this.status = "LEARNING"
            }
        )
    }

    Scaffold(
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
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search words...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
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
                        onClick = { selectedFilter = "All" },
                        label = { Text("All") }
                    )
                    FilterChip(
                        selected = selectedFilter == "Learning",
                        onClick = { selectedFilter = "Learning" },
                        label = { Text("Learning") }
                    )
                    FilterChip(
                        selected = selectedFilter == "Mastered",
                        onClick = { selectedFilter = "Mastered" },
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
                    // Word
                    Text(
                        text = word.word ?: "",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

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
