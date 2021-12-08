package com.github.vol0n.utbotcppclion.services

import com.github.vol0n.utbotcppclion.UTBot
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.CollectionListModel
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.layout.panel
import javax.swing.JList
import java.awt.Dimension

/**
 * Get UTBot project settings and generation settings from settings menu.
 */
class UTBotConfigurable(private val targetProject: Project) : BoundConfigurable(
    "Project Settings for Generating Tests"
) {
    private val projectSettings: ProjectSettings get() = targetProject.service()
    private val generatorSettings: GeneratorSettings get() = targetProject.service()
    private val logger = Logger.getInstance("ProjectConfigurable")
    private val sourcePathListModel =
        CollectionListModel(*targetProject.getService(ProjectSettings::class.java).state.sourcePaths.toTypedArray())
    private val onApplyCallBacks = mutableListOf<() -> Unit>()
    private val onResetCallBacks = mutableListOf<() -> Unit>()

    override fun createPanel(): DialogPanel {
        logger.info("createPanel was called")
        return panel {
            row {
                label(UTBot.message("settings.project.buildDir"))
                textFieldWithBrowseButton(
                    projectSettings::buildDirPath,
                    UTBot.message("settings.project.buildDir.browse.title"),
                    targetProject, FileChooserDescriptorFactory.createSingleFileDescriptor()
                ) { folder ->
                    projectSettings.buildDirPath = folder.path
                    folder.path
                }.component.apply {
                    onApplyCallBacks.add { projectSettings.buildDirPath = this.text }
                    onResetCallBacks.add { this.text = projectSettings.buildDirPath }
                }
            }
            row {
                label(UTBot.message("settings.project.target"))
                textFieldWithBrowseButton(
                    projectSettings::targetPath,
                    UTBot.message("settings.project.target.browse.title"),
                    project = targetProject,
                    fileChooserDescriptor = FileChooserDescriptorFactory.createSingleFileDescriptor()
                ) { file ->
                    projectSettings.targetPath = file.path
                    file.path
                }.component.apply {
                    onApplyCallBacks.add { projectSettings.targetPath = this.text }
                    onResetCallBacks.add { this.text = projectSettings.targetPath }
                }
            }
            row {
                label(UTBot.message("settings.project.testsDir"))
                textFieldWithBrowseButton(
                    projectSettings::testDirPath,
                    UTBot.message("settings.project.testsDir.browse.title"),
                    project = targetProject,
                    fileChooserDescriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
                ) { folder ->
                    projectSettings.testDirPath = folder.path
                    folder.path
                }.component.apply {
                    onApplyCallBacks.add { projectSettings.testDirPath = this.text }
                    onResetCallBacks.add { this.text = projectSettings.testDirPath }
                }
            }
            val checkBoxDs = mapOf(
                UTBot.message("settings.generation.synchronize") to projectSettings::synchronizeCode,
                UTBot.message("settings.generation.stubs") to generatorSettings::useStubs,
                UTBot.message("settings.generation.verbose") to generatorSettings::verbose,
                UTBot.message("settings.generation.searcher") to generatorSettings::useDeterministicSearcher,
                UTBot.message("settings.generation.static") to generatorSettings::generateForStaticFunctions
            )
            checkBoxDs.forEach { message, boolProperty ->
                row {
                    val cb = checkBox(message, boolProperty)
                    cb.component.addItemListener {
                        boolProperty.set(!boolProperty.get())
                    }
                }
            }
            val intFields = mapOf(
                UTBot.message("settings.generation.timeoutFunction") to generatorSettings::timeoutPerFunction,
                UTBot.message("settings.generation.timeoutTest") to generatorSettings::timeoutPerTest
            )
            intFields.forEach { (message, intProperty) ->
                row(message) {
                    intTextField(intProperty).component.apply {
                        onApplyCallBacks.add { intProperty.set(this.text.toInt()) }
                        onResetCallBacks.add { this.text = intProperty.get().toString() }
                    }
                }
            }
            row(UTBot.message("settings.project.sourcePaths")) {
                component(createSourcesListComponent())
            }
            row(UTBot.message("settings.project.remotePath")) {
                textField(projectSettings::remotePath).component.apply {
                    onApplyCallBacks.add { projectSettings.remotePath = this.text }
                    onResetCallBacks.add { this.text = projectSettings.remotePath }
                }
            }
        }
    }

    private fun createSourcesListComponent() =
        ToolbarDecorator.createDecorator(JList(sourcePathListModel))
            .setAddAction { actionBtn ->
                FileChooser.chooseFiles(
                    FileChooserDescriptorFactory.createMultipleFoldersDescriptor(), targetProject, null
                ) { files ->
                    sourcePathListModel.add(files.map { it.path })
                }
            }.setRemoveAction { actionBtn ->
                sourcePathListModel.remove((actionBtn.contextComponent as JList<String>).selectedIndex)
            }.setPreferredSize(Dimension(500, 200))
            .createPanel()

    override fun isModified(): Boolean {
        return super.isModified() || (sourcePathListModel.toList() != projectSettings.sourcePaths)
    }

    override fun apply() {
        logger.info("apply was called")
        onApplyCallBacks.forEach { it() }
        projectSettings.sourcePaths = sourcePathListModel.toList()
    }

    override fun reset() {
        logger.info("reset was called")
        onResetCallBacks.forEach { it() }
        sourcePathListModel.also {
            it.removeAll()
            it.addAll(0, projectSettings.sourcePaths)
        }
    }
}