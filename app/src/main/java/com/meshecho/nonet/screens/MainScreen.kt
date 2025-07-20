package com.example.meshecho.screens

import android.app.Application
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.meshecho.nonet.ui.ChatViewModel
import com.meshecho.nonet.mesh.BluetoothMeshService
import com.meshecho.nonet.screens.GroupChatScreen
// Import your MeshService
import com.meshecho.nonet.ui.ChatScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    application: Application,
    meshService: BluetoothMeshService
) {
    val navController = rememberNavController()
    val chatViewModel: ChatViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(application, meshService) as T
        }
    })

    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController)
        }
    ) { innerPadding ->
        NavHost(
            navController,
            startDestination = "chats",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("chats") {


                ChatScreen(viewModel = chatViewModel)
            }

            composable("groups") { GroupScreen(navController,viewModel = chatViewModel) }
            composable(
                route = "group_chat/{groupId}",
                arguments = listOf(navArgument("groupId") { type = NavType.StringType })
            ) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
                GroupChatScreen(navController = navController, viewModel = chatViewModel, groupId = groupId)
            }
            composable("media") { MediaScreen(navController) }
            composable("settings") { SettingsScreen(navController) }

        }
    }
}

