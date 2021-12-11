package com.github.vol0n.utbotcppclion.ui

import com.github.vol0n.utbotcppclion.messaging.UTBotSettingsChangedListener
import com.github.vol0n.utbotcppclion.services.ProjectSettings
import com.github.vol0n.utbotcppclion.utils.relativize

import com.intellij.execution.RunManager
import com.intellij.icons.AllIcons
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.CollectionListModel
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeWorkspaceListener
import com.jetbrains.cidr.cpp.execution.CMakeAppRunConfiguration

import javax.swing.JComponent
import javax.swing.JList
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

class UTBotTargetsToolWindow(val project: Project) {
    private val projectSettings = project.service<ProjectSettings>()
    private val listModel = CollectionListModel(mutableListOf(UTBotTarget.UTBOT_AUTO_TARGET))
    init {
        listModel.addAll(1, getCMakeTargets())
        projectSettings.targetPath?.let { addTargetPathIfNotPresent(it) }
    }
    private val uiList = JBList(listModel)

    private fun getCMakeTargets() =
        RunManager.getInstance(project).allConfigurationsList.mapNotNull {
            // BuildConfigurations can be 'release', 'debug' ...
            (it as CMakeAppRunConfiguration).cMakeTarget?.buildConfigurations?.mapNotNull { cmakeConfig ->
                cmakeConfig.productFile?.absolutePath?.let { targetPath ->
                    UTBotTarget(targetPath, project)
                }
            }
        }.flatten()

    fun createToolWindow(): JComponent {
        val toolWindowPanel = SimpleToolWindowPanel(true, true)
        val panel = JBScrollPane()

        uiList.cellRenderer = Renderer()
        uiList.addListSelectionListener {
            projectSettings.targetPath = listModel.getElementAt(uiList.selectedIndex).targetAbsolutePath
            uiList.updateUI()
        }
        panel.setViewportView(uiList)
        toolWindowPanel.setContent(panel)

        connectToEvents()
        return toolWindowPanel
    }

    private fun addTargetPathIfNotPresent(possiblyNewTargetPath: String) {
        listModel.apply {
            toList().find { utbotTarget -> utbotTarget.targetAbsolutePath == possiblyNewTargetPath } ?: add(
                UTBotTarget(possiblyNewTargetPath, project)
            )
        }
    }

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

    private fun UTBotTarget.isChosen() = this.targetAbsolutePath == projectSettings.targetPath

    private inner class Renderer : ColoredListCellRenderer<UTBotTarget>() {
        override fun customizeCellRenderer(
            list: JList<out UTBotTarget>,
            target: UTBotTarget,
            index: Int,
            selected: Boolean,
            hasFocus: Boolean
        ) {
            icon = if (target.isChosen()) {
                uiList.selectedIndex = index
                AllIcons.Icons.Ide.MenuArrowSelected;
            } else {
                null
            }
            append(target.name, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
            append(" ")
            append(target.description, SimpleTextAttributes.GRAYED_ATTRIBUTES);
        }
    }
}
