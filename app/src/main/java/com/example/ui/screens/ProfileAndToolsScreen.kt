package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.example.data.model.DonorCertificate
import com.example.data.model.DonorEligibilityUtils
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.ui.components.AccountRecoveryDialog
import com.example.ui.components.AmbitionsDialog
import com.example.ui.components.BloodGroupSelectorRow
import com.example.ui.components.CertificateCard
import com.example.ui.components.CertificateDetailDialog
import com.example.ui.components.DigitalDonorIdCard
import com.example.ui.components.DistrictDropdownField
import com.example.ui.components.LegalDialog
import com.example.ui.components.sharePlainText
import com.example.ui.viewmodel.VerificationLookupState
import com.example.util.BiometricLockHelper
import com.example.util.GeoPoint
import com.example.util.LocationHelper
import com.example.util.ThemeSettings
import kotlinx.coroutines.launch

@Composable
fun ProfileAndToolsScreen(
    currentUser: UserProfile,
    effectiveRole: String,
    isAdmin: Boolean,
    isSaving: Boolean,
    myCertificatesState: UiState<List<DonorCertificate>>,
    verificationState: VerificationLookupState,
    themeSettings: ThemeSettings = ThemeSettings(),
    myLiveLocation: GeoPoint? = null,
    isLiveLocationSharing: Boolean = false,
    onOpenThemePicker: () -> Unit = {},
    onUpdateLiveGps: (GeoPoint) -> Unit = {},
    onToggleLiveSharing: (Boolean) -> Unit = {},
    onUpdateProfile: (
        fullName: String,
        phoneNumber: String,
        nidNumber: String,
        birthday: String,
        weightKg: Long,
        donorEnabled: Boolean,
        bloodGroup: String,
        district: String,
        upazila: String,
        address: String,
        gender: String,
        bio: String,
        profilePic: String?,
        available: Boolean
    ) -> Unit,
    onVerifyQuery: (query: String, isCertificate: Boolean) -> Unit,
    onResetVerification: () -> Unit,
    onOpenAdminPanel: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val activity = context as? FragmentActivity
    val biometricSupported = remember(context) { BiometricLockHelper.isBiometricAvailable(context) }
    var biometricEnabled by remember(currentUser.uid) {
        mutableStateOf(BiometricLockHelper.isBiometricLockEnabled(context, currentUser.uid))
    }
    var biometricNote by remember { mutableStateOf<String?>(null) }
    var isFetchingGps by remember { mutableStateOf(false) }
    var gpsStatusNote by remember { mutableStateOf<String?>(null) }
    var pendingAutoShareEnable by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showAmbitions by remember { mutableStateOf(false) }
    var showLegal by remember { mutableStateOf(false) }
    var showAccountRecovery by remember { mutableStateOf(false) }
    var inspectedCertificate by remember { mutableStateOf<DonorCertificate?>(null) }

    var verifyInput by remember { mutableStateOf("") }
    var verifyCertMode by remember { mutableStateOf(true) }

    fun fetchAndSyncGps() {
        scope.launch {
            isFetchingGps = true
            gpsStatusNote = null
            val point = LocationHelper.fetchCurrentLocation(context)
            isFetchingGps = false
            if (point != null) {
                onUpdateLiveGps(point)
                if (pendingAutoShareEnable) {
                    onToggleLiveSharing(true)
                    pendingAutoShareEnable = false
                }
            } else {
                pendingAutoShareEnable = false
                gpsStatusNote = if (!LocationHelper.isLocationEnabled(context)) {
                    "Turn on device Location/GPS first."
                } else {
                    "Unable to get GPS fix right now."
                }
            }
        }
    }

    val gpsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms.values.any { it }) {
            fetchAndSyncGps()
        } else {
            pendingAutoShareEnable = false
            gpsStatusNote = "Location permission denied."
        }
    }

    val resolvedDonorId = currentUser.donorId ?: DonorEligibilityUtils.generateDonorId(currentUser.uid)

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            DigitalDonorIdCard(
                fullName = currentUser.fullName,
                donorId = resolvedDonorId,
                bloodGroup = currentUser.bloodGroup ?: "--",
                district = currentUser.district ?: "Bangladesh",
                role = effectiveRole,
                totalDonations = currentUser.totalDonations,
                approvalStatus = currentUser.approvalStatus
            )

            OutlinedButton(
                onClick = {
                    val shareBody = "Blood Donor BD · Digital Donor ID\n" +
                        "Name: ${currentUser.fullName}\n" +
                        "Blood Group: ${currentUser.bloodGroup ?: "--"}\n" +
                        "Donor ID: $resolvedDonorId\n" +
                        "District: ${currentUser.district ?: "Bangladesh"}\n" +
                        "Total Donations: ${currentUser.totalDonations}\n" +
                        "Status: ${currentUser.approvalStatus}"
                    sharePlainText(context, "My Blood Donor BD ID Card", shareBody)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("share_my_id_card_button")
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Share Digital Donor ID Card")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { showEditDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("edit_profile_button")
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Edit Profile")
                }
                if (isAdmin) {
                    Button(
                        onClick = onOpenAdminPanel,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("open_admin_panel_button")
                    ) {
                        Icon(Icons.Default.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Admin Panel")
                    }
                }
            }

            // Appearance & Theme Customization Card
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(themeSettings.color.dark, themeSettings.color.primary)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Palette,
                                contentDescription = null,
                                tint = androidx.compose.ui.graphics.Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Appearance & Theme", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Mode: ${themeSettings.mode.label} • Color: ${themeSettings.color.label}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = onOpenThemePicker,
                        modifier = Modifier.heightIn(min = 48.dp).testTag("profile_theme_button")
                    ) {
                        Text("Customize")
                    }
                }
            }

            // Biometric Lock Card
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("Biometric App Lock", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    if (biometricSupported) "Protect donor data with fingerprint or face lock"
                                    else "No biometric hardware enrolled on this device",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = biometricEnabled,
                            enabled = biometricSupported && activity != null,
                            onCheckedChange = { target ->
                                if (activity == null) return@Switch
                                BiometricLockHelper.promptBiometric(
                                    activity = activity,
                                    title = if (target) "Enable Biometric Lock" else "Disable Biometric Lock",
                                    subtitle = "Confirm your device biometric to update lock settings",
                                    negativeButtonText = "Cancel",
                                    onSuccess = {
                                        BiometricLockHelper.setBiometricLockEnabled(context, currentUser.uid, target)
                                        biometricEnabled = target
                                        biometricNote = if (target) "Biometric Lock enabled." else "Biometric Lock disabled."
                                    },
                                    onError = { err -> biometricNote = err }
                                )
                            },
                            modifier = Modifier.testTag("biometric_lock_switch")
                        )
                    }
                    if (!biometricNote.isNullOrBlank()) {
                        Text(biometricNote.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // Live Donor GPS Location Card (GitHub Live Map Sync)
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val latText = myLiveLocation?.lat ?: currentUser.lat
                    val lngText = myLiveLocation?.lng ?: currentUser.lng
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.MyLocation, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("Live Donor GPS Location", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = if (latText != null && lngText != null) {
                                        "Map pin: ${String.format(java.util.Locale.US, "%.4f, %.4f", latText, lngText)}"
                                    } else {
                                        "Not synced yet (${currentUser.district ?: "Bangladesh"})"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = isLiveLocationSharing,
                            onCheckedChange = { enabled ->
                                if (!enabled) {
                                    onToggleLiveSharing(false)
                                } else if (LocationHelper.hasLocationPermission(context)) {
                                    onToggleLiveSharing(true)
                                    fetchAndSyncGps()
                                } else {
                                    pendingAutoShareEnable = true
                                    gpsPermissionLauncher.launch(LocationHelper.REQUIRED_PERMISSIONS)
                                }
                            },
                            modifier = Modifier.testTag("profile_live_gps_switch")
                        )
                    }
                    Text(
                        text = "Turn on Auto-Share or tap Update to broadcast your real GPS position to the Live BD Map.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = {
                            if (LocationHelper.hasLocationPermission(context)) {
                                fetchAndSyncGps()
                            } else {
                                gpsPermissionLauncher.launch(LocationHelper.REQUIRED_PERMISSIONS)
                            }
                        },
                        enabled = !isFetchingGps && !isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("profile_update_gps_button")
                    ) {
                        if (isFetchingGps) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("Update My Live GPS Location")
                    }
                    if (!gpsStatusNote.isNullOrBlank()) {
                        Text(gpsStatusNote.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            // Verify Donor ID or Certificate Tool
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Verify Certificate or Donor ID", style = MaterialTheme.typography.titleMedium)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = verifyCertMode,
                            onClick = { verifyCertMode = true; onResetVerification() },
                            label = { Text("Certificate ID") }
                        )
                        FilterChip(
                            selected = !verifyCertMode,
                            onClick = { verifyCertMode = false; onResetVerification() },
                            label = { Text("Donor ID (BD-...)") }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = verifyInput,
                            onValueChange = { verifyInput = it },
                            label = { Text(if (verifyCertMode) "CERT-..." else "BD-...") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("verify_id_input")
                        )
                        Button(
                            onClick = { onVerifyQuery(verifyInput, verifyCertMode) },
                            modifier = Modifier.heightIn(min = 48.dp).testTag("verify_id_button")
                        ) {
                            Text("Verify")
                        }
                    }

                    when (verificationState) {
                        VerificationLookupState.Idle -> Unit
                        VerificationLookupState.Searching -> CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        is VerificationLookupState.CertificateFound -> CertificateCard(
                            cert = verificationState.certificate,
                            onClick = { inspectedCertificate = verificationState.certificate }
                        )
                        is VerificationLookupState.DonorFound -> {
                            val d = verificationState.donor
                            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.tertiaryContainer) {
                                Text(
                                    "Verified Donor: ${d.fullName} (${d.bloodGroup}) • ${d.district} • ${d.totalDonations} Donations",
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                        is VerificationLookupState.NotFound -> Text(
                            "No verified record found for '${verificationState.query}'.",
                            color = MaterialTheme.colorScheme.error
                        )
                        is VerificationLookupState.Error -> Text(
                            verificationState.message,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // My Certificates
            Text("My Official Certificates", style = MaterialTheme.typography.titleMedium)
            when (myCertificatesState) {
                UiState.Loading -> CircularProgressIndicator()
                is UiState.Error -> Text(myCertificatesState.message, color = MaterialTheme.colorScheme.error)
                is UiState.Success -> {
                    if (myCertificatesState.data.isEmpty()) {
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                            Text(
                                "Certificates issued by administrators for verified donations will appear here.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        myCertificatesState.data.forEach { cert ->
                            CertificateCard(cert = cert, onClick = { inspectedCertificate = cert })
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showAmbitions = true },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Ambitions")
                }
                OutlinedButton(
                    onClick = { showLegal = true },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Default.Gavel, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Privacy & Terms")
                }
            }

            OutlinedButton(
                onClick = { showAccountRecovery = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("profile_account_help_button")
            ) {
                Icon(Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Account Recovery & Helpline")
            }

            OutlinedButton(
                onClick = onSignOut,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp)
                    .testTag("sign_out_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Sign Out")
            }
        }
    }

    if (showEditDialog) {
        EditProfileDialog(
            user = currentUser,
            isSaving = isSaving,
            onDismiss = { showEditDialog = false },
            onSave = { name, phone, nid, bday, weight, donorOn, bg, dist, upz, addr, gen, bio, pic, avail ->
                onUpdateProfile(name, phone, nid, bday, weight, donorOn, bg, dist, upz, addr, gen, bio, pic, avail)
                showEditDialog = false
            }
        )
    }

    inspectedCertificate?.let { cert ->
        CertificateDetailDialog(
            cert = cert,
            onDismiss = { inspectedCertificate = null }
        )
    }

    if (showAmbitions) AmbitionsDialog(onDismiss = { showAmbitions = false })
    if (showLegal) LegalDialog(onDismiss = { showLegal = false })
    if (showAccountRecovery) AccountRecoveryDialog(onDismiss = { showAccountRecovery = false })
}

@Composable
private fun EditProfileDialog(
    user: UserProfile,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        fullName: String,
        phoneNumber: String,
        nidNumber: String,
        birthday: String,
        weightKg: Long,
        donorEnabled: Boolean,
        bloodGroup: String,
        district: String,
        upazila: String,
        address: String,
        gender: String,
        bio: String,
        profilePic: String?,
        available: Boolean
    ) -> Unit
) {
    var fullName by remember { mutableStateOf(user.fullName) }
    var phone by remember { mutableStateOf(user.phoneNumber.orEmpty()) }
    var nid by remember { mutableStateOf(user.nidNumber.orEmpty()) }
    var birthday by remember { mutableStateOf(user.birthday.orEmpty()) }
    var weight by remember { mutableStateOf((user.weightKg ?: 60L).toString()) }
    var donorEnabled by remember { mutableStateOf(user.donorEnabled) }
    var available by remember { mutableStateOf(true) }
    var bloodGroup by remember { mutableStateOf(user.bloodGroup ?: "O+") }
    var district by remember { mutableStateOf(user.district ?: "Dhaka") }
    var upazila by remember { mutableStateOf(user.upazila.orEmpty()) }
    var address by remember { mutableStateOf(user.address.orEmpty()) }
    var gender by remember { mutableStateOf(user.gender ?: "male") }
    var bio by remember { mutableStateOf(user.bio.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Profile") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Full Name *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Register as Blood Donor")
                    Switch(checked = donorEnabled, onCheckedChange = { donorEnabled = it })
                }
                if (donorEnabled) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Available for Emergency Calls")
                        Switch(checked = available, onCheckedChange = { available = it })
                    }
                }
                BloodGroupSelectorRow(selectedGroup = bloodGroup, onSelect = { bloodGroup = it })
                DistrictDropdownField(selectedDistrict = district, onDistrictSelected = { district = it })
                OutlinedTextField(value = upazila, onValueChange = { upazila = it }, label = { Text("Upazila") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it.filter { c -> c.isDigit() } },
                    label = { Text("Weight (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(value = birthday, onValueChange = { birthday = it }, label = { Text("Birthday (YYYY-MM-DD)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = nid, onValueChange = { nid = it }, label = { Text("NID (Private)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address (Private)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = bio, onValueChange = { bio = it }, label = { Text("Bio") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        fullName, phone, nid, birthday,
                        weight.toLongOrNull() ?: 60L,
                        donorEnabled, bloodGroup, district, upazila, address,
                        gender, bio, user.profilePic, available
                    )
                },
                enabled = !isSaving && fullName.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
