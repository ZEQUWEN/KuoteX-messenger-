package com.example.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val languages = listOf(
    Triple("en", "English", "🇺🇸"),
    Triple("ru", "Русский", "🇷🇺"),
    Triple("uk", "Українська", "🇺🇦"),
    Triple("es", "Español", "🇪🇸"),
    Triple("de", "Deutsch", "🇩🇪"),
    Triple("fr", "Français", "🇫🇷"),
    Triple("kz", "Қазақша", "🇰🇿")
)

fun getCountryFlagForPhoneNumber(phone: String): String {
    if (phone.startsWith("+79") || phone.startsWith("+78") || phone.startsWith("+73") || phone.startsWith("+74") || phone.startsWith("+75")) return "🇷🇺"
    if (phone.startsWith("+77") || phone.startsWith("+76") || phone.startsWith("+70") || phone.startsWith("+71") || phone.startsWith("+72")) return "🇰🇿"
    if (phone.startsWith("+1")) return "🇺🇸"
    if (phone.startsWith("+380")) return "🇺🇦"
    if (phone.startsWith("+44")) return "🇬🇧"
    if (phone.startsWith("+49")) return "🇩🇪"
    if (phone.startsWith("+33")) return "🇫🇷"
    if (phone.startsWith("+34")) return "🇪🇸"
    return "🌍"
}

@Composable
fun LanguageSelector() {
    var selectedLanguage by remember { mutableStateOf(languages[0]) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Select Language", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(languages) { lang ->
                    val isSelected = lang == selectedLanguage
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha=0.2f) else Color.Transparent)
                            .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(16.dp))
                            .clickable { selectedLanguage = lang }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("${lang.third} ${lang.second}", style = MaterialTheme.typography.bodyMedium, fontWeight = if(isSelected) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }
    }
}

@Composable
fun LoginScreen(
    accounts: List<UserAccount>,
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: (String) -> Unit,
    forceManualLogin: Boolean = false
) {
    var selectedAccount by remember { mutableStateOf<UserAccount?>(null) }
    var showManualLogin by remember(forceManualLogin) { mutableStateOf(forceManualLogin) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var twoFactorCode by remember { mutableStateOf("") }
    var showTwoFactor by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            NeonLoadingSpinner(size = 80.dp, color = MaterialTheme.colorScheme.primary)
        } else {
            Card(
                modifier = Modifier.padding(32.dp).fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Filled.Person, contentDescription = "Login", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(16.dp))
                    
                    if (accounts.isEmpty() || showManualLogin) {
                        LanguageSelector()
                        
                        Text(if (showManualLogin) "Login" else "Welcome to Neon Messenger", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(16.dp))
                        
                        if (!showTwoFactor) {
                            OutlinedTextField(
                                value = username,
                                onValueChange = { username = it },
                                label = { Text("Email or Phone Number") },
                                leadingIcon = { 
                                    if (username.startsWith("+")) {
                                        Text(getCountryFlagForPhoneNumber(username), modifier = Modifier.padding(start = 8.dp))
                                    } else {
                                        Icon(Icons.Filled.Person, contentDescription = null)
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(16.dp))
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Password") },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(24.dp))
                            Button(
                                onClick = {
                                    if (username.isNotBlank() && password.isNotBlank()) {
                                        showTwoFactor = true
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = username.isNotBlank() && password.isNotBlank()
                            ) {
                                Text("Login")
                            }
                        } else {
                            OutlinedTextField(
                                value = twoFactorCode,
                                onValueChange = { twoFactorCode = it },
                                label = { Text("6-Digit 2FA Code") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            Spacer(Modifier.height(24.dp))
                            Button(
                                onClick = {
                                    if (twoFactorCode.length >= 4) {
                                        scope.launch {
                                            isLoading = true
                                            delay(1500)
                                            isLoading = false
                                            onLoginSuccess(username)
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = twoFactorCode.isNotBlank()
                            ) {
                                Text("Verify & Login")
                            }
                        }
                        
                        Spacer(Modifier.height(8.dp))
                        if (accounts.isNotEmpty()) {
                            TextButton(onClick = { 
                                showManualLogin = false
                                showTwoFactor = false
                                username = ""
                                password = ""
                                twoFactorCode = ""
                            }) {
                                Text("Select existing account")
                            }
                        }
                        
                        // Separate register button
                        OutlinedButton(onClick = onNavigateToRegister, modifier = Modifier.fillMaxWidth()) {
                            Text("Register")
                        }
                    } else if (selectedAccount == null) {
                        LanguageSelector()
                        
                        Text("Select Account", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(16.dp))
                        
                        accounts.forEach { account ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                onClick = { selectedAccount = account }
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primary, shape = CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(account.displayName.take(1).uppercase(), color = MaterialTheme.colorScheme.onPrimary)
                                    }
                                    Spacer(Modifier.width(16.dp))
                                    Column {
                                        Text(account.displayName, style = MaterialTheme.typography.titleMedium)
                                        Text(account.username, style = MaterialTheme.typography.bodySmall)
                                    }
                                    if (account.is2FAEnabled) {
                                        Spacer(Modifier.weight(1f))
                                        Icon(Icons.Filled.Lock, contentDescription = "2FA Enabled", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                        
                        Spacer(Modifier.height(16.dp))
                        TextButton(onClick = { showManualLogin = true }) {
                            Text("Log in with another account")
                        }
                        OutlinedButton(onClick = onNavigateToRegister, modifier = Modifier.fillMaxWidth()) {
                            Text("Register")
                        }
                    } else {
                        LanguageSelector()
                        
                        Text("Welcome Back, ${selectedAccount!!.displayName}", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(24.dp))
                        
                        if (!showTwoFactor) {
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Password") },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(24.dp))
                            Button(
                                onClick = {
                                    if (password.isNotEmpty()) {
                                        if (selectedAccount!!.is2FAEnabled) {
                                            showTwoFactor = true
                                        } else {
                                            scope.launch {
                                                isLoading = true
                                                delay(1500)
                                                isLoading = false
                                                onLoginSuccess(selectedAccount!!.username)
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = password.isNotBlank()
                            ) {
                                Text("Login")
                            }
                        } else {
                            OutlinedTextField(
                                value = twoFactorCode,
                                onValueChange = { twoFactorCode = it },
                                label = { Text("6-Digit 2FA Code") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            Spacer(Modifier.height(24.dp))
                            Button(
                                onClick = {
                                    if (twoFactorCode.length >= 4) {
                                        scope.launch {
                                            isLoading = true
                                            delay(1500)
                                            isLoading = false
                                            onLoginSuccess(selectedAccount!!.username)
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = twoFactorCode.isNotBlank()
                            ) {
                                Text("Verify & Login")
                            }
                        }
                        
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = { 
                            selectedAccount = null 
                            password = ""
                            twoFactorCode = ""
                            showTwoFactor = false
                        }) {
                            Text("Switch Account")
                        }
                    }
                }
            }
        }
    }
}

enum class RegistrationMethod {
    EMAIL, PHONE
}

@Composable
fun RegistrationScreen(
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: (String) -> Unit
) {
    var regMethod by remember { mutableStateOf(RegistrationMethod.PHONE) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    
    val strength = remember(password) { calculatePasswordStrength(password) }
    val strengthColor = when (strength) {
        0 -> Color.Gray
        1, 2 -> Color(0xFFFF1744)
        3 -> Color(0xFFFFEA00)
        4, 5 -> Color(0xFF00E676)
        else -> Color.Gray
    }
    val animatedProgress by animateFloatAsState(
        targetValue = strength / 5f,
        animationSpec = tween(300)
    )
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            NeonLoadingSpinner(size = 80.dp, color = MaterialTheme.colorScheme.primary)
        } else {
            Card(
                modifier = Modifier.padding(32.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Filled.Person, contentDescription = "Register", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("Create Account", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(16.dp))
                    
                    // Module menu for Email or Phone Registration
                    TabRow(
                        selectedTabIndex = if (regMethod == RegistrationMethod.PHONE) 0 else 1,
                        containerColor = Color.Transparent,
                        divider = {}
                    ) {
                        Tab(
                            selected = regMethod == RegistrationMethod.PHONE,
                            onClick = { 
                                regMethod = RegistrationMethod.PHONE 
                                username = ""
                            },
                            text = { Text("Phone") },
                            icon = { Icon(Icons.Filled.Phone, null) }
                        )
                        Tab(
                            selected = regMethod == RegistrationMethod.EMAIL,
                            onClick = { 
                                regMethod = RegistrationMethod.EMAIL 
                                username = ""
                            },
                            text = { Text("Email") },
                            icon = { Icon(Icons.Filled.Email, null) }
                        )
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text(if (regMethod == RegistrationMethod.PHONE) "Phone Number (e.g. +7900...)" else "Email Address") },
                        leadingIcon = { 
                            if (regMethod == RegistrationMethod.PHONE && username.startsWith("+")) {
                                Text(getCountryFlagForPhoneNumber(username), modifier = Modifier.padding(start = 8.dp))
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = if (regMethod == RegistrationMethod.PHONE) KeyboardType.Phone else KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Password Strength",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = when (strength) {
                                0 -> ""
                                1, 2 -> "Weak"
                                3 -> "Fair"
                                4, 5 -> "Strong"
                                else -> ""
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = strengthColor
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .background(Color.DarkGray, CircleShape)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction = animatedProgress)
                                .height(6.dp)
                                .background(strengthColor, CircleShape)
                                .border(1.dp, strengthColor, CircleShape)
                                .shadow(
                                    elevation = if (strength > 0) 8.dp else 0.dp,
                                    shape = CircleShape,
                                    ambientColor = strengthColor,
                                    spotColor = strengthColor
                                )
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        isError = password.isNotEmpty() && confirmPassword.isNotEmpty() && password != confirmPassword
                    )
                    
                    Spacer(Modifier.height(24.dp))
                    
                    Button(
                        onClick = {
                            scope.launch {
                                isLoading = true
                                delay(1500)
                                isLoading = false
                                onRegisterSuccess(username)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = username.isNotBlank() && password.isNotBlank() && password == confirmPassword
                    ) {
                        Text("Register")
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onNavigateToLogin) {
                        Text("Already have an account? Login")
                    }
                }
            }
        }
    }
}
