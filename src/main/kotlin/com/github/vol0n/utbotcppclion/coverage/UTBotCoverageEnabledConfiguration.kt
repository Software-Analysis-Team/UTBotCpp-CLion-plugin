package com.github.vol0n.utbotcppclion.coverage

import com.intellij.coverage.CoverageRunner
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.coverage.CoverageEnabledConfiguration

class UTBotCoverageEnabledConfiguration(conf: RunConfigurationBase<*>) : CoverageEnabledConfiguration(conf) {
    init {
        coverageRunner = CoverageRunner.getInstance(UTBotCoverageRunner::class.java)
        myCoverageFilePath = ""
        currentCoverageSuite = null
    }
}
