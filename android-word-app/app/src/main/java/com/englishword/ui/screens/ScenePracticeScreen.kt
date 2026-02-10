package com.englishword.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.englishword.ui.theme.Primary
import com.englishword.ui.theme.Success

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScenePracticeScreen(
    onBack: () -> Unit
) {
    var currentScene by remember { mutableStateOf(1) }
    var isStarted by remember { mutableStateOf(false) }
    var isCompleted by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scene Practice") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Primary,
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (!isStarted) {
                // Scene selection
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Choose a Scene",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    SceneCard(
                        title = "At the Restaurant",
                        description = "Practice ordering food and asking questions",
                        onClick = {
                            currentScene = 1
                            isStarted = true
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    SceneCard(
                        title = "At the Airport",
                        description = "Practice check-in and boarding procedures",
                        onClick = {
                            currentScene = 2
                            isStarted = true
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    SceneCard(
                        title = "Job Interview",
                        description = "Practice introducing yourself and answering questions",
                        onClick = {
                            currentScene = 3
                            isStarted = true
                        }
                    )
                }
            } else if (!isCompleted) {
                // Practice mode
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Scene ${currentScene}: Practice in Progress",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Practice content will be here\n(Demo - Coming Soon)",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { isCompleted = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Success
                        )
                    ) {
                        Text("Complete Practice", style = MaterialTheme.typography.labelLarge)
                    }
                }
            } else {
                // Completed
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🎉",
                        style = MaterialTheme.typography.titleLarge
                    )

                    Text(
                        text = "Practice Complete!",
                        style = MaterialTheme.typography.titleLarge,
                        color = Success
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Great job! You're making progress.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = {
                            isStarted = false
                            isCompleted = false
                            currentScene = 1
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Text("Back to Scenes", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
fun SceneCard(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}
