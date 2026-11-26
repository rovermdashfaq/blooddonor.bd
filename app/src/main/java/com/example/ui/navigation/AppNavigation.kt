package com.example.ui.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.data.model.UiState
import com.example.data.repository.AdminRepository
import com.example.data.repository.AuthHelper
import com.example.data.repository.DonorRepository
import com.example.data.repository.authStateFlow
import com.example.ui.components.OfflineBanner
import com.example.ui.components.ThemePickerDialog
import com.example.ui.screens.AdminPanelScreen
import com.example.ui.screens.AiAssistantScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CommunityAndStoriesScreen
import com.example.ui.screens.FindDonorsAndMapScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.MessagesScreen
import com.example.ui.screens.OnboardingProfileScreen
import com.example.ui.screens.ProfileAndToolsScreen
import com.example.ui.screens.RequestsScreen
import com.example.ui.viewmodel.AdminViewModel
import com.example.ui.viewmodel.AiAssistantViewModel
import com.example.ui.viewmodel.MainSessionViewModel
import com.example.ui.viewmodel.SocialAndRequestsViewModel
import com.example.util.BiometricLockHelper
import com.example.util.LocationHelper
import com.example.util.NetworkMonitor
import com.example.util.ThemeController
import com.example.util.ThemeSettings
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.serialization.Serializable

@Serializable
private object MainShellRoute

@Serializable
private object MessagesRoute

@Serializable
private object AiAssistantRoute

@Serializable
private object AdminPanelRoute

private enum class BottomTab(val label: String, val icon: ImageVector, val testTag: String) {
    HOME("Home", Icons.Default.Home, "nav_home"),
    DONORS("Donors", Icons.Default.People, "nav_donors"),
    REQUESTS("Requests", Icons.Default.Bloodtype, "nav_requests"),
    COMMUNITY("Feed", Icons.Default.DynamicFeed, "nav_community"),
    PROFILE("Profile", Icons.Default.Person, "nav_profile")
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val themeSettings by ThemeController.settings.collectAsStateWithLifecycle()
    val isOnline by NetworkMonitor.observeIsOnline(context).collectAsStateWithLifecycle(initialValue = true)
    var showThemePicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (auth.currentUser == null) {
            AuthHelper.attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                scope = scope,
                onAuthSuccess = {},
                onUnauthenticated = {}
            )
        }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            onAuthSuccess = {},
            isOnline = isOnline,
            onOpenThemePicker = { showThemePicker = true }
        )
    } else {
        key(user.uid) {
            AuthenticatedSessionGate(
                currentUserId = user.uid,
                currentUserEmail = user.email.orEmpty(),
                isEmailVerified = user.isEmailVerified,
                displayName = user.displayName.orEmpty(),
                isOnline = isOnline,
                themeSettings = themeSettings,
                onOpenThemePicker = { showThemePicker = true },
                onSignOut = {
                    AuthHelper.signOut(credentialManager = credentialManager, scope = scope)
                }
            )
        }
    }

    if (showThemePicker) {
        ThemePickerDialog(
            currentSettings = themeSettings,
            onSelectMode = { mode -> ThemeController.setThemeMode(context, mode) },
            onSelectColor = { color -> ThemeController.setColorTheme(context, color) },
            onDismiss = { showThemePicker = false }
        )
    }
}

@Composable
private fun AuthenticatedSessionGate(
    currentUserId: String,
    currentUserEmail: String,
    isEmailVerified: Boolean,
    displayName: String,
    isOnline: Boolean,
    themeSettings: ThemeSettings,
    onOpenThemePicker: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val requiresBiometric = remember(currentUserId) {
        BiometricLockHelper.isBiometricLockEnabled(context, currentUserId) &&
            BiometricLockHelper.isBiometricAvailable(context)
    }
    var unlocked by rememberSaveable(currentUserId) { mutableStateOf(!requiresBiometric) }
    var lockError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(requiresBiometric, unlocked) {
        if (requiresBiometric && !unlocked && activity != null) {
            BiometricLockHelper.promptBiometric(
                activity = activity,
                title = "Unlock Blood Donor BD",
                subtitle = "Verify your biometric to access donor records",
                negativeButtonText = "Cancel",
                onSuccess = { unlocked = true },
                onError = { err -> lockError = err }
            )
        }
    }

    if (!unlocked) {
        Scaffold(contentWindowInsets = WindowInsets.safeDrawing) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(
                        Icons.Default.Fingerprint,
                        contentDescription = "Biometric Lock",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(64.dp)
                    )
                    Text("Blood Donor BD Locked", style = MaterialTheme.typography.headlineMedium)
                    if (!lockError.isNullOrBlank()) {
                        Text(lockError.orEmpty(), color = MaterialTheme.colorScheme.error)
                    }
                    Button(
                        onClick = {
                            if (activity != null) {
                                lockError = null
                                BiometricLockHelper.promptBiometric(
                                    activity = activity,
                                    title = "Unlock Blood Donor BD",
                                    subtitle = "Verify your biometric to continue",
                                    negativeButtonText = "Cancel",
                                    onSuccess = { unlocked = true },
                                    onError = { err -> lockError = err }
                                )
                            } else {
                                unlocked = true
                            }
                        },
                        modifier = Modifier.heightIn(min = 48.dp).testTag("biometric_unlock_button")
                    ) {
                        Text("Unlock with Biometric")
                    }
                    OutlinedButton(onClick = onSignOut) {
                        Text("Sign Out")
                    }
                }
            }
        }
        return
    }

    val sessionViewModel: MainSessionViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                MainSessionViewModel(
                    repository = DonorRepository(db),
                    currentUserId = currentUserId,
                    currentUserEmail = currentUserEmail,
                    isEmailVerified = isEmailVerified
                )
            }
        }
    )

    val profileState by sessionViewModel.userProfile.collectAsStateWithLifecycle()
    val isSaving by sessionViewModel.isSaving.collectAsStateWithLifecycle()
    val actionMessage by sessionViewModel.actionMessage.collectAsStateWithLifecycle()

    when (val state = profileState) {
        UiState.Loading -> {
            Scaffold(contentWindowInsets = WindowInsets.safeDrawing) { inner ->
                Box(Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White,
                            shadowElevation = 4.dp,
                            modifier = Modifier.size(88.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.blood_donor_bd_logo),
                                contentDescription = "Blood Donor BD Logo",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(4.dp)
                                    .clip(CircleShape)
                            )
                        }
                        CircularProgressIndicator()
                        Text("Loading Blood Donor BD...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        is UiState.Error -> {
            Scaffold(contentWindowInsets = WindowInsets.safeDrawing) { inner ->
                Box(Modifier.fillMaxSize().padding(inner).padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Unable to load profile", style = MaterialTheme.typography.titleLarge)
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                        OutlinedButton(onClick = onSignOut) { Text("Sign Out") }
                    }
                }
            }
        }
        is UiState.Success -> {
            val profile = state.data
            if (profile == null) {
                OnboardingProfileScreen(
                    email = currentUserEmail,
                    initialName = displayName,
                    isSaving = isSaving,
                    errorMessage = actionMessage,
                    onSubmit = { fullName, phone, nid, bday, weight, userType, bg, dist, upz, addr, gender, bio ->
                        sessionViewModel.saveOnboarding(
                            fullName, phone, nid, bday, weight, userType,
                            bg, dist, upz, addr, gender, bio
                        )
                    },
                    onSignOut = onSignOut
                )
            } else {
                AuthenticatedNavGraph(
                    currentUserId = currentUserId,
                    currentUser = profile,
                    sessionViewModel = sessionViewModel,
                    isOnline = isOnline,
                    themeSettings = themeSettings,
                    onOpenThemePicker = onOpenThemePicker,
                    onSignOut = onSignOut
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedNavGraph(
    currentUserId: String,
    currentUser: com.example.data.model.UserProfile,
    sessionViewModel: MainSessionViewModel,
    isOnline: Boolean,
    themeSettings: ThemeSettings,
    onOpenThemePicker: () -> Unit,
    onSignOut: () -> Unit
) {
    val navController = rememberNavController()

    val socialViewModel: SocialAndRequestsViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                SocialAndRequestsViewModel(
                    repository = DonorRepository(db),
                    currentUserId = currentUserId
                )
            }
        }
    )

    val aiViewModel: AiAssistantViewModel = viewModel(
        key = "ai_$currentUserId",
        factory = viewModelFactory {
            initializer { AiAssistantViewModel() }
        }
    )

    val adminRecordRole by sessionViewModel.adminRoleRecord.collectAsStateWithLifecycle()
    val effectiveRole = sessionViewModel.effectiveRole(currentUser, adminRecordRole)
    val isEffectiveAdmin = sessionViewModel.isEffectiveAdmin(currentUser, adminRecordRole)
    val isMainAdmin = effectiveRole == "main_admin"

    val donorsState by sessionViewModel.approvedDonors.collectAsStateWithLifecycle()
    val noticesState by sessionViewModel.activeNotices.collectAsStateWithLifecycle()
    val myCertsState by sessionViewModel.myCertificates.collectAsStateWithLifecycle()
    val verificationState by sessionViewModel.verificationState.collectAsStateWithLifecycle()
    val isSaving by sessionViewModel.isSaving.collectAsStateWithLifecycle()
    val actionMessage by sessionViewModel.actionMessage.collectAsStateWithLifecycle()

    val activeRequestsState by socialViewModel.activeRequests.collectAsStateWithLifecycle()
    val myRequestsState by socialViewModel.myRequests.collectAsStateWithLifecycle()
    val conversationsState by socialViewModel.conversations.collectAsStateWithLifecycle()
    val selectedConversationId by socialViewModel.selectedConversationId.collectAsStateWithLifecycle()
    val dmMessagesState by socialViewModel.dmMessages.collectAsStateWithLifecycle()
    val groupChatState by socialViewModel.communityChat.collectAsStateWithLifecycle()
    val publicPostsState by socialViewModel.publicPosts.collectAsStateWithLifecycle()
    val storiesState by socialViewModel.successStories.collectAsStateWithLifecycle()
    val socialBanner by socialViewModel.statusBanner.collectAsStateWithLifecycle()

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var openDonorsMapNext by rememberSaveable { mutableStateOf(false) }

    val myLiveLocation by sessionViewModel.myLiveLocation.collectAsStateWithLifecycle()
    val isLiveLocationSharing by sessionViewModel.isLiveLocationSharing.collectAsStateWithLifecycle()

    val appContext = LocalContext.current
    LaunchedEffect(currentUserId) {
        if (LocationHelper.hasLocationPermission(appContext)) {
            LocationHelper.getLastKnownGeoPoint(appContext)?.let {
                sessionViewModel.setLocalGpsPreview(it)
            }
        }
    }

    LaunchedEffect(isLiveLocationSharing, currentUserId) {
        if (isLiveLocationSharing && LocationHelper.hasLocationPermission(appContext)) {
            LocationHelper.observeLiveLocation(appContext).collect { point ->
                sessionViewModel.updateLiveGpsLocation(
                    existing = currentUser,
                    point = point,
                    syncToDirectory = true,
                    showToast = false
                )
            }
        }
    }

    val totalUnreadDm = remember(conversationsState, currentUserId) {
        val convs = (conversationsState as? UiState.Success)?.data ?: emptyList()
        convs.sumOf { (it.unreadCounts[currentUserId] ?: 0L).toInt() }
    }

    NavHost(
        navController = navController,
        startDestination = MainShellRoute
    ) {
        composable<MainShellRoute> {
            val snackbarHostState = remember { SnackbarHostState() }
            LaunchedEffect(actionMessage, socialBanner) {
                val msg = actionMessage ?: socialBanner
                if (!msg.isNullOrBlank()) {
                    snackbarHostState.showSnackbar(msg)
                    sessionViewModel.clearMessage()
                    socialViewModel.clearBanner()
                }
            }

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val useRail = maxWidth >= 600.dp

                Scaffold(
                    contentWindowInsets = WindowInsets.safeDrawing,
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        TopAppBar(
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color.White,
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Image(
                                            painter = painterResource(id = R.drawable.blood_donor_bd_logo),
                                            contentDescription = "Blood Donor BD Logo",
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(2.dp)
                                                .clip(CircleShape)
                                        )
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        text = when (BottomTab.entries[selectedTab]) {
                                            BottomTab.HOME -> "Blood Donor BD"
                                            BottomTab.DONORS -> "Verified Donors & Map"
                                            BottomTab.REQUESTS -> "Blood Requests"
                                            BottomTab.COMMUNITY -> "Community & Stories"
                                            BottomTab.PROFILE -> "Profile & Verification"
                                        },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            },
                            actions = {
                                IconButton(
                                    onClick = onOpenThemePicker,
                                    modifier = Modifier.testTag("top_theme_button")
                                ) {
                                    Icon(
                                        Icons.Default.Palette,
                                        contentDescription = "App Theme and Colors"
                                    )
                                }
                                IconButton(
                                    onClick = { navController.navigate(MessagesRoute) },
                                    modifier = Modifier.testTag("top_messages_button")
                                ) {
                                    BadgedBox(
                                        badge = {
                                            if (totalUnreadDm > 0) {
                                                Badge { Text(totalUnreadDm.toString()) }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.Chat,
                                            contentDescription = "Direct Messages and Group Chat"
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { navController.navigate(AiAssistantRoute) },
                                    modifier = Modifier.testTag("top_ai_assistant_button")
                                ) {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = "Blood Donor BD AI Assistant",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        )
                    },
                    bottomBar = {
                        if (!useRail) {
                            NavigationBar {
                                BottomTab.entries.forEachIndexed { idx, tab ->
                                    NavigationBarItem(
                                        selected = selectedTab == idx,
                                        onClick = {
                                            if (idx != BottomTab.DONORS.ordinal) openDonorsMapNext = false
                                            selectedTab = idx
                                        },
                                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                                        label = { Text(tab.label) },
                                        modifier = Modifier.testTag(tab.testTag)
                                    )
                                }
                            }
                        }
                    },
                    floatingActionButton = {
                        if (selectedTab == BottomTab.HOME.ordinal) {
                            FloatingActionButton(
                                onClick = { navController.navigate(AiAssistantRoute) },
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.testTag("home_ai_fab")
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = "Open AI Assistant")
                            }
                        }
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (!isOnline) {
                            OfflineBanner()
                        }
                        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            if (useRail) {
                                NavigationRail(modifier = Modifier.fillMaxHeight()) {
                                    BottomTab.entries.forEachIndexed { idx, tab ->
                                        NavigationRailItem(
                                            selected = selectedTab == idx,
                                            onClick = {
                                                if (idx != BottomTab.DONORS.ordinal) openDonorsMapNext = false
                                                selectedTab = idx
                                            },
                                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                                            label = { Text(tab.label) },
                                            modifier = Modifier.testTag(tab.testTag)
                                        )
                                    }
                                }
                            }

                            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            when (BottomTab.entries[selectedTab]) {
                                    BottomTab.HOME -> {
                                        val donorCount = (donorsState as? UiState.Success)?.data?.size ?: 0
                                        HomeDashboardScreen(
                                            currentUser = currentUser,
                                            effectiveRole = effectiveRole,
                                            noticesState = noticesState,
                                            activeRequestsState = activeRequestsState,
                                            approvedDonorsCount = donorCount,
                                            onOpenFindDonors = {
                                                openDonorsMapNext = false
                                                selectedTab = BottomTab.DONORS.ordinal
                                            },
                                            onOpenDonorMap = {
                                                openDonorsMapNext = true
                                                selectedTab = BottomTab.DONORS.ordinal
                                            },
                                            onOpenRequests = { selectedTab = BottomTab.REQUESTS.ordinal },
                                            onOpenMessages = { navController.navigate(MessagesRoute) },
                                            onOpenVerify = { selectedTab = BottomTab.PROFILE.ordinal },
                                            onOpenAiAssistant = { navController.navigate(AiAssistantRoute) },
                                            onSubmitDonationProof = { date, units, proof, code ->
                                                sessionViewModel.submitDonationProof(date, units, proof, code)
                                            }
                                        )
                                    }

                                    BottomTab.DONORS -> {
                                        val posts = (publicPostsState as? UiState.Success)?.data ?: emptyList()
                                        FindDonorsAndMapScreen(
                                            currentUid = currentUserId,
                                            donorsState = donorsState,
                                            publicPosts = posts,
                                            myLocation = myLiveLocation,
                                            initialMapTab = openDonorsMapNext,
                                            onUpdateMyLocation = { point ->
                                                sessionViewModel.updateLiveGpsLocation(
                                                    existing = currentUser,
                                                    point = point,
                                                    syncToDirectory = currentUser.donorEnabled,
                                                    showToast = currentUser.donorEnabled
                                                )
                                            },
                                            onMessageDonor = { donor ->
                                                socialViewModel.startDmWithDonor(
                                                    myProfile = currentUser,
                                                    peerUid = donor.uid,
                                                    peerName = donor.fullName,
                                                    peerBloodGroup = donor.bloodGroup
                                                ) {
                                                    navController.navigate(MessagesRoute)
                                                }
                                            }
                                        )
                                    }

                                    BottomTab.REQUESTS -> {
                                        RequestsScreen(
                                            currentUser = currentUser,
                                            activeRequestsState = activeRequestsState,
                                            myRequestsState = myRequestsState,
                                            onCreateRequest = { pName, pAge, pGen, bg, units, hosp, hospAddr, dist, phone, reqBy, reqTime, urg, notes ->
                                                socialViewModel.createBloodRequest(
                                                    requesterName = currentUser.fullName,
                                                    patientName = pName,
                                                    patientAge = pAge,
                                                    patientGender = pGen,
                                                    bloodGroup = bg,
                                                    unitsNeeded = units,
                                                    hospitalName = hosp,
                                                    hospitalAddress = hospAddr,
                                                    district = dist,
                                                    contactNumber = phone,
                                                    requiredBy = reqBy,
                                                    requiredTime = reqTime,
                                                    urgency = urg,
                                                    notes = notes
                                                )
                                            },
                                            onMarkStatus = { reqId, status ->
                                                socialViewModel.markRequestStatus(reqId, status)
                                            }
                                        )
                                    }

                                    BottomTab.COMMUNITY -> {
                                        CommunityAndStoriesScreen(
                                            currentUser = currentUser,
                                            postsState = publicPostsState,
                                            storiesState = storiesState,
                                            onCreatePost = { text, img, pq, opts ->
                                                socialViewModel.createPost(currentUser, text, img, pq, opts)
                                            },
                                            onEditPost = { postId, text, img ->
                                                socialViewModel.editPost(postId, text, img)
                                            },
                                            onTogglePostLike = { socialViewModel.togglePostLike(it) },
                                            onAddComment = { postId, text ->
                                                socialViewModel.addComment(postId, currentUser.fullName, text)
                                            },
                                            onVotePoll = { postId, optId -> socialViewModel.votePoll(postId, optId) },
                                            onDeletePost = { socialViewModel.deletePost(it) },
                                            onReportPost = { postId, reason -> socialViewModel.reportPost(postId, reason) },
                                            onCreateStory = { title, story, hosp, bg ->
                                                socialViewModel.createStory(currentUser.fullName, title, story, hosp, bg)
                                            },
                                            onToggleStoryLike = { socialViewModel.toggleStoryLike(it) }
                                        )
                                    }

                                    BottomTab.PROFILE -> {
                                        ProfileAndToolsScreen(
                                            currentUser = currentUser,
                                            effectiveRole = effectiveRole,
                                            isAdmin = isEffectiveAdmin,
                                            isSaving = isSaving,
                                            myCertificatesState = myCertsState,
                                            verificationState = verificationState,
                                            themeSettings = themeSettings,
                                            myLiveLocation = myLiveLocation,
                                            isLiveLocationSharing = isLiveLocationSharing,
                                            onOpenThemePicker = onOpenThemePicker,
                                            onUpdateLiveGps = { point ->
                                                sessionViewModel.updateLiveGpsLocation(
                                                    existing = currentUser,
                                                    point = point,
                                                    syncToDirectory = true,
                                                    showToast = true
                                                )
                                            },
                                            onToggleLiveSharing = { enabled ->
                                                sessionViewModel.setLiveLocationSharing(enabled)
                                            },
                                            onUpdateProfile = { name, phone, nid, bday, weight, donorOn, bg, dist, upz, addr, gen, bio, pic, avail ->
                                                sessionViewModel.updateProfile(
                                                    existing = currentUser,
                                                    fullName = name,
                                                    phoneNumber = phone,
                                                    nidNumber = nid,
                                                    birthday = bday,
                                                    weightKg = weight,
                                                    donorEnabled = donorOn,
                                                    bloodGroup = bg,
                                                    district = dist,
                                                    upazila = upz,
                                                    address = addr,
                                                    gender = gen,
                                                    bio = bio,
                                                    profilePic = pic,
                                                    available = avail
                                                )
                                            },
                                            onVerifyQuery = { query, isCert ->
                                                sessionViewModel.verifyCertificateOrDonor(query, isCert)
                                            },
                                            onResetVerification = { sessionViewModel.resetVerification() },
                                            onOpenAdminPanel = {
                                                if (isEffectiveAdmin) navController.navigate(AdminPanelRoute)
                                            },
                                            onSignOut = onSignOut
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        composable<MessagesRoute> {
            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing,
                topBar = {
                    TopAppBar(
                        title = { Text("Donor Chat & Direct Messages") },
                        navigationIcon = {
                            IconButton(
                                onClick = { navController.popBackStack() },
                                modifier = Modifier.testTag("messages_screen_back")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    )
                }
            ) { innerPadding ->
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                    MessagesScreen(
                        currentUser = currentUser,
                        effectiveRole = effectiveRole,
                        donorsState = donorsState,
                        conversationsState = conversationsState,
                        selectedConversationId = selectedConversationId,
                        dmMessagesState = dmMessagesState,
                        groupChatState = groupChatState,
                        onSelectConversation = { convId, peerUid ->
                            socialViewModel.selectConversation(convId, peerUid)
                        },
                        onStartDmWithDonor = { donor ->
                            socialViewModel.startDmWithDonor(
                                myProfile = currentUser,
                                peerUid = donor.uid,
                                peerName = donor.fullName,
                                peerBloodGroup = donor.bloodGroup,
                                onConversationReady = {}
                            )
                        },
                        onSendDm = { convId, peerUid, peerName, peerBg, text ->
                            socialViewModel.sendDm(convId, currentUser, peerUid, peerName, peerBg, text)
                        },
                        onSendGroupChat = { text ->
                            socialViewModel.sendGroupChat(currentUser, effectiveRole, text)
                        }
                    )
                }
            }
        }

        composable<AiAssistantRoute> {
            AiAssistantScreen(
                viewModel = aiViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<AdminPanelRoute> {
            if (isEffectiveAdmin) {
                val adminViewModel: AdminViewModel = viewModel(
                    key = "admin_$currentUserId",
                    factory = viewModelFactory {
                        initializer {
                            val app = checkNotNull(this[APPLICATION_KEY])
                            val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                            AdminViewModel(
                                adminRepository = AdminRepository(db),
                                adminUid = currentUserId
                            )
                        }
                    }
                )
                AdminPanelScreen(
                    viewModel = adminViewModel,
                    isMainAdmin = isMainAdmin,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
