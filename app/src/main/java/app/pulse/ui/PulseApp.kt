@file:Suppress("ktlint:standard:function-naming", "ktlint:standard:property-naming")

package app.pulse.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

private object Routes {
    const val Welcome = "welcome"
    const val Auth = "auth"
    const val ChatList = "chatlist"
    const val Chat = "chat/{id}"
    const val Settings = "settings"
}

@Composable
fun PulseApp(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.Welcome) {
        composable(Routes.Welcome) {
            PlaceholderScreen(
                title = "Welcome to Pulse",
                body = "Your private, offline-first conversations are coming together.",
                actions =
                    listOf(
                        "Sign in" to { navController.navigate(Routes.Auth) },
                        "Open chats" to { navController.navigate(Routes.ChatList) },
                    ),
            )
        }
        composable(Routes.Auth) {
            PlaceholderScreen(
                title = "Authentication",
                body = "Email, password, and Google sign-in will be available in P3.",
                actions = listOf("Back" to { navController.popBackStack() }),
            )
        }
        composable(Routes.ChatList) {
            PlaceholderScreen(
                title = "Chats",
                body = "Your cached conversations will appear here.",
                actions =
                    listOf(
                        "Open sample chat" to { navController.navigate("chat/sample") },
                        "Settings" to { navController.navigate(Routes.Settings) },
                    ),
            )
        }
        composable(Routes.Chat) { backStackEntry ->
            PlaceholderScreen(
                title = "Chat ${backStackEntry.arguments?.getString("id") ?: ""}",
                body = "Text messaging will be added in P4.",
                actions = listOf("Back" to { navController.popBackStack() }),
            )
        }
        composable(Routes.Settings) {
            PlaceholderScreen(
                title = "Settings",
                body = "Account, notifications, and appearance settings will be added later.",
                actions = listOf("Back" to { navController.popBackStack() }),
            )
        }
    }
}

@Composable
private fun PlaceholderScreen(
    title: String,
    body: String,
    actions: List<Pair<String, () -> Unit>>,
) {
    Scaffold { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(title, style = MaterialTheme.typography.headlineMedium)
            Text(
                text = body,
                modifier = Modifier.padding(top = 12.dp, bottom = 24.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
            actions.forEach { (label, action) ->
                Button(onClick = action, modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(label)
                }
            }
        }
    }
}
