package com.github.vol0n.utbotcppclion.ui

import com.github.vol0n.utbotcppclion.services.GenerateTestsSettings
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.layout.panel
import javax.swing.JComponent

class GenerateTestsSettingsDialog(val applicationService: GenerateTestsSettings) : DialogWrapper(true) {

    init {
        title = "Select Options for Generating Tests";
        init();
    }

    override fun createCenterPanel(): JComponent {
        return panel {
            row {
                checkBox("Use stubs", applicationService::useStubs)
            }
            row {
                checkBox("Use verbose mode", applicationService::verbose)
            }
            row {
                checkBox("Use deterministic searcher", applicationService::useDeterministicSearcher)
            }
            row {
                checkBox("Generate for static functions", applicationService::generateForStaticFunctions)
            }
            row {
                label("Timeout per function: ")
                intTextField(applicationService::timeoutPerFunction)
            }
            row {
                label("Timeout per test: ")
                intTextField(applicationService::timeoutPerTest)
            }
        }
    }
}
