package com.ajayinnovations.argus.core

import java.io.File

/** The synthetic messages in src/test/resources/samples. */
object Samples {
    data class Sample(val name: String, val text: String)

    private fun load(folder: String): List<Sample> {
        val url = requireNotNull(javaClass.classLoader?.getResource("samples/$folder")) { "missing samples/$folder" }
        return File(url.toURI()).listFiles { f -> f.extension == "txt" }.orEmpty()
            .sortedBy { it.name }
            .map { Sample(it.name, it.readText().trim()) }
    }

    val scams: List<Sample> by lazy { load("scam") }
    val normals: List<Sample> by lazy { load("normal") }
}

/** A model stub: replies come from [reply] (given the call number, from 1), calls are counted. */
class FakeModel(private val reply: (call: Int) -> String) : TextGenerator {
    var calls = 0
        private set
    var lastPrompt: String? = null
        private set
    var lastMaxTokens: Int? = null
        private set

    override suspend fun generate(system: String, prompt: String, maxOutputTokens: Int): String {
        calls++
        lastPrompt = prompt
        lastMaxTokens = maxOutputTokens
        return reply(calls)
    }

    companion object {
        fun answering(level: String) = FakeModel { """{"level":"$level","reason":"r","advice":"a"}""" }
        fun answers(line: String) = FakeModel { line }
        fun failing() = FakeModel { throw IllegalStateException("model down") }
    }
}
