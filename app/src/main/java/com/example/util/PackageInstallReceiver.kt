package com.example.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.widget.Toast
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed interface InstallResult {
    data class Success(val sessionId: Int) : InstallResult
    data class Failed(val sessionId: Int, val message: String) : InstallResult
    data class UserActionNeeded(val sessionId: Int, val intent: Intent) : InstallResult
}

class PackageInstallReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_INSTALL_STATUS = "com.example.ACTION_PACKAGE_INSTALL_STATUS"

        private val _installEvents = MutableSharedFlow<InstallResult>(extraBufferCapacity = 10)
        val installEvents = _installEvents.asSharedFlow()
    }

    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        val sessionId = intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1)
        val statusMessage = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: "Installation failed"

        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirmIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_INTENT)
                }

                if (confirmIntent != null) {
                    confirmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    try {
                        context.startActivity(confirmIntent)
                        _installEvents.tryEmit(InstallResult.UserActionNeeded(sessionId, confirmIntent))
                    } catch (e: Exception) {
                        Toast.makeText(context, "Failed to launch installer: ${e.message}", Toast.LENGTH_SHORT).show()
                        _installEvents.tryEmit(InstallResult.Failed(sessionId, e.message ?: "Failed to start installer"))
                    }
                }
            }

            PackageInstaller.STATUS_SUCCESS -> {
                Toast.makeText(context, "App installed successfully", Toast.LENGTH_SHORT).show()
                _installEvents.tryEmit(InstallResult.Success(sessionId))
            }

            PackageInstaller.STATUS_FAILURE_ABORTED -> {
                Toast.makeText(context, "Installation cancelled", Toast.LENGTH_SHORT).show()
                _installEvents.tryEmit(InstallResult.Failed(sessionId, "Installation cancelled"))
            }

            else -> {
                Toast.makeText(context, "Installation failed: $statusMessage", Toast.LENGTH_LONG).show()
                _installEvents.tryEmit(InstallResult.Failed(sessionId, statusMessage))
            }
        }
    }
}
