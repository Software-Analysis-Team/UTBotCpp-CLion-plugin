package com.github.vol0n.utbotcppclion.coverage

import com.github.vol0n.utbotcppclion.RunConfig.UTBotRunWithCoverageRunConfig
import com.intellij.coverage.CoverageDataManager
import com.intellij.coverage.CoverageFileProvider
import com.intellij.coverage.CoverageRunner
import com.intellij.execution.configurations.coverage.CoverageEnabledConfiguration

class DummyFileProvider: CoverageFileProvider {
    override fun getCoverageDataFilePath(): String {
        return ""
    }

    override fun ensureFileExists(): Boolean {
        return true
    }

    override fun isValid(): Boolean {
        return true
    }
}

class UTBotCoverageEnabledConfiguration(conf: UTBotRunWithCoverageRunConfig) : CoverageEnabledConfiguration(conf) {
    init {
        coverageRunner = CoverageRunner.getInstance(UTBotCoverageRunner::class.java)
        val root = conf.project.basePath ?: error("Base path is null")
        myCoverageFilePath = "$root/coverage/coverage.txt"
//        currentCoverageSuite = CoverageDataManager.getInstance(conf.project).addCoverageSuite(
//                "UTBot suite created from coverageEnabled config",
//                DummyFileProvider(), emptyArray(), Date().time, null,
//                coverageRunner, true, true
//            )
        currentCoverageSuite = CoverageDataManager.getInstance(conf.project).addCoverageSuite(this)
    }

//    override fun createCoverageFile(): String? = null
}