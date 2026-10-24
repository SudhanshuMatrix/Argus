package com.ajayinnovations.argus.di

import com.ajayinnovations.argus.ai.DevModelSource
import com.ajayinnovations.argus.ai.ModelSource
import com.ajayinnovations.argus.debug.DebugViewModel
import com.ajayinnovations.argus.debug.SampleRunner
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** dev: reads the adb-pushed model and adds the debug screen. */
val flavorModule = module {
    single<ModelSource> { DevModelSource(androidContext()) }
    single { SampleRunner(androidContext(), get(), get()) }
    viewModel { DebugViewModel(androidContext(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
}
