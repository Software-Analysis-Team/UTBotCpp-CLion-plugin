package com.github.vol0n.utbotcppclion.services

import com.github.vol0n.utbotcppclion.actions.utils.notifyError
import com.github.vol0n.utbotcppclion.messaging.UTBotSettingsChangedListener
import com.github.vol0n.utbotcppclion.ui.UTBotTarget
import com.github.vol0n.utbotcppclion.utils.relativize
import com.intellij.ide.util.RunOnceUtil
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil
import com.jetbrains.cidr.cpp.cmake.model.CMakeConfiguration
import com.jetbrains.cidr.cpp.execution.CMakeAppRunConfiguration
import java.io.File
import java.nio.file.Paths

/**
 * ProjectSettings is a service used to get project related info
 * for generating tests.
 *
 * @see UTBotConfigurable
 */
@State(name = "UTBotProjectSettings")
data class ProjectSettings(
    @com.intellij.util.xmlb.annotations.Transient
    val project: Project? = null,
    ) : PersistentStateComponent<ProjectSettings> {
    @com.intellij.util.xmlb.annotations.Transient
    val logger = Logger.getInstance(this::class.java)

    var targetPath: String = "/"
    var buildDirPath: String = "/"
    var testDirPath: String = "/"
    var synchronizeCode: Boolean = false
    var remotePath: String = "/"
    var sourcePaths: List<String> = emptyList()
    var port: Int = 2121
    var serverName: String = "localhost"

    init {
        logger.info("ProjectSettings instance's constructor is called: project == $project")
        // when user launches the project for the first time, try to predict paths
        project?.let {
            RunOnceUtil.runOnceForProject(
                project, "Predict UTBot paths"
            ) { predictPaths() }
        }
    }

    fun getAbsoluteSourcePaths() = sourcePaths.map { convertToRemotePathIfNeeded(it) }
    fun getRelativeBuildDirPath() = buildDirPath.getRelativeToProjectPath()
    fun getAbsoluteTestDirPath() = convertToRemotePathIfNeeded(testDirPath)
    fun getAbsoluteTargetPath() = convertToRemotePathIfNeeded(targetPath)
    fun getProjectPath(): String {
        val projectPath = project?.basePath ?: let {
            notifyError("Could not get project path")
            "/"
        }
        return convertToRemotePathIfNeeded(projectPath)
    }

    fun isRemoteScenario() = remotePath.isNotEmpty()

    /**
     * Convert absolute path on this machine to corresponding absolute path on docker
     * if path to project on a remote machine was specified in the settings.
     *
     * If remote path == "", this function returns [path] unchanged.
     *
     * @param path - absolute path on local machine to be converted
     */
    fun convertToRemotePathIfNeeded(path: String): String {
        logger.info("Converting $path to remote version")
        var result = path
        if (isRemoteScenario()) {
            val relativeToProjectPath = path.getRelativeToProjectPath()
            result = Paths.get(remotePath, relativeToProjectPath).toString()
        }
        logger.info("The resulting path: $result")
        return result
    }

    /**
     * Convert absolute path on docker container to corresponding absolute path on local machine.
     *
     * If remote path == "", this function returns [path] unchanged.
     *
     * @param path - absolute path on docker to be converted
     */
    fun convertFromRemotePathIfNeeded(path: String): String {
        logger.info("Converting $path to local version")
        var result = path
        if (isRemoteScenario()) {
            val projectLocalPath = project?.basePath ?: let {
                notifyError("Could not get project path.", project)
                return "/"
            }
            val relativeToProjectPath = path.getRelativeToProjectPath(remotePath)
            result = Paths.get(projectLocalPath, relativeToProjectPath).toString()
        }
        logger.info("The resulting path: $result")
        return result
    }

    private fun String.getRelativeToProjectPath(projectPath: String? = project?.basePath): String {
        logger.info("getRelativeToProjectPath was called on $this")
        projectPath ?: let {
            notifyError("Could not get project path.", project)
            return "/"
        }
        return relativize(projectPath, this)
    }

    private fun couldNotGetItem(itemName: String) = notifyError(
        """Could not get $itemName.
               Please, provide paths manually in settings -> tools -> UTBot Settings.
            """.trimMargin(),
        project
    )

    // try to get build dir, tests dir, cmake target paths, and source folders paths from ide
    fun predictPaths() {
        logger.info("predict paths was called")
        fun getSourceFoldersFromSources(sources: Collection<File>) = sources.map {
            it.parent
        }.distinct()

        fun predictTestsDirPath() {
            val projectPath = project?.basePath
            testDirPath = if (projectPath != null) {
                Paths.get(projectPath, "tests").toString()
            } else {
                couldNotGetItem("project path")
                "/"
            }
        }

        fun getCmakeConfiguration(): CMakeConfiguration? {
            val confAndTarget = CMakeAppRunConfiguration.getSelectedConfigurationAndTarget(project)
            val cmakeConfigurations = confAndTarget?.first?.cMakeTarget?.buildConfigurations
            return cmakeConfigurations?.firstOrNull()
        }

        predictTestsDirPath()

        val cmakeConfiguration = getCmakeConfiguration()
        if (cmakeConfiguration != null) {
            // try to get all source paths, so user does not have to choose them by hand in settings
            sourcePaths = getSourceFoldersFromSources(cmakeConfiguration.sources)
            targetPath = UTBotTarget.UTBOT_AUTO_TARGET.targetAbsolutePath
            buildDirPath = cmakeConfiguration.buildWorkingDir.absolutePath
        } else {
            notifyError("CMake is unavailable: automatic configuration failed")
        }
    }

    fun fireUTBotSettingsChanged() {
        project ?: return
        project.messageBus.syncPublisher(UTBotSettingsChangedListener.TOPIC).settingsChanged(this)
    }

    override fun getState(): ProjectSettings {
        logger.info("getState was called: this:$this")
        return this
    }

    override fun loadState(state: ProjectSettings) {
        logger.info("loadState was called: state: $state\n this:$this")

        XmlSerializerUtil.copyBean(state, this)
    }
}
