package top.wsdx233.r2droid.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import top.wsdx233.r2droid.core.data.source.R2DataSource
import top.wsdx233.r2droid.core.data.source.R2PipeDataSource
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindR2DataSource(
        r2PipeDataSource: R2PipeDataSource
    ): R2DataSource
}
