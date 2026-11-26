package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.BloodRequest
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.ui.components.BloodGroupBadge
import com.example.ui.components.BloodGroupSelectorRow
import com.example.ui.components.DistrictDropdownField
import com.example.ui.components.dialPhoneNumber
import com.example.ui.components.sharePlainText
import java.util.Locale

@Composable
fun RequestsScreen(
    currentUser: UserProfile,
    activeRequestsState: UiState<List<BloodRequest>>,
    myRequestsState: UiState<List<BloodRequest>>,
    onCreateRequest: (
        patientName: String,
        patientAge: Long?,
        patientGender: String?,
        bloodGroup: String,
        unitsNeeded: Long,
        hospitalName: String,
        hospitalAddress: String,
        district: String,
        contactNumber: String,
        requiredBy: String,
        requiredTime: String,
        urgency: String,
        notes: String
    ) -> Unit,
    onMarkStatus: (requestId: String, status: String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var bloodFilter by remember { mutableStateOf("All") }
    var districtFilter by remember { mutableStateOf("All") }
    var urgencyFilter by remember { mutableStateOf("All") }
    var matchesMeOnly by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }

    val compatibleGroups = remember(currentUser.bloodGroup) {
        com.example.data.model.BangladeshData.COMPATIBILITY_CHART
            .firstOrNull { it.group == currentUser.bloodGroup }
            ?.donateTo
            ?.toSet()
            ?: emptySet()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize()
                .align(Alignment.TopCenter)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Active Needs") },
                    modifier = Modifier.testTag("requests_active_tab")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("My Requests") },
                    modifier = Modifier.testTag("requests_mine_tab")
                )
            }

            if (selectedTab == 0) {
                BloodGroupSelectorRow(
                    selectedGroup = bloodFilter,
                    onSelect = { bloodFilter = it; if (it != "All") matchesMeOnly = false },
                    includeAll = true
                )
                DistrictDropdownField(
                    selectedDistrict = districtFilter,
                    onDistrictSelected = { districtFilter = it },
                    label = "Filter by District",
                    includeAllOption = true
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (!currentUser.bloodGroup.isNullOrBlank()) {
                        FilterChip(
                            selected = matchesMeOnly,
                            onClick = { matchesMeOnly = !matchesMeOnly },
                            label = { Text("Can Donate (${currentUser.bloodGroup})") },
                            modifier = Modifier.testTag("requests_matches_me_chip")
                        )
                    }
                    listOf("All", "emergency", "urgent").forEach { u ->
                        FilterChip(
                            selected = urgencyFilter == u,
                            onClick = { urgencyFilter = u },
                            label = { Text(u.replaceFirstChar { it.uppercase(Locale.US) }) }
                        )
                    }
                }
            }

            val targetState = if (selectedTab == 0) activeRequestsState else myRequestsState
            when (targetState) {
                UiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is UiState.Error -> {
                    Text(targetState.message, color = MaterialTheme.colorScheme.error)
                }
                is UiState.Success -> {
                    val items = remember(targetState.data, selectedTab, bloodFilter, districtFilter, urgencyFilter, matchesMeOnly, compatibleGroups) {
                        if (selectedTab == 1) {
                            targetState.data
                        } else {
                            targetState.data.filter { req ->
                                val matchesBlood = if (matchesMeOnly) req.bloodGroup in compatibleGroups
                                else (bloodFilter == "All" || req.bloodGroup == bloodFilter)
                                matchesBlood &&
                                    (districtFilter == "All" || req.district.equals(districtFilter, ignoreCase = true)) &&
                                    (urgencyFilter == "All" || req.urgency == urgencyFilter)
                            }
                        }
                    }

                    if (items.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("No blood requests found.", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "Tap 'Post Request' if a patient urgently needs blood.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(items, key = { it.id }) { req ->
                                BloodRequestCard(
                                    request = req,
                                    isOwner = req.requesterUid == currentUser.uid,
                                    onMarkFulfilled = { onMarkStatus(req.id, "fulfilled") },
                                    onCancel = { onMarkStatus(req.id, "cancelled") }
                                )
                            }
                            item { Spacer(Modifier.height(72.dp)) }
                        }
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = { showCreateDialog = true },
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("Post Request") },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("create_blood_request_fab")
        )
    }

    if (showCreateDialog) {
        CreateBloodRequestDialog(
            defaultPhone = currentUser.phoneNumber.orEmpty(),
            defaultDistrict = currentUser.district ?: "Dhaka",
            onDismiss = { showCreateDialog = false },
            onSubmit = { pName, pAge, pGen, bg, units, hosp, hospAddr, dist, phone, reqBy, reqTime, urg, notes ->
                onCreateRequest(pName, pAge, pGen, bg, units, hosp, hospAddr, dist, phone, reqBy, reqTime, urg, notes)
                showCreateDialog = false
            }
        )
    }
}

@Composable
fun BloodRequestCard(
    request: BloodRequest,
    isOwner: Boolean,
    onMarkFulfilled: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val urgencyColor = when (request.urgency) {
        "emergency" -> MaterialTheme.colorScheme.errorContainer
        "urgent" -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BloodGroupBadge(request.bloodGroup)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = request.patientName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${request.unitsNeeded} bag(s) • ${request.district}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = urgencyColor
                ) {
                    Text(
                        text = request.urgency.uppercase(Locale.US),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocalHospital, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(request.hospitalName, style = MaterialTheme.typography.bodyMedium)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "${request.contactNumber} • Needed by ${request.requiredBy}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (!request.notes.isNullOrBlank()) {
                Text(
                    request.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { dialPhoneNumber(context, request.contactNumber) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("call_request_${request.id}")
                ) {
                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Call")
                }
                OutlinedButton(
                    onClick = {
                        val shareBody = "URGENT BLOOD NEEDED (${request.bloodGroup})\n" +
                            "Patient: ${request.patientName}\n" +
                            "Units: ${request.unitsNeeded} bag(s)\n" +
                            "Hospital: ${request.hospitalName}, ${request.district}\n" +
                            "Needed by: ${request.requiredBy}\n" +
                            "Contact: ${request.contactNumber}"
                        sharePlainText(context, "Urgent Blood Request (${request.bloodGroup})", shareBody)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("share_request_${request.id}")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Share")
                }
            }

            if (isOwner && request.status == "active") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onMarkFulfilled,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("fulfill_request_${request.id}")
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Fulfilled")
                    }
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("cancel_request_${request.id}")
                    ) {
                        Text("Cancel")
                    }
                }
            } else if (request.status != "active") {
                Text(
                    text = "Status: ${request.status.uppercase(Locale.US)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun CreateBloodRequestDialog(
    defaultPhone: String,
    defaultDistrict: String,
    onDismiss: () -> Unit,
    onSubmit: (
        patientName: String,
        patientAge: Long?,
        patientGender: String?,
        bloodGroup: String,
        unitsNeeded: Long,
        hospitalName: String,
        hospitalAddress: String,
        district: String,
        contactNumber: String,
        requiredBy: String,
        requiredTime: String,
        urgency: String,
        notes: String
    ) -> Unit
) {
    var patientName by remember { mutableStateOf("") }
    var patientAge by remember { mutableStateOf("") }
    var bloodGroup by remember { mutableStateOf("O+") }
    var units by remember { mutableStateOf("1") }
    var hospitalName by remember { mutableStateOf("") }
    var hospitalAddress by remember { mutableStateOf("") }
    var district by remember { mutableStateOf(defaultDistrict) }
    var contactNumber by remember { mutableStateOf(defaultPhone) }
    var requiredBy by remember { mutableStateOf("2026-12-01") }
    var requiredTime by remember { mutableStateOf("ASAP") }
    var urgency by remember { mutableStateOf("emergency") }
    var notes by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Post Emergency Blood Request") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = patientName,
                    onValueChange = { patientName = it },
                    label = { Text("Patient Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("req_patient_input")
                )
                Text("Blood Group *", style = MaterialTheme.typography.labelMedium)
                BloodGroupSelectorRow(selectedGroup = bloodGroup, onSelect = { bloodGroup = it })

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = units,
                        onValueChange = { units = it.filter { c -> c.isDigit() } },
                        label = { Text("Bags *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = patientAge,
                        onValueChange = { patientAge = it.filter { c -> c.isDigit() } },
                        label = { Text("Age") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = hospitalName,
                    onValueChange = { hospitalName = it },
                    label = { Text("Hospital Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("req_hospital_input")
                )
                DistrictDropdownField(selectedDistrict = district, onDistrictSelected = { district = it })
                OutlinedTextField(
                    value = contactNumber,
                    onValueChange = { contactNumber = it },
                    label = { Text("Attendant Phone *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("req_phone_input")
                )
                OutlinedTextField(
                    value = requiredBy,
                    onValueChange = { requiredBy = it },
                    label = { Text("Required Date (YYYY-MM-DD) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Urgency", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("emergency", "urgent", "normal").forEach { level ->
                        FilterChip(
                            selected = urgency == level,
                            onClick = { urgency = level },
                            label = { Text(level.replaceFirstChar { it.uppercase(Locale.US) }) }
                        )
                    }
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Additional Medical Notes") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Text(error.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val unitVal = units.toLongOrNull() ?: 1L
                    error = when {
                        patientName.isBlank() -> "Patient name is required."
                        hospitalName.isBlank() -> "Hospital name is required."
                        contactNumber.trim().length < 6 -> "Valid phone number is required."
                        requiredBy.trim().length < 4 -> "Required date is needed."
                        else -> null
                    }
                    if (error == null) {
                        onSubmit(
                            patientName,
                            patientAge.toLongOrNull(),
                            null,
                            bloodGroup,
                            unitVal,
                            hospitalName,
                            hospitalAddress,
                            district,
                            contactNumber,
                            requiredBy,
                            requiredTime,
                            urgency,
                            notes
                        )
                    }
                },
                modifier = Modifier.testTag("submit_blood_request_button")
            ) {
                Text("Publish")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
