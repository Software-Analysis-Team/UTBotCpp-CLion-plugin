package com.github.vol0n.utbotcppclion.services

import com.github.vol0n.utbotcppclion.client.GrpcStarter
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing

@State(name = "Generator settings", storages = [Storage(value = "test_generator_settings.xml")])
data class GeneratorSettings(
    var generateForStaticFunctions: Boolean = true,
    var useStubs: Boolean = true,
    var useDeterministicSearcher: Boolean = true,
    var verbose: Boolean = false,
    var timeoutPerFunction: Int = 5,
    var timeoutPerTest: Int = 20
) : PersistentStateComponent<GeneratorSettings> {
    @com.intellij.util.xmlb.annotations.Transient
    val client = GrpcStarter.startClient()

    @com.intellij.util.xmlb.annotations.Transient
    val grpcCoroutineScope: CoroutineScope

    init {
        val handler = CoroutineExceptionHandler { _, exception ->
            println("CoroutineExceptionHandler got $exception")
        }
        grpcCoroutineScope = CoroutineScope(Dispatchers.Swing + handler)
    }

    override fun getState(): GeneratorSettings {
        return this
    }

    override fun loadState(p0: GeneratorSettings) {
        XmlSerializerUtil.copyBean(state, this);
    }
}
