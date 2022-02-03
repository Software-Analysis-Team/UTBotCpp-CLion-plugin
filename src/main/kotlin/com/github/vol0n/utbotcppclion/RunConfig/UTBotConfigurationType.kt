package com.github.vol0n.utbotcppclion.RunConfig

import com.intellij.execution.configurations.ConfigurationTypeUtil;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunConfigurationSingletonPolicy;
import com.intellij.execution.configurations.SimpleConfigurationType;
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NotNullLazyValue
import mu.KotlinLogging

private val log = KotlinLogging.logger {}
class UTBotConfigurationType : SimpleConfigurationType(CONFIGURATION_NAME, NAME, NAME,
    NotNullLazyValue.createValue { AllIcons.Graph.Grid }
) {

    override fun createTemplateConfiguration(project: Project): RunConfiguration {
        log.debug { "createTemplateConfiguration was celled" }
        return UTBotRunWithCoverageRunConfig(project, UTBotRunWithCoverageConfigFactory(this), "Template configuration")
    }

    override fun getTag(): String = NAME

    override fun getSingletonPolicy() = RunConfigurationSingletonPolicy.SINGLE_INSTANCE_ONLY

    companion object {
        val NAME = "UTBot"
        val CONFIGURATION_NAME = "${NAME}TestRunner"
        fun getInstance() = ConfigurationTypeUtil.findConfigurationType(UTBotConfigurationType::class.java)
    }
}
