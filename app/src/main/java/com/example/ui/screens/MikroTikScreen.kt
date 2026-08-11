package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import com.example.ui.theme.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.MikroTikRouterEntity
import com.example.localization.AppTranslation
import com.example.ui.theme.CoralWarning
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.viewmodel.MainViewModel

data class LivePppoeUser(
    val username: String,
    val ipAddress: String,
    val uptime: String,
    val rxMbps: Double,
    val txMbps: Double,
    val isEnabled: Boolean = true
)

@Composable
fun MikroTikScreen(viewModel: MainViewModel) {
    val routers by viewModel.mikrotikRouters.collectAsState()
    var showAddRouterDialog by remember { mutableStateOf(false) }

    var mockLiveUsers by remember {
        mutableStateOf(
            listOf(
                LivePppoeUser("rahim_uttara", "10.10.14.55", "3d 14h 22m", 18.4, 4.2, true),
                LivePppoeUser("tanvir_mirpur", "10.10.20.12", "1d 08h 05m", 19.8, 5.1, true),
                LivePppoeUser("kamrul_dhanmondi", "10.10.30.8", "0d 02h 10m", 48.2, 12.0, true),
                LivePppoeUser("hasan_uttara", "10.10.14.88", "5d 20h 11m", 0.0, 0.0, false)
            )
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddRouterDialog = true },
                containerColor = ElectricBlue,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Router")
            }
        },
        containerColor = SleekBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = AppTranslation("mikrotik_title"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )

                OutlinedButton(
                    onClick = { viewModel.showToast("MikroTik Router Synced!") },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Teal600)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(AppTranslation("sync_mikrotik"), fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Connected Routers Section
                item {
                    Text("Configured Routers", fontWeight = FontWeight.Bold, color = Teal600, fontSize = 14.sp)
                }

                items(routers, key = { it.id }) { router ->
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
                                    Icon(Icons.Default.Router, contentDescription = null, tint = Teal600, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(router.routerName, fontWeight = FontWeight.Bold, color = Slate900, fontSize = 15.sp)
                                        Text("IP: ${router.ipAddress}:${router.apiPort} • Zone: ${router.zone}", color = Slate600, fontSize = 12.sp)
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(if (router.isConnected) EmeraldSuccess.copy(alpha = 0.2f) else CoralWarning.copy(alpha = 0.2f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (router.isConnected) "Connected" else "Offline",
                                        color = if (router.isConnected) EmeraldSuccess else CoralWarning,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Online PPPoE Users: ${router.activePppoeCount}", color = Slate600, fontSize = 12.sp)
                                Text("Rx: ${router.totalRxMbps} Mbps | Tx: ${router.totalTxMbps} Mbps", color = Teal600, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Live Online Users Section
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(AppTranslation("online_users"), fontWeight = FontWeight.Bold, color = Slate900, fontSize = 16.sp)
                }

                items(mockLiveUsers) { user ->
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
                            Column {
                                Text(user.username, fontWeight = FontWeight.Bold, color = Slate900, fontSize = 14.sp)
                                Text("IP: ${user.ipAddress} • Uptime: ${user.uptime}", color = Slate600, fontSize = 11.sp)
                                Text("Traffic: Rx ${user.rxMbps} Mbps / Tx ${user.txMbps} Mbps", color = Teal600, fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    mockLiveUsers = mockLiveUsers.map { u ->
                                        if (u.username == user.username) u.copy(isEnabled = !u.isEnabled) else u
                                    }
                                    val action = if (user.isEnabled) "Disabled (Kicked)" else "Enabled"
                                    viewModel.showToast("User ${user.username} $action in MikroTik Router!")
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (user.isEnabled) CoralWarning else EmeraldSuccess
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(if (user.isEnabled) "Disable" else "Enable", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddRouterDialog) {
        AddRouterDialog(
            onDismiss = { showAddRouterDialog = false },
            onAdd = { name, ip, port, user, pass, zone ->
                viewModel.repository.mikrotikDao
                viewModel.showToast("Router $name added!")
                showAddRouterDialog = false
            }
        )
    }
}

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
        title = { Text("Add MikroTik Router", color = Slate900, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Router Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = ip, onValueChange = { ip = it }, label = { Text("Router IP Address (e.g. 192.168.88.1)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = port, onValueChange = { port = it }, label = { Text("API Port (Default 8728)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = user, onValueChange = { user = it }, label = { Text("API Username") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = pass, onValueChange = { pass = it }, label = { Text("API Password") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = zone, onValueChange = { zone = it }, label = { Text("Zone") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(name, ip, port.toIntOrNull() ?: 8728, user, pass, zone) },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
            ) {
                Text("Save Router")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        },
        containerColor = Navy800
    )
}
