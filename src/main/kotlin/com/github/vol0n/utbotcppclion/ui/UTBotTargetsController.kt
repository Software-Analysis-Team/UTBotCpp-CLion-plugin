package com.github.vol0n.utbotcppclion.ui

import com.github.vol0n.utbotcppclion.messaging.UTBotSettingsChangedListener
import com.github.vol0n.utbotcppclion.services.ProjectSettings
import com.github.vol0n.utbotcppclion.utils.relativize
import com.intellij.execution.RunManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.CollectionListModel
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeWorkspaceListener
import com.jetbrains.cidr.cpp.execution.CMakeAppRunConfiguration
import java.nio.file.Paths

open class UTBotTarget(val targetAbsolutePath: String, val project: Project? = null) {
    open val description: String
        get(): String = project?.basePath?.let {
            relativize(it, targetAbsolutePath)
        } ?: ""

    open val name: String get(): String = Paths.get(targetAbsolutePath).last().toString()

    companion object {
        val UTBOT_AUTO_TARGET: UTBotTarget = object : UTBotTarget("/utbot/auto/target/path") {
            override val description: String
                get() = "Automatically detects a target"

            override val name = "/utbot/auto"
        }
    }
}

class UTBotTargetsController(val project: Project) {
    private val projectSettings = project.service<ProjectSettings>()
    private val listModel = CollectionListModel(mutableListOf(UTBotTarget.UTBOT_AUTO_TARGET))
    init {
        listModel.addAll(1, getCMakeTargets())
        projectSettings.targetPath?.let { addTargetPathIfNotPresent(it) }
        connectToEvents()
    }

    private fun addTargetPathIfNotPresent(possiblyNewTargetPath: String) {
        listModel.apply {
            toList().find { utbotTarget -> utbotTarget.targetAbsolutePath == possiblyNewTargetPath } ?: add(
                UTBotTarget(possiblyNewTargetPath, project)
            )
        }
    }

    fun createTargetsToolWindow(): UTBotTargetsToolWindow {
        return UTBotTargetsToolWindow(listModel, this)
    }

    private fun getCMakeTargets() =
        RunManager.getInstance(project).allConfigurationsList.mapNotNull {
            // BuildConfigurations can be 'release', 'debug' ...
            (it as CMakeAppRunConfiguration).cMakeTarget?.buildConfigurations?.mapNotNull { cmakeConfig ->
                cmakeConfig.productFile?.absolutePath?.let { targetPath ->
                    UTBotTarget(targetPath, project)
                }
            }
        }.flatten()

    fun selectionChanged(index: Int) {
        // when user selects target update model
        projectSettings.targetPath = listModel.getElementAt(index).targetAbsolutePath
    }

    fun getCurrentTargetPath() = projectSettings.targetPath

    private fun connectToEvents() {
        // when new targets are specified in CMakeLists, the view should be updated, it is done on after reloading
        project.messageBus.connect().subscribe(CMakeWorkspaceListener.TOPIC, object : CMakeWorkspaceListener {
            override fun reloadingFinished(canceled: Boolean) {
                listModel.replaceAll(mutableListOf(UTBotTarget.UTBOT_AUTO_TARGET).apply {
                    addAll(getCMakeTargets())
                    projectSettings.targetPath?.let { addTargetPathIfNotPresent(it) }
                })
            }
        })

        // if user specifies some custom target path in settings, it will be added if not already present
        project.messageBus.connect().subscribe(
            UTBotSettingsChangedListener.TOPIC,
            UTBotSettingsChangedListener { settings ->
                val possiblyNewTargetPath = settings.targetPath ?: return@UTBotSettingsChangedListener
                addTargetPathIfNotPresent(possiblyNewTargetPath)
            })
    }
}
