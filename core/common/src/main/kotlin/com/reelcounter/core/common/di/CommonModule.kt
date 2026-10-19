package com.reelcounter.core.common.di

import com.reelcounter.core.common.dispatchers.Dispatcher
import com.reelcounter.core.common.dispatchers.RcDispatchers
import com.reelcounter.core.common.network.ConnectivityManagerNetworkMonitor
import com.reelcounter.core.common.network.NetworkMonitor
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@Module
@InstallIn(SingletonComponent::class)
internal abstract class CommonModule {

    @Binds
    abstract fun bindNetworkMonitor(impl: ConnectivityManagerNetworkMonitor): NetworkMonitor

    companion object {
        @Provides
        @Dispatcher(RcDispatchers.IO)
        fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

        @Provides
        @Dispatcher(RcDispatchers.Default)
        fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
    }
}
