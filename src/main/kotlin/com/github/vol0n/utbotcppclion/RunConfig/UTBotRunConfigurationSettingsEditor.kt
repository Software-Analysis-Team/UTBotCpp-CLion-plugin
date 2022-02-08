package com.github.vol0n.utbotcppclion.RunConfig

import com.intellij.openapi.options.SettingsEditor
import com.intellij.ui.layout.panel
import javax.swing.JComponent
import javax.swing.JPanel

class UTBotRunConfigurationSettingsEditor : SettingsEditor<UTBotRunWithCoverageConfig>() {
    var pathToTestsFile: String = ""
    private val myPanel: JPanel = panel {
        row {
            label("Tests file")
            textFieldWithBrowseButton(this@UTBotRunConfigurationSettingsEditor::pathToTestsFile)
        }
    }

    override fun resetEditorFrom(conf: UTBotRunWithCoverageConfig) {
        pathToTestsFile = conf.pathToTestsFile ?: ""
    }

    override fun applyEditorTo(conf: UTBotRunWithCoverageConfig) {
        conf.pathToTestsFile = pathToTestsFile
    }

    override fun createEditor(): JComponent {
        return myPanel
    }
}
