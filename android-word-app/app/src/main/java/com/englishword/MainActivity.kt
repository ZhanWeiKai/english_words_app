package com.englishword

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.englishword.data.RetrofitClient
import com.englishword.data.TokenManager
import com.englishword.data.model.Word
import com.englishword.ui.screens.*
import com.englishword.ui.theme.EnglishWordTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize TokenManager and RetrofitClient
        tokenManager = TokenManager.getInstance(this)
        RetrofitClient.init(tokenManager)

        setContent {
            EnglishWordTheme {
                EnglishWordApp(tokenManager)
            }
        }
    }
}

@Composable
fun EnglishWordApp(tokenManager: TokenManager) {
    var startDestination by remember { mutableStateOf("login") }
    var username by remember { mutableStateOf("") }
    val context = androidx.compose.ui.platform.LocalContext.current

    // Check login status
    LaunchedEffect(Unit) {
        val isLoggedIn = tokenManager.isLoggedIn().first()
        if (isLoggedIn) {
            startDestination = "wordvault"
            tokenManager.getUsername().first()?.let { username = it }
        }
    }

    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            if (startDestination == "wordvault") {
                BottomNavigationBar(navController)
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(paddingValues)
        ) {
            // Login Screen
            composable("login") {
                val viewModel = remember { LoginViewModel(context) }
                LoginScreen(
                    onLoginSuccess = {
                        // Get username and navigate
                        navController.navigate("wordvault") {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                    onRegisterClick = { /* Handle register */ },
                    viewModel = viewModel
                )
            }

            // Word Vault Screen
            composable("wordvault") {
                WordVaultScreen(
                    username = username,
                    onLogout = {
                        navController.navigate("login") {
                            popUpTo("wordvault") { inclusive = true }
                        }
                    },
                    onAIAssistant = {
                        navController.navigate("aichat")
                    },
                    onStartTraining = { selectedWords ->
                        // Pass selected words as comma-separated string
                        val wordsArg = selectedWords.mapNotNull { it.word }.joinToString(",")
                        navController.navigate("aichat?words=$wordsArg")
                    }
                )
            }

            // AI Chat Screen
            composable("aichat?words={words}") { backStackEntry ->
                val wordsArg = backStackEntry.arguments?.getString("words") ?: ""
                val selectedWords = if (wordsArg.isNotEmpty()) {
                    wordsArg.split(",").map { wordStr ->
                        Word().apply { word = wordStr }
                    }
                } else {
                    emptyList()
                }
                AIChatScreen(
                    onBack = { navController.popBackStack() },
                    selectedWords = selectedWords
                )
            }

            // AI Chat Screen (without words)
            composable("aichat") {
                AIChatScreen(
                    onBack = { navController.popBackStack() },
                    selectedWords = emptyList()
                )
            }

            // AI Chat Screen (view existing conversation)
            composable("aichat_detail?conversationId={conversationId}") { backStackEntry ->
                val convId = backStackEntry.arguments?.getString("conversationId") ?: ""
                AIChatScreen(
                    onBack = { navController.popBackStack() },
                    selectedWords = emptyList(),
                    conversationId = convId
                )
            }

            // Training Summary Screen
            composable("training") {
                TrainingSummaryScreen(
                    onBack = { navController.popBackStack() },
                    onContinue = { navController.popBackStack() }
                )
            }

            // Conversation List Screen (History)
            composable("history") {
                ConversationListScreen(
                    onConversationClick = { conversationId ->
                        navController.navigate("aichat_detail?conversationId=$conversationId")
                    }
                )
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    val items = listOf(
        BottomNavItem("wordvault", "Vault", Icons.Default.Home),
        BottomNavItem("aichat", "AI Chat", Icons.Default.Chat),
        BottomNavItem("history", "History", Icons.Default.History)
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar {
        items.forEach { item ->
            val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                selected = selected,
                onClick = {
                    navController.navigate(item.route) {
                        // Pop up to the start destination of the graph to
                        // avoid building up a large stack of destinations
                        // on the back stack as users select items
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        // Avoid multiple copies of the same destination when
                        // reselecting the same item
                        launchSingleTop = true
                        // Restore state when reselecting a previously selected item
                        restoreState = true
                    }
                }
            )
        }
    }
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)
