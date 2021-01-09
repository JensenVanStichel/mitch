package ua.gardenapple.itchupdater.installer

import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageInstaller
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import ua.gardenapple.itchupdater.NOTIFICATION_CHANNEL_ID_INSTALLING
import ua.gardenapple.itchupdater.NOTIFICATION_TAG_INSTALL_RESULT
import ua.gardenapple.itchupdater.R
import ua.gardenapple.itchupdater.Utils

/**
 * This handler is invoked when an .apk installation is complete, providing a notification.
 * This should *NOT* depend on the AppDatabase as this could be used for the GitLab build update
 * check or other things
 */
class InstallerNotificationHandler(val context: Context) : InstallResultListener {
    override suspend fun onInstallResult(installSessionId: Int, packageName: String, apkName: String?, status: Int) {
        val message = when (status) {
            PackageInstaller.STATUS_FAILURE_ABORTED -> context.resources.getString(R.string.notification_install_cancelled_title)
            PackageInstaller.STATUS_FAILURE_BLOCKED -> context.resources.getString(R.string.notification_install_blocked_title)
            PackageInstaller.STATUS_FAILURE_CONFLICT -> context.resources.getString(R.string.notification_install_conflict_title)
            PackageInstaller.STATUS_FAILURE_INCOMPATIBLE -> context.resources.getString(R.string.notification_install_incompatible_title)
            PackageInstaller.STATUS_FAILURE_INVALID -> context.resources.getString(R.string.notification_install_invalid_title)
            PackageInstaller.STATUS_FAILURE_STORAGE -> context.resources.getString(R.string.notification_install_storage_title)
            PackageInstaller.STATUS_SUCCESS -> context.resources.getString(R.string.notification_install_complete_title)
            else -> context.resources.getString(R.string.notification_install_complete_title)
        }
        val builder =
            NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID_INSTALLING).apply {
                setSmallIcon(R.drawable.ic_mitch_notification)

                if (status == PackageInstaller.STATUS_SUCCESS) {
                    val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
                    setContentTitle(context.packageManager.getApplicationLabel(appInfo))
                } else if (apkName != null)
                    setContentTitle(apkName)
                else
                    setContentTitle(packageName)

                setContentText(message)
//                priority = NotificationCompat.PRIORITY_HIGH
                if (status == PackageInstaller.STATUS_SUCCESS) {
                    val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
                    if (launchIntent != null) {
                        val pendingIntent = PendingIntent.getActivity(context, 0, launchIntent, 0)
                        setContentIntent(pendingIntent)
                        setAutoCancel(true)
                    }

                    val icon = context.packageManager.getApplicationIcon(packageName)
                    setLargeIcon(Utils.drawableToBitmap(icon))
                } else {
                    setAutoCancel(true)
                }
            }

        with(NotificationManagerCompat.from(context)) {
            notify(NOTIFICATION_TAG_INSTALL_RESULT, installSessionId, builder.build())
        }

    }
}