package com.castivio.data.networking

import okhttp3.Call
import okhttp3.Callback
import okhttp3.EventListener
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

/**
 * Opens the connection to the posters' host when a section of films or shows begins to load, so that it is open when the
 * wall's first cards ask for their pictures.
 *
 * Measured at 20:23, a films load without a scroll: every card of the first screen waited 2.55 to 2.66 s for its poster, all
 * of them the same, and each call waited 1.97 s (mean) before it had a connection. They were waiting for one thing: the one
 * HTTP/2 connection to TMDB, opened while the film list's reply of 78.5 MB was already filling the line, so that its handshake
 * took two seconds. The provider takes five to eight seconds to begin that reply; a handshake made at the press is made while
 * the line is still quiet.
 *
 * One `HEAD` of the host's root, through the application's client: its connection pool is the one the image loader's client
 * is derived from (`ArtworkCalls.deriveClient` changes the dispatcher, the listener and a timeout, none of which is part of
 * what makes two connections the same), so the connection it leaves in the pool is the one the posters then use. Redirects are
 * not followed -- the answer does not matter, only the connection -- and no listener is told, so the provider's counts and the
 * posters' are not touched by it. At most once a [MIN_INTERVAL_MS]: a connection is kept for minutes, and a second load soon
 * after finds it there.
 *
 * Fire and forget. A failure is a connection the posters will open for themselves, as they always did.
 */
class PictureHostWarmUp internal constructor(
    private val clock: () -> Long,
    private val send: () -> Unit,
) {

    constructor(client: OkHttpClient) : this(
        clock = System::currentTimeMillis,
        send = sender(client),
    )

    private val lock = Any()
    private var lastAt = Long.MIN_VALUE

    /** Opens the posters' host's connection, unless it was asked for in the last [MIN_INTERVAL_MS]. Returns whether it asked. */
    fun warm(): Boolean {
        synchronized(lock) {
            val now = clock()
            if (lastAt != Long.MIN_VALUE && now - lastAt in 0 until MIN_INTERVAL_MS) return false
            lastAt = now
        }
        runCatching(send)
        return true
    }

    companion object {
        /** The posters' host: where a provider's film and show posters are, as the strip has counted them on every load. */
        const val HOST_URL = "https://image.tmdb.org/"

        /** Well inside the minutes OkHttp keeps an idle connection. */
        const val MIN_INTERVAL_MS = 60_000L

        private fun sender(client: OkHttpClient): () -> Unit {
            val quiet = client.newBuilder()
                .followRedirects(false)
                .followSslRedirects(false)
                .eventListener(EventListener.NONE)
                .build()
            return {
                quiet.newCall(Request.Builder().url(HOST_URL).head().build()).enqueue(
                    object : Callback {
                        override fun onResponse(call: Call, response: Response) = response.close()
                        override fun onFailure(call: Call, e: IOException) = Unit
                    },
                )
            }
        }
    }
}
