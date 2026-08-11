package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.localization.AppLanguage
import com.example.localization.AppTranslation
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.viewmodel.LoginUiState
import com.example.viewmodel.MainViewModel

@Composable
fun LoginScreen(
    viewModel: MainViewModel,
    onLoginSuccess: () -> Unit
) {
    var isMobileMode by remember { mutableStateOf(false) }
    var usernameOrMobile by remember { mutableStateOf("admin") }
    var password by remember { mutableStateOf("admin123") }

    val currentLang by viewModel.currentLanguage.collectAsState()
    val loginUiState by viewModel.loginUiState.collectAsState()

    LaunchedEffect(loginUiState) {
        if (loginUiState is LoginUiState.Success) {
            onLoginSuccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(com.example.ui.theme.SleekBg)
            .padding(24.dp)
    ) {
        // Language Toggle Pill Top Right
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { viewModel.toggleLanguage() },
                shape = CircleShape,
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = com.example.ui.theme.Teal50,
                    contentColor = com.example.ui.theme.Teal600
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.Teal100)
            ) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = "Change Language",
                    modifier = Modifier.size(18.dp),
                    tint = com.example.ui.theme.Teal600
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (currentLang == AppLanguage.BANGLA) "EN" else "বাংলা",
                    fontWeight = FontWeight.Bold,
                    color = com.example.ui.theme.Teal600
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Branding Logo Icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(com.example.ui.theme.Teal600, com.example.ui.theme.Teal700)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Wifi,
                    contentDescription = "Logo",
                    tint = Color.White,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = AppTranslation("login_title"),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = com.example.ui.theme.Slate900,
                textAlign = TextAlign.Center
            )

            Text(
                text = AppTranslation("login_subtitle"),
                style = MaterialTheme.typography.bodyMedium,
                color = com.example.ui.theme.Teal600,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Login Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = com.example.ui.theme.SleekCard
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.SleekBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isMobileMode) AppTranslation("use_mobile_login") else AppTranslation("use_user_pass_login"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = com.example.ui.theme.Slate900
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    if (isMobileMode) {
                        OutlinedTextField(
                            value = usernameOrMobile,
                            onValueChange = { usernameOrMobile = it },
                            label = { Text(AppTranslation("mobile_number")) },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = com.example.ui.theme.Teal600) },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = com.example.ui.theme.Teal600,
                                unfocusedBorderColor = com.example.ui.theme.Slate200,
                                focusedLabelColor = com.example.ui.theme.Teal600,
                                unfocusedLabelColor = com.example.ui.theme.Slate500,
                                focusedTextColor = com.example.ui.theme.Slate900,
                                unfocusedTextColor = com.example.ui.theme.Slate900
                            )
                        )
                    } else {
                        OutlinedTextField(
                            value = usernameOrMobile,
                            onValueChange = { usernameOrMobile = it },
                            label = { Text(AppTranslation("username")) },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = com.example.ui.theme.Teal600) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = com.example.ui.theme.Teal600,
                                unfocusedBorderColor = com.example.ui.theme.Slate200,
                                focusedLabelColor = com.example.ui.theme.Teal600,
                                unfocusedLabelColor = com.example.ui.theme.Slate500,
                                focusedTextColor = com.example.ui.theme.Slate900,
                                unfocusedTextColor = com.example.ui.theme.Slate900
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(AppTranslation("password")) },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = com.example.ui.theme.Teal600) },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = com.example.ui.theme.Teal600,
                            unfocusedBorderColor = com.example.ui.theme.Slate200,
                            focusedLabelColor = com.example.ui.theme.Teal600,
                            unfocusedLabelColor = com.example.ui.theme.Slate500,
                            focusedTextColor = com.example.ui.theme.Slate900,
                            unfocusedTextColor = com.example.ui.theme.Slate900
                        )
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    if (loginUiState is LoginUiState.Error) {
                        Text(
                            text = (loginUiState as LoginUiState.Error).message,
                            color = Color.Red,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.loginUser(usernameOrMobile, password)
                        },
                        enabled = loginUiState !is LoginUiState.Loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = com.example.ui.theme.Teal600,
                            contentColor = Color.White
                        )
                    ) {
                        if (loginUiState is LoginUiState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = AppTranslation("login_btn"),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(
                        onClick = { isMobileMode = !isMobileMode }
                    ) {
                        Text(
                            text = if (isMobileMode) AppTranslation("use_user_pass_login") else AppTranslation("use_mobile_login"),
                            color = com.example.ui.theme.Teal600,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Quick Role Demo Presets
            Text(
                text = "Demo Accounts: admin/admin123 | operator/123456",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Footer Version
        Text(
            text = "NetBill ISP v2.4 (Bangladesh) • BDT ৳ Currency Locked",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
        )
    }
}
