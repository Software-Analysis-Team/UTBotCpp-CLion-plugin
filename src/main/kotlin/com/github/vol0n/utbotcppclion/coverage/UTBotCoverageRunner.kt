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

class UTBotCoverageRunner : CoverageRunner() {
    private val log = Logger.getInstance(this::class.java)
    private fun getLineCount(filePath: String): Int {
        var lineCount: Int
        Files.lines(Paths.get(filePath), StandardCharsets.UTF_8).use { stream -> lineCount = stream.count().toInt() }
        return lineCount
    }

    override fun loadCoverageData(sessionDataFile: File, baseCoverageSuite: CoverageSuite?): ProjectData? {
        log.debug("loadCoverageData was called!")
        val coveragesList = (baseCoverageSuite as? UTBotCoverageSuite)?.coveragesList
        // maybe raise exception instead?
        assert(coveragesList != null)
        if (coveragesList == null) {
            log.warn("loadCoverageData was called with unexpected coverageSuite! $baseCoverageSuite")
            return null
        }
        val projectData = ProjectData()
        var isAnyCoverage = false
        for (simplifiedCovInfo in coveragesList) {
            val filePathFromServer = simplifiedCovInfo.filePath
            if (filePathFromServer.isNotEmpty()) {
                isAnyCoverage = true
                val localFilePath = filePathFromServer.convertFromRemotePathIfNeeded(baseCoverageSuite.project)
                val lines = arrayOfNulls<LineData>(getLineCount(localFilePath))
                val classData = projectData.getOrCreateClassData(localFilePath)
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

    override fun getDataFileExtension(): String {
        log.debug ( "getDataFileExtension was called" )
        return "txt"
    }

    override fun acceptsCoverageEngine(engine: CoverageEngine): Boolean {
        return engine is UTBotCoverageEngine
    }
}