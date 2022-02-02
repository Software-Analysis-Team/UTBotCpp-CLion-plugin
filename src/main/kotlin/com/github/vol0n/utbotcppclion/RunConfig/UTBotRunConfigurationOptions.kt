package com.github.vol0n.utbotcppclion.RunConfig

import com.intellij.execution.configurations.RunConfigurationOptions
import com.intellij.openapi.components.StoredProperty

class UTBotRunConfigurationOptions : RunConfigurationOptions() {
    private val pathToTestFile: StoredProperty<String?> = string("").provideDelegate(this, "pathToTestFile")
    var testFilePath: String?
        get() = pathToTestFile.getValue(this)
        set(value) {
            pathToTestFile.setValue(this, value)
        }
}
