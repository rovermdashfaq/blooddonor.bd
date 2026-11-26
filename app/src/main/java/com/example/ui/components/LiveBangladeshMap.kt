package com.example.ui.components

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.webkit.JavascriptInterface
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.BangladeshData
import com.example.data.model.DonorEligibilityUtils
import com.example.data.model.DonorSummary
import com.example.util.GeoPoint
import com.example.util.LocationHelper
import com.example.util.NetworkMonitor
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

private data class MapPinPoint(
    val donor: DonorSummary,
    val lat: Double,
    val lng: Double,
    val eligibleNow: Boolean,
    val distanceKm: Double?
)

@Composable
fun LiveBangladeshDonorMap(
    donors: List<DonorSummary>,
    myLocation: GeoPoint? = null,
    radiusKm: Double? = null,
    isLocating: Boolean = false,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onLocateMeClick: () -> Unit = {},
    onSelectDonor: (DonorSummary) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val primaryPinColor = MaterialTheme.colorScheme.primary
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    var useStreetMap by remember { mutableStateOf(NetworkMonitor.isCurrentlyOnline(context)) }
    var streetMapReady by remember { mutableStateOf(false) }
    var streetMapFailed by remember { mutableStateOf(false) }
    var selectedHub by remember { mutableStateOf("All BD") }
    var selectedInfo by remember(donors.size, isLoading) {
        mutableStateOf(
            when {
                isLoading -> "Syncing live donor locations..."
                else -> "Tap a pin • ${donors.size} donor(s) on map"
            }
        )
    }

    val divisionHubs = remember {
        listOf("All BD", "Dhaka", "Chattogram", "Sylhet", "Rajshahi", "Khulna", "Barishal", "Rangpur", "Mymensingh")
    }

    val markerPoints = remember(donors, myLocation) {
        val groupedIndices = mutableMapOf<String, Int>()
        donors.map { d ->
            val fallback = BangladeshData.coordForDistrict(d.district)
            val baseLat = d.lat ?: fallback?.lat ?: 23.8103
            val baseLng = d.lng ?: fallback?.lng ?: 90.4125
            val key = "${(baseLat * 100).toInt()}_${(baseLng * 100).toInt()}"
            val slot = groupedIndices[key] ?: 0
            groupedIndices[key] = slot + 1
            val ringRadius = if (slot == 0) 0.0 else 0.022 * (1 + slot / 8)
            val angle = slot * (Math.PI / 4.0)
            val finalLat = baseLat + ringRadius * sin(angle)
            val finalLng = baseLng + ringRadius * cos(angle)
            val eligibleNow = d.available && DonorEligibilityUtils.daysUntilEligible(d.lastDonationDate) == 0L
            val dist = myLocation?.let { LocationHelper.haversineKm(it.lat, it.lng, finalLat, finalLng) }
            MapPinPoint(
                donor = d,
                lat = finalLat,
                lng = finalLng,
                eligibleNow = eligibleNow,
                distanceKm = dist
            )
        }
    }

    var vectorScale by remember { mutableFloatStateOf(1f) }
    var vectorPanX by remember { mutableFloatStateOf(0f) }
    var vectorPanY by remember { mutableFloatStateOf(0f) }

    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    val showInteractiveWebMap = useStreetMap && !streetMapFailed

    // Auto-fallback to native vector map if external OSM/Leaflet CDN is unreachable within 4.5s
    LaunchedEffect(showInteractiveWebMap, streetMapReady) {
        if (showInteractiveWebMap && !streetMapReady) {
            delay(4500L)
            if (!streetMapReady) {
                streetMapFailed = true
            }
        }
    }

    // Smoothly fly Leaflet map to user's live GPS coordinates when updated
    LaunchedEffect(myLocation, streetMapReady) {
        if (myLocation != null && streetMapReady) {
            webViewRef?.evaluateJavascript(
                "if (window.focusDistrict) window.focusDistrict(${myLocation.lat}, ${myLocation.lng}, 12);",
                null
            )
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 360.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Map Header Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        text = errorMessage ?: selectedInfo,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (errorMessage != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(
                        selected = showInteractiveWebMap,
                        onClick = {
                            if (useStreetMap && !streetMapFailed) {
                                useStreetMap = false
                            } else {
                                streetMapFailed = false
                                streetMapReady = false
                                useStreetMap = true
                            }
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (showInteractiveWebMap) Icons.Default.Public else Icons.Default.Layers,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = {
                            Text(if (showInteractiveWebMap) "OSM Live" else "Vector")
                        },
                        modifier = Modifier.testTag("map_mode_toggle")
                    )

                    if (!showInteractiveWebMap) {
                        IconButton(onClick = { vectorScale = (vectorScale * 1.35f).coerceIn(1f, 4f) }) {
                            Icon(Icons.Default.Add, contentDescription = "Zoom in map", modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = { vectorScale = (vectorScale / 1.35f).coerceIn(1f, 4f) }) {
                            Icon(Icons.Default.Remove, contentDescription = "Zoom out map", modifier = Modifier.size(18.dp))
                        }
                    }
                    TextButton(
                        onClick = {
                            selectedHub = "All BD"
                            vectorScale = 1f
                            vectorPanX = 0f
                            vectorPanY = 0f
                            webViewRef?.evaluateJavascript("if (window.focusDistrict) window.focusDistrict(23.6850, 90.3563, 7);", null)
                        },
                        modifier = Modifier.testTag("map_reset_button")
                    ) {
                        Text("Reset")
                    }
                }
            }

            // Division quick-jump bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                divisionHubs.forEach { hub ->
                    FilterChip(
                        selected = selectedHub == hub,
                        onClick = {
                            selectedHub = hub
                            if (hub == "All BD") {
                                vectorScale = 1f
                                vectorPanX = 0f
                                vectorPanY = 0f
                                webViewRef?.evaluateJavascript("if (window.focusDistrict) window.focusDistrict(23.6850, 90.3563, 7);", null)
                            } else {
                                BangladeshData.coordForDistrict(hub)?.let { coord ->
                                    selectedInfo = "Focused: $hub Division"
                                    webViewRef?.evaluateJavascript(
                                        "if (window.focusDistrict) window.focusDistrict(${coord.lat}, ${coord.lng}, 11);",
                                        null
                                    )
                                }
                            }
                        },
                        label = { Text(hub, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clipToBounds()
            ) {
                // Immediate Native Vector Map Layer (also serves as seamless offline fallback)
                NativeBangladeshVectorMap(
                    markerPoints = markerPoints,
                    myLocation = myLocation,
                    primaryPinColor = primaryPinColor,
                    isDark = isDark,
                    scale = vectorScale,
                    panX = vectorPanX,
                    panY = vectorPanY,
                    onTransform = { zoom, pan, width, height ->
                        vectorScale = (vectorScale * zoom).coerceIn(1f, 4f)
                        vectorPanX = (vectorPanX + pan.x).coerceIn(-width * 0.8f, width * 0.8f)
                        vectorPanY = (vectorPanY + pan.y).coerceIn(-height * 0.8f, height * 0.8f)
                    },
                    onSelectPoint = { pt ->
                        selectedInfo = "${pt.donor.fullName} (${pt.donor.bloodGroup}) • ${pt.donor.district}"
                        onSelectDonor(pt.donor)
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Real OpenStreetMap + Leaflet Layer (GitHub donors.map.html parity)
                if (showInteractiveWebMap) {
                    LeafletBangladeshWebMap(
                        markerPoints = markerPoints,
                        myLocation = myLocation,
                        radiusKm = radiusKm,
                        primaryColorHex = String.format("#%06X", 0xFFFFFF and primaryPinColor.toArgb()),
                        isDark = isDark,
                        onWebViewCreated = { webViewRef = it },
                        onReady = { streetMapReady = true },
                        onFailed = { streetMapFailed = true },
                        onSelectDonor = { donor ->
                            selectedInfo = "${donor.fullName} (${donor.bloodGroup}) • ${donor.district}"
                            onSelectDonor(donor)
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    if (!streetMapReady) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("Loading OpenStreetMap...", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                // My Live GPS Location Floating Button (Bottom-End)
                SmallFloatingActionButton(
                    onClick = onLocateMeClick,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .size(48.dp)
                        .testTag("map_locate_me_button")
                ) {
                    if (isLocating) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.MyLocation, contentDescription = "Use my live GPS location")
                    }
                }

                // Map Legend Pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(10.dp).clip(CircleShape).background(primaryPinColor))
                            Spacer(Modifier.width(4.dp))
                            Text("Available", style = MaterialTheme.typography.labelSmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFF57C00)))
                            Spacer(Modifier.width(4.dp))
                            Text("Cooldown", style = MaterialTheme.typography.labelSmall)
                        }
                        if (myLocation != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF1976D2)))
                                Spacer(Modifier.width(4.dp))
                                Text("You", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@Composable
private fun LeafletBangladeshWebMap(
    markerPoints: List<MapPinPoint>,
    myLocation: GeoPoint?,
    radiusKm: Double?,
    primaryColorHex: String,
    isDark: Boolean,
    onWebViewCreated: (WebView) -> Unit,
    onReady: () -> Unit,
    onFailed: () -> Unit,
    onSelectDonor: (DonorSummary) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentPoints by rememberUpdatedState(markerPoints)
    val currentOnSelect by rememberUpdatedState(onSelectDonor)
    val currentOnReady by rememberUpdatedState(onReady)
    val currentOnFailed by rememberUpdatedState(onFailed)
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    val markersJson = remember(markerPoints, primaryColorHex) {
        JSONArray().apply {
            markerPoints.forEach { pt ->
                put(
                    JSONObject().apply {
                        put("uid", pt.donor.uid)
                        put("name", pt.donor.fullName)
                        put("bloodGroup", pt.donor.bloodGroup)
                        put("district", pt.donor.district)
                        put("upazila", pt.donor.upazila ?: "")
                        put("lat", pt.lat)
                        put("lng", pt.lng)
                        put("available", pt.eligibleNow)
                        put("distance", pt.distanceKm?.let { LocationHelper.formatDistance(it) } ?: "")
                        put("color", if (pt.eligibleNow) primaryColorHex else "#F57C00")
                    }
                )
            }
        }.toString()
    }

    val userLocJson = remember(myLocation, radiusKm) {
        if (myLocation == null) "null"
        else JSONObject().apply {
            put("lat", myLocation.lat)
            put("lng", myLocation.lng)
            put("radiusMeters", (radiusKm ?: 10.0) * 1000.0)
        }.toString()
    }

    // Authentic OpenStreetMap tiles (matching GitHub donors.map.html) in light mode; Carto Dark Matter in dark mode
    val tileUrl = if (isDark) {
        "https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png"
    } else {
        "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
    }

    LaunchedEffect(markersJson, userLocJson, tileUrl, webViewInstance) {
        val wv = webViewInstance ?: return@LaunchedEffect
        wv.evaluateJavascript("if (window.updateMarkers) window.updateMarkers($markersJson);", null)
        wv.evaluateJavascript("if (window.updateUserLocation) window.updateUserLocation($userLocJson);", null)
        wv.evaluateJavascript("if (window.setTileTheme) window.setTileTheme(${JSONObject.quote(tileUrl)}, $isDark);", null)
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewInstance?.destroy()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.userAgentString = "BloodDonorBD-Android/1.0 (+https://blooddonorbd.app)"
                setBackgroundColor(android.graphics.Color.TRANSPARENT)

                setOnTouchListener { v, event ->
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE ->
                            v.parent?.requestDisallowInterceptTouchEvent(true)
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                            v.parent?.requestDisallowInterceptTouchEvent(false)
                    }
                    false
                }

                val bridge = object {
                    @JavascriptInterface
                    fun selectDonor(uid: String) {
                        mainHandler.post {
                            currentPoints.firstOrNull { it.donor.uid == uid }?.let {
                                currentOnSelect(it.donor)
                            }
                        }
                    }

                    @JavascriptInterface
                    fun onMapReady() {
                        mainHandler.post { currentOnReady() }
                    }

                    @JavascriptInterface
                    fun onMapUnavailable() {
                        mainHandler.post { currentOnFailed() }
                    }
                }
                addJavascriptInterface(bridge, "AndroidBridge")

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        view?.evaluateJavascript(
                            "if (typeof L === 'undefined') { AndroidBridge.onMapUnavailable(); } else { if (window.updateMarkers) window.updateMarkers($markersJson); if (window.updateUserLocation) window.updateUserLocation($userLocJson); }",
                            null
                        )
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        if (request?.isForMainFrame == true) {
                            currentOnFailed()
                        }
                    }
                }

                val html = buildLeafletHtml(initialTileUrl = tileUrl, isDark = isDark)
                loadDataWithBaseURL("https://blooddonorbd.app/", html, "text/html", "UTF-8", null)
                webViewInstance = this
                onWebViewCreated(this)
            }
        }
    )
}

@Composable
private fun NativeBangladeshVectorMap(
    markerPoints: List<MapPinPoint>,
    myLocation: GeoPoint?,
    primaryPinColor: Color,
    isDark: Boolean,
    scale: Float,
    panX: Float,
    panY: Float,
    onTransform: (zoom: Float, pan: Offset, width: Float, height: Float) -> Unit,
    onSelectPoint: (MapPinPoint) -> Unit,
    modifier: Modifier = Modifier
) {
    val waterColor = if (isDark) Color(0xFF15202B) else Color(0xFFEAF3FA)
    val landColor = if (isDark) Color(0xFF1E3329) else Color(0xFFDCECE2)
    val borderColor = if (isDark) Color(0xFF66BB6A) else Color(0xFF2E7D32)
    val riverColor = if (isDark) Color(0xFF2B4C65) else Color(0xFF90CAF9)
    val labelColor = if (isDark) Color(0xFFCFD8DC) else Color(0xFF263238)
    val textMeasurer = rememberTextMeasurer()

    val minLat = 20.6
    val maxLat = 26.7
    val minLng = 88.0
    val maxLng = 92.7

    val divisionHubs = remember {
        setOf("Dhaka", "Chattogram", "Rajshahi", "Khulna", "Sylhet", "Barishal", "Rangpur", "Mymensingh")
    }

    val bdBoundary = remember {
        listOf(
            26.63 to 88.40, 26.42 to 88.95, 26.25 to 89.72, 25.75 to 89.88,
            25.18 to 89.86, 25.15 to 90.65, 25.18 to 91.45, 25.15 to 92.25,
            24.85 to 92.48, 24.30 to 91.95, 23.85 to 91.28, 23.15 to 91.42,
            23.72 to 92.28, 22.95 to 92.42, 22.15 to 92.60, 21.45 to 92.25,
            20.75 to 92.34, 21.25 to 92.02, 22.25 to 91.80, 22.75 to 91.45,
            22.40 to 91.05, 21.95 to 90.35, 21.75 to 89.85, 21.65 to 89.25,
            22.25 to 88.98, 22.85 to 88.90, 23.55 to 88.62, 24.15 to 88.45,
            24.55 to 88.08, 25.05 to 88.45, 25.62 to 88.85, 26.18 to 88.35,
            26.63 to 88.40
        )
    }

    val jamunaPadmaRiver = remember {
        listOf(25.95 to 89.78, 24.85 to 89.70, 23.82 to 89.78, 23.48 to 90.35, 22.82 to 90.65, 22.25 to 90.85)
    }
    val gangesPadmaRiver = remember {
        listOf(24.50 to 88.15, 24.22 to 88.78, 23.92 to 89.35, 23.82 to 89.78)
    }

    Box(
        modifier = modifier
            .background(waterColor)
            .clipToBounds()
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        onTransform(zoom, pan, size.width.toFloat(), size.height.toFloat())
                    }
                }
                .pointerInput(markerPoints, scale, panX, panY) {
                    detectTapGestures { tapOffset ->
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        var closest: MapPinPoint? = null
                        var bestDist = 56f
                        markerPoints.forEach { pt ->
                            val nx = ((pt.lng - minLng) / (maxLng - minLng)).toFloat()
                            val ny = (1.0 - (pt.lat - minLat) / (maxLat - minLat)).toFloat()
                            val sx = (nx * w - w / 2f) * scale + w / 2f + panX
                            val sy = (ny * h - h / 2f) * scale + h / 2f + panY
                            val dist = hypot(tapOffset.x - sx, tapOffset.y - sy)
                            if (dist < bestDist) {
                                bestDist = dist
                                closest = pt
                            }
                        }
                        closest?.let(onSelectPoint)
                    }
                }
        ) {
            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f) return@Canvas

            fun toScreen(lat: Double, lng: Double): Offset {
                val nx = ((lng - minLng) / (maxLng - minLng)).toFloat()
                val ny = (1.0 - (lat - minLat) / (maxLat - minLat)).toFloat()
                return Offset(
                    x = (nx * w - w / 2f) * scale + w / 2f + panX,
                    y = (ny * h - h / 2f) * scale + h / 2f + panY
                )
            }

            val bdPath = Path().apply {
                bdBoundary.forEachIndexed { idx, (lat, lng) ->
                    val pt = toScreen(lat, lng)
                    if (idx == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                }
                close()
            }
            drawPath(path = bdPath, color = landColor)
            drawPath(path = bdPath, color = borderColor, style = Stroke(width = 3.5f))

            fun drawRiver(coords: List<Pair<Double, Double>>) {
                val path = Path().apply {
                    coords.forEachIndexed { idx, (lat, lng) ->
                        val pt = toScreen(lat, lng)
                        if (idx == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                    }
                }
                drawPath(path = path, color = riverColor, style = Stroke(width = 4f, cap = StrokeCap.Round))
            }
            drawRiver(jamunaPadmaRiver)
            drawRiver(gangesPadmaRiver)

            BangladeshData.DISTRICTS.forEach { d ->
                val pos = toScreen(d.lat, d.lng)
                drawCircle(
                    color = borderColor.copy(alpha = 0.45f),
                    radius = (3.5f * scale.coerceAtMost(1.8f)),
                    center = pos
                )
                if (d.name in divisionHubs) {
                    val measured = textMeasurer.measure(
                        text = d.name,
                        style = TextStyle(fontSize = 10.sp, color = labelColor, fontWeight = FontWeight.SemiBold)
                    )
                    val lx = (pos.x + 6f).coerceIn(4f, (w - measured.size.width - 4f).coerceAtLeast(4f))
                    val ly = (pos.y - measured.size.height / 2f).coerceIn(4f, (h - measured.size.height - 4f).coerceAtLeast(4f))
                    drawText(measured, topLeft = Offset(lx, ly))
                }
            }

            // User GPS position marker
            myLocation?.let { loc ->
                val myPos = toScreen(loc.lat, loc.lng)
                drawCircle(
                    color = Color(0xFF1976D2).copy(alpha = 0.22f),
                    radius = 28f,
                    center = myPos
                )
                drawCircle(color = Color.White, radius = 11f, center = myPos)
                drawCircle(color = Color(0xFF1976D2), radius = 7.5f, center = myPos)
            }

            // Donor pins
            markerPoints.forEach { pt ->
                val pos = toScreen(pt.lat, pt.lng)
                val pinColor = if (pt.eligibleNow) primaryPinColor else Color(0xFFF57C00)
                drawCircle(color = Color.White, radius = 16f, center = pos)
                drawCircle(color = pinColor, radius = 12f, center = pos)
                val bgMeasured = textMeasurer.measure(
                    text = pt.donor.bloodGroup,
                    style = TextStyle(fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
                )
                drawText(
                    bgMeasured,
                    topLeft = Offset(pos.x - bgMeasured.size.width / 2f, pos.y - bgMeasured.size.height / 2f)
                )
            }
        }
    }
}

private fun buildLeafletHtml(initialTileUrl: String, isDark: Boolean): String {
    val bgHex = if (isDark) "#15202B" else "#EAF3FA"
    return """
        <!DOCTYPE html>
        <html>
        <head>
          <meta charset="utf-8" />
          <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
          <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
          <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
          <style>
            html, body, #map {
              margin: 0;
              padding: 0;
              width: 100%;
              height: 100%;
              background: transparent;
              font-family: sans-serif;
            }
            .bd-donor-pin {
              width: 34px;
              height: 34px;
              border-radius: 50%;
              border: 2.5px solid #ffffff;
              color: #ffffff;
              font-size: 11px;
              font-weight: 700;
              display: flex;
              align-items: center;
              justify-content: center;
              box-shadow: 0 2px 6px rgba(0,0,0,0.35);
            }
            .bd-popup {
              min-width: 170px;
              line-height: 1.4;
            }
            .bd-popup-btn {
              margin-top: 8px;
              width: 100%;
              padding: 7px 10px;
              border: none;
              border-radius: 8px;
              background: #b71c1c;
              color: #ffffff;
              font-weight: 600;
              font-size: 12px;
              cursor: pointer;
            }
          </style>
        </head>
        <body>
          <div id="map"></div>
          <script>
            (function() {
              if (typeof L === 'undefined') {
                if (window.AndroidBridge) AndroidBridge.onMapUnavailable();
                return;
              }
              var map = L.map('map', {
                center: [23.6850, 90.3563],
                zoom: 7,
                minZoom: 6,
                maxZoom: 18,
                zoomControl: true
              });
              var tileLoaded = false;
              var tileErrors = 0;
              var tileLayer = L.tileLayer(${JSONObject.quote(initialTileUrl)}, {
                attribution: '&copy; OpenStreetMap contributors',
                maxZoom: 18
              });
              tileLayer.on('tileload', function() {
                if (!tileLoaded) {
                  tileLoaded = true;
                  document.getElementById('map').style.background = '$bgHex';
                  if (window.AndroidBridge) AndroidBridge.onMapReady();
                }
              });
              tileLayer.on('tileerror', function() {
                tileErrors++;
                if (!tileLoaded && tileErrors >= 4) {
                  // Try Carto fallback before falling back to Vector map
                  if (tileLayer._url.indexOf('openstreetmap.org') !== -1) {
                    tileErrors = 0;
                    tileLayer.setUrl('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png');
                  } else if (window.AndroidBridge) {
                    AndroidBridge.onMapUnavailable();
                  }
                }
              });
              tileLayer.addTo(map);

              var markersLayer = L.layerGroup().addTo(map);
              var userLayer = L.layerGroup().addTo(map);

              function escapeHtml(str) {
                return String(str || '').replace(/[&<>"']/g, function(m) {
                  return {'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[m];
                });
              }

              window.setTileTheme = function(url, dark) {
                document.body.style.background = dark ? '#15202B' : '#EAF3FA';
                tileLayer.setUrl(url);
              };

              window.focusDistrict = function(lat, lng, zoom) {
                map.flyTo([lat, lng], zoom || 11, { duration: 0.7 });
              };

              window.updateUserLocation = function(loc) {
                userLayer.clearLayers();
                if (!loc) return;
                L.circle([loc.lat, loc.lng], {
                  radius: loc.radiusMeters || 10000,
                  color: '#1976D2',
                  fillColor: '#1976D2',
                  fillOpacity: 0.08,
                  weight: 1.5
                }).addTo(userLayer);
                L.circleMarker([loc.lat, loc.lng], {
                  radius: 8,
                  color: '#ffffff',
                  weight: 3,
                  fillColor: '#1976D2',
                  fillOpacity: 1
                }).bindPopup('<strong>Your Live GPS Location</strong>').addTo(userLayer);
              };

              window.updateMarkers = function(list) {
                markersLayer.clearLayers();
                if (!Array.isArray(list)) return;
                list.forEach(function(d) {
                  var icon = L.divIcon({
                    className: '',
                    html: '<div class="bd-donor-pin" style="background:' + escapeHtml(d.color) + '">' + escapeHtml(d.bloodGroup) + '</div>',
                    iconSize: [34, 34],
                    iconAnchor: [17, 17]
                  });
                  var statusText = d.available ? 'Available Now' : 'In Recovery / Cooldown';
                  var areaText = d.upazila ? (escapeHtml(d.upazila) + ', ' + escapeHtml(d.district)) : escapeHtml(d.district);
                  var distLine = d.distance ? '<br/><small style="color:#1565c0;font-weight:600">' + escapeHtml(d.distance) + '</small>' : '';
                  var popupHtml = '<div class="bd-popup">' +
                    '<strong>' + escapeHtml(d.name) + '</strong> (' + escapeHtml(d.bloodGroup) + ')<br/>' +
                    '<small>' + areaText + '</small>' + distLine + '<br/>' +
                    '<small>' + statusText + '</small>' +
                    '<button class="bd-popup-btn" onclick="AndroidBridge.selectDonor(\'' + escapeHtml(d.uid) + '\')">View Profile &amp; Message</button>' +
                    '</div>';
                  L.marker([d.lat, d.lng], { icon: icon })
                    .bindPopup(popupHtml)
                    .addTo(markersLayer);
                });
              };
            })();
          </script>
        </body>
        </html>
    """.trimIndent()
}
