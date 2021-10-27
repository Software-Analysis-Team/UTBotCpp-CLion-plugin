package com.github.vol0n.utbotcppclion.services

import com.github.vol0n.utbotcppclion.client.GrpcStarter
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

@State(name = "Generator settings", storages = [Storage(value = "test_generator_settings.xml")])
data class GenerateTestsSettings(
    var generateForStaticFunctions: Boolean = true,
    var useStubs: Boolean = true,
    var useDeterministicSearcher: Boolean = true,
    var verbose: Boolean = false,
    var timeoutPerFunction: Int = 5,
    var timeoutPerTest: Int = 20
) : PersistentStateComponent<GenerateTestsSettings> {
    @com.intellij.util.xmlb.annotations.Transient
    val client = GrpcStarter.startClient()

    override fun getState(): GenerateTestsSettings {
        return this
    }

    override fun loadState(p0: GenerateTestsSettings) {
        XmlSerializerUtil.copyBean(state, this);
    }
}
