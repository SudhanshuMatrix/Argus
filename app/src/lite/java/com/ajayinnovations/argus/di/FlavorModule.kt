package com.ajayinnovations.argus.di

import com.ajayinnovations.argus.ai.BundledModelSource
import com.ajayinnovations.argus.ai.ModelSource
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/** This flavor carries its model inside the APK. */
val flavorModule = module {
    single<ModelSource> { BundledModelSource(androidContext()) }
}
