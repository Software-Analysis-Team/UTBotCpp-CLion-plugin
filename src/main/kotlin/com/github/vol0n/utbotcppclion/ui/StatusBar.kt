package com.github.vol0n.utbotcppclion.ui

import com.github.vol0n.utbotcppclion.actions.AskServerToGenerateJson
import com.github.vol0n.utbotcppclion.actions.ConfigureProjectAction
import com.github.vol0n.utbotcppclion.actions.HeartBeatAction
import com.github.vol0n.utbotcppclion.messaging.ConnectionStatus
import com.github.vol0n.utbotcppclion.messaging.UTBotConnectionChangedNotifier
import com.github.vol0n.utbotcppclion.services.Client
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.ListPopup
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.Consumer
import java.awt.Component
import java.awt.Point
import java.awt.event.MouseEvent

const val STATUS_BAR_ID = "UTBot plugin"
const val ID = "UTBot.Status"
const val STATUS_BAR_DISPLAY_NAME = "UTBot plugin"

class StatusBar : StatusBarWidgetFactory {
    override fun getId() = STATUS_BAR_ID

    override fun getDisplayName() = STATUS_BAR_DISPLAY_NAME

    override fun isAvailable(p0: Project): Boolean = true

    override fun createWidget(p0: Project): StatusBarWidget {
        return UTBotStatusBar()
    }

    override fun disposeWidget(p0: StatusBarWidget) {}

    override fun canBeEnabledOn(p0: StatusBar): Boolean {
        return p0.project != null
    }
}

class UTBotStatusBar : StatusBarWidget, StatusBarWidget.TextPresentation {
    private var statusBar: StatusBar? = null

    override fun ID(): String = ID

    override fun install(statusbar: StatusBar) {
        statusBar = statusbar
        statusbar.project?.messageBus?.connect()?.subscribe(UTBotConnectionChangedNotifier.CONNECTION_CHANGED_TOPIC,
        object : UTBotConnectionChangedNotifier {
            override fun onChange(oldStatus: ConnectionStatus, newStatus: ConnectionStatus) {
                println("onChange listener in status bar was called!")
                println("statusBar: $statusBar")
                statusBar?.updateWidget(ID())
            }
        })
    }

    override fun dispose() {
        // Nothing
    }

    override fun getTooltipText() = STATUS_BAR_DISPLAY_NAME

    override fun getClickConsumer() = Consumer<MouseEvent> { event ->
        val component = event.component
        val popup = StatusBarActionsPopup.getPopup(DataManager.getInstance().getDataContext(component))
        val dimension = popup.content.preferredSize

        val at = Point(0, -dimension.height)
        popup.show(RelativePoint(component, at))
    }

    override fun getText(): String =
        "UTBot: " + (statusBar?.project?.service<Client>()?.getConnectionStatus()?.description ?: "not connected")

    override fun getAlignment(): Float = Component.CENTER_ALIGNMENT

    override fun getPresentation(): StatusBarWidget.WidgetPresentation = this
}

object StatusBarActionsPopup {
    fun getPopup(dataContext: DataContext): ListPopup {
        val actions = getActions()
        val popup = JBPopupFactory.getInstance()
            .createActionGroupPopup(
                STATUS_BAR_DISPLAY_NAME, actions,
                dataContext, JBPopupFactory.ActionSelectionAid.SPEEDSEARCH, false,
                ActionPlaces.POPUP
            )

        return popup
    }

    fun getActions(): DefaultActionGroup {
        val actionGroup = DefaultActionGroup()
        actionGroup.isPopup = true

        actionGroup.add(ConfigureProjectAction())
        actionGroup.addSeparator()
        actionGroup.add(HeartBeatAction())
        actionGroup.addSeparator()
        actionGroup.add(AskServerToGenerateJson())
        return actionGroup
    }
}
