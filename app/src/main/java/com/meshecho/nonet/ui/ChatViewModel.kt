package com.meshecho.nonet.ui

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.meshecho.nonet.mesh.BluetoothConnectionManager
import com.meshecho.nonet.mesh.BluetoothMeshDelegate
import com.meshecho.nonet.mesh.BluetoothMeshService
import com.meshecho.nonet.mesh.MeshService1
import com.meshecho.nonet.model.BitchatMessage
import com.meshecho.nonet.model.DeliveryAck
import com.meshecho.nonet.model.EchoPacket
import com.meshecho.nonet.model.Group
import com.meshecho.nonet.model.Message
import com.meshecho.nonet.model.ReadReceipt
import com.meshecho.nonet.model.RoutedPacket
import com.meshecho.nonet.protocol.BitchatPacket
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import utils.ChatViewModelUtils
import java.util.Date
import java.util.UUID
import kotlin.random.Random

/**
 * Refactored ChatViewModel - Main coordinator for Echo functionality
 * Delegates specific responsibilities to specialized managers while maintaining 100% iOS compatibility
 */
class ChatViewModel(
    application: Application,
    val meshService: BluetoothMeshService
) : AndroidViewModel(application), BluetoothMeshDelegate {
    private lateinit var meshService1: MeshService1
    init {
        meshService1 = MeshService1(
            connectionManager = meshService.connectionManager,
            myPeerID = meshService.myPeerID
        )
    }


    private val context: Context = application.applicationContext

    // State management
    private val state = ChatState()

    // Specialized managers
    private val dataManager = DataManager(context)
    private val messageManager = MessageManager(state)
    private val channelManager = ChannelManager(state, messageManager, dataManager, viewModelScope)
    val privateChatManager = PrivateChatManager(state, messageManager, dataManager)
    private val commandProcessor =
        CommandProcessor(state, messageManager, channelManager, privateChatManager)
    private val notificationManager = NotificationManager(application.applicationContext)

    // Delegate handler for mesh callbacks
    private val meshDelegateHandler = MeshDelegateHandler(
        state = state,
        messageManager = messageManager,
        channelManager = channelManager,
        privateChatManager = privateChatManager,
        notificationManager = notificationManager,
        coroutineScope = viewModelScope,
        onHapticFeedback = { ChatViewModelUtils.triggerHapticFeedback(context) },
        getMyPeerID = { meshService.myPeerID }
    )
    // Expose state through LiveData (maintaining the same interface)
    val messages: LiveData<List<BitchatMessage>> = state.messages
    val connectedPeers: LiveData<List<String>> = state.connectedPeers
    val nickname: LiveData<String> = state.nickname
    val isConnected: LiveData<Boolean> = state.isConnected
    val privateChats: LiveData<Map<String, List<BitchatMessage>>> = state.privateChats
    val selectedPrivateChatPeer: LiveData<String?> = state.selectedPrivateChatPeer
    val unreadPrivateMessages: LiveData<Set<String>> = state.unreadPrivateMessages
    val joinedChannels: LiveData<Set<String>> = state.joinedChannels
    val currentChannel: LiveData<String?> = state.currentChannel
    val channelMessages: LiveData<Map<String, List<BitchatMessage>>> = state.channelMessages
    val unreadChannelMessages: LiveData<Map<String, Int>> = state.unreadChannelMessages
    val passwordProtectedChannels: LiveData<Set<String>> = state.passwordProtectedChannels
    val showPasswordPrompt: LiveData<Boolean> = state.showPasswordPrompt
    val passwordPromptChannel: LiveData<String?> = state.passwordPromptChannel
    val showSidebar: LiveData<Boolean> = state.showSidebar
    val hasUnreadChannels = state.hasUnreadChannels
    val hasUnreadPrivateMessages = state.hasUnreadPrivateMessages
    val showCommandSuggestions: LiveData<Boolean> = state.showCommandSuggestions
    val commandSuggestions: LiveData<List<CommandSuggestion>> = state.commandSuggestions
    val favoritePeers: LiveData<Set<String>> = state.favoritePeers
    val showAppInfo: LiveData<Boolean> = state.showAppInfo

    init {
        // Note: Mesh service delegate is now set by MainActivity
        loadAndInitialize()
    }

    private fun loadAndInitialize() {
        // Load nickname
        val nickname = dataManager.loadNickname()
        state.setNickname(nickname)

        // Load data
        val (joinedChannels, protectedChannels) = channelManager.loadChannelData()
        state.setJoinedChannels(joinedChannels)
        state.setPasswordProtectedChannels(protectedChannels)

        // Initialize channel messages
        joinedChannels.forEach { channel ->
            if (!state.getChannelMessagesValue().containsKey(channel)) {
                val updatedChannelMessages = state.getChannelMessagesValue().toMutableMap()
                updatedChannelMessages[channel] = emptyList()
                state.setChannelMessages(updatedChannelMessages)
            }
        }

        // Load other data
        dataManager.loadFavorites()
        state.setFavoritePeers(dataManager.favoritePeers)
        dataManager.loadBlockedUsers()

        // Log all favorites at startup
        dataManager.logAllFavorites()
        logCurrentFavoriteState()

        // Note: Mesh service is now started by MainActivity

        // Show welcome message if no peers after delay
        viewModelScope.launch {
            delay(3000)
            if (state.getConnectedPeersValue().isEmpty() && state.getMessagesValue().isEmpty()) {
                val welcomeMessage = BitchatMessage(
                    sender = "system",
                    content = "get people around you to download Echo…and chat with them here!",
                    timestamp = Date(),
                    isRelay = false
                )
                messageManager.addMessage(welcomeMessage)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Note: Mesh service lifecycle is now managed by MainActivity
    }

    // MARK: - Nickname Management

    fun setNickname(newNickname: String) {
        state.setNickname(newNickname)
        dataManager.saveNickname(newNickname)
        meshService.sendBroadcastAnnounce()
    }

    // MARK: - Channel Management (delegated)

    fun joinChannel(channel: String, password: String? = null): Boolean {
        return channelManager.joinChannel(channel, password, meshService.myPeerID)
    }

    fun switchToChannel(channel: String?) {
        channelManager.switchToChannel(channel)
    }

    fun leaveChannel(channel: String) {
        channelManager.leaveChannel(channel)
        meshService.sendMessage("left $channel")
    }

    // MARK: - Private Chat Management (delegated)

    fun startPrivateChat(peerID: String) {
        val success = privateChatManager.startPrivateChat(peerID, meshService)
        if (success) {
            // Notify notification manager about current private chat
            setCurrentPrivateChatPeer(peerID)
            // Clear notifications for this sender since user is now viewing the chat
            clearNotificationsForSender(peerID)
        }
    }

    fun endPrivateChat() {
        privateChatManager.endPrivateChat()
        // Notify notification manager that no private chat is active
        setCurrentPrivateChatPeer(null)
    }

    // MARK: - Message Sending

    fun sendMessage(content: String) {
        if (content.isEmpty()) return

        // Check for commands
        if (content.startsWith("/")) {
            commandProcessor.processCommand(content, meshService, meshService.myPeerID) { messageContent, mentions, channel ->
                meshService.sendMessage(messageContent, mentions, channel)
            }
            return
        }

        val mentions = messageManager.parseMentions(content, meshService.getPeerNicknames().values.toSet(), state.getNicknameValue())
        val channels = messageManager.parseChannels(content)

        // Auto-join mentioned channels
        channels.forEach { channel ->
            if (!state.getJoinedChannelsValue().contains(channel)) {
                joinChannel(channel)
            }
        }

        val selectedPeer = state.getSelectedPrivateChatPeerValue()
        val currentChannelValue = state.getCurrentChannelValue()

        if (selectedPeer != null) {
            // Send private message
            val recipientNickname = meshService.getPeerNicknames()[selectedPeer]
            privateChatManager.sendPrivateMessage(
                content,
                selectedPeer,
                recipientNickname,
                state.getNicknameValue(),
                meshService.myPeerID
            ) { messageContent, peerID, recipientNicknameParam, messageId ->
                meshService.sendPrivateMessage(messageContent, peerID, recipientNicknameParam, messageId)
            }
        } else {
            // Send public/channel message
            val message = BitchatMessage(
                sender = state.getNicknameValue() ?: meshService.myPeerID,
                content = content,
                timestamp = Date(),
                isRelay = false,
                senderPeerID = meshService.myPeerID,
                mentions = if (mentions.isNotEmpty()) mentions else null,
                channel = currentChannelValue
            )

            if (currentChannelValue != null) {
                channelManager.addChannelMessage(currentChannelValue, message, meshService.myPeerID)

                // Check if encrypted channel
                if (channelManager.hasChannelKey(currentChannelValue)) {
                    channelManager.sendEncryptedChannelMessage(
                        content,
                        mentions,
                        currentChannelValue,
                        state.getNicknameValue(),
                        meshService.myPeerID,
                        onEncryptedPayload = { encryptedData ->
                            // This would need proper mesh service integration
                            meshService.sendMessage(content, mentions, currentChannelValue)
                        },
                        onFallback = {
                            meshService.sendMessage(content, mentions, currentChannelValue)
                        }
                    )
                } else {
                    meshService.sendMessage(content, mentions, currentChannelValue)
                }
            } else {
                messageManager.addMessage(message)
                meshService.sendMessage(content, mentions, null)
            }
        }
    }

    // MARK: - Utility Functions

    fun getPeerIDForNickname(nickname: String): String? {
        return meshService.getPeerNicknames().entries.find { it.value == nickname }?.key
    }

    fun toggleFavorite(peerID: String) {
        Log.d("ChatViewModel", "toggleFavorite called for peerID: $peerID")
        privateChatManager.toggleFavorite(peerID)

        // Log current state after toggle
        logCurrentFavoriteState()
    }

    private fun logCurrentFavoriteState() {
        Log.i("ChatViewModel", "=== CURRENT FAVORITE STATE ===")
        Log.i("ChatViewModel", "LiveData favorite peers: ${favoritePeers.value}")
        Log.i("ChatViewModel", "DataManager favorite peers: ${dataManager.favoritePeers}")
        Log.i("ChatViewModel", "Peer fingerprints: ${privateChatManager.getAllPeerFingerprints()}")
        Log.i("ChatViewModel", "==============================")
    }

    // MARK: - Debug and Troubleshooting

    fun getDebugStatus(): String {
        return meshService.getDebugStatus()
    }

    // Note: Mesh service restart is now handled by MainActivity
    // This function is no longer needed

    fun setAppBackgroundState(inBackground: Boolean) {
        // Forward to notification manager for notification logic
        notificationManager.setAppBackgroundState(inBackground)
    }

    fun setCurrentPrivateChatPeer(peerID: String?) {
        // Update notification manager with current private chat peer
        notificationManager.setCurrentPrivateChatPeer(peerID)
    }

    fun clearNotificationsForSender(peerID: String) {
        // Clear notifications when user opens a chat
        notificationManager.clearNotificationsForSender(peerID)
    }

    // MARK: - Command Autocomplete (delegated)

    fun updateCommandSuggestions(input: String) {
        commandProcessor.updateCommandSuggestions(input)
    }

    fun selectCommandSuggestion(suggestion: CommandSuggestion): String {
        return commandProcessor.selectCommandSuggestion(suggestion)
    }

    // MARK: - BluetoothMeshDelegate Implementation (delegated)

    override fun didReceiveMessage(message: BitchatMessage) {
        meshDelegateHandler.didReceiveMessage(message)
    }

    override fun didConnectToPeer(peerID: String) {
        meshDelegateHandler.didConnectToPeer(peerID)
    }

    override fun didDisconnectFromPeer(peerID: String) {
        meshDelegateHandler.didDisconnectFromPeer(peerID)
    }

    override fun didUpdatePeerList(peers: List<String>) {
        meshDelegateHandler.didUpdatePeerList(peers)
    }

    override fun didReceiveChannelLeave(channel: String, fromPeer: String) {
        meshDelegateHandler.didReceiveChannelLeave(channel, fromPeer)
    }

    override fun didReceiveDeliveryAck(ack: DeliveryAck) {
        meshDelegateHandler.didReceiveDeliveryAck(ack)
    }

    override fun didReceiveReadReceipt(receipt: ReadReceipt) {
        meshDelegateHandler.didReceiveReadReceipt(receipt)
    }

    override fun decryptChannelMessage(encryptedContent: ByteArray, channel: String): String? {
        return meshDelegateHandler.decryptChannelMessage(encryptedContent, channel)
    }

    override fun getNickname(): String? {
        return meshDelegateHandler.getNickname()
    }

    override fun isFavorite(peerID: String): Boolean {
        return meshDelegateHandler.isFavorite(peerID)
    }

    override fun registerPeerPublicKey(peerID: String, publicKeyData: ByteArray) {
        privateChatManager.registerPeerPublicKey(peerID, publicKeyData)
    }

    // MARK: - Emergency Clear

    fun panicClearAllData() {
        // Clear all managers
        messageManager.clearAllMessages()
        channelManager.clearAllChannels()
        privateChatManager.clearAllPrivateChats()
        dataManager.clearAllData()

        // Reset nickname
        val newNickname = "anon${Random.Default.nextInt(1000, 9999)}"
        state.setNickname(newNickname)
        dataManager.saveNickname(newNickname)

        // Note: Mesh service restart is now handled by MainActivity
        // This method now only clears data, not mesh service lifecycle
    }

    // MARK: - Navigation Management

    fun showAppInfo() {
        state.setShowAppInfo(true)
    }

    fun hideAppInfo() {
        state.setShowAppInfo(false)
    }

    fun showSidebar() {
        state.setShowSidebar(true)
    }

    fun hideSidebar() {
        state.setShowSidebar(false)
    }
    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    fun createGroup(name: String, selectedPeerIDs: Set<String>): String {
        val groupId = UUID.randomUUID().toString()
        val newGroup = Group(
            groupId = groupId,
            groupName = name,
            members = selectedPeerIDs + getOwnPeerID()
        )
        _groups.value = _groups.value + newGroup
        return groupId
    }


    fun getOwnPeerID(): String {
        return meshService.myPeerID
    }

 /**
  * implement sendgroup messages
  * **/




 private val _groupMessages = MutableLiveData<Map<String, List<Message>>>(emptyMap())
    val groupMessages: LiveData<Map<String, List<Message>>> = _groupMessages

    fun getGroupMessages(groupId: String): LiveData<List<Message>> {
     return _groupMessages.map { it[groupId] ?: emptyList() }
 }

    fun sendGroupMessage(groupId: String, content: String) {
        val group = _groups.value.find { it.groupId == groupId } ?: return
        val message = Message(
            sender = getOwnPeerID(),
            content = content,
            timestamp = Date()
        )
        _groupMessages.value = _groupMessages.value.orEmpty() + (groupId to (getGroupMessages(groupId).value.orEmpty() + message))

        for (peer in group.members) {
            if (peer != getOwnPeerID()) {
                val packet = EchoPacket(
                    type = "group_message",
                    destination = peer,
                    payload = content,
                    sender = getOwnPeerID(),
                    groupId = groupId
                )
                meshService1.sendPacket(packet)
            }
        }
    }
    fun getGroupById(groupId: String): Group? {
        return _groups.value.find { it.groupId == groupId }
    }

    /**
     * Handle Android back navigation
     * Returns true if the back press was handled, false if it should be passed to the system
     */
    fun handleBackPressed(): Boolean {
        return when {
            // Close app info dialog
            state.getShowAppInfoValue() -> {
                hideAppInfo()
                true
            }
            // Close sidebar
            state.getShowSidebarValue() -> {
                hideSidebar()
                true
            }
            // Close password dialog
            state.getShowPasswordPromptValue() -> {
                state.setShowPasswordPrompt(false)
                state.setPasswordPromptChannel(null)
                true
            }
            // Exit private chat
            state.getSelectedPrivateChatPeerValue() != null -> {
                endPrivateChat()
                true
            }
            // Exit channel view
            state.getCurrentChannelValue() != null -> {
                switchToChannel(null)
                true
            }
            // No special navigation state - let system handle (usually exits app)
            else -> false
        }
    }
}