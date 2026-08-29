package ee.tallinntastebuds.service

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import ee.tallinntastebuds.R
import ee.tallinntastebuds.model.RadioStation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The map's view of the radio. Everything that actually plays lives in
 * [RadioService]; this is the handle the button holds, and the reason it can go
 * on holding it while the reader moves between tabs.
 */
class RadioPlayer(context: Context) {

    private val app = context.applicationContext

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _failed = MutableStateFlow(false)
    val failed: StateFlow<Boolean> = _failed.asStateFlow()

    private val _station = MutableStateFlow<RadioStation?>(null)
    val station: StateFlow<RadioStation?> = _station.asStateFlow()

    private var connection: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(playing: Boolean) {
            // Buffering is not "off": a live stream spends its first seconds
            // there, and a button that flicked back on every reconnection would
            // read as broken.
            if (playing) {
                _isPlaying.value = true
            } else if (controller?.playbackState != Player.STATE_BUFFERING) {
                _isPlaying.value = false
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            _isPlaying.value = false
            _station.value = null
            _failed.value = true
            controller?.clearMediaItems()
        }
    }

    fun toggle(station: RadioStation?) {
        if (station == null) return
        if (_isPlaying.value && _station.value == station) stop() else play(station)
    }

    /**
     * Follow a language change while playing. `data/radio.json` gives most
     * languages a station of their own, so the station is part of what the
     * language picker chooses — leaving the old one playing would make the choice
     * half-apply. Does nothing when the radio is off, or when both languages
     * share a station and there is nothing to move to.
     */
    fun follow(station: RadioStation?) {
        if (!_isPlaying.value || station == null || station == _station.value) return
        play(station)
    }

    fun play(station: RadioStation) {
        _failed.value = false
        // Set eagerly rather than on the player's callback: the reader pressed a
        // button and the button has to answer now, not when the stream connects.
        _station.value = station
        _isPlaying.value = true
        withController { controller ->
            controller.setMediaItem(mediaItem(station))
            controller.prepare()
            controller.play()
        }
    }

    fun stop() {
        _isPlaying.value = false
        _station.value = null
        withController { controller ->
            controller.stop()
            controller.clearMediaItems()
        }
    }

    /** Acknowledge a failure once it has been shown, so the next tap starts
     *  clean and the message does not reappear on the next recomposition. */
    fun clearFailure() {
        _failed.value = false
    }

    private fun mediaItem(station: RadioStation) = MediaItem.Builder()
        .setUri(station.url)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(station.name)
                .setArtist(app.getString(R.string.app_name))
                .setIsBrowsable(false)
                .setIsPlayable(true)
                .build()
        )
        .build()

    /**
     * Connecting to the service is asynchronous and happens once. Until it has,
     * actions queue behind the connection rather than being dropped, so the very
     * first tap on the button plays rather than doing nothing.
     */
    private fun withController(action: (MediaController) -> Unit) {
        val connected = controller
        if (connected != null) {
            action(connected)
            return
        }

        val future = connection ?: MediaController.Builder(
            app,
            SessionToken(app, ComponentName(app, RadioService::class.java)),
        ).buildAsync().also { connection = it }

        future.addListener({
            val session = runCatching { future.get() }.getOrNull()
            if (session == null) {
                _isPlaying.value = false
                _failed.value = true
                connection = null
                return@addListener
            }
            if (controller == null) {
                controller = session
                session.addListener(playerListener)
            }
            action(session)
        }, ContextCompat.getMainExecutor(app))
    }
}
