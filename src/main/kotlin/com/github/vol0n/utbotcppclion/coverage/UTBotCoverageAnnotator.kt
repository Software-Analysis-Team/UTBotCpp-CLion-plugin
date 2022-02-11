package com.github.vol0n.utbotcppclion.coverage

import com.intellij.coverage.CoverageBundle
import com.intellij.coverage.CoverageDataManager
import com.intellij.coverage.CoverageSuitesBundle
import com.intellij.coverage.SimpleCoverageAnnotator
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import java.io.File

class UTBotCoverageAnnotator(project: Project?) : SimpleCoverageAnnotator(project) {
    private val log = Logger.getInstance(this::class.java)
    companion object {
        fun getInstance(project: Project): UTBotCoverageAnnotator = project.service()
    }

    override fun shouldCollectCoverageInsideLibraryDirs() = false

    override fun fillInfoForUncoveredFile(file: File) = FileCoverageInfo()

    override fun getDirCoverageInformationString(
        directory: PsiDirectory,
        currentSuite: CoverageSuitesBundle,
        manager: CoverageDataManager
    ): String? {
        log.debug("getDirCoverage: $directory")
        val coverageInfo = getDirCoverageInfo(directory, currentSuite) ?: return null

        return if (manager.isSubCoverageActive) {
            if (coverageInfo.coveredLineCount > 0) CoverageBundle.message("coverage.view.text.covered") else null
        } else getFilesCoverageInformationString(coverageInfo)?.let { filesCoverageInfo ->
            val builder = StringBuilder()
            builder.append(filesCoverageInfo)
            getLinesCoverageInformationString(coverageInfo)?.let {
                builder.append(": ").append(it)
            }
            builder.toString()
        }
    }

    override fun getFileCoverageInformationString(
        psiFile: PsiFile,
        currentSuite: CoverageSuitesBundle,
        manager: CoverageDataManager
    ): String? = null

    override fun getFilesCoverageInformationString(info: DirCoverageInfo): String? =
        when {
            info.totalFilesCount == 0 -> null
            info.coveredFilesCount == 0 -> "${info.coveredFilesCount} of ${info.totalFilesCount} files covered"
            else -> "${info.coveredFilesCount} of ${info.totalFilesCount} files"
        }

    override fun getLinesCoverageInformationString(info: FileCoverageInfo): String? {
        log.debug("getLinesCoverageInformationString: ${info}")
        return when {
            info.totalLineCount == 0 -> null
            info.coveredLineCount == 0 -> CoverageBundle.message("lines.covered.info.not.covered")
            else -> {
                val message = CoverageBundle.message("lines.covered.info.percent.lines.covered")
                "${calcCoveragePercentage(info)} $message"
            }
        }
    }

    override fun getRoots(
        project: Project?,
        dataManager: CoverageDataManager,
        suite: CoverageSuitesBundle?
    ): Array<VirtualFile> {
        log.debug("getRoots was called: ${suite?.coverageData}")
        return emptyArray()
    }
}
