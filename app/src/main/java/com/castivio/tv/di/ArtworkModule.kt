package com.castivio.tv.di

import com.castivio.core.design.components.ArtworkPolicy
import com.castivio.data.networking.ArtworkConcurrency
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * The image loader's limits, as one object for the process.
 *
 * The loader that owns the pictures' dispatcher and the importer that knows when a section of films begins and ends are built
 * apart, in different modules, and share only this: the loader gives it its dispatcher ([com.castivio.tv.CastivioApp]), and the
 * importer takes a hold on it while the films arrive. It is provided here because the numbers are [ArtworkPolicy]'s, which
 * the networking and playlist modules cannot see, and because stating them twice is how two copies of a limit part.
 */
@Module
@InstallIn(SingletonComponent::class)
object ArtworkModule {

    @Provides
    @Singleton
    fun artworkConcurrency(): ArtworkConcurrency = ArtworkConcurrency(
        normal = ArtworkConcurrency.Limits(
            maxRequests = ArtworkPolicy.MAX_REQUESTS,
            maxRequestsPerHost = ArtworkPolicy.MAX_REQUESTS_PER_HOST,
        ),
        whileFilmsArrive = ArtworkConcurrency.Limits(
            maxRequests = ArtworkPolicy.MAX_REQUESTS_WHILE_FILMS_ARRIVE,
            maxRequestsPerHost = ArtworkPolicy.MAX_REQUESTS_PER_HOST_WHILE_FILMS_ARRIVE,
        ),
    )
}
