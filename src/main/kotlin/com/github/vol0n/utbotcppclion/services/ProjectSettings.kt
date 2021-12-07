package com.github.vol0n.utbotcppclion.services

import com.github.vol0n.utbotcppclion.actions.utils.notifyError
import com.github.vol0n.utbotcppclion.utils.relativize
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil
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
    var targetPath: String = ""
    var buildDirPath: String = ""
    var testDirPath: String = ""
    var synchronizeCode: Boolean = false
    var sourcePaths: List<String> = emptyList()

    init {
        logger.info("ProjectSettings instance's constructor is called: project == $project")
        // project is null when ide creates ProjectSettings instance from serialized xml file
        // project is not null when plugin is running in user's ide, so we can access ide for paths
        if (project != null) {
            checkForUninitializedDataAndInit()
        }
    }

    fun getRelativeBuildDirPath() = buildDirPath.getRelativeToProjectPath()

    private fun String.getRelativeToProjectPath(): String {
        logger.info("getRelativeToProjectPath was called on $this")
        val projectPath = project?.basePath ?: let {
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
