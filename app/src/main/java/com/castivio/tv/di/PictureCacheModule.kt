package com.castivio.tv.di

import android.content.Context
import coil.imageLoader
import com.castivio.domain.PictureCache
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Singleton

/**
 * The pictures' cache as Settings sees it: the image loader's, which `CastivioApp.newImageLoader` builds with a directory
 * of its own. `:app` binds it because only `:app` holds the image loader.
 */
@Module
@InstallIn(SingletonComponent::class)
object PictureCacheModule {

    @Provides
    @Singleton
    fun pictureCache(@ApplicationContext context: Context): PictureCache = ImageLoaderCache(context)
}

/** Its size and its emptying, off the main thread: both walk the cache's directory. */
private class ImageLoaderCache(private val context: Context) : PictureCache {

    override suspend fun sizeBytes(): Long = withContext(Dispatchers.IO) {
        context.imageLoader.diskCache?.size ?: 0L
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        val loader = context.imageLoader
        loader.memoryCache?.clear()
        loader.diskCache?.clear()
        Unit
    }
}
