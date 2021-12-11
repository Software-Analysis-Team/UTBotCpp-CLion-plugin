package com.github.vol0n.utbotcppclion.ui


import com.intellij.icons.AllIcons
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.CollectionListModel
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane

import javax.swing.JList


class UTBotTargetsToolWindow(
    listModel: CollectionListModel<UTBotTarget>,
    val controller: UTBotTargetsController
): SimpleToolWindowPanel(true, true) {
    private val uiList = JBList(listModel)

    init {
        val panel = JBScrollPane()

        uiList.cellRenderer = Renderer()
        uiList.addListSelectionListener {
            controller.selectionChanged(uiList.selectedIndex)
            uiList.updateUI()
        }
        panel.setViewportView(uiList)
        setContent(panel)
    }

    private inner class Renderer : ColoredListCellRenderer<UTBotTarget>() {
        override fun customizeCellRenderer(
            list: JList<out UTBotTarget>,
            target: UTBotTarget,
            index: Int,
            selected: Boolean,
            hasFocus: Boolean
        ) {
            icon = if (target.targetAbsolutePath == controller.getCurrentTargetPath()) {
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
