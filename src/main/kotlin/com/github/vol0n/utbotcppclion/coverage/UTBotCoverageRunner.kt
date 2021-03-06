package com.github.vol0n.utbotcppclion.coverage

import com.github.vol0n.utbotcppclion.actions.utils.convertFromRemotePathIfNeeded
import com.intellij.coverage.CoverageEngine
import com.intellij.coverage.CoverageRunner
import com.intellij.coverage.CoverageSuite
import com.intellij.openapi.diagnostic.Logger
import com.intellij.rt.coverage.data.LineData
import com.intellij.rt.coverage.data.ProjectData
import testsgen.Testgen
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths

/**
 * This class is used to convert from our representation of coverage to IntelliJ's [ProjectData]
 */
class UTBotCoverageRunner : CoverageRunner() {
    private val log = Logger.getInstance(this::class.java)
    private fun getLineCount(filePath: String): Int {
        var lineCount: Int
        Files.lines(Paths.get(filePath), StandardCharsets.UTF_8).use { stream -> lineCount = stream.count().toInt() }
        return lineCount
    }

    /**
     * Convert from our coverage representation to IntelliJ Platform representation - [ProjectData]
     */
    override fun loadCoverageData(sessionDataFile: File, baseCoverageSuite: CoverageSuite?): ProjectData? {
        log.debug("loadCoverageData was called!")
        val coveragesList = (baseCoverageSuite as? UTBotCoverageSuite)?.coveragesList
        coveragesList ?: error("Coverage list is empty in loadCoverageData!")
        val projectData = ProjectData()
        var isAnyCoverage = false
        for (simplifiedCovInfo in coveragesList) {
            val filePathFromServer = simplifiedCovInfo.filePath
            if (filePathFromServer.isNotEmpty()) {
                isAnyCoverage = true
                val localFilePath = filePathFromServer.convertFromRemotePathIfNeeded(baseCoverageSuite.project)
                val lines = arrayOfNulls<LineData>(getLineCount(localFilePath))
                val classData = projectData.getOrCreateClassData(provideQualifiedNameForFile(localFilePath))
                fun processRanges(rangesList: List<Testgen.SourceRange?>, isCovered: Boolean) {
                    rangesList.filterNotNull().forEach {
                        for (i in (it.start.line+1)..(it.end.line+1)) {
                            val lineData = LineData(i, null)
                            lineData.hits = if (isCovered) 1 else 0
                            lines[i-1] = lineData
                            classData.registerMethodSignature(lineData)
                        }
                    }
                }
                processRanges(simplifiedCovInfo.coveredRangesList, true)
                processRanges(simplifiedCovInfo.uncoveredRangesList, false)
                classData.setLines(lines)
            }
        }
        return if (isAnyCoverage) projectData else null
    }

    override fun getPresentableName(): String {
        return "Presentable name for CoverageRunner"
    }

    override fun getId(): String {
        return "Coverage runner ID"
    }

    // actually no coverage file exists, but this method must be implemented, see UTBotCoverageFileProvider
    override fun getDataFileExtension(): String {
        return "txt"
    }

    override fun acceptsCoverageEngine(engine: CoverageEngine): Boolean {
        return engine is UTBotCoverageEngine
    }

    companion object {
        fun provideQualifiedNameForFile(absolutePath: String) = absolutePath
    }
}
