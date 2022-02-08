package com.github.vol0n.utbotcppclion.coverage

import com.github.vol0n.utbotcppclion.RunConfig.UTBotRunWithCoverageConfig
import com.intellij.coverage.CoverageDataManager
import com.intellij.coverage.CoverageFileProvider
import com.intellij.coverage.CoverageRunner
import com.intellij.execution.configurations.coverage.CoverageEnabledConfiguration
import java.util.*

class UTBotCoverageEnabledConfiguration(conf: UTBotRunWithCoverageConfig) : CoverageEnabledConfiguration(conf) {
    init {
        coverageRunner = CoverageRunner.getInstance(UTBotCoverageRunner::class.java)
        myCoverageFilePath = ""
        currentCoverageSuite = CoverageDataManager.getInstance(conf.project).addExternalCoverageSuite(
            "UTBot just created coverage suite",
            Date().time,
            coverageRunner,
            UTBotCoverageFileProvider(conf)
        )
    }
}
