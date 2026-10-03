package com.example.ui.viewers

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.ui.theme.CategoryApks
import com.example.util.FileUtils
import com.example.util.PackageDetails
import com.example.util.PackageFormat
import com.example.util.PackageInstallerHelper
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun PackageInstallerDialog(
    file: File,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var packageDetails by remember { mutableStateOf<PackageDetails?>(null) }
    var isLoadingDetails by remember { mutableStateOf(true) }
    var isInstalling by remember { mutableStateOf(false) }
    var installProgress by remember { mutableFloatStateOf(0f) }
    var installStatusText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var hasInstallPermission by remember { mutableStateOf(PackageInstallerHelper.canRequestPackageInstalls(context)) }

    // Re-check permission when app returns from Settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasInstallPermission = PackageInstallerHelper.canRequestPackageInstalls(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(file.path) {
        isLoadingDetails = true
        val details = PackageInstallerHelper.parsePackageDetails(context, file)
        packageDetails = details
        isLoadingDetails = false
    }

    Dialog(
        onDismissRequest = {
            if (!isInstalling) onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !isInstalling,
            dismissOnClickOutside = !isInstalling
        )
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp)
                .testTag("package_installer_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.install_package),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    IconButton(
                        onClick = onDismiss,
                        enabled = !isInstalling,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.close),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                if (isLoadingDetails) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Reading package information…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    val details = packageDetails

                    // App Icon with Rounded Squircle Background
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(CategoryApks.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (details?.icon != null) {
                            Image(
                                bitmap = details.icon.asImageBitmap(),
                                contentDescription = details.appName,
                                modifier = Modifier
                                    .size(58.dp)
                                    .clip(RoundedCornerShape(16.dp))
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Android,
                                contentDescription = null,
                                tint = CategoryApks,
                                modifier = Modifier.size(42.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // App Name
                    Text(
                        text = details?.appName ?: file.nameWithoutExtension,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Format Badge & Version Tag
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val badgeColor = when (details?.format) {
                            PackageFormat.XAPK -> Color(0xFF8B5CF6)
                            PackageFormat.APKS -> Color(0xFF0284C7)
                            else -> CategoryApks
                        }

                        Surface(
                            shape = CircleShape,
                            color = badgeColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = details?.format?.label ?: file.extension.uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = badgeColor,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }

                        if (!details?.versionName.isNullOrBlank()) {
                            Text(
                                text = "v${details?.versionName}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(16.dp))

                    // Package details card
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (!details?.packageName.isNullOrBlank()) {
                            DetailRow(
                                label = stringResource(R.string.package_name),
                                value = details?.packageName ?: ""
                            )
                        }

                        DetailRow(
                            label = "File size",
                            value = FileUtils.formatFileSize(file.length())
                        )

                        if (details != null && details.splitApkNames.size > 1) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Layers,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Split APKs",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "${details.splitApkNames.size} modules",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        if (details?.hasObb == true) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "OBB Expansion",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = stringResource(R.string.includes_obb),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF10B981)
                                )
                            }
                        }
                    }

                    // Unknown apps permission warning
                    if (!hasInstallPermission) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.permission_install_unknown_title),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = stringResource(R.string.permission_install_unknown_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = { PackageInstallerHelper.openInstallPermissionSettings(context) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(stringResource(R.string.open_settings))
                                }
                            }
                        }
                    }

                    // Installation Progress
                    AnimatedVisibility(visible = isInstalling) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            LinearProgressIndicator(
                                progress = { installProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = installStatusText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Error display
                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = errorMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Bottom Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            enabled = !isInstalling,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.cancel))
                        }

                        Button(
                            onClick = {
                                if (!hasInstallPermission) {
                                    PackageInstallerHelper.openInstallPermissionSettings(context)
                                    return@Button
                                }
                                details?.let { pkg ->
                                    isInstalling = true
                                    errorMessage = null
                                    installStatusText = "Preparing installation…"
                                    scope.launch {
                                        PackageInstallerHelper.installPackage(
                                            context = context,
                                            details = pkg,
                                            onProgress = { step, prog ->
                                                installStatusText = step
                                                installProgress = prog
                                            },
                                            onSuccess = {
                                                isInstalling = false
                                                onDismiss()
                                            },
                                            onError = { err ->
                                                isInstalling = false
                                                errorMessage = err
                                            }
                                        )
                                    }
                                }
                            },
                            enabled = !isInstalling && details != null,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("button_install_package")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(if (isInstalling) R.string.installing else R.string.install))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
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
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}
