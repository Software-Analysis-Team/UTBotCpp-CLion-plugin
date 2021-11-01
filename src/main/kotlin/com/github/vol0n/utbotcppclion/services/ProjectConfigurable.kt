package com.github.vol0n.utbotcppclion.services

import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.observable.properties.GraphPropertyImpl.Companion.graphProperty
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.CollectionListModel
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.layout.panel
import javax.swing.JList
import java.awt.Dimension

/**
 * Get project related info from settings menu.
 */
class ProjectConfigurable(private val targetProject: Project) : BoundConfigurable(
    "Project Settings for Generating Tests"
) {
    private val graphProperty = PropertyGraph()
    private val settingsState = targetProject.getService(ProjectSettings::class.java).state
    private val buildDirPath = graphProperty.graphProperty { settingsState.buildDirPath }
    private val targetPath = graphProperty.graphProperty { settingsState.targetPath }
    private val testsDirPath = graphProperty.graphProperty { settingsState.testDirPath }
    private val synchronizeCode = graphProperty.graphProperty { settingsState.synchronizeCode }
    private val sourcePathListModel =
        CollectionListModel(*targetProject.getService(ProjectSettings::class.java).state.sourcePaths.toTypedArray())

    override fun createPanel(): DialogPanel {
        return panel {
            row {
                label("Build directory: ")
                textFieldWithBrowseButton(
                    buildDirPath,
                    project = targetProject,
                    fileChooserDescriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
                ) { folder ->
                    folder.path
                }
            }
            row {
                label("Target path: ")
                textFieldWithBrowseButton(
                    targetPath,
                    project = targetProject,
                    fileChooserDescriptor = FileChooserDescriptorFactory.createSingleFileDescriptor()
                ) { file ->
                    file.path
                }
            }
            row {
                label("Tests directory: ")
                textFieldWithBrowseButton(
                    testsDirPath,
                    project = targetProject,
                    fileChooserDescriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
                ) { folder ->
                    folder.path
                }
            }
            row {
                checkBox("Synchronize code", synchronizeCode)
            }
            row {
                label("Source paths: ")
            }
            row {
                component(
                    ToolbarDecorator.createDecorator(JList(sourcePathListModel))
                        .setAddAction {
                            FileChooser.chooseFile(
                                FileChooserDescriptorFactory.createMultipleFilesNoJarsDescriptor(), targetProject, null
                            ) {
                                sourcePathListModel.add(it.path)
                            }
                        }.setRemoveAction { actionBtn ->
                            sourcePathListModel.remove((actionBtn.contextComponent as JList<String>).selectedIndex)
                        }.setPreferredSize(Dimension(500, 200))
                        .createPanel()
                )
            }

        }
    }

    override fun isModified(): Boolean {
        return super.isModified() || (targetPath.get() != settingsState.targetPath)
                || (buildDirPath.get() != settingsState.buildDirPath)
                || (testsDirPath.get() != settingsState.testDirPath)
                || (synchronizeCode.get() != settingsState.synchronizeCode)
                || (sourcePathListModel.toList() != settingsState.sourcePaths)
    }

    override fun apply() {
        super.apply()
        settingsState.targetPath = targetPath.get()
        settingsState.buildDirPath = buildDirPath.get()
        settingsState.testDirPath = testsDirPath.get()
        settingsState.synchronizeCode = synchronizeCode.get()
        settingsState.sourcePaths = sourcePathListModel.toList()
    }

    override fun reset() {
        super.reset()
        targetPath.set(settingsState.targetPath)
        buildDirPath.set(settingsState.buildDirPath)
        testsDirPath.set(settingsState.testDirPath)
        synchronizeCode.set(settingsState.synchronizeCode)
        sourcePathListModel.also {
            it.removeAll()
            it.addAll(0, settingsState.sourcePaths)
        }
    }
}
