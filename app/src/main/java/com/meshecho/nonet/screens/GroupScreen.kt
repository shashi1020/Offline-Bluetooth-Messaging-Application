package com.example.meshecho.screens

import androidx.compose.runtime.Composable
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.meshecho.nonet.ui.ChatViewModel
import com.meshecho.nonet.ui.convertRSSIToSignalStrength
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupScreen(
    navController: NavController,
    viewModel: ChatViewModel
) {
    val colorScheme = MaterialTheme.colorScheme

    val connectedPeers by viewModel.connectedPeers.observeAsState(emptyList())
    val peerNicknames = viewModel.meshService.getPeerNicknames()
    val peerRSSI = viewModel.meshService.getPeerRSSI()
    val nickname by viewModel.nickname.observeAsState("")

    val selectedPeers = remember { mutableStateListOf<String>() }
    var groupName by remember { mutableStateOf("") }

    val isScanning = connectedPeers.isEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Group") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colorScheme.primaryContainer)
            )
        },
        bottomBar = {
            if (selectedPeers.isNotEmpty()) {
                Column(Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        label = { Text("Group Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val groupId = viewModel.createGroup(
                                groupName.ifBlank { "Group-${UUID.randomUUID().toString().take(4)}" },
                                selectedPeers.toSet()
                            )
                            navController.navigate("group_chat/$groupId")// navigate back to group list
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Create Group")
                    }
                }
            }
        }
    ) { innerPadding ->
        if (isScanning) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("get EchoMessage...")
                    CircularProgressIndicator()
                    Spacer(Modifier.height(8.dp))
                    Text("Scanning for devices...")
                }
            }
        } else {
            LazyColumn(
                contentPadding = innerPadding,
                modifier = Modifier
                    .fillMaxSize()
                    .background(colorScheme.surface),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(connectedPeers.size) { index ->
                    val peerId = connectedPeers[index]
                    val displayName = if (peerId == nickname) "You" else peerNicknames[peerId] ?: peerId
                    val signalStrength = convertRSSIToSignalStrength(peerRSSI[peerId])
                    val isSelected = selectedPeers.contains(peerId)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clickable {
                                if (isSelected) selectedPeers.remove(peerId)
                                else selectedPeers.add(peerId)
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) colorScheme.primaryContainer else colorScheme.surface
                        )
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("$displayName (Signal: $signalStrength)")
                            Spacer(Modifier.weight(1f))
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = {
                                    if (it) selectedPeers.add(peerId)
                                    else selectedPeers.remove(peerId)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
