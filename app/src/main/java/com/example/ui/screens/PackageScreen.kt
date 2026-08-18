package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import com.example.ui.theme.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.example.data.entity.PackageEntity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localization.AppTranslation
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.viewmodel.MainViewModel

@Composable
fun PackageScreen(viewModel: MainViewModel) {
    val packages by viewModel.packagesList.collectAsState()
    val currency = AppTranslation("currency_symbol")
    var showAddPkgDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddPkgDialog = true },
                containerColor = ElectricBlue,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Package")
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
            Text(
                text = AppTranslation("package_management"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Slate900
            )

            Spacer(modifier = Modifier.height(14.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(packages, key = { it.id }) { pkg ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SleekCard),
                        border = BorderStroke(1.dp, SleekBorder)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Speed, contentDescription = null, tint = Teal600)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(pkg.name, fontWeight = FontWeight.Bold, color = Slate900, fontSize = 16.sp)
                                    Text("Bandwidth Speed: ${pkg.speed}", color = Slate600, fontSize = 12.sp)
                                    Text("Active Subscribers: ${pkg.activeUserCount}", color = Slate600, fontSize = 11.sp)
                                }
                            }

                            Text(
                                text = "$currency ${pkg.monthlyPrice.toInt()}/mo",
                                fontWeight = FontWeight.Bold,
                                color = EmeraldSuccess,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddPkgDialog) {
        AddPackageDialog(
            onDismiss = { showAddPkgDialog = false },
            onAdd = { name, speed, price ->
                viewModel.addPackage(name, speed, price)
                showAddPkgDialog = false
            }
        )
    }
}

@Composable
fun AddPackageDialog(onDismiss: () -> Unit, onAdd: (String, String, Double) -> Unit) {
    var name by remember { mutableStateOf("") }
    var speed by remember { mutableStateOf("20 Mbps") }
    var price by remember { mutableStateOf("800") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Internet Package", color = Slate900, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Package Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = speed, onValueChange = { speed = it }, label = { Text("Speed (e.g. 20 Mbps)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Monthly Price (৳ BDT)") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = { onAdd(name, speed, price.toDoubleOrNull() ?: 800.0) }, colors = ButtonDefaults.buttonColors(containerColor = Teal600)) {
                Text("Save Package", color = Color.White)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Slate600) } },
        containerColor = SleekCard
    )
}
