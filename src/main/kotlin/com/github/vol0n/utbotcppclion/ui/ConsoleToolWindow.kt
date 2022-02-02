package com.github.vol0n.utbotcppclion.ui

import com.github.vol0n.utbotcppclion.messaging.ConnectionStatus
import com.github.vol0n.utbotcppclion.messaging.UTBotEventsListener
import com.github.vol0n.utbotcppclion.services.Client
import com.github.vol0n.utbotcppclion.services.LogLevel
import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBTabbedPane
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.components.BorderLayoutPanel
import javax.swing.JPanel
import java.awt.BorderLayout
import java.awt.Component
import java.awt.GridLayout
import java.awt.Insets
import java.awt.event.ItemEvent
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
        val content =
            contentManager.factory.createContent(ConsoleToolWindow(project), null, false)
        toolWindow.contentManager.addContent(content)
    }
}

class UTBotConsole(project: Project) : ConsoleViewImpl(project, true) {
    fun info(message: String) = print(message, ConsoleViewContentType.NORMAL_OUTPUT)
    fun error(message: String) = print(message, ConsoleViewContentType.LOG_ERROR_OUTPUT)

    fun println(message: String, type: ConsoleViewContentType) {
        print(message, type)
        print("\n", ConsoleViewContentType.NORMAL_OUTPUT)
    }
}

enum class OutputType(val id: String) {
    SERVER_LOG("Server log"), CLIENT_LOG("Client log"), GTEST("GTest log")
}

class OutputWindowProvider private constructor(project: Project) {
    val outputs: Map<OutputType, UTBotConsole>
    val outputUI: Map<OutputType, Component>

    data class OutputChannel(val uiComponent: Component, val output: UTBotConsole)

    init {
        cache[project] = this
        val outputChannels = OutputType.values().map { type -> type to createOutputChannel(type, project) }
        outputUI = outputChannels.associate { it.first to it.second.uiComponent }
        outputs = outputChannels.associate { it.first to it.second.output }
    }

    private fun createServerLogOutputWindow(project: Project): OutputChannel {
        val toolWindowPanel = SimpleToolWindowPanel(true, true)
        val console = UTBotConsole(project)
        toolWindowPanel.setContent(console.component)
        toolWindowPanel.toolbar = BorderLayoutPanel().apply {
            border = JBUI.Borders.empty()
            withPreferredHeight(JBUI.scale(30))
            withMinimumHeight(JBUI.scale(30))
            add(JPanel().apply {
                layout = GridLayout(1, components.size)
                add(
                    ComboBox(LogLevel.values().map { it.id }.toTypedArray()).apply {

                        project.messageBus.connect().subscribe(UTBotEventsListener.CONNECTION_CHANGED_TOPIC,
                            object : UTBotEventsListener {
                                override fun onConnectionChange(
                                    oldStatus: ConnectionStatus,
                                    newStatus: ConnectionStatus
                                ) {
                                    isEnabled = newStatus == ConnectionStatus.CONNECTED
                                }
                            })

                        addItemListener { itemEvent ->
                            if (itemEvent.stateChange == ItemEvent.SELECTED) {
                                project.service<Client>().setLoggingLevel(
                                    LogLevel.values().find { it.id == (itemEvent.item as String) }!!
                                )
                            }
                        }
                    },
                )
            }, BorderLayout.WEST)
        }
        return OutputChannel(toolWindowPanel, console)
    }

    private fun createOutputChannel(type: OutputType, project: Project): OutputChannel {
        return when (type) {
            OutputType.SERVER_LOG -> createServerLogOutputWindow(project)
            else -> {
                UTBotConsole(project).let {
                    OutputChannel(it.component, it)
                }
            }
        }
    }

    companion object {
        private val cache: MutableMap<Project, OutputWindowProvider> = mutableMapOf()
        fun getInstance(project: Project): OutputWindowProvider {
            if (!cache.contains(project))
                cache[project] = OutputWindowProvider(project)
            return cache[project]!!
        }

        fun getOutput(project: Project, outputType: OutputType): UTBotConsole =
            getInstance(project).outputs[outputType]!!
    }
}

class ConsoleToolWindow(val project: Project) : SimpleToolWindowPanel(true, true) {
    private var mainUI: JBTabbedPane = JBTabbedPane()

    init {
        mainUI.tabComponentInsets = Insets(0, 0, 0, 0)

        val factory = OutputWindowProvider.getInstance(project)
        for (type in OutputType.values()) {
            mainUI.addTab(type.id, factory.outputUI[type])
        }

        setContent(mainUI)
    }
}
