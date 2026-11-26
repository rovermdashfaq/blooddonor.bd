package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.BangladeshData
import com.example.data.model.DonorCertificate
import com.example.data.model.NoticeItem
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GoldMainAdmin
import com.example.ui.theme.MedicalBlue
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

fun sharePlainText(context: Context, subject: String, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, text)
    }
    val chooser = Intent.createChooser(intent, subject)
    runCatching { context.startActivity(chooser) }
}

fun dialPhoneNumber(context: Context, phone: String) {
    val cleaned = phone.trim().replace(" ", "")
    if (cleaned.isEmpty()) return
    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleaned"))
    runCatching { context.startActivity(intent) }
}

fun sendEmailSupport(context: Context, email: String, subject: String) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:$email")
        putExtra(Intent.EXTRA_SUBJECT, subject)
    }
    runCatching { context.startActivity(intent) }
}

@Composable
fun OfflineBanner(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("offline_mode_banner"),
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CloudOff,
                contentDescription = "Offline mode",
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Offline Mode — Showing cached records; updates sync automatically online.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
fun BloodGroupBadge(
    bloodGroup: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.primary
    ) {
        Text(
            text = bloodGroup.ifBlank { "?" },
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun BangladeshClockPill(modifier: Modifier = Modifier) {
    var timeText by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val formatter = SimpleDateFormat("dd MMM · hh:mm:ss a", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("Asia/Dhaka")
        }
        while (true) {
            timeText = formatter.format(Date())
            delay(1000L)
        }
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = "Bangladesh standard time",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "BDT: $timeText",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun NoticeTickerBanner(
    notices: List<NoticeItem>,
    modifier: Modifier = Modifier
) {
    if (notices.isEmpty()) return
    var currentIndex by remember(notices.size) { mutableIntStateOf(0) }
    LaunchedEffect(notices) {
        if (notices.size > 1) {
            while (true) {
                delay(4500L)
                currentIndex = (currentIndex + 1) % notices.size
            }
        }
    }
    val notice = notices[currentIndex.coerceIn(0, notices.lastIndex)]
    val containerColor = when (notice.priority) {
        "urgent" -> MaterialTheme.colorScheme.errorContainer
        "normal" -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    val contentColor = when (notice.priority) {
        "urgent" -> MaterialTheme.colorScheme.onErrorContainer
        "normal" -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                if (notices.size > 1) currentIndex = (currentIndex + 1) % notices.size
            },
        shape = RoundedCornerShape(14.dp),
        color = containerColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Campaign,
                contentDescription = "Official notice",
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notice.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = notice.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.88f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (notices.size > 1) {
                Text(
                    text = "${currentIndex + 1}/${notices.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor
                )
            }
        }
    }
}

@Composable
fun DigitalDonorIdCard(
    fullName: String,
    donorId: String,
    bloodGroup: String,
    district: String,
    role: String,
    totalDonations: Long,
    approvalStatus: String,
    modifier: Modifier = Modifier
) {
    val themePrimary = MaterialTheme.colorScheme.primary
    val themeDeep = lerp(themePrimary, Color.Black, 0.35f)
    val gradientColors = when (role) {
        "main_admin" -> listOf(Color(0xFF7B5600), GoldMainAdmin)
        "admin" -> listOf(Color(0xFF0D47A1), MedicalBlue)
        "moderator" -> listOf(Color(0xFF1B5E20), EmeraldSuccess)
        else -> listOf(themeDeep, themePrimary)
    }
    val roleLabel = when (role) {
        "main_admin" -> "MAIN ADMIN"
        "admin" -> "ADMIN"
        "moderator" -> "MODERATOR"
        else -> "VOLUNTARY DONOR"
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(gradientColors))
                .padding(18.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_certificate_seal),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(110.dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .alpha(0.18f)
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.blood_donor_bd_logo),
                                contentDescription = "Blood Donor BD Logo",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .padding(2.dp)
                                    .clip(CircleShape)
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "BLOOD DONOR BD",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = roleLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White
                    ) {
                        Text(
                            text = bloodGroup.ifBlank { "--" },
                            color = gradientColors.last(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = fullName.ifBlank { "Member" },
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "ID: ${donorId.ifBlank { "PENDING" }}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.92f),
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "District: ${district.ifBlank { "Bangladesh" }} • Donations: $totalDonations",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Approval status",
                                tint = if (approvalStatus == "approved") Color(0xFFB9F6CA) else Color(0xFFFFE082),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = approvalStatus.uppercase(Locale.US),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }

                    // Deterministic verification pattern badge
                    VerificationMatrixBadge(seed = donorId.ifBlank { fullName })
                }
            }
        }
    }
}

@Composable
private fun VerificationMatrixBadge(seed: String) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color.White,
        modifier = Modifier.size(64.dp)
    ) {
        Canvas(modifier = Modifier.padding(6.dp)) {
            val grid = 5
            val cellW = size.width / grid
            val cellH = size.height / grid
            val hash = seed.hashCode()
            for (r in 0 until grid) {
                for (c in 0 until grid) {
                    val bit = (hash shr ((r * grid + c) % 31)) and 1
                    val isCorner = (r == 0 || r == grid - 1) && (c == 0 || c == grid - 1)
                    if (bit == 1 || isCorner) {
                        drawRect(
                            color = Color(0xFF1A1A1A),
                            topLeft = Offset(c * cellW, r * cellH),
                            size = Size(cellW * 0.88f, cellH * 0.88f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CertificateCard(
    cert: DonorCertificate,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val isValid = cert.status == "valid"
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_certificate_seal),
                contentDescription = "Certificate Seal",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Certificate of Appreciation",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${cert.userName} (${cert.bloodGroup}) • ${cert.donorId}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "ID: ${cert.certificateId} • Issued: ${cert.issuedDate}" +
                        if (onClick != null) " • Tap to open" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isValid) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.errorContainer
            ) {
                Text(
                    text = cert.status.uppercase(Locale.US),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isValid) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun CertificateDetailDialog(
    cert: DonorCertificate,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isValid = cert.status == "valid"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Official Donor Certificate", style = MaterialTheme.typography.titleLarge) },
        text = {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(18.dp)
                    )
            ) {
                Column(
                    modifier = Modifier
                        .padding(18.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        shadowElevation = 2.dp,
                        modifier = Modifier.size(76.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.blood_donor_bd_logo),
                            contentDescription = "Official Blood Donor BD Logo",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .padding(3.dp)
                                .clip(CircleShape)
                        )
                    }
                    Text(
                        text = "BLOOD DONOR BD",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Certificate of Appreciation",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        text = "Proudly presented to",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = cert.userName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Blood Group: ${cert.bloodGroup} • District: ${cert.district ?: "Bangladesh"}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "In recognition of selfless voluntary blood donation helping save lives across Bangladesh.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        text = "Certificate ID: ${cert.certificateId}\nDonor ID: ${cert.donorId}\nIssued: ${cert.issuedDate}",
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isValid) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = if (isValid) "STATUS: VERIFIED VALID" else "STATUS: REVOKED",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isValid) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val shareText = "Blood Donor BD Official Certificate\n" +
                        "Recipient: ${cert.userName} (${cert.bloodGroup})\n" +
                        "Donor ID: ${cert.donorId}\n" +
                        "District: ${cert.district ?: "Bangladesh"}\n" +
                        "Certificate ID: ${cert.certificateId}\n" +
                        "Issued: ${cert.issuedDate}\n" +
                        "Status: ${cert.status.uppercase(Locale.US)}"
                    sharePlainText(context, "Blood Donor BD Certificate", shareText)
                },
                modifier = Modifier.testTag("share_certificate_button")
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Share")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun AccountRecoveryDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.HelpOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Account Recovery & Help", style = MaterialTheme.typography.titleLarge)
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Blood Donor BD uses passwordless Google Sign-In for stronger donor security.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "1. Forgot Password? Recover your Google Account directly at accounts.google.com/signin/recovery, then tap 'Continue with Google' in this app.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "2. Switching Phones? Sign in with the same Google email to restore your Digital Donor ID, donation history, and official certificates.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "3. Need Donor ID / Profile Recovery Help? Contact Blood Donor BD Volunteer Support:",
                    style = MaterialTheme.typography.bodySmall
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { dialPhoneNumber(context, BangladeshData.EMERGENCY_HELPLINE) },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Call Help")
                    }
                    OutlinedButton(
                        onClick = {
                            sendEmailSupport(
                                context,
                                BangladeshData.SUPPORT_EMAIL,
                                "Blood Donor BD Account Support"
                            )
                        },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Email")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DistrictDropdownField(
    selectedDistrict: String,
    onDistrictSelected: (String) -> Unit,
    label: String = "District (64 BD Districts)",
    includeAllOption: Boolean = false,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val options = remember(includeAllOption) {
        if (includeAllOption) listOf("All") + BangladeshData.districtNames()
        else BangladeshData.districtNames()
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedDistrict,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
                .testTag("district_dropdown_field")
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { district ->
                DropdownMenuItem(
                    text = { Text(district) },
                    onClick = {
                        onDistrictSelected(district)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun BloodGroupSelectorRow(
    selectedGroup: String,
    onSelect: (String) -> Unit,
    includeAll: Boolean = false,
    modifier: Modifier = Modifier
) {
    val groups = remember(includeAll) {
        if (includeAll) listOf("All") + BangladeshData.BLOOD_GROUPS else BangladeshData.BLOOD_GROUPS
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        groups.forEach { group ->
            FilterChip(
                selected = selectedGroup == group,
                onClick = { onSelect(group) },
                label = { Text(group) },
                modifier = Modifier.testTag("blood_group_chip_${group.lowercase()}")
            )
        }
    }
}

@Composable
fun AmbitionsDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Our Ambitions · Save Lives", style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close ambitions")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Blood Donor BD connects verified voluntary blood donors across all 64 districts of Bangladesh with patients in urgent medical need.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "1. Rapid District-Level Coordination: Instant access to verified donors, hospital blood banks, and emergency requests 24/7.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "2. Zero-Commercial Voluntary Culture: 100% humanitarian, transparent donor verification, digital donor IDs, and official appreciation certificates.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "3. Donor Health & Privacy Protection: Strict 90-day safe recovery tracking and isolated private contact details.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "Emergency Helpline: ${BangladeshData.EMERGENCY_HELPLINE}\nEmail: ${BangladeshData.SUPPORT_EMAIL}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_ambitions_button")
            ) {
                Text("Got It")
            }
        }
    )
}

@Composable
fun LegalDialog(
    initialTab: Int = 0,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(initialTab) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Legal & Policies", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Privacy Policy") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Terms") }
                    )
                }
                Spacer(Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .height(300.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (selectedTab == 0) {
                        Text("Privacy Policy — Blood Donor BD", fontWeight = FontWeight.Bold)
                        Text(
                            "• Private PII Isolation: Your email, phone number, NID, birthday, and street address are stored in a protected user profile readable only by you and authorized administrators.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            "• Public Donor Directory: Approved donors appear in the directory using only non-PII details (name, blood group, district/upazila, gender, Donor ID, availability, and donation count).",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            "• Direct Messaging & Security: All connections are secured via Google Sign-In, Firebase App Check, and optional on-device Biometric Lock.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        Text("Terms & Conditions — Blood Donor BD", fontWeight = FontWeight.Bold)
                        Text(
                            "• Voluntary & Non-Commercial: Blood Donor BD strictly prohibits buying, selling, or brokering blood. All donations must be 100% voluntary.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            "• Donor Eligibility: Donors confirm they are at least 18 years old, weigh 45 kg or above, and follow at least a 90-day interval between whole blood donations.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            "• Medical Disclaimer: Always consult qualified hospital medical officers and follow proper cross-matching and screening before any transfusion.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

