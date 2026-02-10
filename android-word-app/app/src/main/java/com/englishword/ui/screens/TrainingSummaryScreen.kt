package com.englishword.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.englishword.ui.theme.Primary
import com.englishword.ui.theme.Success

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingSummaryScreen(
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Training Summary") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Celebration icon
            Text(
                text = "🏆",
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Well Done!",
                style = MaterialTheme.typography.titleLarge,
                color = Success
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "You've completed today's training",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Stats cards
            StatCard(
                title = "Words Learned",
                value = "12",
                icon = "📚"
            )

            Spacer(modifier = Modifier.height(16.dp))

            StatCard(
                title = "Practice Time",
                value = "25 min",
                icon = "⏱️"
            )

            Spacer(modifier = Modifier.height(16.dp))

            StatCard(
                title = "Accuracy",
                value = "85%",
                icon = "🎯"
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Continue button
            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    "Continue Learning",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = icon,
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
