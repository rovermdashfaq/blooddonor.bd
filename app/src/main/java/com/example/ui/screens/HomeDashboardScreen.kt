package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.BangladeshData
import com.example.data.model.BloodRequest
import com.example.data.model.DonorEligibilityUtils
import com.example.data.model.NoticeItem
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.ui.components.BangladeshClockPill
import com.example.ui.components.BloodGroupBadge
import com.example.ui.components.NoticeTickerBanner
import com.example.ui.components.dialPhoneNumber
import com.example.ui.components.sendEmailSupport
import com.example.ui.theme.EmeraldSuccess
import java.util.Locale

@Composable
fun HomeDashboardScreen(
    currentUser: UserProfile,
    effectiveRole: String,
    noticesState: UiState<List<NoticeItem>>,
    activeRequestsState: UiState<List<BloodRequest>>,
    approvedDonorsCount: Int,
    onOpenFindDonors: () -> Unit,
    onOpenDonorMap: () -> Unit,
    onOpenRequests: () -> Unit,
    onOpenMessages: () -> Unit = {},
    onOpenVerify: () -> Unit = {},
    onOpenAiAssistant: () -> Unit,
    onSubmitDonationProof: (date: String, units: Long, proof: String, code: String) -> Unit
) {
    val context = LocalContext.current
    val themePrimary = MaterialTheme.colorScheme.primary
    val themeDeep = lerp(themePrimary, Color.Black, 0.35f)
    var showProofDialog by remember { mutableStateOf(false) }
    var bankSearch by remember { mutableStateOf("") }
    var selectedCompatGroup by remember { mutableStateOf(currentUser.bloodGroup ?: "O+") }
    var showQuizSection by remember { mutableStateOf(false) }
    var showEstimatorSection by remember { mutableStateOf(false) }
    val quizAnswers = remember { mutableStateMapOf<Int, Boolean>() }
    var weightEstimatorText by remember { mutableStateOf((currentUser.weightKg ?: 62L).toString()) }
    var bagsCostText by remember { mutableStateOf("1") }
    var factIndex by remember { mutableIntStateOf(0) }

    val daysRemaining = remember(currentUser.lastDonationDate) {
        DonorEligibilityUtils.daysUntilEligible(currentUser.lastDonationDate)
    }
    val daysSince = remember(currentUser.lastDonationDate) {
        DonorEligibilityUtils.daysSinceLastDonation(currentUser.lastDonationDate)
    }
    val isBirthday = remember(currentUser.birthday) {
        DonorEligibilityUtils.isBirthdayToday(currentUser.birthday)
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BangladeshClockPill()
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = "$approvedDonorsCount Verified Donors",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            if (noticesState is UiState.Success && noticesState.data.isNotEmpty()) {
                NoticeTickerBanner(notices = noticesState.data)
            }

            if (isBirthday) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Cake,
                            contentDescription = "Birthday wish",
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Happy Birthday, ${currentUser.fullName}! Thank you for being a lifesaver in Blood Donor BD.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Hero Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(listOf(themeDeep, themePrimary)))
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Welcome, ${currentUser.fullName}",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Role: ${effectiveRole.uppercase(Locale.US)} • ${currentUser.district ?: "Bangladesh"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                            Surface(shape = RoundedCornerShape(12.dp), color = Color.White) {
                                Text(
                                    text = currentUser.bloodGroup ?: "--",
                                    color = themePrimary,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }

                        if (currentUser.donorEnabled) {
                            val progress = if (daysSince == null) 1f else ((90f - daysRemaining) / 90f).coerceIn(0f, 1f)
                            Text(
                                text = if (daysRemaining == 0L) {
                                    "You are eligible to donate blood today!"
                                } else {
                                    "$daysRemaining days until next 90-day safe donation"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White
                            )
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = Color(0xFFB9F6CA),
                                trackColor = Color.White.copy(alpha = 0.25f)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Status: ${currentUser.approvalStatus.uppercase(Locale.US)} • Donations: ${currentUser.totalDonations}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                                TextButton(
                                    onClick = { showProofDialog = true },
                                    modifier = Modifier.testTag("submit_donation_proof_button")
                                ) {
                                    Text("Log Donation", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            Text(
                                text = "Account mode: Blood Seeker • You can enable Donor mode anytime in Profile.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }

            // Impact KPI Strip & Donor Milestone Tier
            val activeReqList = (activeRequestsState as? UiState.Success)?.data ?: emptyList()
            val tierLabel = when {
                currentUser.totalDonations >= 10L -> "Platinum Champion"
                currentUser.totalDonations >= 6L -> "Gold Lifesaver"
                currentUser.totalDonations >= 3L -> "Silver Hero"
                currentUser.totalDonations >= 1L -> "Bronze Saver"
                else -> "New Member"
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricMiniCard(
                    label = "Verified Donors",
                    value = approvedDonorsCount.toString(),
                    modifier = Modifier.weight(1f)
                )
                MetricMiniCard(
                    label = "Active Needs",
                    value = activeReqList.size.toString(),
                    modifier = Modifier.weight(1f)
                )
                MetricMiniCard(
                    label = "Donor Tier",
                    value = tierLabel,
                    icon = Icons.Default.EmojiEvents,
                    modifier = Modifier.weight(1.3f)
                )
            }

            // Smart Compatibility Alert if user can donate to an active urgent request
            val myBg = currentUser.bloodGroup
            val compatibleTargets = remember(myBg) {
                BangladeshData.COMPATIBILITY_CHART.firstOrNull { it.group == myBg }?.donateTo ?: emptyList()
            }
            val matchingRequests = remember(activeReqList, compatibleTargets) {
                if (compatibleTargets.isEmpty()) emptyList()
                else activeReqList.filter { it.bloodGroup in compatibleTargets }
            }
            if (currentUser.donorEnabled && daysRemaining == 0L && matchingRequests.isNotEmpty()) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenRequests() }
                        .testTag("home_matching_request_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.NotificationsActive,
                            contentDescription = "Urgent blood match",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "You Can Help Today! (${matchingRequests.size} Compatible Request${if (matchingRequests.size > 1) "s" else ""})",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "Your $myBg blood matches ${matchingRequests.first().patientName} at ${matchingRequests.first().hospitalName}.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            // Quick Actions Grid (2 rows of 3)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionTile(
                    title = "Find Donors",
                    icon = Icons.Default.Search,
                    modifier = Modifier.weight(1f).testTag("quick_find_donors"),
                    onClick = onOpenFindDonors
                )
                QuickActionTile(
                    title = "Donor Map",
                    icon = Icons.Default.Map,
                    modifier = Modifier.weight(1f).testTag("quick_donor_map"),
                    onClick = onOpenDonorMap
                )
                QuickActionTile(
                    title = "Requests",
                    icon = Icons.Default.Bloodtype,
                    modifier = Modifier.weight(1f).testTag("quick_requests"),
                    onClick = onOpenRequests
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionTile(
                    title = "Messages",
                    icon = Icons.AutoMirrored.Filled.Chat,
                    modifier = Modifier.weight(1f).testTag("quick_messages"),
                    onClick = onOpenMessages
                )
                QuickActionTile(
                    title = "Verify ID",
                    icon = Icons.Default.VerifiedUser,
                    modifier = Modifier.weight(1f).testTag("quick_verify_id"),
                    onClick = onOpenVerify
                )
                QuickActionTile(
                    title = "AI Guide",
                    icon = Icons.Default.AutoAwesome,
                    modifier = Modifier.weight(1f).testTag("quick_ai_guide"),
                    onClick = onOpenAiAssistant
                )
            }

            // Emergency Requests Preview
            if (activeRequestsState is UiState.Success && activeRequestsState.data.isNotEmpty()) {
                val topUrgent = activeRequestsState.data.take(3)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Urgent Blood Requests", style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = onOpenRequests) { Text("View All") }
                    }
                    topUrgent.forEach { req ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenRequests() },
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BloodGroupBadge(req.bloodGroup)
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "${req.patientName} (${req.unitsNeeded} bag)",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "${req.hospitalName}, ${req.district}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    req.urgency.uppercase(Locale.US),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Interactive Compatibility Chart
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Favorite, contentDescription = null, tint = themePrimary)
                        Spacer(Modifier.width(8.dp))
                        Text("Blood Compatibility Explorer", style = MaterialTheme.typography.titleMedium)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        BangladeshData.BLOOD_GROUPS.forEach { bg ->
                            FilterChip(
                                selected = selectedCompatGroup == bg,
                                onClick = { selectedCompatGroup = bg },
                                label = { Text(bg) }
                            )
                        }
                    }
                    val compat = BangladeshData.COMPATIBILITY_CHART.first { it.group == selectedCompatGroup }
                    Text(compat.note, style = MaterialTheme.typography.labelLarge, color = themePrimary)
                    Text(
                        "Can Donate To: ${compat.donateTo.joinToString(", ")}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = EmeraldSuccess
                    )
                    Text(
                        "Can Receive From: ${compat.receiveFrom.joinToString(", ")}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    val tip = BangladeshData.HEALTH_TIPS_BY_GROUP[selectedCompatGroup].orEmpty()
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "Health Tip ($selectedCompatGroup): $tip",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }

            // Tools Row: Eligibility Quiz & Blood Volume/Cost Estimator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { showQuizSection = !showQuizSection },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Default.FactCheck, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Eligibility Quiz")
                }
                OutlinedButton(
                    onClick = { showEstimatorSection = !showEstimatorSection },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Calculators")
                }
            }

            AnimatedVisibility(visible = showQuizSection) {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Quick Donor Eligibility Self-Check", style = MaterialTheme.typography.titleMedium)
                        BangladeshData.ELIGIBILITY_QUIZ.forEachIndexed { idx, item ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("${idx + 1}. ${item.question}", style = MaterialTheme.typography.bodyMedium)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(
                                        selected = quizAnswers[idx] == true,
                                        onClick = { quizAnswers[idx] = true },
                                        label = { Text("Yes") }
                                    )
                                    FilterChip(
                                        selected = quizAnswers[idx] == false,
                                        onClick = { quizAnswers[idx] = false },
                                        label = { Text("No") }
                                    )
                                }
                            }
                        }
                        if (quizAnswers.size == BangladeshData.ELIGIBILITY_QUIZ.size) {
                            val allPassed = BangladeshData.ELIGIBILITY_QUIZ.indices.all { quizAnswers[it] == true }
                            Text(
                                text = if (allPassed) {
                                    "Result: You meet the initial self-check criteria! Final screening happens at the blood center."
                                } else {
                                    "Result: You may need to wait or consult a physician before donating."
                                },
                                color = if (allPassed) EmeraldSuccess else MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(visible = showEstimatorSection) {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Blood Volume & Screening Cost Estimator", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(
                            value = weightEstimatorText,
                            onValueChange = { weightEstimatorText = it.filter { c -> c.isDigit() } },
                            label = { Text("Body Weight (kg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        val w = weightEstimatorText.toIntOrNull() ?: 60
                        val estVolumeMl = w * 70
                        val safeSingleDraw = if (w >= 50) "450 ml" else if (w >= 45) "350 ml" else "Wait until >=45 kg"
                        Text(
                            "Estimated Total Blood Volume: ~$estVolumeMl ml • Safe Draw: $safeSingleDraw",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        OutlinedTextField(
                            value = bagsCostText,
                            onValueChange = { bagsCostText = it.filter { c -> c.isDigit() } },
                            label = { Text("Units / Cross-Match Bags") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        val bags = (bagsCostText.toIntOrNull() ?: 1).coerceAtLeast(1)
                        Text(
                            "Estimated Gov/Red Crescent Screening & Bag Processing Fee: ৳${bags * 650}–৳${bags * 1200} BDT (Blood itself is always FREE).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Blood Bank Directory
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocalHospital, contentDescription = null, tint = themePrimary)
                        Spacer(Modifier.width(8.dp))
                        Text("Hospital & Blood Bank Directory", style = MaterialTheme.typography.titleMedium)
                    }
                    OutlinedTextField(
                        value = bankSearch,
                        onValueChange = { bankSearch = it },
                        placeholder = { Text("Search blood bank or district...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    val matchingBanks = remember(bankSearch) {
                        BangladeshData.BLOOD_BANKS.filter {
                            bankSearch.isBlank() ||
                                it.name.contains(bankSearch, ignoreCase = true) ||
                                it.district.contains(bankSearch, ignoreCase = true)
                        }.take(5)
                    }
                    matchingBanks.forEach { bank ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(bank.name, style = MaterialTheme.typography.labelLarge)
                                    Text(
                                        "${bank.district} • ${bank.hours} • ${bank.phone}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = themePrimary
                                    )
                                    Text(
                                        bank.address,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                FilledTonalIconButton(
                                    onClick = { dialPhoneNumber(context, bank.phone) },
                                    modifier = Modifier.testTag("call_bank_${bank.district.lowercase(Locale.US)}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Call,
                                        contentDescription = "Call ${bank.name}"
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Myth Busters & Did You Know
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Did You Know & Myth Busters", style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = {
                            factIndex = (factIndex + 1) % BangladeshData.DID_YOU_KNOW_FACTS.size
                        }) {
                            Text("Next Fact")
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.VolunteerActivism, contentDescription = null, tint = themePrimary)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                BangladeshData.DID_YOU_KNOW_FACTS[factIndex],
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    BangladeshData.MYTH_BUSTERS.forEach { item ->
                        Column {
                            Text("Myth: ${item.myth}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.error)
                            Text("Fact: ${item.fact}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            // Emergency Support & Helpline Card
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("24/7 Emergency Helpline & Support", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Need urgent coordination across Dhaka or any of the 64 districts? Reach our volunteer dispatch directly.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { dialPhoneNumber(context, BangladeshData.EMERGENCY_HELPLINE) },
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("home_call_helpline")
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Call Helpline")
                        }
                        OutlinedButton(
                            onClick = {
                                sendEmailSupport(
                                    context,
                                    BangladeshData.SUPPORT_EMAIL,
                                    "Blood Donor BD Urgent Inquiry"
                                )
                            },
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("home_email_support")
                        ) {
                            Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Email")
                        }
                    }
                }
            }

            Image(
                painter = painterResource(id = R.drawable.img_community_care),
                contentDescription = "Community care illustration",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(18.dp))
            )
        }
    }

    if (showProofDialog) {
        SubmitDonationProofDialog(
            onDismiss = { showProofDialog = false },
            onSubmit = { date, units, proof, code ->
                onSubmitDonationProof(date, units, proof, code)
                showProofDialog = false
            }
        )
    }
}

@Composable
private fun MetricMiniCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun QuickActionTile(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .heightIn(min = 76.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Text(title, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

@Composable
private fun SubmitDonationProofDialog(
    onDismiss: () -> Unit,
    onSubmit: (date: String, units: Long, proof: String, code: String) -> Unit
) {
    var date by remember { mutableStateOf("2026-11-25") }
    var unitsText by remember { mutableStateOf("1") }
    var proof by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Submit Last Donation Proof") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Donation Date (YYYY-MM-DD) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("proof_date_input")
                )
                OutlinedTextField(
                    value = unitsText,
                    onValueChange = { unitsText = it.filter { c -> c.isDigit() } },
                    label = { Text("Bags / Units Donated") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = proof,
                    onValueChange = { proof = it },
                    label = { Text("Hospital / Blood Bank Receipt Info *") },
                    modifier = Modifier.fillMaxWidth().testTag("proof_details_input")
                )
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Donor Slip / Reference Code") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (err != null) {
                    Text(err.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!date.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$"))) {
                        err = "Enter date as YYYY-MM-DD."
                    } else if (proof.isBlank()) {
                        err = "Enter hospital or blood bank details."
                    } else {
                        onSubmit(date, (unitsText.toLongOrNull() ?: 1L).coerceIn(1L, 10L), proof, code)
                    }
                },
                modifier = Modifier.testTag("confirm_submit_proof_button")
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
