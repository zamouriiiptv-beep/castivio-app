package com.castivio.domain

/**
 * The posters, logos and stills kept on this device so they are not fetched again (Settings → Application).
 *
 * What Settings says of it is its size; what it does with it is empty it -- on a television box with little storage a
 * viewer frees space, and on any device a picture the provider has replaced is fetched again. Emptying it costs nothing
 * but the pictures being downloaded once more as they are next shown. Behind an interface because the cache is the image
 * loader's, which is the platform's.
 */
interface PictureCache {

    /** The bytes the pictures take on this device's storage. */
    suspend fun sizeBytes(): Long

    /** Empties it, on storage and in memory. */
    suspend fun clear()
}
