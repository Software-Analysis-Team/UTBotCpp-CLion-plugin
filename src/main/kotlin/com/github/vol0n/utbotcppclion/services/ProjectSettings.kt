package com.github.vol0n.utbotcppclion.services

import com.github.vol0n.utbotcppclion.actions.utils.notifyError
import com.github.vol0n.utbotcppclion.messaging.UTBotSettingsChangedListener
import com.github.vol0n.utbotcppclion.utils.relativize
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil
import com.jetbrains.cidr.cpp.cmake.model.CMakeConfiguration
import com.jetbrains.cidr.cpp.execution.CMakeAppRunConfiguration
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

    // the true value indicates that plugin was launched for the first time,
    // and the plugin should try to get paths from ide
    // @com.intellij.util.xmlb.annotations.Attribute
    var isFirstTimeLaunch: Boolean = true
    var targetPath: String = "/"
    var buildDirPath: String = "/"
    var testDirPath: String = "/"
    var synchronizeCode: Boolean = false
    var remotePath: String = "/"
    var sourcePaths: List<String> = emptyList()

    init {
        logger.info("ProjectSettings instance's constructor is called: project == $project")
        // project is null when ide creates ProjectSettings instance from serialized xml file
        // project is not null when plugin is running in user's ide, so we can access ide for paths
        if (project != null) {
            checkForUninitializedDataAndInit()
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

    // try to get build dir, tests dir and cmake target paths from ide
    private fun init() {

        val confAndTarget = CMakeAppRunConfiguration.getSelectedConfigurationAndTarget(project)
        val cmakeConfigurations = confAndTarget?.first?.cMakeTarget?.buildConfigurations
        if (cmakeConfigurations == null || cmakeConfigurations.isEmpty()) {
            couldNotGetItem("cmakeConfigurations")
            return
        }

        val cmakeConfiguration = cmakeConfigurations.first() // todo: when there are more than one configuration?
        targetPath = cmakeConfiguration.productFile?.absolutePath ?: let {
            couldNotGetItem("targetPath")
            "/"
        }
        buildDirPath = cmakeConfiguration.buildWorkingDir.absolutePath
        val projectPath = project?.basePath
        testDirPath = if (projectPath != null) {
            Paths.get(projectPath, "tests").toString()
        } else {
            couldNotGetItem("testDirPath")
            "/"
        }
    }

    fun fireUTBotSettingsChanged() {
        project ?: return
        project.messageBus.syncPublisher(UTBotSettingsChangedListener.TOPIC).settingsChanged(this)
    }

    private fun checkForUninitializedDataAndInit() {
        if (isFirstTimeLaunch) {
            isFirstTimeLaunch = false
            init()
        }
    }

    override fun getState(): ProjectSettings {
        logger.info("getState was called: this:$this")
        return this
    }

    override fun loadState(state: ProjectSettings) {
        logger.info("loadState was called: state: $state\n this:$this")

        XmlSerializerUtil.copyBean(state, this)
        isFirstTimeLaunch = false
    }
}
