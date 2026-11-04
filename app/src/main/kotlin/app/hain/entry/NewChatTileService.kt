package app.hain.entry

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import app.hain.R

/**
 * Quick settings tile "Neuer Chat" (T-144): one tap opens Hain with a new chat and the keyboard.
 * On a locked phone it asks to unlock first, because chats are private.
 */
class NewChatTileService : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            state = Tile.STATE_INACTIVE
            label = getString(R.string.tile_new_chat)
            contentDescription = getString(R.string.tile_new_chat)
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        if (isLocked) unlockAndRun { open() } else open()
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun open() {
        val intent = LaunchIntents.newChat(this, focus = true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pending =
                PendingIntent.getActivity(
                    this,
                    REQUEST_NEW_CHAT,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
            startActivityAndCollapse(pending)
        } else {
            @Suppress("DEPRECATION") // the PendingIntent version only exists from Android 14
            startActivityAndCollapse(intent)
        }
    }

    private companion object {
        const val REQUEST_NEW_CHAT = 144
    }
}
