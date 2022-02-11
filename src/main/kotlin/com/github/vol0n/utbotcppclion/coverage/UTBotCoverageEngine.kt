package com.github.vol0n.utbotcppclion.coverage

import com.github.vol0n.utbotcppclion.utils.isCPPFileName
import com.intellij.coverage.CoverageAnnotator
import com.intellij.coverage.CoverageEngine
import com.intellij.coverage.CoverageFileProvider
import com.intellij.coverage.CoverageLineMarkerRenderer
import com.intellij.coverage.CoverageRunner
import com.intellij.coverage.CoverageSuite
import com.intellij.coverage.CoverageSuitesBundle
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.coverage.CoverageEnabledConfiguration
import com.intellij.execution.testframework.AbstractTestProxy
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.rt.coverage.data.LineData
import com.intellij.util.Function
import java.io.File
import java.util.*

class UTBotCoverageEngine : CoverageEngine() {
    private val log = Logger.getInstance(this::class.java)
    companion object {
        internal fun provideQualifiedName(sourceFile: PsiFile): String? = sourceFile.virtualFile?.path
    }

    override fun isApplicableTo(conf: RunConfigurationBase<*>) = false

    override fun getLineMarkerRenderer(
        lineNumber: Int,
        className: String?,
        lines: TreeMap<Int, LineData>?,
        coverageByTestApplicable: Boolean,
        coverageSuite: CoverageSuitesBundle,
        newToOldConverter: Function<in Int, Int>?,
        oldToNewConverter: Function<in Int, Int>?,
        subCoverageActive: Boolean
    ): CoverageLineMarkerRenderer {
        log.debug("getLineMarkerRenderer was called! Classname: $className, line: $lineNumber, lines: $lines")
        return super.getLineMarkerRenderer(
            lineNumber,
            className,
            lines,
            coverageByTestApplicable,
            coverageSuite,
            newToOldConverter,
            oldToNewConverter,
            subCoverageActive
        )
    }

    override fun coverageEditorHighlightingApplicableTo(psiFile: PsiFile): Boolean {
        log.debug("coverageEditorHighlightingApplicableTo was called on: $psiFile")
        return true
    }

    override fun coverageProjectViewStatisticsApplicableTo(fileOrDir: VirtualFile): Boolean {
        return !fileOrDir.isDirectory && isCPPFileName(fileOrDir.name)
    }

    override fun canHavePerTestCoverage(conf: RunConfigurationBase<*>) = false

    override fun createCoverageEnabledConfiguration(conf: RunConfigurationBase<*>): CoverageEnabledConfiguration {
        return UTBotCoverageEnabledConfiguration(conf)
    }

    override fun createCoverageSuite(
        covRunner: CoverageRunner,
        name: String,
        coverageDataFileProvider: CoverageFileProvider,
        filters: Array<out String>?,
        lastCoverageTimeStamp: Long,
        suiteToMerge: String?,
        coverageByTestEnabled: Boolean,
        tracingEnabled: Boolean,
        trackTestFolders: Boolean,
        project: Project
    ): CoverageSuite? {
        val utbotFileProvider = (coverageDataFileProvider as? UTBotCoverageFileProvider)
        if (utbotFileProvider == null) {
            log.debug("createCoverageSuite was called with unexpected coverageDataFileProvider!")
            return null
        }
        return UTBotCoverageSuite(
            this, null, name, utbotFileProvider, lastCoverageTimeStamp,
            coverageByTestEnabled, tracingEnabled, tracingEnabled, covRunner, project
        )
    }

    override fun createCoverageSuite(
        covRunner: CoverageRunner,
        name: String,
        coverageDataFileProvider: CoverageFileProvider,
        config: CoverageEnabledConfiguration
    ): CoverageSuite {
        return UTBotCoverageSuite(
            this,
            coverageRunner = covRunner,
            name = name,
            project = config.configuration.project
        )
    }

    override fun createEmptyCoverageSuite(coverageRunner: CoverageRunner): CoverageSuite? {
        log.debug("createEmptyCoverageSuite was called!")
        return null
    }

    override fun getCoverageAnnotator(project: Project?): CoverageAnnotator {
        return UTBotCoverageAnnotator(project)
    }

    override fun recompileProjectAndRerunAction(
        module: Module,
        suite: CoverageSuitesBundle,
        chooseSuiteAction: Runnable
    ) = false

    override fun getQualifiedNames(sourceFile: PsiFile): MutableSet<String> {
        log.debug("getQualifiedNames was called: $sourceFile")
        return provideQualifiedName(sourceFile)?.let {
            mutableSetOf(it)
        } ?: mutableSetOf()
    }

    override fun getQualifiedName(outputFile: File, sourceFile: PsiFile): String {
        log.debug("getQualifiedName was called: $sourceFile")
        return outputFile.absolutePath
    }

    override fun includeUntouchedFileInCoverage(
        qualifiedName: String,
        outputFile: File,
        sourceFile: PsiFile,
        suite: CoverageSuitesBundle
    ) = false

    override fun acceptedByFilters(psiFile: PsiFile, suite: CoverageSuitesBundle): Boolean {
        log.debug("acceptedByFilters: $psiFile, ${suite.coverageData}")
       return true
    }

    override fun collectSrcLinesForUntouchedFile(classFile: File, suite: CoverageSuitesBundle): MutableList<Int>? =
        mutableListOf()

    override fun findTestsByNames(testNames: Array<out String>, project: Project): MutableList<PsiElement> {
        log.debug("findTestsByNames was called: $testNames")
        return mutableListOf()
    }


    override fun getTestMethodName(element: PsiElement, testProxy: AbstractTestProxy): String? {
        log.debug("getTestMethodName: ${element.text}, testProxy: ${testProxy.allTests}")
        return null
    }

    override fun getPresentableText(): String {
        return "UTBot Coverage Engine"
    }
}
