package com.meshecho.nonet.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.meshecho.nonet.ui.ChatViewModel

import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.unit.dp
import com.meshecho.nonet.model.Message


@ExperimentalMaterial3Api
@Composable
fun GroupChatScreen(
    navController: NavController,
    viewModel: ChatViewModel,
    groupId: String
) {
    val group = viewModel.getGroupById(groupId)
    val messages by viewModel.getGroupMessages(groupId).observeAsState(emptyList())
    val myPeerId = viewModel.getOwnPeerID()

    var input by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text(group?.groupName ?: "Group") })

        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
            items(messages) { msg ->
                val isMine = msg.sender == myPeerId
                Text(
                    text = "${if (isMine) "you" else msg.sender}: ${msg.content}",
                    modifier = Modifier.padding(8.dp)
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            TextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message") }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                viewModel.sendGroupMessage(groupId, input)
                input = ""
            }) {
                Text("Send")
            }
        }
    }
}

