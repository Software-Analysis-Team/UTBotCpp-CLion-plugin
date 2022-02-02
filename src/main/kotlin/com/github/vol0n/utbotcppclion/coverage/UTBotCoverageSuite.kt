package com.github.vol0n.utbotcppclion.coverage

import com.github.vol0n.utbotcppclion.RunConfig.UTBotRunWithCoverageRunConfig
import com.intellij.coverage.BaseCoverageSuite
import com.intellij.coverage.CoverageEngine
import com.intellij.coverage.CoverageFileProvider
import com.intellij.coverage.CoverageRunner
import com.intellij.openapi.project.Project
import java.util.*

class UTBotCoverageSuite(
    coverageEngine: UTBotCoverageEngine,
    runConfig: UTBotRunWithCoverageRunConfig? = null,
    name: String? = null,
    fileProvider: CoverageFileProvider? = null,
    lastCoverageTimeStamp: Long = Date().time,
    coverageByTestEnabled: Boolean = true,
    tracingEnabled: Boolean = true,
    trackTestFolders: Boolean = true,
    coverageRunner: CoverageRunner? = null,
    project: Project? = null,
) : BaseCoverageSuite(
    name, fileProvider, lastCoverageTimeStamp, coverageByTestEnabled, tracingEnabled, trackTestFolders,
    coverageRunner, project
) {
    val covEngine = coverageEngine
    val coverageData = runConfig?.coveragesList

    override fun getCoverageEngine(): CoverageEngine {
        return covEngine
    }

    override fun deleteCachedCoverageData() {}
}
