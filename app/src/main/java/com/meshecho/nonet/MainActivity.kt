package com.meshecho.nonet

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import com.example.meshecho.screens.MainScreen
import com.meshecho.nonet.MainViewModel
import com.meshecho.nonet.mesh.BluetoothMeshService
import com.meshecho.nonet.onboarding.BluetoothCheckScreen
import com.meshecho.nonet.onboarding.BluetoothStatus
import com.meshecho.nonet.onboarding.BluetoothStatusManager
import com.meshecho.nonet.onboarding.InitializationErrorScreen
import com.meshecho.nonet.onboarding.InitializingScreen
import com.meshecho.nonet.onboarding.LocationCheckScreen
import com.meshecho.nonet.onboarding.LocationStatus
import com.meshecho.nonet.onboarding.LocationStatusManager
import com.meshecho.nonet.onboarding.OnboardingCoordinator
import com.meshecho.nonet.onboarding.OnboardingState
import com.meshecho.nonet.onboarding.PermissionExplanationScreen
import com.meshecho.nonet.onboarding.PermissionManager
import com.meshecho.nonet.ui.ChatViewModel
import com.meshecho.nonet.ui.ChatScreen
import com.meshecho.nonet.ui.theme.BitchatTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    
    private lateinit var permissionManager: PermissionManager
    private lateinit var onboardingCoordinator: OnboardingCoordinator
    private lateinit var bluetoothStatusManager: BluetoothStatusManager
    private lateinit var locationStatusManager: LocationStatusManager
    
    // Core mesh service - managed at app level
    private lateinit var meshService: BluetoothMeshService
    private val mainViewModel: MainViewModel by viewModels()
    private val chatViewModel: ChatViewModel by viewModels { 
        object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return ChatViewModel(application, meshService) as T
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize core mesh service first
        meshService = BluetoothMeshService(this)
        
        // Initialize permission management
        permissionManager = PermissionManager(this)
        bluetoothStatusManager = BluetoothStatusManager(
            activity = this,
            context = this,
            onBluetoothEnabled = ::handleBluetoothEnabled,
            onBluetoothDisabled = ::handleBluetoothDisabled
        )
        locationStatusManager = LocationStatusManager(
            activity = this,
            context = this,
            onLocationEnabled = ::handleLocationEnabled,
            onLocationDisabled = ::handleLocationDisabled
        )
        onboardingCoordinator = OnboardingCoordinator(
            activity = this,
            permissionManager = permissionManager,
            onOnboardingComplete = ::handleOnboardingComplete,
            onOnboardingFailed = ::handleOnboardingFailed
        )
        
        setContent {
            BitchatTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    OnboardingFlowScreen()
                }
            }
        }
        
        // Collect state changes in a lifecycle-aware manner
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainViewModel.onboardingState.collect { state ->
                    handleOnboardingStateChange(state)
                }
            }
        }
        
        // Only start onboarding process if we're in the initial CHECKING state
        // This prevents restarting onboarding on configuration changes
        if (mainViewModel.onboardingState.value == OnboardingState.CHECKING) {
            checkOnboardingStatus()
        }
    }
    
    @Composable
    private fun OnboardingFlowScreen() {
        val onboardingState by mainViewModel.onboardingState.collectAsState()
        val bluetoothStatus by mainViewModel.bluetoothStatus.collectAsState()
        val locationStatus by mainViewModel.locationStatus.collectAsState()
        val errorMessage by mainViewModel.errorMessage.collectAsState()
        val isBluetoothLoading by mainViewModel.isBluetoothLoading.collectAsState()
        val isLocationLoading by mainViewModel.isLocationLoading.collectAsState()
        
        when (onboardingState) {
            OnboardingState.CHECKING -> {
                InitializingScreen()
            }
            
            OnboardingState.BLUETOOTH_CHECK -> {
                BluetoothCheckScreen(
                    status = bluetoothStatus,
                    onEnableBluetooth = {
                        mainViewModel.updateBluetoothLoading(true)
                        bluetoothStatusManager.requestEnableBluetooth()
                    },
                    onRetry = {
                        checkBluetoothAndProceed()
                    },
                    isLoading = isBluetoothLoading
                )
            }
            
            OnboardingState.LOCATION_CHECK -> {
                LocationCheckScreen(
                    status = locationStatus,
                    onEnableLocation = {
                        mainViewModel.updateLocationLoading(true)
                        locationStatusManager.requestEnableLocation()
                    },
                    onRetry = {
                        checkLocationAndProceed()
                    },
                    isLoading = isLocationLoading
                )
            }
            
            OnboardingState.PERMISSION_EXPLANATION -> {
                PermissionExplanationScreen(
                    permissionCategories = permissionManager.getCategorizedPermissions(),
                    onContinue = {
                        mainViewModel.updateOnboardingState(OnboardingState.PERMISSION_REQUESTING)
                        onboardingCoordinator.requestPermissions()
                    }
                )
            }
            
            OnboardingState.PERMISSION_REQUESTING -> {
                InitializingScreen()
            }
            
            OnboardingState.INITIALIZING -> {
                InitializingScreen()
            }
            
            OnboardingState.COMPLETE -> {
                // Set up back navigation handling for the chat screen
                val backCallback = object : OnBackPressedCallback(true) {
                    override fun handleOnBackPressed() {
                        // Let ChatViewModel handle navigation state
                        val handled = chatViewModel.handleBackPressed()
                        if (!handled) {
                            // If ChatViewModel doesn't handle it, disable this callback 
                            // and let the system handle it (which will exit the app)
                            this.isEnabled = false
                            onBackPressedDispatcher.onBackPressed()
                            this.isEnabled = true
                        }
                    }
                }
                
                // Add the callback - this will be automatically removed when the activity is destroyed
                onBackPressedDispatcher.addCallback(this, backCallback)
                MainScreen(
                    application = application,
                    meshService = meshService
                )

            }
            
            OnboardingState.ERROR -> {
                InitializationErrorScreen(
                    errorMessage = errorMessage,
                    onRetry = {
                        mainViewModel.updateOnboardingState(OnboardingState.CHECKING)
                        checkOnboardingStatus()
                    },
                    onOpenSettings = {
                        onboardingCoordinator.openAppSettings()
                    }
                )
            }
        }
    }
    
    private fun handleOnboardingStateChange(state: OnboardingState) {

        when (state) {
            OnboardingState.COMPLETE -> {
                // App is fully initialized, mesh service is running
                android.util.Log.d("MainActivity", "Onboarding completed - app ready")
            }
            OnboardingState.ERROR -> {
                android.util.Log.e("MainActivity", "Onboarding error state reached")
            }
            else -> {}
        }
    }
    
    private fun checkOnboardingStatus() {
        android.util.Log.d("MainActivity", "Checking onboarding status")
        
        lifecycleScope.launch {
            // Small delay to show the checking state
            delay(500)
            
            // First check Bluetooth status (always required)
            checkBluetoothAndProceed()
        }
    }
    
    /**
     * Check Bluetooth status and proceed with onboarding flow
     */
    private fun checkBluetoothAndProceed() {
        // android.util.Log.d("MainActivity", "Checking Bluetooth status")
        
        // For first-time users, skip Bluetooth check and go straight to permissions
        // We'll check Bluetooth after permissions are granted
        if (permissionManager.isFirstTimeLaunch()) {
            android.util.Log.d("MainActivity", "First-time launch, skipping Bluetooth check - will check after permissions")
            proceedWithPermissionCheck()
            return
        }
        
        // For existing users, check Bluetooth status first
        bluetoothStatusManager.logBluetoothStatus()
        mainViewModel.updateBluetoothStatus(bluetoothStatusManager.checkBluetoothStatus())
        
        when (mainViewModel.bluetoothStatus.value) {
            BluetoothStatus.ENABLED -> {
                // Bluetooth is enabled, check location services next
                checkLocationAndProceed()
            }
            BluetoothStatus.DISABLED -> {
                // Show Bluetooth enable screen (should have permissions as existing user)
                android.util.Log.d("MainActivity", "Bluetooth disabled, showing enable screen")
                mainViewModel.updateOnboardingState(OnboardingState.BLUETOOTH_CHECK)
                mainViewModel.updateBluetoothLoading(false)
            }
            BluetoothStatus.NOT_SUPPORTED -> {
                // Device doesn't support Bluetooth
                android.util.Log.e("MainActivity", "Bluetooth not supported")
                mainViewModel.updateOnboardingState(OnboardingState.BLUETOOTH_CHECK)
                mainViewModel.updateBluetoothLoading(false)
            }
        }
    }
    
    /**
     * Proceed with permission checking 
     */
    private fun proceedWithPermissionCheck() {
        android.util.Log.d("MainActivity", "Proceeding with permission check")
        
        lifecycleScope.launch {
            delay(200) // Small delay for smooth transition
            
            if (permissionManager.isFirstTimeLaunch()) {
                android.util.Log.d("MainActivity", "First time launch, showing permission explanation")
                mainViewModel.updateOnboardingState(OnboardingState.PERMISSION_EXPLANATION)
            } else if (permissionManager.areAllPermissionsGranted()) {
                android.util.Log.d("MainActivity", "Existing user with permissions, initializing app")
                mainViewModel.updateOnboardingState(OnboardingState.INITIALIZING)
                initializeApp()
            } else {
                android.util.Log.d("MainActivity", "Existing user missing permissions, showing explanation")
                mainViewModel.updateOnboardingState(OnboardingState.PERMISSION_EXPLANATION)
            }
        }
    }
    
    /**
     * Handle Bluetooth enabled callback
     */
    private fun handleBluetoothEnabled() {
        android.util.Log.d("MainActivity", "Bluetooth enabled by user")
        mainViewModel.updateBluetoothLoading(false)
        mainViewModel.updateBluetoothStatus(BluetoothStatus.ENABLED)
        checkLocationAndProceed()
    }

    /**
     * Check Location services status and proceed with onboarding flow
     */
    private fun checkLocationAndProceed() {
        android.util.Log.d("MainActivity", "Checking location services status")
        
        // For first-time users, skip location check and go straight to permissions
        // We'll check location after permissions are granted
        if (permissionManager.isFirstTimeLaunch()) {
            android.util.Log.d("MainActivity", "First-time launch, skipping location check - will check after permissions")
            proceedWithPermissionCheck()
            return
        }
        
        // For existing users, check location status
        locationStatusManager.logLocationStatus()
        mainViewModel.updateLocationStatus(locationStatusManager.checkLocationStatus())
        
        when (mainViewModel.locationStatus.value) {
            LocationStatus.ENABLED -> {
                // Location services enabled, proceed with permission/onboarding check
                proceedWithPermissionCheck()
            }
            LocationStatus.DISABLED -> {
                // Show location enable screen (should have permissions as existing user)
                android.util.Log.d("MainActivity", "Location services disabled, showing enable screen")
                mainViewModel.updateOnboardingState(OnboardingState.LOCATION_CHECK)
                mainViewModel.updateLocationLoading(false)
            }
            LocationStatus.NOT_AVAILABLE -> {
                // Device doesn't support location services (very unusual)
                android.util.Log.e("MainActivity", "Location services not available")
                mainViewModel.updateOnboardingState(OnboardingState.LOCATION_CHECK)
                mainViewModel.updateLocationLoading(false)
            }
        }
    }

    /**
     * Handle Location enabled callback
     */
    private fun handleLocationEnabled() {
        android.util.Log.d("MainActivity", "Location services enabled by user")
        mainViewModel.updateLocationLoading(false)
        mainViewModel.updateLocationStatus(LocationStatus.ENABLED)
        proceedWithPermissionCheck()
    }

    /**
     * Handle Location disabled callback
     */
    private fun handleLocationDisabled(message: String) {
        android.util.Log.w("MainActivity", "Location services disabled or failed: $message")
        mainViewModel.updateLocationLoading(false)
        mainViewModel.updateLocationStatus(locationStatusManager.checkLocationStatus())
        
        when {
            mainViewModel.locationStatus.value == LocationStatus.NOT_AVAILABLE -> {
                // Show permanent error for devices without location services
                mainViewModel.updateErrorMessage(message)
                mainViewModel.updateOnboardingState(OnboardingState.ERROR)
            }
            else -> {
                // Stay on location check screen for retry
                mainViewModel.updateOnboardingState(OnboardingState.LOCATION_CHECK)
            }
        }
    }
    
    /**
     * Handle Bluetooth disabled callback
     */
    private fun handleBluetoothDisabled(message: String) {
        android.util.Log.w("MainActivity", "Bluetooth disabled or failed: $message")
        mainViewModel.updateBluetoothLoading(false)
        mainViewModel.updateBluetoothStatus(bluetoothStatusManager.checkBluetoothStatus())
        
        when {
            mainViewModel.bluetoothStatus.value == BluetoothStatus.NOT_SUPPORTED -> {
                // Show permanent error for unsupported devices
                mainViewModel.updateErrorMessage(message)
                mainViewModel.updateOnboardingState(OnboardingState.ERROR)
            }
            message.contains("Permission") && permissionManager.isFirstTimeLaunch() -> {
                // During first-time onboarding, if Bluetooth enable fails due to permissions,
                // proceed to permission explanation screen where user will grant permissions first
                android.util.Log.d("MainActivity", "Bluetooth enable requires permissions, proceeding to permission explanation")
                proceedWithPermissionCheck()
            }
            message.contains("Permission") -> {
                // For existing users, redirect to permission explanation to grant missing permissions
                android.util.Log.d("MainActivity", "Bluetooth enable requires permissions, showing permission explanation")
                mainViewModel.updateOnboardingState(OnboardingState.PERMISSION_EXPLANATION)
            }
            else -> {
                // Stay on Bluetooth check screen for retry
                mainViewModel.updateOnboardingState(OnboardingState.BLUETOOTH_CHECK)
            }
        }
    }
    
    private fun handleOnboardingComplete() {
        android.util.Log.d("MainActivity", "Onboarding completed, checking Bluetooth and Location before initializing app")
        
        // After permissions are granted, re-check both Bluetooth and Location status
        val currentBluetoothStatus = bluetoothStatusManager.checkBluetoothStatus()
        val currentLocationStatus = locationStatusManager.checkLocationStatus()
        
        when {
            currentBluetoothStatus != BluetoothStatus.ENABLED -> {
                // Bluetooth still disabled, but now we have permissions to enable it
                android.util.Log.d("MainActivity", "Permissions granted, but Bluetooth still disabled. Showing Bluetooth enable screen.")
                mainViewModel.updateBluetoothStatus(currentBluetoothStatus)
                mainViewModel.updateOnboardingState(OnboardingState.BLUETOOTH_CHECK)
                mainViewModel.updateBluetoothLoading(false)
            }
            currentLocationStatus != LocationStatus.ENABLED -> {
                // Location services still disabled, but now we have permissions to enable it
                android.util.Log.d("MainActivity", "Permissions granted, but Location services still disabled. Showing Location enable screen.")
                mainViewModel.updateLocationStatus(currentLocationStatus)
                mainViewModel.updateOnboardingState(OnboardingState.LOCATION_CHECK)
                mainViewModel.updateLocationLoading(false)
            }
            else -> {
                // Both are enabled, proceed to app initialization
                android.util.Log.d("MainActivity", "Both Bluetooth and Location services are enabled, proceeding to initialization")
                mainViewModel.updateOnboardingState(OnboardingState.INITIALIZING)
                initializeApp()
            }
        }
    }
    
    private fun handleOnboardingFailed(message: String) {
        android.util.Log.e("MainActivity", "Onboarding failed: $message")
        mainViewModel.updateErrorMessage(message)
        mainViewModel.updateOnboardingState(OnboardingState.ERROR)
    }
    
    private fun initializeApp() {
        android.util.Log.d("MainActivity", "Starting app initialization")
        
        lifecycleScope.launch {
            try {
                // Initialize the app with a proper delay to ensure Bluetooth stack is ready
                // This solves the issue where app needs restart to work on first install
                delay(1000) // Give the system time to process permission grants
                
                android.util.Log.d("MainActivity", "Permissions verified, initializing chat system")
                
                // Ensure all permissions are still granted (user might have revoked in settings)
                if (!permissionManager.areAllPermissionsGranted()) {
                    val missing = permissionManager.getMissingPermissions()
                    android.util.Log.w("MainActivity", "Permissions revoked during initialization: $missing")
                    handleOnboardingFailed("Some permissions were revoked. Please grant all permissions to continue.")
                    return@launch
                }
                
                // Set up mesh service delegate and start services
                meshService.delegate = chatViewModel
                meshService.startServices()
                
                android.util.Log.d("MainActivity", "Mesh service started successfully")
                
                // Handle any notification intent
                handleNotificationIntent(intent)
                
                // Small delay to ensure mesh service is fully initialized
                delay(500)
                
                android.util.Log.d("MainActivity", "App initialization complete")
                mainViewModel.updateOnboardingState(OnboardingState.COMPLETE)
                
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Failed to initialize app", e)
                handleOnboardingFailed("Failed to initialize the app: ${e.message}")
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Handle notification intents when app is already running
        if (mainViewModel.onboardingState.value == OnboardingState.COMPLETE) {
            handleNotificationIntent(intent)
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Check Bluetooth and Location status on resume and handle accordingly
        if (mainViewModel.onboardingState.value == OnboardingState.COMPLETE) {
            // Set app foreground state
            meshService.connectionManager.setAppBackgroundState(false)
            chatViewModel.setAppBackgroundState(false)
            
            // Check if Bluetooth was disabled while app was backgrounded
            val currentBluetoothStatus = bluetoothStatusManager.checkBluetoothStatus()
            if (currentBluetoothStatus != BluetoothStatus.ENABLED) {
                android.util.Log.w("MainActivity", "Bluetooth disabled while app was backgrounded")
                mainViewModel.updateBluetoothStatus(currentBluetoothStatus)
                mainViewModel.updateOnboardingState(OnboardingState.BLUETOOTH_CHECK)
                mainViewModel.updateBluetoothLoading(false)
                return
            }
            
            // Check if location services were disabled while app was backgrounded
            val currentLocationStatus = locationStatusManager.checkLocationStatus()
            if (currentLocationStatus != LocationStatus.ENABLED) {
                android.util.Log.w("MainActivity", "Location services disabled while app was backgrounded")
                mainViewModel.updateLocationStatus(currentLocationStatus)
                mainViewModel.updateOnboardingState(OnboardingState.LOCATION_CHECK)
                mainViewModel.updateLocationLoading(false)
            }
        }
    }
    
    override fun onPause() {
        super.onPause()
        // Only set background state if app is fully initialized
        if (mainViewModel.onboardingState.value == OnboardingState.COMPLETE) {
            // Set app background state
            meshService.connectionManager.setAppBackgroundState(true)
            chatViewModel.setAppBackgroundState(true)
        }
    }
    
    /**
     * Handle intents from notification clicks - open specific private chat
     */
    private fun handleNotificationIntent(intent: Intent) {
        val shouldOpenPrivateChat = intent.getBooleanExtra(
            com.meshecho.nonet.ui.NotificationManager.EXTRA_OPEN_PRIVATE_CHAT,
            false
        )
        
        if (shouldOpenPrivateChat) {
            val peerID = intent.getStringExtra(com.meshecho.nonet.ui.NotificationManager.EXTRA_PEER_ID)
            val senderNickname = intent.getStringExtra(com.meshecho.nonet.ui.NotificationManager.EXTRA_SENDER_NICKNAME)
            
            if (peerID != null) {
                android.util.Log.d("MainActivity", "Opening private chat with $senderNickname (peerID: $peerID) from notification")
                
                // Open the private chat with this peer
                chatViewModel.startPrivateChat(peerID)
                
                // Clear notifications for this sender since user is now viewing the chat
                chatViewModel.clearNotificationsForSender(peerID)
            }
        }
    }
    
    /**
     * Restart mesh services (for debugging/troubleshooting)
     */
    fun restartMeshServices() {
        if (mainViewModel.onboardingState.value == OnboardingState.COMPLETE) {
            lifecycleScope.launch {
                try {
                    android.util.Log.d("MainActivity", "Restarting mesh services")
                    meshService.stopServices()
                    delay(1000)
                    meshService.startServices()
                    android.util.Log.d("MainActivity", "Mesh services restarted successfully")
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "Error restarting mesh services: ${e.message}")
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        
        // Cleanup location status manager
        try {
            locationStatusManager.cleanup()
            android.util.Log.d("MainActivity", "Location status manager cleaned up successfully")
        } catch (e: Exception) {
            android.util.Log.w("MainActivity", "Error cleaning up location status manager: ${e.message}")
        }
        
        // Stop mesh services if app was fully initialized
        if (mainViewModel.onboardingState.value == OnboardingState.COMPLETE) {
            try {
                meshService.stopServices()
                android.util.Log.d("MainActivity", "Mesh services stopped successfully")
            } catch (e: Exception) {
                android.util.Log.w("MainActivity", "Error stopping mesh services in onDestroy: ${e.message}")
            }
        }
    }
}
