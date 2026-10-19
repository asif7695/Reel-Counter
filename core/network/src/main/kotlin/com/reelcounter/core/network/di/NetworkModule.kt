package com.reelcounter.core.network.di

import com.reelcounter.core.network.BuildConfig
import com.reelcounter.core.network.SupabaseConfig
import com.reelcounter.core.network.activity.ActivityRemoteDataSource
import com.reelcounter.core.network.activity.SupabaseActivityRemoteDataSource
import com.reelcounter.core.network.auth.AuthRemoteDataSource
import com.reelcounter.core.network.auth.SupabaseAuthRemoteDataSource
import com.reelcounter.core.network.profile.ProfileRemoteDataSource
import com.reelcounter.core.network.profile.SupabaseProfileRemoteDataSource
import com.reelcounter.core.network.session.EncryptedAuthStorage
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class NetworkModule {

    @Binds
    abstract fun bindAuthRemoteDataSource(impl: SupabaseAuthRemoteDataSource): AuthRemoteDataSource

    @Binds
    abstract fun bindProfileRemoteDataSource(impl: SupabaseProfileRemoteDataSource): ProfileRemoteDataSource

    @Binds
    abstract fun bindActivityRemoteDataSource(impl: SupabaseActivityRemoteDataSource): ActivityRemoteDataSource

    companion object {
        @Provides
        @Singleton
        fun provideSupabaseConfig(): SupabaseConfig = SupabaseConfig(
            url = BuildConfig.SUPABASE_URL,
            publishableKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY,
        )

        @Provides
        @Singleton
        fun provideSupabaseClient(config: SupabaseConfig, storage: EncryptedAuthStorage): SupabaseClient {
            check(config.isConfigured) { "Supabase is not configured; set it in secrets.properties." }
            return createSupabaseClient(supabaseUrl = config.url, supabaseKey = config.publishableKey) {
                install(Auth) {
                    // Email codes don't need it, but PKCE is the safe default for any redirect flow added later.
                    flowType = FlowType.PKCE
                    sessionManager = storage
                    codeVerifierCache = storage
                    autoLoadFromStorage = true
                    autoSaveToStorage = true
                    alwaysAutoRefresh = true
                }
                install(Postgrest)
                install(Storage)
            }
        }
    }
}
