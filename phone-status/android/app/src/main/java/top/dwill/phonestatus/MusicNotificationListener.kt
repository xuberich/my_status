package top.dwill.phonestatus

import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class MusicNotificationListener : NotificationListenerService() {

    private var mediaSessionManager: MediaSessionManager? = null
    private val sessionListener = object : MediaSessionManager.OnActiveSessionsChangedListener {
        override fun onActiveSessionsChanged(controllers: MutableList<MediaController>?) {
            updateMusicInfo(controllers)
        }
    }

    override fun onCreate() {
        super.onCreate()
        try {
            mediaSessionManager = getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
            mediaSessionManager?.addOnActiveSessionsChangedListener(
                sessionListener,
                android.content.ComponentName(this, MusicNotificationListener::class.java)
            )
            // 初始获取
            val controllers = mediaSessionManager?.getActiveSessions(
                android.content.ComponentName(this, MusicNotificationListener::class.java)
            )
            updateMusicInfo(controllers)
        } catch (e: Exception) {
            Log.e("MusicListener", "Failed to init", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            mediaSessionManager?.removeOnActiveSessionsChangedListener(sessionListener)
        } catch (e: Exception) {
            Log.e("MusicListener", "Failed to cleanup", e)
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        // 也可以从通知中获取音乐信息
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {}

    private fun updateMusicInfo(controllers: List<MediaController>?) {
        if (controllers.isNullOrEmpty()) {
            StatusService.currentMusic = null
            StatusService.currentMusicArtist = null
            return
        }

        // 找到正在播放的
        val playingController = controllers.find { controller ->
            controller.playbackState?.state == PlaybackState.STATE_PLAYING
        }

        if (playingController != null) {
            val metadata = playingController.metadata
            StatusService.currentMusic = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
            StatusService.currentMusicArtist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            Log.d("MusicListener", "Now playing: ${StatusService.currentMusic} - ${StatusService.currentMusicArtist}")
        } else {
            StatusService.currentMusic = null
            StatusService.currentMusicArtist = null
        }
    }
}
