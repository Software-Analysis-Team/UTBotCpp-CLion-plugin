package com.github.vol0n.utbotcppclion.RunConfig

import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import testsgen.Testgen

class UTBotRunWithCoverageRunConfig(project: Project, factory: ConfigurationFactory?, name: String?) :
    RunConfigurationBase<UTBotRunConfigurationOptions?>(project, factory, name) {
    override fun getOptions(): UTBotRunConfigurationOptions {
        return super.getOptions() as UTBotRunConfigurationOptions
    }

    var coveragesList: List<Testgen.FileCoverageSimplified>? = null
        private set

    constructor(coverages: List<Testgen.FileCoverageSimplified>, project: Project, name: String?) : this(
        project,
        getDefaultFactory(),
        name
    ) {
        coveragesList = coverages
    }

    var pathToTestsFile: String?
        get() = options.testFilePath
        set(value) {
            options.testFilePath = value
        }

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration?> {
        return UTBotRunConfigurationSettingsEditor()
    }

    override fun checkConfiguration() {}

    override fun getState(executor: Executor, executionEnvironment: ExecutionEnvironment): RunProfileState? {
        return RunProfileState { _, _ -> null }
    }

    companion object {
        private fun getDefaultFactory(): UTBotRunWithCoverageConfigFactory {
            val type = UTBotConfigurationType.getInstance()
            return UTBotRunWithCoverageConfigFactory(type)
        }
    }
}
