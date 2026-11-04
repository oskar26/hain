package app.hain

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import app.hain.tools.api.GeoPoint

/**
 * 3b-2, "Standort fürs Wetter": the phone's last known rough location. No Play Services and no new
 * GPS request: the newest fix any enabled provider still has. Only used when the user switched it on
 * and Android allowed it.
 */
internal object WeatherLocation {
    fun allowed(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission") // checked in allowed()
    fun last(context: Context): GeoPoint? {
        val manager = context.getSystemService(LocationManager::class.java)
        if (!allowed(context) || manager == null) return null
        val newest =
            runCatching {
                manager.getProviders(true).mapNotNull { manager.getLastKnownLocation(it) }.maxByOrNull { it.time }
            }.getOrNull()
        return newest?.let { GeoPoint(it.latitude, it.longitude) }
    }
}
