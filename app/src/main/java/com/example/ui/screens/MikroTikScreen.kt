package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.MikroTikRouterEntity
import com.example.localization.AppTranslation
import com.example.ui.theme.AmberAlert
import com.example.ui.theme.CoralWarning
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.SleekBg
import com.example.ui.theme.SleekBorder
import com.example.ui.theme.SleekCard
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate900
import com.example.ui.theme.Teal100
import com.example.ui.theme.Teal500
import com.example.ui.theme.Teal600
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

// Enum for Session Filter
enum class SessionTypeFilter {
    ALL, PPPOE, HOTSPOT, DHCP, QUEUES
}

// Model for Active Connections
data class MikroTikSession(
    val id: String,
    val username: String,
    val customerName: String,
    val customerCode: String,
    val sessionType: String, // "PPPoE", "Hotspot", "DHCP Lease"
    val ipAddress: String,
    val macAddress: String,
    val uptime: String,
    val rxMbps: Double,
    val txMbps: Double,
    val totalDataGb: Double,
    val packageName: String,
    val callerId: String,
    val status: String = "Connected", // "Connected", "Throttled", "Disabled"
    val queueLimit: String = "20M/20M",
    val signalStrength: Int? = null
)

// Model for Router Interfaces
data class RouterInterfaceStatus(
    val name: String,
    val type: String,
    val linkSpeed: String,
    val rxMbps: Double,
    val txMbps: Double,
    val isUp: Boolean,
    val droppedPackets: Int = 0
)

// Model for Traffic Queues
data class TrafficQueueItem(
    val name: String,
    val target: String,
    val maxLimit: String,
    val currentRxMbps: Double,
    val currentTxMbps: Double,
    val burstActive: Boolean
)

// Model for Router Log Entry
data class RouterLogEntry(
    val time: String,
    val topic: String, // "pppoe,info", "system,info", "dhcp,warning", "firewall,alert"
    val message: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MikroTikScreen(viewModel: MainViewModel) {
    val routers by viewModel.mikrotikRouters.collectAsState()
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    // Active Router Selection
    var selectedRouterId by remember { mutableStateOf<Long?>(null) }
    val currentRouter = routers.find { it.id == selectedRouterId } ?: routers.firstOrNull() ?: MikroTikRouterEntity(
        id = 1,
        routerName = "Core RouterBOARD CCR2116",
        ipAddress = "192.168.88.1",
        apiPort = 8728,
        username = "admin",
        password = "",
        isConnected = true,
        activePppoeCount = 412,
        totalRxMbps = 845.2,
        totalTxMbps = 320.8,
        zone = "Uttara Core Hub"
    )

    // Hardware Status State
    var cpuLoad by remember { mutableStateOf(18) }
    var ramUsedMb by remember { mutableStateOf(1240) }
    val ramTotalMb = 4096
    var tempCelsius by remember { mutableStateOf(41.5) }
    var voltageVolts by remember { mutableStateOf(24.2) }
    var uptimeString by remember { mutableStateOf("18d 14h 22m") }

    // Live Real-Time Bandwidth Samples (Download / Upload)
    val rxHistory = remember { mutableStateListOf(400f, 420f, 480f, 510f, 600f, 650f, 720f, 810f, 845f) }
    val txHistory = remember { mutableStateListOf(150f, 180f, 210f, 220f, 260f, 290f, 310f, 300f, 320f) }

    // Active Sessions State
    var activeSessions by remember {
        mutableStateOf(
            listOf(
                MikroTikSession("s-101", "rahim_uttara", "Rahim Uddin", "NET-1001", "PPPoE", "10.10.14.55", "DC:2C:6E:9A:12:44", "3d 14h 22m", 18.4, 4.2, 45.2, "20 Mbps Standard", "vlan100-ether2", "Connected", "20M/20M"),
                MikroTikSession("s-102", "tanvir_mirpur", "Tanvir Hasan", "NET-1004", "PPPoE", "10.10.20.12", "E4:5F:01:88:32:10", "1d 08h 05m", 19.8, 5.1, 28.6, "20 Mbps Standard", "vlan100-ether2", "Connected", "20M/20M"),
                MikroTikSession("s-103", "kamrul_dhanmondi", "Kamrul Islam", "NET-1008", "PPPoE", "10.10.30.8", "70:85:C2:11:00:22", "0d 02h 10m", 48.2, 12.0, 112.4, "50 Mbps Gaming", "vlan100-ether2", "Connected", "50M/50M"),
                MikroTikSession("s-104", "hotspot_guest_44", "Visitor Guest 44", "HST-44", "Hotspot", "192.168.88.110", "A0:2B:11:33:44:55", "0d 00h 45m", 4.2, 1.1, 1.2, "Hotspot 5 Mbps", "hs-wlan1", "Connected", "5M/5M", signalStrength = -62),
                MikroTikSession("s-105", "hotspot_cafe_vip", "Uttara Coffee Shop", "HST-12", "Hotspot", "192.168.88.115", "B2:3C:4D:5E:6F:70", "2d 11h 30m", 12.5, 3.8, 18.9, "Hotspot 15 Mbps", "hs-wlan1", "Connected", "15M/15M", signalStrength = -55),
                MikroTikSession("s-106", "static_office_server", "Main Accounts PC", "OFF-01", "DHCP Lease", "192.168.1.50", "00:1A:2B:3C:4D:5E", "15d 02h 11m", 2.1, 0.4, 8.4, "Static Internal", "ether3-Office", "Connected", "100M/100M")
            )
        )
    }

    // Router Interfaces List
    var interfacesList by remember {
        mutableStateOf(
            listOf(
                RouterInterfaceStatus("sfp-sfpplus1-WAN1", "10G Optical SFP+", "10 Gbps Full Duplex", 520.4, 180.2, true),
                RouterInterfaceStatus("ether2-WAN2-Fiber", "1G Gigabit Ethernet", "1 Gbps Full Duplex", 324.8, 140.6, true),
                RouterInterfaceStatus("bridge-LAN", "Internal Bridge", "10 Gbps Virtual", 780.0, 290.0, true),
                RouterInterfaceStatus("vlan100-PPPoE", "Customer VLAN 100", "Dynamic VLAN", 412.0, 180.0, true),
                RouterInterfaceStatus("vlan200-Hotspot", "Public WiFi VLAN", "Dynamic VLAN", 110.5, 30.2, true)
            )
        )
    }

    // Traffic Queues List
    var queuesList by remember {
        mutableStateOf(
            listOf(
                TrafficQueueItem("q-kamrul_dhanmondi", "10.10.30.8/32", "50M/50M", 48.2, 12.0, true),
                TrafficQueueItem("q-rahim_uttara", "10.10.14.55/32", "20M/20M", 18.4, 4.2, false),
                TrafficQueueItem("q-tanvir_mirpur", "10.10.20.12/32", "20M/20M", 19.8, 5.1, false),
                TrafficQueueItem("q-hotspot-pool", "192.168.88.0/24", "100M/100M", 16.7, 4.9, false)
            )
        )
    }

    // Filters and Search
    var selectedFilter by remember { mutableStateOf(SessionTypeFilter.ALL) }
    var searchQuery by remember { mutableStateOf("") }

    // Dialogs & Modals Control
    var showAddRouterDialog by remember { mutableStateOf(false) }
    var pingTargetIp by remember { mutableStateOf<String?>(null) }
    var showLogsSheet by remember { mutableStateOf(false) }
    var sessionToDisconnect by remember { mutableStateOf<MikroTikSession?>(null) }
    var sessionToThrottle by remember { mutableStateOf<MikroTikSession?>(null) }
    var sessionDetailTarget by remember { mutableStateOf<MikroTikSession?>(null) }

    // Continuous Real-Time Sampling Loop
    LaunchedEffect(Unit) {
        while (true) {
            delay(1200)
            // Generate subtle live fluctuation in bandwidth metrics
            val newRx = (780f + Random.nextFloat() * 120f).coerceIn(400f, 1200f)
            val newTx = (280f + Random.nextFloat() * 60f).coerceIn(100f, 500f)

            if (rxHistory.size >= 12) rxHistory.removeAt(0)
            if (txHistory.size >= 12) txHistory.removeAt(0)

            rxHistory.add(newRx)
            txHistory.add(newTx)

            // Randomize minor CPU & RAM changes
            cpuLoad = (15 + Random.nextInt(12)).coerceIn(5, 95)
            ramUsedMb = (1200 + Random.nextInt(120)).coerceIn(500, 3800)
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddRouterDialog = true },
                containerColor = ElectricBlue,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add RouterBOARD")
            }
        },
        containerColor = SleekBg
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Dashboard Top Bar & Router Selector
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "MikroTik Connection Monitor",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                            Text(
                                text = "Live ISP Network Traffic & RouterBOARD Control",
                                fontSize = 12.sp,
                                color = Slate600
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { showLogsSheet = true },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Teal100)
                            ) {
                                Icon(Icons.Default.Terminal, contentDescription = "Router Logs", tint = Teal600)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            OutlinedButton(
                                onClick = {
                                    viewModel.showToast("Router Connection Synced!")
                                },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, SleekBorder),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Teal600)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Sync", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Horizontal Router Selector Chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(routers) { router ->
                            val isSelected = router.id == currentRouter.id
                            Card(
                                modifier = Modifier.clickable { selectedRouterId = router.id },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) Teal600 else SleekCard
                                ),
                                border = BorderStroke(1.dp, if (isSelected) Teal600 else SleekBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Router,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else Teal600,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = router.routerName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (isSelected) Color.White else Slate900
                                        )
                                        Text(
                                            text = "${router.ipAddress} • ${router.zone}",
                                            fontSize = 10.sp,
                                            color = if (isSelected) Color.White.copy(alpha = 0.85f) else Slate600
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            OutlinedButton(
                                onClick = { showAddRouterDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, SleekBorder)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Teal600)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("New Router", fontSize = 12.sp, color = Teal600)
                            }
                        }
                    }
                }
            }

            // 2. Active Router Banner & Live Health Status Bar
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SleekCard),
                    border = BorderStroke(1.dp, SleekBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(if (currentRouter.isConnected) EmeraldSuccess.copy(alpha = 0.15f) else CoralWarning.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeveloperBoard,
                                        contentDescription = null,
                                        tint = if (currentRouter.isConnected) EmeraldSuccess else CoralWarning,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = currentRouter.routerName,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 16.sp,
                                            color = Slate900
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (currentRouter.isConnected) EmeraldSuccess else CoralWarning)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = if (currentRouter.isConnected) "ONLINE" else "OFFLINE",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 9.sp
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Model: RouterBOARD 2116-12G-4S+ • OS: RouterOS v7.14.2",
                                        fontSize = 11.sp,
                                        color = Slate600
                                    )
                                }
                            }

                            IconButton(
                                onClick = { pingTargetIp = "8.8.8.8" },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(ElectricBlue.copy(alpha = 0.1f))
                            ) {
                                Icon(Icons.Default.NetworkCheck, contentDescription = "Ping Test", tint = ElectricBlue)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = SleekBorder)
                        Spacer(modifier = Modifier.height(14.dp))

                        // Hardware Metrics Row (CPU, RAM, Temp, Voltage, Uptime)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            MetricItem(
                                icon = Icons.Default.Speed,
                                label = "CPU Load",
                                value = "$cpuLoad%",
                                tint = if (cpuLoad > 80) CoralWarning else Teal600
                            )
                            MetricItem(
                                icon = Icons.Default.Memory,
                                label = "RAM Memory",
                                value = "${ramUsedMb}M / ${ramTotalMb}M",
                                tint = ElectricBlue
                            )
                            MetricItem(
                                icon = Icons.Default.Thermostat,
                                label = "Board Temp",
                                value = "${tempCelsius}°C",
                                tint = AmberAlert
                            )
                            MetricItem(
                                icon = Icons.Default.Timer,
                                label = "Uptime",
                                value = uptimeString,
                                tint = EmeraldSuccess
                            )
                        }
                    }
                }
            }

            // 3. Live Bandwidth Usage Speedometer & Real-Time Canvas Graph
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SleekCard),
                    border = BorderStroke(1.dp, SleekBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.SwapVert, contentDescription = null, tint = CyanAccent)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Live Network Bandwidth Throughput",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Slate900
                                    )
                                    Text(
                                        text = "Real-time Rx/Tx interface traffic graph",
                                        fontSize = 11.sp,
                                        color = Slate600
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(EmeraldSuccess.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(EmeraldSuccess)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "LIVE 1s Feed",
                                        color = EmeraldSuccess,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Download / Upload Meter Cards
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Download Rx
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = CyanAccent.copy(alpha = 0.08f)),
                                border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.3f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Download (Rx)", fontSize = 11.sp, color = Slate600)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${rxHistory.lastOrNull()?.toInt() ?: 845} Mbps",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp,
                                        color = CyanAccent
                                    )
                                    Text("Peak: 1.20 Gbps", fontSize = 10.sp, color = Slate600)
                                }
                            }

                            // Upload Tx
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = AmberAlert.copy(alpha = 0.08f)),
                                border = BorderStroke(1.dp, AmberAlert.copy(alpha = 0.3f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = AmberAlert, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Upload (Tx)", fontSize = 11.sp, color = Slate600)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${txHistory.lastOrNull()?.toInt() ?: 320} Mbps",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp,
                                        color = AmberAlert
                                    )
                                    Text("Peak: 480.0 Mbps", fontSize = 10.sp, color = Slate600)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Custom Canvas Line Chart for Real-Time Traffic
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .padding(8.dp)
                        ) {
                            val cyanColor = CyanAccent
                            val amberColor = AmberAlert
                            val gridColor = SleekBorder

                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val width = size.width
                                val height = size.height
                                val maxVal = 1200f

                                // Draw horizontal grid lines
                                repeat(4) { i ->
                                    val y = height * (i / 3f)
                                    drawLine(
                                        color = gridColor,
                                        start = Offset(0f, y),
                                        end = Offset(width, y),
                                        strokeWidth = 1f
                                    )
                                }

                                // Function to draw smooth path
                                fun drawGraphPath(points: List<Float>, color: Color) {
                                    if (points.isEmpty()) return
                                    val path = Path()
                                    val dx = width / (points.size - 1).coerceAtLeast(1)

                                    points.forEachIndexed { index, value ->
                                        val x = index * dx
                                        val y = height - (value / maxVal) * height
                                        if (index == 0) {
                                            path.moveTo(x, y)
                                        } else {
                                            val prevX = (index - 1) * dx
                                            val prevY = height - (points[index - 1] / maxVal) * height
                                            val controlX1 = prevX + (x - prevX) / 2f
                                            val controlX2 = prevX + (x - prevX) / 2f
                                            path.cubicTo(controlX1, prevY, controlX2, y, x, y)
                                        }
                                    }

                                    drawPath(
                                        path = path,
                                        color = color,
                                        style = Stroke(width = 4f, cap = StrokeCap.Round)
                                    )
                                }

                                // Draw Download (Rx) Path & Upload (Tx) Path
                                drawGraphPath(rxHistory, cyanColor)
                                drawGraphPath(txHistory, amberColor)
                            }
                        }
                    }
                }
            }

            // 4. Interface Status Breakdown
            item {
                Column {
                    Text(
                        text = "Router Interfaces Status",
                        fontWeight = FontWeight.Bold,
                        color = Slate900,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        interfacesList.forEachIndexed { index, iface ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = SleekCard),
                                border = BorderStroke(1.dp, SleekBorder)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(if (iface.isUp) EmeraldSuccess else CoralWarning)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(iface.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Slate900)
                                            Text("${iface.type} • ${iface.linkSpeed}", fontSize = 11.sp, color = Slate600)
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("Rx: ${iface.rxMbps} Mbps", fontSize = 11.sp, color = CyanAccent, fontWeight = FontWeight.Bold)
                                            Text("Tx: ${iface.txMbps} Mbps", fontSize = 11.sp, color = AmberAlert, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = {
                                                interfacesList = interfacesList.mapIndexed { idx, item ->
                                                    if (idx == index) item.copy(isUp = !item.isUp) else item
                                                }
                                                val state = if (!iface.isUp) "Enabled" else "Disabled"
                                                viewModel.showToast("Interface ${iface.name} $state")
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (iface.isUp) CoralWarning.copy(alpha = 0.15f) else EmeraldSuccess.copy(alpha = 0.15f),
                                                contentColor = if (iface.isUp) CoralWarning else EmeraldSuccess
                                            ),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Text(if (iface.isUp) "Disable" else "Enable", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. Active Connections & Sessions Management Header
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Active Connected Sessions (${activeSessions.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Slate900
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Filter Tabs (All, PPPoE, Hotspot, DHCP, Queues)
                    ScrollableTabRow(
                        selectedTabIndex = selectedFilter.ordinal,
                        edgePadding = 0.dp,
                        containerColor = Color.Transparent,
                        divider = {}
                    ) {
                        SessionTypeFilter.values().forEach { filter ->
                            Tab(
                                selected = selectedFilter == filter,
                                onClick = { selectedFilter = filter },
                                text = {
                                    val label = when (filter) {
                                        SessionTypeFilter.ALL -> "All Sessions"
                                        SessionTypeFilter.PPPOE -> "PPPoE (${activeSessions.count { it.sessionType == "PPPoE" }})"
                                        SessionTypeFilter.HOTSPOT -> "Hotspot (${activeSessions.count { it.sessionType == "Hotspot" }})"
                                        SessionTypeFilter.DHCP -> "DHCP Leases (${activeSessions.count { it.sessionType == "DHCP Lease" }})"
                                        SessionTypeFilter.QUEUES -> "Traffic Queues (${queuesList.size})"
                                    }
                                    Text(
                                        text = label,
                                        fontWeight = if (selectedFilter == filter) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 12.sp
                                    )
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Search Field for Active Users
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search by User, IP, MAC or Package...", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Slate600) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = Slate600)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // 6. Active Sessions or Queues List
            val filteredSessions = activeSessions.filter { session ->
                val typeMatch = when (selectedFilter) {
                    SessionTypeFilter.ALL -> true
                    SessionTypeFilter.PPPOE -> session.sessionType == "PPPoE"
                    SessionTypeFilter.HOTSPOT -> session.sessionType == "Hotspot"
                    SessionTypeFilter.DHCP -> session.sessionType == "DHCP Lease"
                    SessionTypeFilter.QUEUES -> false
                }
                val queryMatch = searchQuery.isEmpty() ||
                        session.username.contains(searchQuery, ignoreCase = true) ||
                        session.ipAddress.contains(searchQuery, ignoreCase = true) ||
                        session.customerName.contains(searchQuery, ignoreCase = true) ||
                        session.macAddress.contains(searchQuery, ignoreCase = true)

                typeMatch && queryMatch
            }

            if (selectedFilter == SessionTypeFilter.QUEUES) {
                // Render Simple Queues
                items(queuesList) { queue ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SleekCard),
                        border = BorderStroke(1.dp, SleekBorder)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(queue.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Slate900)
                                    Text("Target IP: ${queue.target} • Limit: ${queue.maxLimit}", fontSize = 11.sp, color = Slate600)
                                }

                                if (queue.burstActive) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(AmberAlert.copy(alpha = 0.2f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("BURST ACTIVE", color = AmberAlert, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Rx: ${queue.currentRxMbps} Mbps", color = CyanAccent, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                Text("Tx: ${queue.currentTxMbps} Mbps", color = AmberAlert, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            } else {
                // Render Active Sessions
                if (filteredSessions.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = SleekCard),
                            border = BorderStroke(1.dp, SleekBorder)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No active sessions match the filter criteria.", fontSize = 13.sp, color = Slate600)
                            }
                        }
                    }
                } else {
                    items(filteredSessions, key = { it.id }) { session ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = SleekCard),
                            border = BorderStroke(1.dp, SleekBorder)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    when (session.sessionType) {
                                                        "PPPoE" -> EmeraldSuccess.copy(alpha = 0.15f)
                                                        "Hotspot" -> AmberAlert.copy(alpha = 0.15f)
                                                        else -> ElectricBlue.copy(alpha = 0.15f)
                                                    }
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = when (session.sessionType) {
                                                    "PPPoE" -> Icons.Default.Wifi
                                                    "Hotspot" -> Icons.Default.WifiTethering
                                                    else -> Icons.Default.Router
                                                },
                                                contentDescription = null,
                                                tint = when (session.sessionType) {
                                                    "PPPoE" -> EmeraldSuccess
                                                    "Hotspot" -> AmberAlert
                                                    else -> ElectricBlue
                                                },
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = session.username,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = Slate900
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(
                                                            when (session.sessionType) {
                                                                "PPPoE" -> EmeraldSuccess.copy(alpha = 0.15f)
                                                                "Hotspot" -> AmberAlert.copy(alpha = 0.15f)
                                                                else -> ElectricBlue.copy(alpha = 0.15f)
                                                            }
                                                        )
                                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                                ) {
                                                    Text(
                                                        text = session.sessionType,
                                                        color = when (session.sessionType) {
                                                            "PPPoE" -> EmeraldSuccess
                                                            "Hotspot" -> AmberAlert
                                                            else -> ElectricBlue
                                                        },
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 9.sp
                                                    )
                                                }
                                            }

                                            Text(
                                                text = "${session.customerName} (${session.customerCode}) • ${session.ipAddress}",
                                                fontSize = 11.sp,
                                                color = Slate600
                                            )
                                        }
                                    }

                                    // Status Badge (Connected / Throttled)
                                    if (session.status == "Throttled") {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(AmberAlert)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("THROTTLED", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Session Stats Pill
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Uptime: ${session.uptime}", fontSize = 11.sp, color = Slate600)
                                    Text("Rx: ${session.rxMbps} M / Tx: ${session.txMbps} M", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Teal600)
                                    Text("Data: ${session.totalDataGb} GB", fontSize = 11.sp, color = Slate600)
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Action Buttons Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row {
                                        OutlinedButton(
                                            onClick = { sessionDetailTarget = session },
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, SleekBorder),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Details", fontSize = 11.sp)
                                        }

                                        Spacer(modifier = Modifier.width(6.dp))

                                        OutlinedButton(
                                            onClick = { pingTargetIp = session.ipAddress },
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, SleekBorder),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(14.dp), tint = ElectricBlue)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Ping", fontSize = 11.sp, color = ElectricBlue)
                                        }

                                        Spacer(modifier = Modifier.width(6.dp))

                                        OutlinedButton(
                                            onClick = { sessionToThrottle = session },
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, SleekBorder),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(14.dp), tint = AmberAlert)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Speed", fontSize = 11.sp, color = AmberAlert)
                                        }
                                    }

                                    Button(
                                        onClick = { sessionToDisconnect = session },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = CoralWarning),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text("Disconnect", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal: Disconnect Session Confirmation
    if (sessionToDisconnect != null) {
        val target = sessionToDisconnect!!
        AlertDialog(
            onDismissRequest = { sessionToDisconnect = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PowerSettingsNew, contentDescription = null, tint = CoralWarning)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Disconnect Session?", fontWeight = FontWeight.Bold, color = Slate900)
                }
            },
            text = {
                Text(
                    "Are you sure you want to disconnect user '${target.username}' (${target.ipAddress}) from MikroTik router '${currentRouter.routerName}'?\n\nThis will immediately terminate the active PPPoE/Hotspot line.",
                    fontSize = 13.sp,
                    color = Slate600
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        activeSessions = activeSessions.filter { it.id != target.id }
                        viewModel.showToast("Session '${target.username}' disconnected from Router!")
                        sessionToDisconnect = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralWarning)
                ) {
                    Text("Kick & Disconnect", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDisconnect = null }) {
                    Text("Cancel", color = Slate600)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Modal: Speed Throttle / Override Dialog
    if (sessionToThrottle != null) {
        val target = sessionToThrottle!!
        var selectedSpeed by remember { mutableStateOf("10M/10M") }

        AlertDialog(
            onDismissRequest = { sessionToThrottle = null },
            title = { Text("Bandwidth Speed Override", fontWeight = FontWeight.Bold, color = Slate900) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Select new rate limit queue for user '${target.username}':", fontSize = 12.sp, color = Slate600)

                    val speeds = listOf("1M/1M (Throttled)", "5M/5M", "10M/10M", "20M/20M", "50M/50M (Turbo)")
                    speeds.forEach { speed ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedSpeed == speed) Teal100 else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { selectedSpeed = speed }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (selectedSpeed == speed) Icons.Default.CheckCircle else Icons.Default.Speed,
                                contentDescription = null,
                                tint = if (selectedSpeed == speed) Teal600 else Slate600,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(speed, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Slate900)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        activeSessions = activeSessions.map { s ->
                            if (s.id == target.id) s.copy(queueLimit = selectedSpeed, status = "Throttled") else s
                        }
                        viewModel.showToast("Queue limit for '${target.username}' set to $selectedSpeed!")
                        sessionToThrottle = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Teal600)
                ) {
                    Text("Apply Speed Limit")
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToThrottle = null }) {
                    Text("Cancel", color = Slate600)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Modal: Session Details Bottom Sheet
    if (sessionDetailTarget != null) {
        val target = sessionDetailTarget!!
        ModalBottomSheet(
            onDismissRequest = { sessionDetailTarget = null },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Session Detailed Metrics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Slate900)

                MikroTikDetailRow("Username", target.username)
                MikroTikDetailRow("Customer Name", target.customerName)
                MikroTikDetailRow("Customer Code", target.customerCode)
                MikroTikDetailRow("Connection Type", target.sessionType)
                MikroTikDetailRow("IP Address", target.ipAddress)
                MikroTikDetailRow("MAC Address", target.macAddress)
                MikroTikDetailRow("Caller ID Interface", target.callerId)
                MikroTikDetailRow("Uptime Duration", target.uptime)
                MikroTikDetailRow("Current Rx / Tx Speed", "${target.rxMbps} Mbps / ${target.txMbps} Mbps")
                MikroTikDetailRow("Total Transferred", "${target.totalDataGb} GB")
                MikroTikDetailRow("Assigned Rate Limit", target.queueLimit)

                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = { sessionDetailTarget = null },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Teal600)
                ) {
                    Text("Close Details")
                }
            }
        }
    }

    // Modal: Live Ping Diagnostic Tool
    if (pingTargetIp != null) {
        PingDiagnosticModal(
            ipAddress = pingTargetIp!!,
            onDismiss = { pingTargetIp = null }
        )
    }

    // Modal: Router Logs Bottom Sheet
    if (showLogsSheet) {
        RouterLogsBottomSheet(
            onDismiss = { showLogsSheet = false }
        )
    }

    // Modal: Add MikroTik Router Dialog
    if (showAddRouterDialog) {
        AddRouterDialog(
            onDismiss = { showAddRouterDialog = false },
            onAdd = { name, ip, port, user, pass, zone ->
                viewModel.addMikroTikRouter(
                    MikroTikRouterEntity(
                        routerName = name,
                        ipAddress = ip,
                        apiPort = port,
                        username = user,
                        password = pass,
                        zone = zone,
                        isConnected = true
                    )
                )
                showAddRouterDialog = false
            }
        )
    }
}

@Composable
fun MetricItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, tint: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.height(2.dp))
        Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = Slate900)
        Text(label, fontSize = 9.sp, color = Slate600)
    }
}

@Composable
fun MikroTikDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = Slate600)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate900)
    }
}

// Live Ping Diagnostic Modal
@Composable
fun PingDiagnosticModal(ipAddress: String, onDismiss: () -> Unit) {
    var pingResults by remember { mutableStateOf(listOf<String>()) }
    var isPinging by remember { mutableStateOf(true) }

    LaunchedEffect(ipAddress) {
        pingResults = listOf("Initiating ICMP Echo Request to $ipAddress...")
        for (seq in 1..5) {
            delay(400)
            val rtt = (8 + Random.nextInt(14))
            pingResults = pingResults + "64 bytes from $ipAddress: icmp_seq=$seq ttl=64 time=${rtt}ms"
        }
        delay(300)
        pingResults = pingResults + "--- $ipAddress ping statistics ---" +
                "5 packets transmitted, 5 received, 0% packet loss, time 2004ms" +
                "rtt min/avg/max = 8.2/12.4/22.1 ms"
        isPinging = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.NetworkCheck, contentDescription = null, tint = ElectricBlue)
                Spacer(modifier = Modifier.width(8.dp))
                Text("ICMP Ping Diagnostic", fontWeight = FontWeight.Bold, color = Slate900)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F172A))
                    .padding(10.dp)
            ) {
                if (isPinging) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = ElectricBlue)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                LazyColumn {
                    items(pingResults) { line ->
                        Text(line, fontSize = 11.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)) {
                Text("Close Diagnostic")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

// Router Event Logs Bottom Sheet
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouterLogsBottomSheet(onDismiss: () -> Unit) {
    val mockLogs = remember {
        listOf(
            RouterLogEntry("11:42:01", "pppoe,info", "PPPoE connection established for user 'rahim_uttara' from MAC DC:2C:6E:9A:12:44"),
            RouterLogEntry("11:40:15", "dhcp,info", "DHCP lease 192.168.88.110 assigned to Hostspot client 'Visitor Guest 44'"),
            RouterLogEntry("11:38:00", "system,info", "RouterBOARD NTP time synchronized with time.google.com"),
            RouterLogEntry("11:32:10", "firewall,warning", "Port scan detected from WAN IP 185.220.101.4 - Drop rule executed"),
            RouterLogEntry("11:25:40", "interface,info", "sfp-sfpplus1-WAN1 link speed changed to 10Gbps Full Duplex"),
            RouterLogEntry("11:12:00", "pppoe,info", "User 'kamrul_dhanmondi' authenticated via RADIUS server")
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Terminal, contentDescription = null, tint = Teal600)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("RouterOS Live Event Logs", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Slate900)
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Clear, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(mockLogs) { log ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(log.topic, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Teal600)
                                Text(log.time, fontSize = 10.sp, color = Slate600)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(log.message, fontSize = 12.sp, color = Slate900)
                        }
                    }
                }
            }
        }
    }
}

// Add Router Dialog
@Composable
fun AddRouterDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, Int, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var ip by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("8728") }
    var user by remember { mutableStateOf("admin") }
    var pass by remember { mutableStateOf("") }
    var zone by remember { mutableStateOf("Uttara Zone") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add MikroTik RouterBOARD", color = Slate900, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Router Name (e.g. Uttara POP)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = ip, onValueChange = { ip = it }, label = { Text("IP Address (e.g. 192.168.88.1)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = port, onValueChange = { port = it }, label = { Text("API Port (Default 8728)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = user, onValueChange = { user = it }, label = { Text("API Username") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = pass, onValueChange = { pass = it }, label = { Text("API Password") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = zone, onValueChange = { zone = it }, label = { Text("ISP Zone") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && ip.isNotBlank()) {
                        onAdd(name, ip, port.toIntOrNull() ?: 8728, user, pass, zone)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
            ) {
                Text("Save RouterBOARD")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Slate600) }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}
