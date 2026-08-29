package ee.tallinntastebuds.service

import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import ee.tallinntastebuds.R

/**
 * The rail's radio button, natively: an audio stream from `data/radio.json` that
 * keeps playing while the phone is locked, which is the part a web page cannot
 * do.
 *
 * A media session rather than a bare player, so the stream gets the lock screen
 * controls, the headphone buttons and the audio focus that any other music app
 * on the phone would get.
 */
@UnstableApi
class RadioService : MediaSessionService() {

    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                // Duck for a navigation prompt, stop for a phone call. Anything
                // else would be a radio that talks over the directions.
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            // A live stream is nothing but network, and without this the radio
            // goes quiet the moment the phone decides the screen has been off
            // long enough.
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        session = MediaSession.Builder(this, player).build()

        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this)
                .setChannelId(CHANNEL_ID)
                .setChannelName(R.string.radio_channel_name)
                .build()
        )
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    /**
     * Swiping the app away with the radio off should not leave a service behind.
     * Swiping it away with the radio on leaves it playing, which is what every
     * other player on the phone does and what the notification's stop button is
     * for.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = session?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        session?.run {
            player.release()
            release()
        }
        session = null
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "ttb.radio"
    }
}
