package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
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
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.model.BangladeshData
import com.example.data.model.DonorEligibilityUtils
import com.example.data.repository.AuthHelper
import com.example.ui.components.AccountRecoveryDialog
import com.example.ui.components.AmbitionsDialog
import com.example.ui.components.BloodGroupSelectorRow
import com.example.ui.components.DistrictDropdownField
import com.example.ui.components.LegalDialog
import com.example.ui.components.OfflineBanner
import com.example.ui.components.dialPhoneNumber

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    isOnline: Boolean = true,
    onOpenThemePicker: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var isLoading by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }
    var showAmbitions by remember { mutableStateOf(false) }
    var showAccountHelp by remember { mutableStateOf(false) }
    var legalTab by remember { mutableStateOf<Int?>(null) }

    val themePrimary = MaterialTheme.colorScheme.primary
    val themeDeep = lerp(themePrimary, Color.Black, 0.35f)

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (!isOnline) {
                    OfflineBanner()
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .clip(RoundedCornerShape(24.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_banner),
                        contentDescription = "Blood Donor BD Bangladesh humanitarian banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Transparent,
                                        themeDeep.copy(alpha = 0.88f)
                                    )
                                )
                            )
                    )
                    FilledTonalIconButton(
                        onClick = onOpenThemePicker,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                            .testTag("auth_theme_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Change app theme and colors"
                        )
                    }
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(18.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White,
                                shadowElevation = 4.dp,
                                modifier = Modifier.size(56.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.blood_donor_bd_logo),
                                    contentDescription = "Blood Donor BD Official Logo",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(2.dp)
                                        .clip(CircleShape)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Blood Donor BD",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Every Drop Saves a Life · 64 Districts Network",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.92f)
                                )
                            }
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Connect With Verified Donors Across Bangladesh",
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Sign in securely with Google to search verified donors, post urgent hospital requests, message donors directly, verify certificates, and ask our AI assistant.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        if (authError != null) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = authError.orEmpty(),
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        Button(
                            onClick = {
                                authError = null
                                isLoading = true
                                AuthHelper.onGoogleSignInClicked(
                                    context = context,
                                    credentialManager = credentialManager,
                                    scope = scope,
                                    onAuthSuccess = {
                                        isLoading = false
                                        onAuthSuccess()
                                    },
                                    onAuthError = { msg ->
                                        isLoading = false
                                        authError = msg
                                    },
                                    onAuthCancelled = {
                                        isLoading = false
                                    }
                                )
                            },
                            enabled = !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .testTag("google_sign_in_button")
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.Security, contentDescription = null)
                                Spacer(Modifier.width(10.dp))
                                Text("Continue with Google", style = MaterialTheme.typography.labelLarge)
                            }
                        }

                        TextButton(
                            onClick = { showAccountHelp = true },
                            modifier = Modifier.testTag("auth_help_button")
                        ) {
                            Icon(Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Account Recovery & Sign-In Help")
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showAmbitions = true },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("auth_ambitions_button")
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Ambitions")
                    }
                    OutlinedButton(
                        onClick = { legalTab = 0 },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("auth_privacy_button")
                    ) {
                        Icon(Icons.Default.Gavel, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Privacy & Terms")
                    }
                }

                TextButton(
                    onClick = { dialPhoneNumber(context, BangladeshData.EMERGENCY_HELPLINE) },
                    modifier = Modifier.testTag("auth_helpline_button")
                ) {
                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Emergency Helpline: ${BangladeshData.EMERGENCY_HELPLINE}",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }

    if (showAmbitions) AmbitionsDialog(onDismiss = { showAmbitions = false })
    if (showAccountHelp) AccountRecoveryDialog(onDismiss = { showAccountHelp = false })
    if (legalTab != null) LegalDialog(initialTab = legalTab ?: 0, onDismiss = { legalTab = null })
}

@Composable
fun OnboardingProfileScreen(
    email: String,
    initialName: String,
    isSaving: Boolean,
    errorMessage: String?,
    onSubmit: (
        fullName: String,
        phoneNumber: String,
        nidNumber: String,
        birthday: String,
        weightKg: Long,
        userType: String,
        bloodGroup: String,
        district: String,
        upazila: String,
        address: String,
        gender: String,
        bio: String
    ) -> Unit,
    onSignOut: () -> Unit
) {
    var fullName by remember { mutableStateOf(initialName) }
    var phoneNumber by remember { mutableStateOf("") }
    var nidNumber by remember { mutableStateOf("") }
    var birthday by remember { mutableStateOf("2000-01-15") }
    var weightText by remember { mutableStateOf("60") }
    var userType by remember { mutableStateOf("donor") }
    var bloodGroup by remember { mutableStateOf("O+") }
    var district by remember { mutableStateOf("Dhaka") }
    var upazila by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("male") }
    var bio by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    val weightNum = weightText.toLongOrNull() ?: 0L
    val ageValid = DonorEligibilityUtils.isEligibleByAge(birthday)

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Complete Your Profile",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Signed in as $email",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(
                        onClick = onSignOut,
                        modifier = Modifier.testTag("onboarding_sign_out_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Sign Out")
                    }
                }

                Text("Account Role", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(
                        selected = userType == "donor",
                        onClick = { userType = "donor" },
                        leadingIcon = { Icon(Icons.Default.Favorite, contentDescription = null) },
                        label = { Text("Voluntary Donor") },
                        modifier = Modifier.testTag("role_donor_chip")
                    )
                    FilterChip(
                        selected = userType == "seeker",
                        onClick = { userType = "seeker" },
                        leadingIcon = { Icon(Icons.Default.Bloodtype, contentDescription = null) },
                        label = { Text("Blood Seeker") },
                        modifier = Modifier.testTag("role_seeker_chip")
                    )
                }

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full Name *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("onboarding_name_input")
                )

                Text("Blood Group *", style = MaterialTheme.typography.labelLarge)
                BloodGroupSelectorRow(
                    selectedGroup = bloodGroup,
                    onSelect = { bloodGroup = it }
                )

                DistrictDropdownField(
                    selectedDistrict = district,
                    onDistrictSelected = { district = it }
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = { Text("Phone (+880...)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("onboarding_phone_input")
                    )
                    OutlinedTextField(
                        value = weightText,
                        onValueChange = { weightText = it.filter { c -> c.isDigit() } },
                        label = { Text("Weight (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(0.7f)
                            .testTag("onboarding_weight_input")
                    )
                }

                OutlinedTextField(
                    value = birthday,
                    onValueChange = { birthday = it },
                    label = { Text("Date of Birth (YYYY-MM-DD)") },
                    supportingText = {
                        Text(
                            if (ageValid) "Age verified (>=18 yrs)"
                            else "Donors must be at least 18 years old"
                        )
                    },
                    isError = userType == "donor" && !ageValid,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("onboarding_birthday_input")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(
                        selected = gender == "male",
                        onClick = { gender = "male" },
                        label = { Text("Male") },
                        modifier = Modifier.testTag("gender_male_chip")
                    )
                    FilterChip(
                        selected = gender == "female",
                        onClick = { gender = "female" },
                        label = { Text("Female") },
                        modifier = Modifier.testTag("gender_female_chip")
                    )
                }

                OutlinedTextField(
                    value = upazila,
                    onValueChange = { upazila = it },
                    label = { Text("Upazila / Area") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = nidNumber,
                    onValueChange = { nidNumber = it },
                    label = { Text("NID / Student ID (Private - Admin Only)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Full Address (Private)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Short Donor Bio / Availability Note") },
                    modifier = Modifier.fillMaxWidth()
                )

                val displayError = validationError ?: errorMessage
                if (!displayError.isNullOrBlank()) {
                    Text(
                        text = displayError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Button(
                    onClick = {
                        validationError = when {
                            fullName.isBlank() -> "Please enter your full name."
                            userType == "donor" && !ageValid -> "Donors must be at least 18 years old (YYYY-MM-DD)."
                            userType == "donor" && weightNum < 45L -> "Minimum donor weight is 45 kg."
                            else -> null
                        }
                        if (validationError == null) {
                            onSubmit(
                                fullName, phoneNumber, nidNumber, birthday,
                                weightNum, userType, bloodGroup, district,
                                upazila, address, gender, bio
                            )
                        }
                    },
                    enabled = !isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .testTag("onboarding_submit_button")
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Create Blood Donor BD Profile")
                    }
                }
            }
        }
    }
}
