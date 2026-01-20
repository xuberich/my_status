package top.dwill.phonestatus

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = context.getSharedPreferences("config", Context.MODE_PRIVATE)
            val autoStart = prefs.getBoolean("auto_start", true)
            
            if (autoStart) {
                val serviceIntent = Intent(context, StatusService::class.java)
                ContextCompat.startForegroundService(context, serviceIntent)
            }
        }
    }
}
