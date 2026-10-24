package com.ajayinnovations.argus.debug

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.ajayinnovations.argus.alert.ReasonText
import com.ajayinnovations.argus.core.CheckMode
import com.ajayinnovations.argus.core.ScamChecker
import com.ajayinnovations.argus.settings.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * MAIN: the samples rules and prompts may be tuned against. HOLDOUT: never tune against these,
 * only report them (samples/holdout/{scam,normal}).
 */
/** HOLDOUT2: a second unseen set, written separately (samples/holdout2). Report only, like HOLDOUT. */
enum class SampleSet(val root: String) { MAIN("samples"), HOLDOUT("samples/holdout"), HOLDOUT2("samples/holdout2") }

/** dev only: runs the samples shipped as assets through the real ScamChecker. */
class SampleRunner(
    private val context: Context,
    private val checker: ScamChecker,
    private val settings: Settings,
) {
    private data class Sample(val file: String, val expectedScam: Boolean, val path: String)

    private fun samples(set: SampleSet): List<Sample> = FOLDERS.flatMap { (folder, scam) ->
        val dir = "${set.root}/$folder"
        runCatching { context.assets.list(dir) }.getOrNull().orEmpty()
            .filter { it.endsWith(".txt") }
            .sorted()
            .map { Sample("$folder/$it", scam, "$dir/$it") }
    }

    fun count(set: SampleSet): Int = samples(set).size

    suspend fun runAll(set: SampleSet, mode: CheckMode, onResult: (SampleResult) -> Unit): List<SampleResult> = samples(set).map { sample ->
        val text = withContext(Dispatchers.IO) {
            context.assets.open(sample.path).bufferedReader().use { it.readText().trim() }
        }
        val start = SystemClock.elapsedRealtime()
        val result = checker.check(text, "SMS", settings.language, mode)
        val millis = SystemClock.elapsedRealtime() - start
        // File name, levels, Y/N answers and timing only: never the message or model-written text
        Log.i(
            TAG,
            "$mode ${set.name} ${sample.file} expected=${if (sample.expectedScam) "scam" else "normal"} " +
                "level=${result.verdict.level} by=${result.decidedBy} answers=${result.answers ?: "-"} ms=$millis",
        )
        SampleResult(
            file = sample.file,
            expectedScam = sample.expectedScam,
            level = result.verdict.level,
            decidedBy = result.decidedBy,
            answers = result.answers,
            reason = ReasonText.reasonOf(context, result.verdict),
            millis = millis,
            replies = result.replies,
        ).also(onResult)
    }

    private companion object {
        const val TAG = "SampleRunner"
        val FOLDERS = listOf("scam" to true, "normal" to false)
    }
}
