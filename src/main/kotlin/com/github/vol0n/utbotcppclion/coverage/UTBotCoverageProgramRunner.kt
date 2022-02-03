package com.github.vol0n.utbotcppclion.coverage

import com.github.vol0n.utbotcppclion.RunConfig.UTBotRunWithCoverageRunConfig
import com.github.vol0n.utbotcppclion.actions.getCoverageAndResultsRequest
import com.github.vol0n.utbotcppclion.services.Client
import com.github.vol0n.utbotcppclion.services.ProjectSettings
import com.intellij.coverage.CoverageRunnerData
import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.ConfigurationInfoProvider
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger


class UTBotCoverageProgramRunner : ProgramRunner<RunnerSettings> {
    private val log = Logger.getInstance(this::class.java)
    override fun getRunnerId(): String {
        return COVERAGE_RUNNER_ID
    }

    override fun canRun(executorId: String, profile: RunProfile): Boolean {
        return "Coverage" == executorId && profile is UTBotRunWithCoverageRunConfig
    }

    override fun createConfigurationData(settingsProvider: ConfigurationInfoProvider): RunnerSettings {
        return CoverageRunnerData()
    }

    @Throws(ExecutionException::class)
    override fun execute(environment: ExecutionEnvironment) {
        log.debug("execute was called: ${environment.runProfile}, ${environment.state}")
        val conf = environment.runProfile as UTBotRunWithCoverageRunConfig
        val project = conf.project
        println(conf.pathToTestsFile ?: "Null")
        val request = getCoverageAndResultsRequest(project.service<ProjectSettings>(), conf.pathToTestsFile!!)
        project.service<Client>().getCoverageAndResults(request)
    }

    companion object {
        val COVERAGE_RUNNER_ID = UTBotCoverageProgramRunner::class.java.simpleName
    }
}
