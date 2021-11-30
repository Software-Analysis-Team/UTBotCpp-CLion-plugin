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
 * @see ProjectConfigurable
 */
@State(name = "utBotProjectSettings")
data class ProjectSettings(
    @com.intellij.util.xmlb.annotations.Transient
    val project: Project? = null,
) : PersistentStateComponent<ProjectSettings> {
    val logger = Logger.getInstance(this::class.java)

    // the null value indicates that plugin was launched for the first time,
    // and the plugin should try to get these paths from ide
    var targetPath: String? = null
    var buildDirPath: String? = null
    var testDirPath: String? = null
    var synchronizeCode: Boolean = false
    var sourcePaths: List<String> = emptyList()

    init {
        logger.info("ProjectSettings instance's constructor is called")
        // project is null when ide creates ProjectSettings instance from serialized xml file
        // project is not null when plugin is running in user's ide, so we can access ide for paths
        if (project != null) {
            checkForUninitializedDataAndInit()
        }
    }

    fun getRelativeTargetPath() = targetPath.getRelativeToProjectPath()
    fun getRelativeBuildDirPath() = buildDirPath.getRelativeToProjectPath()
    fun getRelativeTestDirPath() = testDirPath.getRelativeToProjectPath()
    fun getRelativeSourcesPaths() = sourcePaths.map { it.getRelativeToProjectPath() }

    private fun String?.getRelativeToProjectPath(): String {
        logger.info("getRelativeToProjectPath was called on $this")
        this ?: error("Paths are not initialized.")
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
        if (targetPath == null || buildDirPath == null || testDirPath == null)
            init()
    }

    override fun getState() = this

    override fun loadState(state: ProjectSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }
}
