package com.englishword.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordVaultScreen(
    username: String,
    onLogout: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    var words by remember { mutableStateOf(listOf<Word>()) }
    var isLoading by remember { mutableStateOf(true) }

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
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Word Vault") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                ),
                actions = {
                    TextButton(onClick = onLogout) {
                        Text("Logout", color = Color.White)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* TODO: Add word */ },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Word")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Welcome message
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
                        WordCard(word = word)
                    }
                }
            }
        }
    }
}

@Composable
fun WordCard(word: Word) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
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
