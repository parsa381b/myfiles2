package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.PhoneInfo
import com.example.util.DeviceInfoHelper
import com.example.util.FileUtils

@Composable
fun PhoneInfoScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onNavigateBack() }
    val context = LocalContext.current

    var phoneInfo by remember { mutableStateOf<PhoneInfo?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }

    fun refreshInfo() {
        phoneInfo = DeviceInfoHelper.getPhoneInfo(context)
    }

    LaunchedEffect(Unit) {
        refreshInfo()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("phone_info_screen")
    ) {
        // Top App Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("phone_info_button_back")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.close)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Phone Info",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = { refreshInfo() },
                    modifier = Modifier.testTag("phone_info_button_refresh")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.retry)
                    )
                }
            }
        }

        HorizontalDivider()

        val info = phoneInfo

        if (info == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(36.dp))
            }
            return
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Card
            item {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF3B82F6).copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhoneAndroid,
                                    contentDescription = null,
                                    tint = Color(0xFF3B82F6),
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${info.brand} ${info.model}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = info.deviceName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            InfoBadge(text = "Android ${info.androidVersion}", color = Color(0xFF10B981))
                            InfoBadge(text = if (info.is64Bit) "64-bit" else "32-bit", color = Color(0xFF8B5CF6))
                            InfoBadge(text = "${info.densityDpi} DPI", color = Color(0xFFF59E0B))
                        }
                    }
                }
            }

            // Processor & Architecture Card (arm7 or arm8, etc.)
            item {
                InfoSectionCard(
                    title = "Processor & Architecture",
                    icon = Icons.Default.Memory,
                    accentColor = Color(0xFF8B5CF6)
                ) {
                    InfoRow(label = "CPU Architecture", value = info.primaryArchitecture, highlight = true)
                    InfoRow(label = "Instruction Set", value = if (info.is64Bit) "64-bit Architecture" else "32-bit Architecture")
                    InfoRow(label = "CPU Cores", value = "${info.cpuCores} Cores")
                    InfoRow(label = "Supported ABIs", value = info.supportedAbis.joinToString(", "))
                }
            }

            // Battery Health & Power Card
            item {
                InfoSectionCard(
                    title = "Battery Status & Health",
                    icon = Icons.Default.BatteryChargingFull,
                    accentColor = Color(0xFF10B981)
                ) {
                    InfoRow(label = "Battery Health", value = info.batteryHealth, highlight = true)
                    InfoRow(label = "Battery Level", value = "${info.batteryLevel}%")
                    InfoRow(label = "Status", value = info.batteryStatus)
                    InfoRow(label = "Power Source", value = info.batteryPlugged)
                    InfoRow(label = "Technology", value = info.batteryTech)
                    InfoRow(label = "Temperature", value = "${String.format("%.1f", info.batteryTempCelsius)} °C")
                    if (info.batteryVoltageMv > 0) {
                        InfoRow(label = "Voltage", value = "${info.batteryVoltageMv} mV (${String.format("%.2f", info.batteryVoltageMv / 1000f)} V)")
                    }
                }
            }

            // Display & Screen Card (DPI, resolution, refresh rate)
            item {
                InfoSectionCard(
                    title = "Display & Screen",
                    icon = Icons.Default.Smartphone,
                    accentColor = Color(0xFFF59E0B)
                ) {
                    InfoRow(label = "Screen Density", value = "${info.densityDpi} DPI (${info.densityBucket})", highlight = true)
                    InfoRow(label = "Resolution", value = info.resolution)
                    InfoRow(label = "Refresh Rate", value = "${info.refreshRate.toInt()} Hz")
                }
            }

            // Android OS & Software Card
            item {
                InfoSectionCard(
                    title = "Android & OS System",
                    icon = Icons.Default.Android,
                    accentColor = Color(0xFF3B82F6)
                ) {
                    InfoRow(label = "Android Version", value = "Android ${info.androidVersion}", highlight = true)
                    InfoRow(label = "Code Name", value = info.androidCodeName)
                    InfoRow(label = "API Level (SDK)", value = info.apiLevel.toString())
                    InfoRow(label = "Security Patch", value = info.securityPatch)
                    InfoRow(label = "Build ID", value = info.buildId)
                }
            }

            // Device Hardware & RAM Card
            item {
                InfoSectionCard(
                    title = "Device Hardware & Memory",
                    icon = Icons.Default.Speed,
                    accentColor = Color(0xFFEC4899)
                ) {
                    InfoRow(label = "Brand", value = info.brand)
                    InfoRow(label = "Manufacturer", value = info.manufacturer)
                    InfoRow(label = "Model", value = info.model)
                    InfoRow(label = "Board / Hardware", value = "${info.board} / ${info.hardware}")
                    if (info.totalRamBytes > 0) {
                        val usedRam = info.totalRamBytes - info.freeRamBytes
                        val pct = ((usedRam.toDouble() / info.totalRamBytes) * 100).toInt()
                        InfoRow(
                            label = "RAM (Used / Total)",
                            value = "${FileUtils.formatFileSize(usedRam)} / ${FileUtils.formatFileSize(info.totalRamBytes)} ($pct%)"
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { (usedRam.toFloat() / info.totalRamBytes).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = Color(0xFFEC4899)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun InfoBadge(text: String, color: Color) {
    Surface(
        shape = CircleShape,
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun InfoSectionCard(
    title: String,
    icon: ImageVector,
    accentColor: Color,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            content()
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    highlight: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Medium,
            color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}
