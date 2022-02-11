package com.github.vol0n.utbotcppclion.coverage

import com.intellij.coverage.BaseCoverageSuite
import com.intellij.coverage.CoverageEngine
import com.intellij.coverage.CoverageLogger
import com.intellij.coverage.CoverageRunner
import com.intellij.openapi.project.Project
import com.intellij.rt.coverage.data.ProjectData
import testsgen.Testgen
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit

class UTBotCoverageSuite(
    coverageEngine: UTBotCoverageEngine,
    covLists: List<Testgen.FileCoverageSimplified>? = null,
    name: String? = null,
    utbotFileProvider: UTBotCoverageFileProvider? = UTBotCoverageFileProvider(),
    lastCoverageTimeStamp: Long = Date().time,
    coverageByTestEnabled: Boolean = false,
    tracingEnabled: Boolean = false,
    trackTestFolders: Boolean = false,
    coverageRunner: CoverageRunner? = null,
    project: Project,
) : BaseCoverageSuite(
    name, utbotFileProvider, lastCoverageTimeStamp, coverageByTestEnabled, tracingEnabled, trackTestFolders,
    coverageRunner, project
) {

    val covEngine = coverageEngine
    val covRunner = coverageRunner
    val coveragesList: List<Testgen.FileCoverageSimplified>? = covLists

    override fun getCoverageEngine(): CoverageEngine {
        return covEngine
    }

    override fun deleteCachedCoverageData() {}

    override fun loadProjectInfo(): ProjectData? {
        val startNs = System.nanoTime()
        val projectData = covRunner?.loadCoverageData(File(""), this)
        val timeMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs)
        if (projectData != null) {
            CoverageLogger.logReportLoading(project, covRunner!!, timeMs, projectData.classesNumber)
        }
        return projectData
    }
}
