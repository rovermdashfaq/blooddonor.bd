package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.BangladeshData
import com.example.data.model.CommunityPost
import com.example.data.model.DonorEligibilityUtils
import com.example.data.model.DonorSummary
import com.example.data.model.UiState
import com.example.ui.components.BloodGroupBadge
import com.example.ui.components.BloodGroupSelectorRow
import com.example.ui.components.DigitalDonorIdCard
import com.example.ui.components.DistrictDropdownField
import com.example.ui.components.LiveBangladeshDonorMap
import com.example.ui.components.sharePlainText
import com.example.ui.theme.EmeraldSuccess
import com.example.util.GeoPoint
import com.example.util.LocationHelper
import kotlinx.coroutines.launch

@Composable
fun FindDonorsAndMapScreen(
    currentUid: String,
    donorsState: UiState<List<DonorSummary>>,
    publicPosts: List<CommunityPost> = emptyList(),
    myLocation: GeoPoint? = null,
    initialMapTab: Boolean = false,
    onUpdateMyLocation: (GeoPoint) -> Unit = {},
    onMessageDonor: (DonorSummary) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTab by remember(initialMapTab) { mutableIntStateOf(if (initialMapTab) 1 else 0) }
    var searchQuery by remember { mutableStateOf("") }
    var bloodFilter by remember { mutableStateOf("All") }
    var districtFilter by remember { mutableStateOf("All") }
    var availableOnly by remember { mutableStateOf(false) }
    var includeCompatible by remember { mutableStateOf(false) }
    var sortByNearest by remember { mutableStateOf(false) }
    var maxRadiusKm by remember { mutableStateOf<Double?>(null) }
    var isLocating by remember { mutableStateOf(false) }
    var locationNote by remember { mutableStateOf<String?>(null) }
    var inspectedDonor by remember { mutableStateOf<DonorSummary?>(null) }

    fun fetchAndApplyGps() {
        scope.launch {
            isLocating = true
            locationNote = null
            val point = LocationHelper.fetchCurrentLocation(context)
            isLocating = false
            if (point != null) {
                onUpdateMyLocation(point)
                sortByNearest = true
            } else {
                locationNote = if (!LocationHelper.isLocationEnabled(context)) {
                    "Please turn on device Location/GPS to find nearby donors."
                } else {
                    "Could not get GPS fix yet. Try again outdoors."
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms.values.any { it }
        if (granted) {
            fetchAndApplyGps()
        } else {
            locationNote = "Location permission is required for Near Me & Live GPS."
        }
    }

    fun requestOrFetchLocation() {
        if (LocationHelper.hasLocationPermission(context)) {
            fetchAndApplyGps()
        } else {
            permissionLauncher.launch(LocationHelper.REQUIRED_PERMISSIONS)
        }
    }

    val acceptedGroups = remember(bloodFilter, includeCompatible) {
        if (bloodFilter == "All") null
        else if (!includeCompatible) setOf(bloodFilter)
        else {
            val compat = BangladeshData.COMPATIBILITY_CHART.firstOrNull { it.group == bloodFilter }
            (compat?.receiveFrom ?: listOf(bloodFilter)).toSet()
        }
    }

    fun distanceForDonor(d: DonorSummary, loc: GeoPoint?): Double? {
        if (loc == null) return null
        val fallback = BangladeshData.coordForDistrict(d.district)
        val lat = d.lat ?: fallback?.lat ?: return null
        val lng = d.lng ?: fallback?.lng ?: return null
        return LocationHelper.haversineKm(loc.lat, loc.lng, lat, lng)
    }

    val allDonors = (donorsState as? UiState.Success)?.data ?: emptyList()
    val filtered = remember(
        allDonors,
        acceptedGroups,
        districtFilter,
        searchQuery,
        availableOnly,
        myLocation,
        sortByNearest,
        maxRadiusKm
    ) {
        val base = allDonors.filter { d ->
            val matchesGroup = acceptedGroups == null || d.bloodGroup in acceptedGroups
            val matchesAvailability = !availableOnly ||
                (d.available && DonorEligibilityUtils.daysUntilEligible(d.lastDonationDate) == 0L)
            val dist = distanceForDonor(d, myLocation)
            val matchesRadius = maxRadiusKm == null || (dist != null && dist <= maxRadiusKm!!)
            matchesGroup &&
                matchesAvailability &&
                matchesRadius &&
                (districtFilter == "All" || d.district.equals(districtFilter, ignoreCase = true)) &&
                (searchQuery.isBlank() ||
                    d.fullName.contains(searchQuery, ignoreCase = true) ||
                    (d.donorId?.contains(searchQuery, ignoreCase = true) == true) ||
                    (d.upazila?.contains(searchQuery, ignoreCase = true) == true))
        }
        if (sortByNearest && myLocation != null) {
            base.sortedBy { distanceForDonor(it, myLocation) ?: Double.MAX_VALUE }
        } else {
            base
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Donor Directory") },
                    modifier = Modifier.testTag("donors_directory_tab")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Live BD Map") },
                    modifier = Modifier.testTag("donors_map_tab")
                )
            }

            BloodGroupSelectorRow(
                selectedGroup = bloodFilter,
                onSelect = { bloodFilter = it },
                includeAll = true
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = sortByNearest && myLocation != null,
                    onClick = {
                        if (myLocation == null) {
                            requestOrFetchLocation()
                        } else {
                            sortByNearest = !sortByNearest
                        }
                    },
                    leadingIcon = {
                        if (isLocating) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    },
                    label = { Text(if (myLocation != null) "Near Me (GPS)" else "Use Live GPS") },
                    modifier = Modifier.testTag("donors_near_me_chip")
                )
                FilterChip(
                    selected = availableOnly,
                    onClick = { availableOnly = !availableOnly },
                    leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text("Available Now") },
                    modifier = Modifier.testTag("donors_available_chip")
                )
                if (myLocation != null) {
                    listOf(5.0, 10.0, 25.0, 50.0).forEach { km ->
                        FilterChip(
                            selected = maxRadiusKm == km,
                            onClick = { maxRadiusKm = if (maxRadiusKm == km) null else km },
                            label = { Text("${km.toInt()} km") },
                            modifier = Modifier.testTag("donors_radius_${km.toInt()}km")
                        )
                    }
                }
                if (bloodFilter != "All") {
                    FilterChip(
                        selected = includeCompatible,
                        onClick = { includeCompatible = !includeCompatible },
                        label = { Text("Include Compatible for $bloodFilter") },
                        modifier = Modifier.testTag("donors_compatible_chip")
                    )
                }
            }

            if (!locationNote.isNullOrBlank()) {
                Text(
                    text = locationNote.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (selectedTab == 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Name, Donor ID, Upazila") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("donor_search_input")
                    )
                    DistrictDropdownField(
                        selectedDistrict = districtFilter,
                        onDistrictSelected = { districtFilter = it },
                        label = "District",
                        includeAllOption = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                when (donorsState) {
                    UiState.Loading -> {
                        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is UiState.Error -> {
                        Text(donorsState.message, color = MaterialTheme.colorScheme.error)
                    }
                    is UiState.Success -> {
                        if (filtered.isEmpty()) {
                            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("No verified donors match your filters.", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "Try expanding the km radius, selecting 'All' districts, or another blood group.",
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
                                items(filtered, key = { it.uid }) { donor ->
                                    DonorDirectoryCard(
                                        donor = donor,
                                        distanceKm = distanceForDonor(donor, myLocation),
                                        isSelf = donor.uid == currentUid,
                                        onViewCard = { inspectedDonor = donor },
                                        onMessage = { onMessageDonor(donor) }
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                LiveBangladeshDonorMap(
                    donors = filtered,
                    myLocation = myLocation,
                    radiusKm = maxRadiusKm,
                    isLocating = isLocating,
                    isLoading = donorsState is UiState.Loading,
                    errorMessage = (donorsState as? UiState.Error)?.message,
                    onLocateMeClick = { requestOrFetchLocation() },
                    onSelectDonor = { inspectedDonor = it },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    inspectedDonor?.let { donor ->
        val donorPosts = remember(publicPosts, donor.uid) {
            publicPosts.filter { it.userId == donor.uid }.take(3)
        }
        val resolvedDonorId = donor.donorId ?: DonorEligibilityUtils.generateDonorId(donor.uid)
        val daysWait = DonorEligibilityUtils.daysUntilEligible(donor.lastDonationDate)
        val distKm = distanceForDonor(donor, myLocation)

        AlertDialog(
            onDismissRequest = { inspectedDonor = null },
            title = { Text("Donor Public Profile & ID") },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DigitalDonorIdCard(
                        fullName = donor.fullName,
                        donorId = resolvedDonorId,
                        bloodGroup = donor.bloodGroup,
                        district = donor.district,
                        role = donor.role,
                        totalDonations = donor.totalDonations,
                        approvalStatus = donor.approvalStatus
                    )
                    if (!donor.upazila.isNullOrBlank()) {
                        Text("Area: ${donor.upazila}, ${donor.district}", style = MaterialTheme.typography.bodyMedium)
                    }
                    if (distKm != null) {
                        Text(
                            text = "Distance: ${LocationHelper.formatDistance(distKm)}",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    Text(
                        text = if (daysWait == 0L && donor.available) "Available to donate now" else "Recovery / Cooldown: $daysWait days left",
                        color = if (daysWait == 0L && donor.available) EmeraldSuccess else MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.labelLarge
                    )

                    OutlinedButton(
                        onClick = {
                            val cardText = "Blood Donor BD · Verified Donor Card\n" +
                                "Name: ${donor.fullName}\n" +
                                "Blood Group: ${donor.bloodGroup}\n" +
                                "Donor ID: $resolvedDonorId\n" +
                                "Location: ${if (!donor.upazila.isNullOrBlank()) "${donor.upazila}, " else ""}${donor.district}\n" +
                                "Total Donations: ${donor.totalDonations}"
                            sharePlainText(context, "Verified Donor Card", cardText)
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("share_donor_card_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Share Donor ID Card")
                    }

                    if (donorPosts.isNotEmpty()) {
                        HorizontalDivider()
                        Text("Recent Community Updates", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        donorPosts.forEach { p ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    if (!p.text.isNullOrBlank()) {
                                        Text(p.text, style = MaterialTheme.typography.bodySmall)
                                    }
                                    Text(
                                        DonorEligibilityUtils.formatTimestamp(p.createdAt),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (donor.uid != currentUid) {
                    Button(
                        onClick = {
                            inspectedDonor = null
                            onMessageDonor(donor)
                        },
                        modifier = Modifier.testTag("map_card_message_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Message Donor")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { inspectedDonor = null }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun DonorDirectoryCard(
    donor: DonorSummary,
    distanceKm: Double?,
    isSelf: Boolean,
    onViewCard: () -> Unit,
    onMessage: () -> Unit
) {
    val waitDays = DonorEligibilityUtils.daysUntilEligible(donor.lastDonationDate)
    val eligibleNow = donor.available && waitDays == 0L

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    BloodGroupBadge(donor.bloodGroup)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            donor.fullName + if (isSelf) " (You)" else "",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "${donor.district}${if (!donor.upazila.isNullOrBlank()) ", ${donor.upazila}" else ""}" +
                                    if (distanceKm != null) " • ${LocationHelper.formatDistance(distanceKm)}" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (eligibleNow) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = if (eligibleNow) "Available" else "Cooldown ${waitDays}d",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (eligibleNow) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onViewCard,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("donor_id_card_btn_${donor.uid}")
                ) {
                    Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("ID Card (${donor.totalDonations})")
                }
                if (!isSelf) {
                    Button(
                        onClick = onMessage,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("donor_dm_btn_${donor.uid}")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Message")
                    }
                }
            }
        }
    }
}
