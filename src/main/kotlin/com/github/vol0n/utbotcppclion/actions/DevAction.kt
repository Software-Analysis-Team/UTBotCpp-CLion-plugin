package com.github.vol0n.utbotcppclion.actions

import com.github.vol0n.utbotcppclion.actions.utils.client
import com.intellij.coverage.LineMarkerRendererWithErrorStripe
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.LineMarkerRendererEx
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.awt.Color
import java.awt.Graphics
import java.awt.Rectangle

// For development only
internal class DevAction: AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        /*
        val editor = e.getRequiredData(CommonDataKeys.EDITOR)
        // how to show coverage
        val hl = editor.markupModel.addLineHighlighter(null,
            0, HighlighterLayer.LAST
        )
        hl.lineMarkerRenderer = DiffCoverageLineMarkerRenderer(Color.GREEN)
        e.client.grpcCoroutineScope.launch {
            delay(1000L)
            editor.markupModel.removeAllHighlighters()
        }
         */
        e.client.doHandShake()
    }
}

class DiffCoverageLineMarkerRenderer(val color: Color) : LineMarkerRendererWithErrorStripe {

    override fun paint(editor: Editor, g: Graphics, r: Rectangle) {
        g.color = color
        g.fillRect(r.x, r.y, r.width, +r.height)
    }

    override fun getPosition(): LineMarkerRendererEx.Position {
        return LineMarkerRendererEx.Position.LEFT
    }

    override fun getErrorStripeColor(editor: Editor?): Color {
        return Color.CYAN
    }
}
