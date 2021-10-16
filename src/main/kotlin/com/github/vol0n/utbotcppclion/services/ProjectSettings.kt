package com.github.vol0n.utbotcppclion.services

import com.github.vol0n.utbotcppclion.utils.relativize
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil

/**
 * ProjectSettings is a service used to get project related info
 * for generating tests.
 *
 * @see ProjectConfigurable
 */
@State(name = "utBotProjectSettings")
data class ProjectSettings(
    @com.intellij.util.xmlb.annotations.Transient
    val project: Project,
) : PersistentStateComponent<ProjectSettings.GrpcProjectSettings> {

    private val projectState = GrpcProjectSettings()

    override fun getState(): GrpcProjectSettings {
        return projectState
    }

    /**
     * Stores project paths for generating tests.
     * @param targetPath - path to executable which launches tests.
     * @param buildDirPath - path to build directory.
     * @param testDirPath - path to directory for storing test files.
     */
    data class GrpcProjectSettings(
        var targetPath: String = "unknown",
        var buildDirPath: String = "build",
        var testDirPath: String = "tests",
        var synchronizeCode: Boolean = false,
        var sourcePaths: List<String> = emptyList(),
    )

    fun getTargetPath() = relativize(project.basePath ?: "", state.targetPath)
    fun getBuildDirPath() = relativize(project.basePath ?: "", state.buildDirPath)
    fun getTestDirPath() = relativize(project.basePath ?: "", state.testDirPath)
    fun getSynchronizeCode() = state.synchronizeCode
    fun getSourcePaths() = state.sourcePaths.map { relativize(project.basePath ?: "", it) }.toList()

    override fun loadState(state: GrpcProjectSettings) {
        XmlSerializerUtil.copyBean(state, projectState)
    }
}