package com.github.vol0n.utbotcppclion.RunConfig

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.project.Project

class UTBotRunWithCoverageConfigFactory(type: UTBotConfigurationType) : ConfigurationFactory(type)
{
    override fun getId(): String {
        return "${UTBotConfigurationType.NAME} factory"
    }

    override fun createTemplateConfiguration(project: Project): RunConfiguration {
        return UTBotRunWithCoverageConfig(project, this, "UTBot run with coverage template configuration")
    }

    override fun getOptionsClass(): Class<out BaseState?> {
        return UTBotRunConfigurationOptions::class.java
    }
}
