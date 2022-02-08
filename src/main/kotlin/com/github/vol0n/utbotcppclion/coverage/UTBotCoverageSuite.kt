package com.github.vol0n.utbotcppclion.coverage

import com.intellij.coverage.BaseCoverageSuite
import com.intellij.coverage.CoverageEngine
import com.intellij.coverage.CoverageLogger
import com.intellij.coverage.CoverageRunner
import com.intellij.openapi.project.Project
import com.intellij.rt.coverage.data.ProjectData
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit

class UTBotCoverageSuite(
    coverageEngine: UTBotCoverageEngine,
    name: String? = null,
    utbotFileProvider: UTBotCoverageFileProvider,
    lastCoverageTimeStamp: Long = Date().time,
    coverageByTestEnabled: Boolean = true,
    tracingEnabled: Boolean = true,
    trackTestFolders: Boolean = true,
    coverageRunner: CoverageRunner? = null,
    project: Project? = null,
) : BaseCoverageSuite(
    name, utbotFileProvider, lastCoverageTimeStamp, coverageByTestEnabled, tracingEnabled, trackTestFolders,
    coverageRunner, project
) {

    val covEngine = coverageEngine
    val covRunner = coverageRunner
    val coveragesList = utbotFileProvider.config.coveragesList

    override fun getCoverageEngine(): CoverageEngine {
        return covEngine
    }

    override fun deleteCachedCoverageData() {}

    override fun loadProjectInfo(): ProjectData? {
        println("loadProjectInfo of UTBotCovSuite was called!")
        val startNs = System.nanoTime()
        val projectData = covRunner?.loadCoverageData(File(""), this)
        val timeMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs)
        if (projectData != null) {
            CoverageLogger.logReportLoading(project, covRunner!!, timeMs, projectData.classesNumber)
        }
        return projectData
    }
}
