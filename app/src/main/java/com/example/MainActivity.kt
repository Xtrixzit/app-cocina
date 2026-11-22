package com.example

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.AuthManager
import com.example.auth.SessionPreferences
import com.example.data.model.DistributionConfig
import com.example.data.model.Recipe
import com.example.data.repository.CookingRepository
import com.example.ui.MainViewModel
import com.example.ui.UiState
import com.example.ui.components.CommercialSettingsDialog
import com.example.ui.components.LicenseActivationDialog
import com.example.ui.components.NotebookBottomBar
import com.example.ui.components.PrivacyPolicyDialog
import com.example.ui.components.UpdateAvailableDialog
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CookingAssistantScreen
import com.example.ui.screens.PantryFormDialog
import com.example.ui.screens.PantryScreen
import com.example.ui.screens.ProductionScreen
import com.example.ui.screens.RecipeDetailScreen
import com.example.ui.screens.RecipeFormDialog
import com.example.ui.screens.RecipesScreen
import com.example.ui.screens.ShoppingListScreen
import com.example.ui.theme.CaveatFontFamily
import com.example.ui.theme.MyApplicationTheme
import com.example.util.AppShortcutsUpdater
import com.example.util.LocaleHelper
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

import com.example.ai.VoiceCommandRouter

data class VoiceLaunchRequest(
    val tab: Int = 0,
    val query: String? = null,
    val autoListen: Boolean = false,
    val nonce: Long = 0L
)

class MainActivity : ComponentActivity() {
    private var voiceLaunchState by mutableStateOf(VoiceLaunchRequest())

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
        )
        voiceLaunchState = resolveVoiceLaunchRequest(intent)
        setContent {
            MyApplicationTheme {
                AppNavigation(
                    initialTab = voiceLaunchState.tab,
                    voiceLaunchRequest = voiceLaunchState,
                    onConsumeVoiceLaunch = {
                        voiceLaunchState = voiceLaunchState.copy(query = null, autoListen = false)
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        voiceLaunchState = resolveVoiceLaunchRequest(intent)
    }

    private fun resolveVoiceLaunchRequest(intent: Intent?): VoiceLaunchRequest {
        if (intent == null) return VoiceLaunchRequest()
        val nowNonce = System.nanoTime()

        val dataUri = intent.data
        val isVoiceDeepLink = dataUri != null &&
            dataUri.scheme.equals(VoiceCommandRouter.DEEP_LINK_SCHEME, ignoreCase = true) &&
            dataUri.host.equals(VoiceCommandRouter.DEEP_LINK_HOST_VOICE, ignoreCase = true)

        val deepLinkQuery = if (isVoiceDeepLink) {
            dataUri?.getQueryParameter("q") ?: dataUri?.getQueryParameter("query")
        } else null
        val deepLinkAutoListen = isVoiceDeepLink &&
            dataUri?.getQueryParameter("autoListen").equals("true", ignoreCase = true)

        val extraListen = intent.getBooleanExtra(VoiceCommandRouter.EXTRA_VOICE_LISTEN, false) ||
            intent.getStringExtra(VoiceCommandRouter.EXTRA_VOICE_LISTEN).equals("true", ignoreCase = true)

        val isSearchIntent = intent.action == Intent.ACTION_SEARCH ||
            intent.action == "com.google.android.gms.actions.SEARCH_ACTION"

        val searchQuery = intent.getStringExtra(SearchManager.QUERY)
            ?: intent.getStringExtra("query")
            ?: deepLinkQuery

        val featureName = intent.getStringExtra("featureName")

        if (!searchQuery.isNullOrBlank()) {
            return VoiceLaunchRequest(
                tab = 4,
                query = searchQuery.trim(),
                autoListen = false,
                nonce = nowNonce
            )
        }

        if (isSearchIntent || extraListen || deepLinkAutoListen || isVoiceDeepLink) {
            return VoiceLaunchRequest(
                tab = 4,
                query = null,
                autoListen = extraListen || deepLinkAutoListen || isVoiceDeepLink || isSearchIntent,
                nonce = nowNonce
            )
        }

        val explicitTab = when {
            intent.hasExtra(AppShortcutsUpdater.EXTRA_OPEN_TAB) -> {
                val intVal = intent.getIntExtra(AppShortcutsUpdater.EXTRA_OPEN_TAB, -1)
                if (intVal in 0..4) {
                    intVal
                } else {
                    intent.getStringExtra(AppShortcutsUpdater.EXTRA_OPEN_TAB)?.toIntOrNull()?.coerceIn(0, 4) ?: 0
                }
            }
            !featureName.isNullOrBlank() -> {
                val normalized = featureName.lowercase()
                when {
                    normalized.contains("despensa") || normalized.contains("pantry") || normalized.contains("stock") -> 1
                    normalized.contains("compra") || normalized.contains("shopping") || normalized.contains("check") -> 2
                    normalized.contains("produccion") || normalized.contains("producción") || normalized.contains("production") -> 3
                    normalized.contains("voz") || normalized.contains("cocina") || normalized.contains("asistente") || normalized.contains("voice") -> 4
                    else -> 0
                }
            }
            else -> 0
        }

        return VoiceLaunchRequest(tab = explicitTab, nonce = nowNonce)
    }
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun AppNavigation(
    initialTab: Int = 0,
    voiceLaunchRequest: VoiceLaunchRequest = VoiceLaunchRequest(tab = initialTab),
    onConsumeVoiceLaunch: () -> Unit = {},
    auth: FirebaseAuth = Firebase.auth
) {
    val context = LocalContext.current
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val isGuestMode by SessionPreferences.observeGuestMode(context).collectAsStateWithLifecycle()

    LaunchedEffect(currentUser?.uid) {
        if (currentUser != null && SessionPreferences.isGuestMode(context)) {
            SessionPreferences.setGuestMode(context, false)
        }
    }

    LaunchedEffect(isGuestMode) {
        if (auth.currentUser == null && !isGuestMode) {
            val credentialManager = CredentialManager.create(context)
            AuthManager.attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {
                    SessionPreferences.setGuestMode(context, false)
                },
                onUnauthenticated = {},
                scope = this
            )
        }
    }

    val user = currentUser
    when {
        user != null -> {
            key(user.uid) {
                MainFeatureScreen(
                    currentUserId = user.uid,
                    currentUserEmail = user.email.orEmpty(),
                    currentUserName = user.displayName.orEmpty(),
                    isGuestMode = false,
                    initialTab = initialTab,
                    voiceLaunchRequest = voiceLaunchRequest,
                    onConsumeVoiceLaunch = onConsumeVoiceLaunch
                )
            }
        }
        isGuestMode -> {
            key(CookingRepository.GUEST_USER_ID) {
                MainFeatureScreen(
                    currentUserId = CookingRepository.GUEST_USER_ID,
                    currentUserEmail = "",
                    currentUserName = stringResource(R.string.guest_display_name),
                    isGuestMode = true,
                    initialTab = initialTab,
                    voiceLaunchRequest = voiceLaunchRequest,
                    onConsumeVoiceLaunch = onConsumeVoiceLaunch
                )
            }
        }
        else -> {
            AuthScreen(
                onAuthSuccess = {
                    SessionPreferences.setGuestMode(context, false)
                },
                onContinueAsGuest = {
                    SessionPreferences.setGuestMode(context, true)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainFeatureScreen(
    currentUserId: String,
    currentUserEmail: String,
    currentUserName: String,
    isGuestMode: Boolean = false,
    initialTab: Int = 0,
    voiceLaunchRequest: VoiceLaunchRequest = VoiceLaunchRequest(tab = initialTab),
    onConsumeVoiceLaunch: () -> Unit = {},
    viewModel: MainViewModel = viewModel(
        key = if (isGuestMode) CookingRepository.GUEST_USER_ID else currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val repository = CookingRepository(app)
                MainViewModel(
                    repository = repository,
                    currentUserId = currentUserId,
                    currentUserEmail = currentUserEmail,
                    currentUserName = currentUserName,
                    isGuestMode = isGuestMode
                )
            }
        }
    )
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember(initialTab, voiceLaunchRequest.nonce) {
        mutableIntStateOf(if (voiceLaunchRequest.query != null || voiceLaunchRequest.autoListen) 4 else initialTab)
    }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showAccountDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var manualUpdateConfig by remember { mutableStateOf<DistributionConfig?>(null) }

    val distributionConfig by viewModel.distributionConfig.collectAsStateWithLifecycle()
    val licenseValidated by viewModel.licenseValidated.collectAsStateWithLifecycle()
    val localGuestCount by viewModel.localGuestCountForMigration.collectAsStateWithLifecycle()

    val selectedRecipe by viewModel.selectedRecipe.collectAsState()
    val targetServings by viewModel.targetServings.collectAsState()
    var recipeFormTarget by remember { mutableStateOf<Recipe?>(null) }
    var showRecipeForm by remember { mutableStateOf(false) }
    var showPantryForm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.userMessage.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val pantryUiState by viewModel.pantryState.collectAsState()
    val pantryItems = when (val s = pantryUiState) {
        is UiState.Success -> s.data
        else -> emptyList()
    }

    val shoppingUiState by viewModel.shoppingState.collectAsState()
    val pendingShoppingCount = when (val s = shoppingUiState) {
        is UiState.Success -> s.data.count { !it.isPurchased }
        else -> 0
    }

    val recipesUiState by viewModel.recipesState.collectAsState()
    val recipesList = when (val s = recipesUiState) {
        is UiState.Success -> s.data
        else -> emptyList()
    }

    if (selectedRecipe != null) {
        RecipeDetailScreen(
            recipe = selectedRecipe!!,
            pantryItems = pantryItems,
            targetServings = targetServings,
            onServingsChanged = { viewModel.setTargetServings(it) },
            onBack = { viewModel.selectRecipe(null) },
            onEdit = {
                recipeFormTarget = selectedRecipe
                showRecipeForm = true
            },
            onDelete = {
                viewModel.deleteRecipe(selectedRecipe!!.id)
            },
            onAddMissingToShoppingList = {
                viewModel.addMissingRecipeIngredientsToShoppingList(
                    selectedRecipe!!,
                    targetServings,
                    pantryItems
                )
            },
            onUpdateRecipe = { updated ->
                viewModel.saveRecipe(updated)
            }
        )
    } else {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        if (selectedTab == 0) {
                            Text(
                                text = stringResource(R.string.topbar_recipes),
                                fontFamily = CaveatFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        } else {
                            val title = when (selectedTab) {
                                1 -> stringResource(R.string.topbar_pantry)
                                2 -> stringResource(R.string.topbar_shopping)
                                3 -> stringResource(R.string.topbar_production)
                                4 -> stringResource(R.string.topbar_cooking)
                                else -> stringResource(R.string.app_name)
                            }
                            Text(
                                text = title,
                                fontFamily = CaveatFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 26.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    actions = {
                        if (isGuestMode) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(12.dp),
                                onClick = { showAccountDialog = true },
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .testTag("guest_mode_badge")
                            ) {
                                Text(
                                    text = stringResource(R.string.guest_badge_short),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        IconButton(
                            onClick = { showSettingsDialog = true },
                            modifier = Modifier.testTag("action_settings")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = stringResource(R.string.cd_settings),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(
                            onClick = { showAccountDialog = true },
                            modifier = Modifier.testTag("action_account")
                        ) {
                            Icon(
                                Icons.Default.AccountCircle,
                                contentDescription = if (isGuestMode) {
                                    stringResource(R.string.cd_guest_mode)
                                } else {
                                    stringResource(R.string.cd_cloud_account)
                                },
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                        IconButton(
                            onClick = { showAboutDialog = true },
                            modifier = Modifier.testTag("action_about")
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = stringResource(R.string.cd_info),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                )
            },
            bottomBar = {
                NotebookBottomBar(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    pendingShoppingCount = pendingShoppingCount
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "TabContent"
                ) { tab ->
                    when (tab) {
                        0 -> RecipesScreen(
                            viewModel = viewModel,
                            pantryItems = pantryItems,
                            onSelectRecipe = { recipe -> viewModel.selectRecipe(recipe) },
                            onAddNewRecipe = {
                                recipeFormTarget = null
                                showRecipeForm = true
                            },
                            onNavigateToCheckList = { selectedTab = 2 },
                            onNavigateToProduction = { selectedTab = 3 },
                            onNavigateToSurpriseMe = { selectedTab = 4 },
                            onShowHelp = { showAboutDialog = true }
                        )
                        1 -> PantryScreen(
                            viewModel = viewModel,
                            onAddNewItem = { showPantryForm = true }
                        )
                        2 -> ShoppingListScreen(
                            viewModel = viewModel,
                            pantryItems = pantryItems
                        )
                        3 -> ProductionScreen(
                            viewModel = viewModel,
                            recipes = recipesList,
                            pantryItems = pantryItems
                        )
                        4 -> CookingAssistantScreen(
                            viewModel = viewModel,
                            recipes = recipesList,
                            pantryItems = pantryItems,
                            onSelectRecipe = { recipe -> viewModel.selectRecipe(recipe) },
                            voiceLaunchRequest = voiceLaunchRequest,
                            onConsumeVoiceLaunch = onConsumeVoiceLaunch
                        )
                    }
                }
            }
        }
    }

    if (showRecipeForm) {
        RecipeFormDialog(
            initialRecipe = recipeFormTarget,
            onDismiss = { showRecipeForm = false },
            onSave = { recipe -> viewModel.saveRecipe(recipe) }
        )
    }

    if (showPantryForm) {
        PantryFormDialog(
            initialItem = null,
            onDismiss = { showPantryForm = false },
            onSave = { item -> viewModel.savePantryItem(item) }
        )
    }

    if (showSettingsDialog) {
        CommercialSettingsDialog(
            isGuestMode = isGuestMode,
            localGuestCountForMigration = localGuestCount,
            onDismiss = { showSettingsDialog = false },
            onMigrateGuestToCloud = {
                viewModel.migrateGuestDataToCloud()
            },
            onExportFullBackupJson = { onResult ->
                viewModel.exportFullBackupJson(onResult)
            },
            onImportFullBackupJson = { jsonText, replace ->
                viewModel.importFullBackupJson(jsonText, replace)
            },
            onActivateLicense = { code ->
                viewModel.activateLicense(code)
            },
            onCheckUpdates = {
                viewModel.checkUpdatesManual(
                    currentVersionCode = BuildConfig.VERSION_CODE,
                    currentVersionName = BuildConfig.VERSION_NAME
                ) { cfg ->
                    manualUpdateConfig = cfg
                }
            },
            onShowPrivacyPolicy = { showPrivacyDialog = true },
            onDeleteAccount = {
                showSettingsDialog = false
                viewModel.deleteAccountAndCloudData {
                    coroutineScope.launch {
                        if (isGuestMode) {
                            SessionPreferences.setGuestMode(context, false)
                        } else {
                            AuthManager.signOut(credentialManager, {}, this)
                        }
                    }
                }
            }
        )
    }

    if (showPrivacyDialog) {
        PrivacyPolicyDialog(onDismiss = { showPrivacyDialog = false })
    }

    val forcedUpdateConfig = distributionConfig?.takeIf {
        it.minRequiredVersionCode > BuildConfig.VERSION_CODE
    }
    if (forcedUpdateConfig != null) {
        UpdateAvailableDialog(
            config = forcedUpdateConfig,
            isMandatory = true,
            onDismiss = {}
        )
    } else if (manualUpdateConfig != null) {
        UpdateAvailableDialog(
            config = manualUpdateConfig!!,
            isMandatory = false,
            onDismiss = { manualUpdateConfig = null }
        )
    }

    if (!isGuestMode && distributionConfig?.requireLicense == true && !licenseValidated) {
        LicenseActivationDialog(
            onActivate = { code -> viewModel.activateLicense(code) },
            onSignOut = {
                coroutineScope.launch {
                    viewModel.prepareSignOut()
                    AuthManager.signOut(credentialManager, {}, this)
                }
            }
        )
    }

    if (showAccountDialog) {
        if (isGuestMode) {
            AlertDialog(
                onDismissRequest = { showAccountDialog = false },
                title = { Text(stringResource(R.string.account_guest_title), fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text(
                            text = stringResource(R.string.account_guest_active),
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.account_guest_desc),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showAccountDialog = false
                            coroutineScope.launch {
                                viewModel.prepareSignOut()
                                SessionPreferences.setGuestMode(context, false)
                            }
                        }
                    ) {
                        Text(stringResource(R.string.account_connect_google))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAccountDialog = false }) {
                        Text(stringResource(R.string.account_keep_guest))
                    }
                }
            )
        } else {
            AlertDialog(
                onDismissRequest = { showAccountDialog = false },
                title = { Text(stringResource(R.string.account_cloud_title), fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text(
                            text = if (currentUserName.isNotBlank()) {
                                stringResource(R.string.account_cloud_active_named, currentUserName)
                            } else {
                                stringResource(R.string.account_cloud_active)
                            },
                            fontWeight = FontWeight.SemiBold
                        )
                        if (currentUserEmail.isNotBlank()) {
                            Text(
                                text = currentUserEmail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.account_cloud_desc),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showAccountDialog = false
                            viewModel.retryCloudSync()
                        }
                    ) {
                        Text(stringResource(R.string.account_sync_now))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showAccountDialog = false
                            coroutineScope.launch {
                                viewModel.prepareSignOut()
                                AuthManager.signOut(
                                    credentialManager = credentialManager,
                                    onSignOutComplete = {},
                                    scope = this
                                )
                            }
                        }
                    ) {
                        Text(stringResource(R.string.account_sign_out), color = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = {
                Text(
                    stringResource(R.string.about_title, BuildConfig.VERSION_NAME),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    if (isGuestMode) {
                        stringResource(R.string.about_guest_desc)
                    } else {
                        stringResource(R.string.about_cloud_desc)
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text(stringResource(R.string.action_understood))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAboutDialog = false
                        showPrivacyDialog = true
                    }
                ) {
                    Text(stringResource(R.string.privacy_title))
                }
            }
        )
    }
}
