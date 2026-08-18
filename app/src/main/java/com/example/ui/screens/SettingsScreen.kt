package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localization.AppLanguage
import com.example.localization.AppTranslation
import com.example.service.GatewayConfig
import com.example.service.GatewayEnvironment
import com.example.ui.theme.BkashPink
import com.example.ui.theme.CoralWarning
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NagadOrange
import com.example.ui.theme.SleekBorder
import com.example.ui.theme.SleekCard
import com.example.viewmodel.MainViewModel

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onLogout: () -> Unit
) {
    val settings by viewModel.settingsState.collectAsState()
    val gatewayConfig by viewModel.gatewayConfig.collectAsState()
    val currentLang by viewModel.currentLanguage.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var ispName by remember(settings) { mutableStateOf(settings?.ispName ?: "NetBill Broadband ISP") }
    var address by remember(settings) { mutableStateOf(settings?.address ?: "Uttara, Dhaka-1230, Bangladesh") }
    var mobile by remember(settings) { mutableStateOf(settings?.mobileNumber ?: "01711000000") }
    var helpline by remember(settings) { mutableStateOf(settings?.supportNumber ?: "01711000000") }
    var personalBkash by remember(settings) { mutableStateOf(settings?.personalBkashNo ?: "017XXXXXXXX") }
    var personalNagad by remember(settings) { mutableStateOf(settings?.personalNagadNo ?: "018XXXXXXXX") }

    // SMS Gateway Local State
    var smsApiUrl by remember(settings) { mutableStateOf(settings?.smsApiUrl ?: "https://api.greenweb.com.bd/api.php?json&apikey={API_KEY}&to={MOBILE}&senderid={SENDER_ID}&message={MESSAGE}") }
    var smsApiKey by remember(settings) { mutableStateOf(settings?.smsApiKey ?: "") }
    var smsSenderId by remember(settings) { mutableStateOf(settings?.smsSenderId ?: "") }
    var isAutoSmsEnabled by remember(settings) { mutableStateOf(settings?.isAutoSmsEnabled ?: false) }

    // WhatsApp Configuration State
    var adminWhatsapp by remember(settings) { mutableStateOf(settings?.adminWhatsappNumber ?: "") }
    var waUrl by remember(settings) { mutableStateOf(settings?.whatsappApiUrl ?: "") }
    var waInstance by remember(settings) { mutableStateOf("") } // whatsappInstanceId is missing from entity
    var waToken by remember(settings) { mutableStateOf(settings?.whatsappToken ?: "") }
    var isWaEnabled by remember(settings) { mutableStateOf(settings?.isWhatsappAlertEnabled ?: false) }

    // Gateway Config Local State
    var gwMode by remember(gatewayConfig) { mutableStateOf(gatewayConfig.environment) }
    var bkashAppKey by remember(gatewayConfig) { mutableStateOf(gatewayConfig.bkashAppKey) }
    var bkashAppSecret by remember(gatewayConfig) { mutableStateOf(gatewayConfig.bkashAppSecret) }
    var bkashUsername by remember(gatewayConfig) { mutableStateOf(gatewayConfig.bkashUsername) }
    var nagadMerchantId by remember(gatewayConfig) { mutableStateOf(gatewayConfig.nagadMerchantId) }
    var nagadMerchantMobile by remember(gatewayConfig) { mutableStateOf(gatewayConfig.nagadMerchantNumber) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = AppTranslation("settings_title"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Global Technician Night Mode Switch Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isDarkMode) Icons.Default.WbSunny else Icons.Default.DarkMode,
                                contentDescription = null,
                                tint = if (isDarkMode) com.example.ui.theme.IspAmberTertiary else com.example.ui.theme.Teal600
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Technician Night Mode / নাইট মোড",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = if (isDarkMode) "Active: High-Contrast Dark Theme" else "Active: Crisp Daytime Light Theme",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = isDarkMode,
                            onCheckedChange = { viewModel.setDarkMode(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = com.example.ui.theme.Teal600
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Optimized dark color palette for ISP field technicians working in low-light outdoor conditions and night installations.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Language Switcher Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Language & Region / ভাষা এবং অঞ্চল", fontWeight = FontWeight.Bold, color = com.example.ui.theme.Teal600, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (currentLang == AppLanguage.BANGLA) "বাংলা (Bangla Active)" else "English Active",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = { viewModel.toggleLanguage() },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(if (currentLang == AppLanguage.BANGLA) "Switch to English" else "বাংলায় পরিবর্তন করুন", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = com.example.ui.theme.Teal600, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Locked Currency: Bangladeshi Taka (৳ BDT)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                }
            }
        }

        // SMS Gateway Configuration Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null, tint = com.example.ui.theme.Teal600, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Automatic SMS Gateway (Billing Alerts)", fontWeight = FontWeight.Bold, color = com.example.ui.theme.Teal600, fontSize = 14.sp)
                        }
                        Switch(
                            checked = isAutoSmsEnabled,
                            onCheckedChange = { isAutoSmsEnabled = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = com.example.ui.theme.Teal600)
                        )
                    }

                    Text("Configure your HTTP SMS API to send automated bill alerts and payment receipts.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    OutlinedTextField(value = smsApiUrl, onValueChange = { smsApiUrl = it }, label = { Text("Gateway URL (with placeholders)") }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("https://api.gateway.com/send?key={API_KEY}&to={MOBILE}&msg={MESSAGE}") })
                    OutlinedTextField(value = smsApiKey, onValueChange = { smsApiKey = it }, label = { Text("SMS API Key") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = smsSenderId, onValueChange = { smsSenderId = it }, label = { Text("Sender ID / Masking") }, modifier = Modifier.fillMaxWidth())

                    Button(
                        onClick = { viewModel.saveISPSettings(ispName, address, mobile, helpline, smsApiUrl, smsApiKey, smsSenderId, isAutoSmsEnabled, waUrl, waToken, adminWhatsapp, isWaEnabled, personalBkash, personalNagad) },
                        colors = ButtonDefaults.buttonColors(containerColor = com.example.ui.theme.Teal600),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save SMS Configuration")
                    }
                }
            }
        }

        // WhatsApp Alert Configuration Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = com.example.ui.theme.Teal600, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("WhatsApp Expiry Alerts", fontWeight = FontWeight.Bold, color = com.example.ui.theme.Teal600, fontSize = 14.sp)
                        }
                        Switch(
                            checked = isWaEnabled,
                            onCheckedChange = { isWaEnabled = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = com.example.ui.theme.Teal600)
                        )
                    }

                    Text("Send automated expiry alerts to Admin and selected Staff members via WhatsApp.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    OutlinedTextField(value = adminWhatsapp, onValueChange = { adminWhatsapp = it }, label = { Text("Admin WhatsApp Number") }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("যেমন: 017XXXXXXXX") })
                    OutlinedTextField(value = waUrl, onValueChange = { waUrl = it }, label = { Text("WhatsApp API URL") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = waToken, onValueChange = { waToken = it }, label = { Text("API Token / Key") }, modifier = Modifier.fillMaxWidth())

                    Button(
                        onClick = { 
                            viewModel.saveISPSettings(
                                ispName = ispName, 
                                address = address, 
                                mobile = mobile, 
                                support = helpline,
                                waAlerts = isWaEnabled,
                                adminWa = adminWhatsapp,
                                waUrl = waUrl,
                                waToken = waToken,
                                smsUrl = smsApiUrl,
                                smsKey = smsApiKey,
                                smsSender = smsSenderId,
                                autoSms = isAutoSmsEnabled,
                                personalBkash = personalBkash,
                                personalNagad = personalNagad
                            ) 
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save WhatsApp Config")
                    }
                }
            }
        }

        // ISP Company Info Editor
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("ISP Company Profile", fontWeight = FontWeight.Bold, color = com.example.ui.theme.Teal600, fontSize = 14.sp)

                    OutlinedTextField(value = ispName, onValueChange = { ispName = it }, label = { Text("ISP Business Name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Office Address") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = mobile, onValueChange = { mobile = it }, label = { Text("Primary Contact Number") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = helpline, onValueChange = { helpline = it }, label = { Text("24/7 Support Helpline") }, modifier = Modifier.fillMaxWidth())

                    Text("Personal Payment Numbers (For Manual Pay)", fontWeight = FontWeight.Bold, color = com.example.ui.theme.Teal600, fontSize = 14.sp)
                    OutlinedTextField(value = personalBkash, onValueChange = { personalBkash = it }, label = { Text("Personal bKash Number") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = personalNagad, onValueChange = { personalNagad = it }, label = { Text("Personal Nagad Number") }, modifier = Modifier.fillMaxWidth())

                    Button(
                        onClick = { viewModel.saveISPSettings(ispName, address, mobile, helpline, smsApiUrl, smsApiKey, smsSenderId, isAutoSmsEnabled, waUrl, waToken, adminWhatsapp, isWaEnabled, personalBkash, personalNagad) },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Company Info")
                    }
                }
            }
        }

        // Payment Gateway Credentials Configuration
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Payment, contentDescription = null, tint = BkashPink, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("bKash & Nagad Payment Gateway APIs", fontWeight = FontWeight.Bold, color = com.example.ui.theme.Teal600, fontSize = 14.sp)
                        }
                    }

                    Text("API Mode:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = gwMode == GatewayEnvironment.SANDBOX,
                            onClick = { gwMode = GatewayEnvironment.SANDBOX },
                            label = { Text("Sandbox (Test Mode)", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ElectricBlue, selectedLabelColor = Color.White)
                        )
                        FilterChip(
                            selected = gwMode == GatewayEnvironment.PRODUCTION,
                            onClick = { gwMode = GatewayEnvironment.PRODUCTION },
                            label = { Text("Production (Live API)", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = BkashPink, selectedLabelColor = Color.White)
                        )
                    }

                    Text("bKash Tokenized Credentials:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BkashPink)
                    OutlinedTextField(value = bkashAppKey, onValueChange = { bkashAppKey = it }, label = { Text("bKash App Key") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = bkashAppSecret, onValueChange = { bkashAppSecret = it }, label = { Text("bKash App Secret") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = bkashUsername, onValueChange = { bkashUsername = it }, label = { Text("bKash Merchant Username") }, modifier = Modifier.fillMaxWidth())

                    Text("Nagad Payment Gateway Credentials:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NagadOrange)
                    OutlinedTextField(value = nagadMerchantId, onValueChange = { nagadMerchantId = it }, label = { Text("Nagad Merchant ID") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = nagadMerchantMobile, onValueChange = { nagadMerchantMobile = it }, label = { Text("Nagad Merchant Mobile Number") }, modifier = Modifier.fillMaxWidth())

                    Button(
                        onClick = {
                            viewModel.updateGatewayConfig(
                                GatewayConfig(
                                    environment = gwMode,
                                    bkashAppKey = bkashAppKey,
                                    bkashAppSecret = bkashAppSecret,
                                    bkashUsername = bkashUsername,
                                    nagadMerchantId = nagadMerchantId,
                                    nagadMerchantNumber = nagadMerchantMobile
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BkashPink),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Gateway API Credentials")
                    }
                }
            }
        }

        // Web Version & Web Admin Portal Card
        item {
            var webUrl by remember { mutableStateOf("https://netbill-isp-portal.web.app") }
            var isWebSyncEnabled by remember { mutableStateOf(true) }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Computer, contentDescription = null, tint = com.example.ui.theme.Teal600)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Web Version & Portal Access / ওয়েব সংস্করণ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp)
                        }
                    }

                    Text(
                        text = "Use both the Mobile App and Desktop Web Dashboard simultaneously with real-time cloud data synchronization.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )

                    OutlinedTextField(
                        value = webUrl,
                        onValueChange = { webUrl = it },
                        label = { Text("Web Dashboard URL / ওয়েব লিংক") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = {
                                viewModel.showToast("Web URL Copied: $webUrl")
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Web URL", tint = com.example.ui.theme.Teal600)
                            }
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.showToast("Opening Web Dashboard Portal...")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open Web Portal")
                        }

                        Button(
                            onClick = {
                                isWebSyncEnabled = !isWebSyncEnabled
                                viewModel.showToast(if (isWebSyncEnabled) "Web Sync Enabled!" else "Web Sync Paused")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isWebSyncEnabled) com.example.ui.theme.EmeraldSuccess else Color.Gray),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isWebSyncEnabled) "Web Sync Active" else "Enable Sync")
                        }
                    }
                }
            }
        }

        // User Profile & Logout
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Logged in User", fontWeight = FontWeight.Bold, color = com.example.ui.theme.Teal600, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(currentUser?.name ?: "Admin User", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp)
                    Text("Role: ${currentUser?.role ?: "Super Admin"}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = {
                            viewModel.logout()
                            onLogout()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralWarning),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Logout Account")
                    }
                }
            }
        }
    }
}
