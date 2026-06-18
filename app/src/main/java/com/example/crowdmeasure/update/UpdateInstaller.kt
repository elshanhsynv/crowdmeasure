package com.example.crowdmeasure.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.net.toUri

@Singleton
class UpdateInstaller @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun canRequestPackageInstalls(): Boolean =
        context.packageManager.canRequestPackageInstalls()

    fun openUnknownAppSourcesSettings() {
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            "package:${context.packageName}".toUri()
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun install(apkFile: File) {
        if (!canRequestPackageInstalls()) {
            throw InstallPermissionRequiredException()
        }

        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
            .apply { setAppPackageName(context.packageName) }
        val sessionId = installer.createSession(params)

        try {
            installer.openSession(sessionId).use { session ->
                apkFile.inputStream().use { input ->
                    session.openWrite(apkFile.name, 0, apkFile.length()).use { output ->
                        input.copyTo(output)
                        session.fsync(output)
                    }
                }

                val flags = PendingIntent.FLAG_UPDATE_CURRENT or if (
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                ) {
                    PendingIntent.FLAG_MUTABLE
                } else {
                    0
                }

                val callback = PendingIntent.getBroadcast(
                    context,
                    sessionId,
                    Intent(context, UpdateInstallReceiver::class.java)
                        .setPackage(context.packageName),
                    flags
                )
                session.commit(callback.intentSender)
            }
        } catch (throwable: Throwable) {
            runCatching { installer.abandonSession(sessionId) }
            throw throwable
        }
    }
}
