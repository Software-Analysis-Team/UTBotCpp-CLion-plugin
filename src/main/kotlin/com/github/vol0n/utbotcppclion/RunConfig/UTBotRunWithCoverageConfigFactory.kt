package com.github.vol0n.utbotcppclion.RunConfig

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.project.Project

// Copyright 2000-2022 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.


class UTBotRunWithCoverageConfigFactory(type: UTBotConfigurationType) : ConfigurationFactory(type)
{
    override fun getId(): String {
        return "${UTBotConfigurationType.NAME} factory"
    }

    override fun createTemplateConfiguration(project: Project): RunConfiguration {
        return UTBotRunWithCoverageRunConfig(project, this, "UTBot run with coverage template configuration")
    }

    override fun getOptionsClass(): Class<out BaseState?> {
        return UTBotRunConfigurationOptions::class.java
    }
}
