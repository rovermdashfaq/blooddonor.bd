package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DonorCertificate
import com.example.data.model.UiState
import com.example.data.repository.BulkDonorInput
import com.example.ui.components.BloodGroupBadge
import com.example.ui.components.BloodGroupSelectorRow
import com.example.ui.components.CertificateCard
import com.example.ui.components.CertificateDetailDialog
import com.example.ui.components.DistrictDropdownField
import com.example.ui.viewmodel.AdminViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    viewModel: AdminViewModel,
    isMainAdmin: Boolean,
    onNavigateBack: () -> Unit
) {
    BackHandler { onNavigateBack() }

    val usersState by viewModel.allUsers.collectAsStateWithLifecycle()
    val requestsState by viewModel.allRequests.collectAsStateWithLifecycle()
    val certsState by viewModel.allCertificates.collectAsStateWithLifecycle()
    val noticesState by viewModel.allNotices.collectAsStateWithLifecycle()
    val reportsState by viewModel.allReports.collectAsStateWithLifecycle()
    val toast by viewModel.adminToast.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(toast) {
        if (!toast.isNullOrBlank()) {
            snackbarHostState.showSnackbar(toast.orEmpty())
            viewModel.clearToast()
        }
    }

    var selectedSection by remember { mutableIntStateOf(0) }
    var userSearchQuery by remember { mutableStateOf("") }
    var previewCert by remember { mutableStateOf<DonorCertificate?>(null) }
    val sections = remember(isMainAdmin) {
        buildList {
            add("Approvals")
            add("Donation Proofs")
            add("Certificates")
            add("Notices")
            add("Requests")
            add("Reports")
            add("Bulk Import")
            if (isMainAdmin) add("Roles")
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Blood Donor BD · Admin Panel") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("admin_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 820.dp)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    sections.forEachIndexed { idx, label ->
                        FilterChip(
                            selected = selectedSection == idx,
                            onClick = { selectedSection = idx },
                            label = { Text(label) },
                            modifier = Modifier.testTag("admin_tab_${label.lowercase(Locale.US).replace(" ", "_")}")
                        )
                    }
                }

                when (sections.getOrElse(selectedSection) { "Approvals" }) {
                    "Approvals" -> {
                        when (val s = usersState) {
                            UiState.Loading -> CircularProgressIndicator()
                            is UiState.Error -> Text(s.message, color = MaterialTheme.colorScheme.error)
                            is UiState.Success -> {
                                val sortedUsers = remember(s.data, userSearchQuery) {
                                    s.data
                                        .filter { u ->
                                            userSearchQuery.isBlank() ||
                                                u.fullName.contains(userSearchQuery, ignoreCase = true) ||
                                                u.email.contains(userSearchQuery, ignoreCase = true) ||
                                                (u.district?.contains(userSearchQuery, ignoreCase = true) == true)
                                        }
                                        .sortedBy { if (it.approvalStatus == "pending") 0 else 1 }
                                }
                                OutlinedTextField(
                                    value = userSearchQuery,
                                    onValueChange = { userSearchQuery = it },
                                    placeholder = { Text("Search users by name, email, or district...") },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("admin_user_search_input")
                                )
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    items(sortedUsers, key = { it.uid }) { u ->
                                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    BloodGroupBadge(u.bloodGroup ?: "--")
                                                    Spacer(Modifier.width(10.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(u.fullName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                                        Text("${u.district ?: ""} • ${u.email}", style = MaterialTheme.typography.bodySmall)
                                                        Text("Status: ${u.approvalStatus} • Role: ${u.role}", style = MaterialTheme.typography.labelSmall)
                                                    }
                                                }
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Button(
                                                        onClick = { viewModel.reviewDonor(u, true) },
                                                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                                                    ) { Text("Approve") }
                                                    OutlinedButton(
                                                        onClick = { viewModel.reviewDonor(u, false) },
                                                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                                                    ) { Text("Reject") }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    "Donation Proofs" -> {
                        when (val s = usersState) {
                            UiState.Loading -> CircularProgressIndicator()
                            is UiState.Error -> Text(s.message, color = MaterialTheme.colorScheme.error)
                            is UiState.Success -> {
                                val pendingProofs = remember(s.data) {
                                    s.data.filter { it.lastDonationApprovalStatus == "pending" }
                                }
                                if (pendingProofs.isEmpty()) {
                                    Text("No pending donation proofs.")
                                } else {
                                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        items(pendingProofs, key = { it.uid }) { u ->
                                            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Text("${u.fullName} (${u.bloodGroup ?: ""})", style = MaterialTheme.typography.titleMedium)
                                                    Text("Date: ${u.lastDonationDate ?: "--"} • Units: ${u.lastDonationUnits ?: 1L}")
                                                    Text("Proof: ${u.lastDonationProof ?: "N/A"} • Code: ${u.lastDonationCode ?: "--"}", style = MaterialTheme.typography.bodySmall)
                                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        Button(
                                                            onClick = { viewModel.reviewDonationProof(u, true) },
                                                            modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                                                        ) { Text("Verify & Increment") }
                                                        OutlinedButton(
                                                            onClick = { viewModel.reviewDonationProof(u, false) },
                                                            modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                                                        ) { Text("Reject") }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    "Certificates" -> {
                        val users = (usersState as? UiState.Success)?.data?.filter { it.approvalStatus == "approved" && !it.bloodGroup.isNullOrBlank() } ?: emptyList()
                        val certs = (certsState as? UiState.Success)?.data ?: emptyList()
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            item {
                                Text("Issue Certificate to Approved Donor", style = MaterialTheme.typography.titleMedium)
                            }
                            items(users.take(15), key = { "issue_${it.uid}" }) { u ->
                                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("${u.fullName} (${u.bloodGroup})", fontWeight = FontWeight.Bold)
                                            Text("Donations: ${u.totalDonations} • ${u.donorId ?: ""}", style = MaterialTheme.typography.bodySmall)
                                        }
                                        Button(onClick = { viewModel.issueCertificate(u) }) {
                                            Text("Issue")
                                        }
                                    }
                                }
                            }
                            item { Text("Issued Certificates (${certs.size})", style = MaterialTheme.typography.titleMedium) }
                            items(certs, key = { it.certificateId }) { cert ->
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    CertificateCard(cert = cert, onClick = { previewCert = cert })
                                    OutlinedButton(
                                        onClick = {
                                            val next = if (cert.status == "valid") "revoked" else "valid"
                                            viewModel.setCertificateStatus(cert.certificateId, next)
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(if (cert.status == "valid") "Revoke Certificate" else "Restore Valid Status")
                                    }
                                }
                            }
                        }
                    }

                    "Notices" -> {
                        var title by remember { mutableStateOf("") }
                        var message by remember { mutableStateOf("") }
                        var priority by remember { mutableStateOf("urgent") }
                        val notices = (noticesState as? UiState.Success)?.data ?: emptyList()

                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            item {
                                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("Publish Notice Ticker", style = MaterialTheme.typography.titleMedium)
                                        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title *") }, modifier = Modifier.fillMaxWidth())
                                        OutlinedTextField(value = message, onValueChange = { message = it }, label = { Text("Message *") }, modifier = Modifier.fillMaxWidth())
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            listOf("info", "normal", "urgent").forEach { p ->
                                                FilterChip(selected = priority == p, onClick = { priority = p }, label = { Text(p) })
                                            }
                                        }
                                        Button(
                                            onClick = {
                                                viewModel.createNotice(title, message, priority)
                                                title = ""
                                                message = ""
                                            },
                                            enabled = title.trim().length >= 2 && message.trim().length >= 2,
                                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                        ) { Text("Publish Notice") }
                                    }
                                }
                            }
                            items(notices, key = { it.id }) { n ->
                                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("${n.title} (${n.priority})", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                            Switch(checked = n.active, onCheckedChange = { viewModel.toggleNotice(n.id, it) })
                                        }
                                        Text(n.message, style = MaterialTheme.typography.bodySmall)
                                        OutlinedButton(onClick = { viewModel.deleteNotice(n.id) }) { Text("Delete") }
                                    }
                                }
                            }
                        }
                    }

                    "Requests" -> {
                        val reqs = (requestsState as? UiState.Success)?.data ?: emptyList()
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(reqs, key = { it.id }) { r ->
                                BloodRequestCard(
                                    request = r,
                                    isOwner = true,
                                    onMarkFulfilled = { viewModel.updateRequestStatus(r.id, "fulfilled") },
                                    onCancel = { viewModel.updateRequestStatus(r.id, "cancelled") }
                                )
                            }
                        }
                    }

                    "Reports" -> {
                        val reports = (reportsState as? UiState.Success)?.data ?: emptyList()
                        if (reports.isEmpty()) {
                            Text("No community post reports submitted.")
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(reports, key = { it.id }) { rep ->
                                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text("Post ID: ${rep.postId} • Status: ${rep.status}", fontWeight = FontWeight.Bold)
                                            Text("Reason: ${rep.reason}")
                                            if (rep.status == "open") {
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Button(
                                                        onClick = { viewModel.deleteReportedPost(rep.postId, rep.id) },
                                                        modifier = Modifier.weight(1f)
                                                    ) { Text("Delete Post") }
                                                    OutlinedButton(
                                                        onClick = { viewModel.resolveReport(rep.id) },
                                                        modifier = Modifier.weight(1f)
                                                    ) { Text("Dismiss") }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    "Bulk Import" -> {
                        var csvText by remember { mutableStateOf("") }
                        var quickName by remember { mutableStateOf("") }
                        var quickGroup by remember { mutableStateOf("O+") }
                        var quickDistrict by remember { mutableStateOf("Dhaka") }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.UploadFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Bulk Donor Registration (CSV)", style = MaterialTheme.typography.titleMedium)
                                    }
                                    Text(
                                        "Format per line: Name, BloodGroup, District, Gender(optional), Upazila(optional), Donations(optional)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    OutlinedTextField(
                                        value = csvText,
                                        onValueChange = { csvText = it },
                                        label = { Text("Paste donor CSV rows") },
                                        minLines = 5,
                                        modifier = Modifier.fillMaxWidth().testTag("bulk_csv_input")
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = {
                                                csvText = listOf(
                                                    "Arifur Rahman, O+, Dhaka, male, Dhanmondi, 4",
                                                    "Nusrat Jahan, A+, Chattogram, female, Panchlaish, 2",
                                                    "Mahmudul Hasan, B+, Rajshahi, male, Boalia, 5"
                                                ).joinToString("\n")
                                            },
                                            modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                                        ) {
                                            Text("Sample CSV")
                                        }
                                        Button(
                                            onClick = {
                                                val parsed = csvText.lines()
                                                    .mapNotNull { line ->
                                                        val cols = line.split(",").map { it.trim() }
                                                        if (cols.size >= 3 && cols[0].isNotBlank()) {
                                                            BulkDonorInput(
                                                                fullName = cols[0],
                                                                bloodGroup = cols[1],
                                                                district = cols[2],
                                                                gender = cols.getOrNull(3).orEmpty().ifBlank { "male" },
                                                                upazila = cols.getOrNull(4).orEmpty(),
                                                                totalDonations = cols.getOrNull(5)?.toLongOrNull() ?: 0L
                                                            )
                                                        } else null
                                                    }
                                                viewModel.bulkRegisterDonors(parsed)
                                                if (parsed.isNotEmpty()) csvText = ""
                                            },
                                            enabled = csvText.isNotBlank(),
                                            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("bulk_import_submit_button")
                                        ) {
                                            Text("Import Donors")
                                        }
                                    }
                                }
                            }

                            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("Quick Row Builder", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    OutlinedTextField(
                                        value = quickName,
                                        onValueChange = { quickName = it },
                                        label = { Text("Donor Name") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    BloodGroupSelectorRow(selectedGroup = quickGroup, onSelect = { quickGroup = it })
                                    DistrictDropdownField(selectedDistrict = quickDistrict, onDistrictSelected = { quickDistrict = it })
                                    OutlinedButton(
                                        onClick = {
                                            if (quickName.isNotBlank()) {
                                                val newRow = "${quickName.trim()}, $quickGroup, $quickDistrict, male, , 0"
                                                csvText = if (csvText.isBlank()) newRow else "$csvText\n$newRow"
                                                quickName = ""
                                            }
                                        },
                                        enabled = quickName.isNotBlank(),
                                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                    ) {
                                        Text("Append Row to CSV")
                                    }
                                }
                            }
                        }
                    }

                    "Roles" -> {
                        val users = (usersState as? UiState.Success)?.data ?: emptyList()
                        val filteredUsers = remember(users, userSearchQuery) {
                            users.filter { u ->
                                userSearchQuery.isBlank() ||
                                    u.fullName.contains(userSearchQuery, ignoreCase = true) ||
                                    u.email.contains(userSearchQuery, ignoreCase = true)
                            }
                        }
                        OutlinedTextField(
                            value = userSearchQuery,
                            onValueChange = { userSearchQuery = it },
                            placeholder = { Text("Filter members by name or email...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(filteredUsers, key = { "role_${it.uid}" }) { u ->
                                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("${u.fullName} (${u.email})", fontWeight = FontWeight.Bold)
                                        Text("Current role: ${u.role}", style = MaterialTheme.typography.bodySmall)
                                        Row(
                                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            listOf("user", "moderator", "admin", "main_admin").forEach { roleOpt ->
                                                FilterChip(
                                                    selected = u.role == roleOpt,
                                                    onClick = { viewModel.updateRole(u, roleOpt) },
                                                    label = { Text(roleOpt) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    previewCert?.let { cert ->
        CertificateDetailDialog(cert = cert, onDismiss = { previewCert = null })
    }
}
