package com.github.vol0n.utbotcppclion.ui

import com.github.vol0n.utbotcppclion.services.Client
import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.components.BorderLayoutPanel
import javax.swing.JComponent
import javax.swing.JPanel
import java.awt.BorderLayout
import java.awt.GridLayout
import java.awt.event.ItemEvent
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*


class ConsoleToolWindowProvider : ToolWindowFactory {
    private val logger = Logger.getInstance(this::class.java)

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        logger.info("createToolWindowContent was called")
        val contentManager = toolWindow.contentManager
        val client = project.service<Client>()
        val content =
            contentManager.factory.createContent(ConsoleToolWindow(client.getOutputConsoles()), null, false)
        toolWindow.contentManager.addContent(content)
    }
}

class UTBotConsole(name: String, project: Project) : ConsoleViewImpl(project, true) {
    private val formatter = DateTimeFormatter.ofLocalizedDateTime( FormatStyle.SHORT )
        .withLocale( Locale.UK )
        .withZone( ZoneId.systemDefault() );
    init {
        this.s
        setName(name)
    }

    fun info(message: String) = println(message, ConsoleViewContentType.NORMAL_OUTPUT)
    fun error(message: String) = println(message, ConsoleViewContentType.LOG_ERROR_OUTPUT)

    fun println(message: String, type: ConsoleViewContentType) {
        val time = "${formatter.format(Instant.now())} : "
        print(time + message, type)
        print("\n", ConsoleViewContentType.NORMAL_OUTPUT)
    }

}

class ConsoleToolWindow(private val consoles: List<UTBotConsole>) : SimpleToolWindowPanel(true, true) {
    val consoleNames = CollectionComboBoxModel(consoles.map { it.name })
    private var currentConsole = consoles[0]

    private fun updateConsole() = setContent(currentConsole.component)

    init {
        updateConsole()
        toolbar = BorderLayoutPanel().apply {
            border = JBUI.Borders.empty()
            withPreferredHeight(JBUI.scale(30))
            withMinimumHeight(JBUI.scale(30))
            addOnLeft(
                horizontalFlow(
                    ComboBox(consoleNames).apply {
                        addItemListener { itemEvent ->
                            if (itemEvent.stateChange == ItemEvent.SELECTED) {
                                currentConsole =
                                    consoles.find { it.name == (itemEvent.item as String) } ?: return@addItemListener
                                updateConsole()
                            }
                        }
                    },
                ))
        }
    }

    private fun <C : JComponent> JPanel.addOnLeft(component: C): C {
        add(component, BorderLayout.WEST)
        return component
    }

    private fun horizontalFlow(vararg components: JComponent): JComponent {
        return JPanel().apply {
            layout = GridLayout(1, components.size)
            components.forEach(::add)
        }
    }
}
