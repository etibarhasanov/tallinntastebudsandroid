package ee.tallinntastebuds.content

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Fetches the website's JSON, with three layers behind it so the app always has
 * something to draw:
 *
 *  1. the network, revalidated with the stored ETag so an unchanged file costs a
 *     304 and no body;
 *  2. the disk copy of whatever was fetched last, in the app's files directory;
 *  3. the seed copy bundled with the app at build time.
 *
 * Layers 2 and 3 mean the app opens instantly and offline; layer 1 means an edit
 * on the website reaches the reader on the next refresh.
 */
class ContentClient(
    context: Context,
    private val prefs: SharedPreferences,
    private val http: OkHttpClient = defaultClient(),
) {
    private val assets = context.applicationContext.assets
    private val cacheDirectory = File(context.applicationContext.filesDir, "content")

    private val json = Json {
        // The site adds keys the app has no use for — deals.json carries the
        // secret its codes come from — and a new one must never stop the app
        // decoding the rest of the file.
        ignoreUnknownKeys = true
        isLenient = true
    }

    init {
        cacheDirectory.mkdirs()
    }

    /**
     * The best copy available right now without touching the network. Used for
     * the first frame, so nothing ever renders empty.
     */
    fun <T> cached(strategy: DeserializationStrategy<T>, document: ContentSource.Document): T? {
        val file = File(cacheDirectory, "${document.fileName}.json")
        if (file.exists()) {
            runCatching { json.decodeFromString(strategy, file.readText()) }
                .getOrNull()
                ?.let { return it }
        }
        return seed(strategy, document)
    }

    /** The copy compiled into the app's assets. */
    fun <T> seed(strategy: DeserializationStrategy<T>, document: ContentSource.Document): T? =
        runCatching {
            assets.open(document.seedAsset).bufferedReader().use { it.readText() }
        }.mapCatching { json.decodeFromString(strategy, it) }.getOrNull()

    /**
     * Fetch [document] from the site, revalidating against the stored ETag.
     * Returns null when the server says "not modified" — the caller already has
     * the current content and does not need to redraw.
     */
    suspend fun <T> fetch(
        strategy: DeserializationStrategy<T>,
        document: ContentSource.Document,
    ): T? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(ContentSource.url(document))
            // OkHttp's own cache is off; the ETag below is the revalidation, and
            // two caches disagreeing about the same file is a bug waiting to be
            // filed.
            .cacheControl(CacheControl.FORCE_NETWORK)
            .apply { etag(document)?.let { header("If-None-Match", it) } }
            .build()

        http.newCall(request).execute().use { response ->
            if (response.code == 304) return@withContext null
            if (!response.isSuccessful) throw IOException("the server answered ${response.code}")

            val body = response.body?.string() ?: throw IOException("the server sent no body")
            val value = json.decodeFromString(strategy, body)
            // Only persist once it has decoded: a half-written file the app
            // cannot read is worse than the older one it would replace.
            write(body, document)
            response.header("ETag")?.let { setEtag(it, document) }
            value
        }
    }

    // region Disk

    private fun write(body: String, document: ContentSource.Document) {
        runCatching {
            val target = File(cacheDirectory, "${document.fileName}.json")
            val scratch = File(cacheDirectory, "${document.fileName}.json.tmp")
            scratch.writeText(body)
            if (!scratch.renameTo(target)) {
                target.writeText(body)
                scratch.delete()
            }
        }
    }

    private fun etag(document: ContentSource.Document): String? =
        prefs.getString("ttb.etag.${document.fileName}", null)

    private fun setEtag(tag: String, document: ContentSource.Document) {
        prefs.edit().putString("ttb.etag.${document.fileName}", tag).apply()
    }

    // endregion

    companion object {
        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}
