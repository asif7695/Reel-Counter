package com.reelcounter.core.data.di

import com.reelcounter.core.data.auth.DataStorePasswordResetFlag
import com.reelcounter.core.data.auth.DefaultAuthRepository
import com.reelcounter.core.data.auth.PasswordResetFlag
import com.reelcounter.core.data.profile.AndroidAvatarImageProcessor
import com.reelcounter.core.data.profile.DefaultProfileRepository
import com.reelcounter.core.domain.auth.AuthRepository
import com.reelcounter.core.domain.profile.AvatarImageProcessor
import com.reelcounter.core.domain.profile.ProfileRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataModule {

    @Binds
    abstract fun bindAuthRepository(impl: DefaultAuthRepository): AuthRepository

    @Binds
    abstract fun bindProfileRepository(impl: DefaultProfileRepository): ProfileRepository

    @Binds
    abstract fun bindAvatarImageProcessor(impl: AndroidAvatarImageProcessor): AvatarImageProcessor

    @Binds
    abstract fun bindPasswordResetFlag(impl: DataStorePasswordResetFlag): PasswordResetFlag
}
