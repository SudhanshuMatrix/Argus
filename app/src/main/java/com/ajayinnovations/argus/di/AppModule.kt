package com.ajayinnovations.argus.di

import com.ajayinnovations.argus.ai.GemmaEngine
import com.ajayinnovations.argus.alert.AlertNotifier
import com.ajayinnovations.argus.alert.FamilyNotifier
import com.ajayinnovations.argus.alert.PhoneSound
import com.ajayinnovations.argus.alert.VerdictStore
import com.ajayinnovations.argus.alert.VoicePlayer
import com.ajayinnovations.argus.alert.WarningViewModel
import com.ajayinnovations.argus.ai.LlmEngine
import com.ajayinnovations.argus.ai.ModelCache
import com.ajayinnovations.argus.core.Alert
import com.ajayinnovations.argus.core.CarefulVoiceLimit
import com.ajayinnovations.argus.core.ChatCursor
import com.ajayinnovations.argus.core.CheckQueue
import com.ajayinnovations.argus.core.Deduper
import com.ajayinnovations.argus.core.FamilySmsTracker
import com.ajayinnovations.argus.core.ScamChecker
import com.ajayinnovations.argus.core.Warmup
import com.ajayinnovations.argus.ai.ModelSource
import com.ajayinnovations.argus.ingest.IngestHealth
import com.ajayinnovations.argus.ingest.CheckScope
import com.ajayinnovations.argus.device.DeviceInfo
import com.ajayinnovations.argus.settings.AppLanguage
import com.ajayinnovations.argus.settings.Settings
import com.ajayinnovations.argus.ui.check.CheckViewModel
import com.ajayinnovations.argus.ui.check.LastClip
import com.ajayinnovations.argus.ui.home.HomeViewModel
import com.ajayinnovations.argus.ui.settings.SettingsViewModel
import com.ajayinnovations.argus.ui.setup.SetupChecks
import com.ajayinnovations.argus.ui.setup.SetupViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { Settings(androidContext()) }
    single { AppLanguage(androidContext(), get()) }
    single { ModelCache(androidContext()) }
    single { DeviceInfo(androidContext(), get(), get()) }
    single<LlmEngine> { GemmaEngine(androidContext(), get(), get(), get()) }
    single { ScamChecker(get<LlmEngine>()) }
    single { Deduper() }
    single { ChatCursor() }
    single { IngestHealth(androidContext()) }
    single { VerdictStore() }
    single { PhoneSound(androidContext()) }
    single { VoicePlayer(androidContext(), get(), get()) }
    single { FamilySmsTracker() }
    single { FamilyNotifier(androidContext(), get(), get()) }
    single { CarefulVoiceLimit() }
    single { AlertNotifier(androidContext(), get(), get(), get(), get(), get(), get()) }
    single { LastClip() }
    single {
        val settings = get<Settings>()
        val models = get<ModelSource>()
        val engine = get<LlmEngine>()
        Warmup(
            scope = CheckScope.scope,
            isReady = { settings.setupComplete && models.readyFile() != null },
            isLoaded = { engine.state.value.loaded },
            model = engine,
        )
    }
    single {
        val settings = get<Settings>()
        val alerts = get<AlertNotifier>()
        CheckQueue(
            checker = get(),
            deduper = get(),
            // Background checks never trigger the model copy: they skip until setup has finished.
            // After that, a missing or broken model still leaves the hard rules and the safety
            // floor working (the engine throws, ScamChecker falls back); the home screen says so.
            isReady = { settings.setupComplete },
            language = { settings.language },
            // Level, decided-by and seconds are counted in CheckQueue.stats for the debug screen
            onChecked = { message, result, _ -> alerts.show(Alert.from(message, result)) },
            clock = android.os.SystemClock::elapsedRealtime,
            scope = CheckScope.scope,
            onError = { name -> android.util.Log.w("CheckQueue", "Check failed ($name)") },
        )
    }
    single { SetupChecks(androidContext(), get(), get(), get(), get()) }
    viewModel { HomeViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { SetupViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { SettingsViewModel(get(), get()) }
    viewModel { CheckViewModel(get(), get(), get(), get()) }
    viewModel { WarningViewModel(get(), get(), get(), get()) }
}
