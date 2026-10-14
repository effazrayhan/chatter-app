@file:Suppress("ktlint:standard:function-naming", "ktlint:standard:property-naming")

package app.pulse.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.pulse.core.AppContainer
import app.pulse.feature.auth.AuthViewModel
import app.pulse.feature.chat.ChatViewModel
import app.pulse.feature.chatlist.ChatListViewModel
import app.pulse.feature.profile.ProfileViewModel
import java.util.UUID

private object Routes {
    const val Welcome = "welcome"
    const val Auth = "auth"
    const val ChatList = "chatlist"
    const val Chat = "chat/{id}"
    const val Settings = "settings"
    const val Profile = "profile"
    const val Diagnostics = "diagnostics"
}

@Composable
fun PulseApp(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.Welcome) {
        composable(Routes.Welcome) { WelcomeScreen(navController) }
        composable(Routes.Auth) { AuthScreen(navController) }
        composable(Routes.ChatList) { ChatListScreen(navController) }
        composable(Routes.Chat) { entry ->
            val id = entry.arguments?.getString("id")?.let(UUID::fromString) ?: UUID.nameUUIDFromBytes("sample".toByteArray())
            ChatScreen(id, navController)
        }
        composable(Routes.Settings) {
            PlaceholderScreen(
                "Settings",
                "Notification and appearance settings arrive in P9.",
                listOf(
                    "Profile" to {
                        navController.navigate(Routes.Profile)
                    },
                    "Back" to { navController.popBackStack() },
                ),
            )
        }
        composable(Routes.Profile) { ProfileScreen(navController) }
        composable(Routes.Diagnostics) { DiagnosticsScreen(navController) }
    }
}

@Composable
private fun WelcomeScreen(navController: NavHostController) {
    PlaceholderScreen(
        "Welcome to Pulse",
        "Offline-first conversations with a Supabase backend.",
        listOf(
            "Sign in" to {
                navController.navigate(Routes.Auth)
            },
            "Open chats" to { navController.navigate(Routes.ChatList) },
            "Diagnostics" to { navController.navigate(Routes.Diagnostics) },
        ),
    )
}

@Composable
private fun AuthScreen(
    navController: NavHostController,
    viewModel: AuthViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var register by remember { mutableStateOf(false) }
    Scaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), verticalArrangement = Arrangement.Center) {
            Text(if (register) "Create your Pulse account" else "Sign in to Pulse", style = MaterialTheme.typography.headlineSmall)
            OutlinedTextField(
                state.email,
                viewModel::emailChanged,
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            )
            OutlinedTextField(state.password, viewModel::passwordChanged, label = {
                Text("Password")
            }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            if (register) {
                OutlinedTextField(state.displayName, viewModel::displayNameChanged, label = {
                    Text("Display name")
                }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
            Button(onClick = {
                viewModel.submit(register)
            }, enabled = !state.loading, modifier = Modifier.padding(top = 16.dp)) { Text(if (register) "Create account" else "Sign in") }
            Button(onClick = {
                register = !register
            }, modifier = Modifier.padding(top = 8.dp)) { Text(if (register) "Already have an account" else "Create an account") }
            Button(onClick = { navController.popBackStack() }, modifier = Modifier.padding(top = 8.dp)) { Text("Back") }
            if (state.signedIn) {
                Button(onClick = {
                    navController.navigate(Routes.ChatList)
                }, modifier = Modifier.padding(top = 8.dp)) { Text("Continue to chats") }
            }
        }
    }
}

@Composable
private fun ChatListScreen(
    navController: NavHostController,
    viewModel: ChatListViewModel = viewModel(),
) {
    val chats by viewModel.chats.collectAsStateWithLifecycle()
    Scaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Chats", style = MaterialTheme.typography.headlineMedium)
                Button(onClick = { navController.navigate(Routes.Settings) }) { Text("Settings") }
            }
            if (chats.isEmpty()) {
                Text(
                    "No cached chats yet. Open the sample chat to try the offline message queue.",
                    modifier = Modifier.padding(top = 24.dp),
                )
                Button(onClick = {
                    navController.navigate("chat/${UUID.nameUUIDFromBytes("sample".toByteArray())}")
                }, modifier = Modifier.padding(top = 12.dp)) { Text("Open sample chat") }
            } else {
                LazyColumn(Modifier.padding(top = 16.dp)) {
                    items(chats, key = {
                        it.id
                    }) { chat ->
                        Button(onClick = {
                            navController.navigate("chat/${chat.id}")
                        }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Text(chat.title) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatScreen(
    chatId: UUID,
    navController: NavHostController,
) {
    val viewModel = remember(chatId) { ChatViewModel(chatId) }
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    var draft by remember { mutableStateOf("") }
    Scaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { navController.popBackStack() }) { Text("Back") }
                Text("Chat", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 12.dp))
            }
            HorizontalDivider()
            LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(16.dp), reverseLayout = true) {
                items(messages.asReversed(), key = {
                    it.id
                }) { message -> Text(message.body, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) }
            }
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.Bottom) {
                OutlinedTextField(draft, { draft = it.take(4096) }, label = { Text("Message") }, modifier = Modifier.weight(1f))
                Button(onClick = {
                    viewModel.send(draft)
                    draft = ""
                }, modifier = Modifier.padding(start = 8.dp)) { Text("Send") }
            }
        }
    }
}

@Composable
private fun ProfileScreen(
    navController: NavHostController,
    viewModel: ProfileViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
            Text("Profile", style = MaterialTheme.typography.headlineMedium)
            OutlinedTextField(state.displayName, viewModel::displayNameChanged, label = {
                Text("Display name")
            }, modifier = Modifier.fillMaxWidth().padding(top = 16.dp))
            OutlinedTextField(state.username, viewModel::usernameChanged, label = {
                Text("Username")
            }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            OutlinedTextField(
                state.bio,
                viewModel::bioChanged,
                label = { Text("Bio") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
            Button(
                onClick = viewModel::save,
                modifier = Modifier.padding(top = 16.dp),
            ) { Text(if (state.saved) "Saved" else "Save profile") }
            Button(onClick = { navController.popBackStack() }, modifier = Modifier.padding(top = 8.dp)) { Text("Back") }
        }
    }
}

@Composable
private fun DiagnosticsScreen(navController: NavHostController) {
    val chats by AppContainer.chatRepository.observeChats().collectAsStateWithLifecycle(emptyList())
    PlaceholderScreen(
        "Diagnostics",
        "Local database is ready. Cached chats: ${chats.size}.",
        listOf(
            "Back" to {
                navController.popBackStack()
            },
        ),
    )
}

@Composable
private fun PlaceholderScreen(
    title: String,
    body: String,
    actions: List<Pair<String, () -> Unit>>,
) {
    Scaffold { paddingValues ->
        Column(
            Modifier.fillMaxSize().padding(paddingValues).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(title, style = MaterialTheme.typography.headlineMedium)
            Text(body, Modifier.padding(top = 12.dp, bottom = 24.dp), style = MaterialTheme.typography.bodyLarge)
            actions.forEach { (label, action) -> Button(onClick = action, modifier = Modifier.padding(vertical = 4.dp)) { Text(label) } }
        }
    }
}
