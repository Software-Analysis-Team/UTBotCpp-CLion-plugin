package com.github.vol0n.utbotcppclion.actions

import com.github.vol0n.utbotcppclion.grpcBuildMessages.buildFunctionRequest
import com.github.vol0n.utbotcppclion.services.GenerateTestsSettings
import com.github.vol0n.utbotcppclion.services.ProjectSettings
import com.github.vol0n.utbotcppclion.utils.relativize
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.ComponentValidator
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.components.fields.ExtendableTextField
import javax.swing.ListSelectionModel
import javax.swing.event.DocumentEvent
import kotlinx.coroutines.runBlocking
import testsgen.Util.ValidationType
import java.awt.Dimension
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.math.BigInteger
import java.util.function.Supplier
import com.jetbrains.cidr.*

class GenerateForPredicateAction: AnAction() {
    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        val psiFile = e.getData(CommonDataKeys.PSI_FILE)
        if (editor == null || psiFile == null) {
            e.presentation.isEnabled = false
            return
        }
        val offset = editor.caretModel.offset
        val element = psiFile.findElementAt(offset)
        if (element == null) {
            e.presentation.isEnabled = false
            return
        }
         val containingFun = PsiTreeUtil.getParentOfType(element, OCFunctionDefinition::class.java)
         // println(containingFun?.name)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        val project = e.getData(CommonDataKeys.PROJECT)
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        val lineNumber = editor!!.caretModel.logicalPosition.line
        val client = ApplicationManager.getApplication().getService(GenerateTestsSettings::class.java).client
        val projectSettings = project!!.getService(ProjectSettings::class.java) ?: return
        runBlocking {
            val type = client.getFunctionReturnType(
                buildFunctionRequest(project, projectSettings, lineNumber,
                relativize(project.basePath!!, file!!.path)
                )
            )
            getPredicate(type.validationType, e)
        }
    }

    private fun getReturnValue(validationType: ValidationType, predicate: String, e: AnActionEvent) {
        val textfield = ExtendableTextField()
        textfield.minimumSize = Dimension(100, textfield.width)
        textfield.text = defaultReturnValues[validationType]
        textfield.selectAll()
        val popup = JBPopupFactory.getInstance().createComponentPopupBuilder(textfield, null)
            .setFocusable(true)
            .setRequestFocus(true)
            .setTitle("Specify Return Value of type ${validationTypeName[validationType]}")
            .createPopup()

        var canClosePopup = false
        ComponentValidator(popup).withValidator(Supplier<ValidationInfo?> {
            val validationResult = returnValueValidators[validationType]?.let { it(textfield.text) }
            if (validationResult == null) {
                canClosePopup = true
                null
            } else {
                canClosePopup = false
                ValidationInfo(validationResult, textfield)
            }
        }).installOn(textfield)

        textfield.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(p0: DocumentEvent) {
                ComponentValidator.getInstance(textfield).ifPresent { v ->
                    v.revalidate()
                }
            }
        })

        textfield.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_ENTER) {
                    if (canClosePopup) {
                        popup.cancel()
                    }
                }
            }
        })
        popup.showInBestPositionFor(e.dataContext)
    }

    private fun getPredicate(validationType: ValidationType, e: AnActionEvent) {
        val possiblePredicates = if (validationType == ValidationType.STRING) {
            listOf("==")
        } else {
            listOf("==", "<=", "=>", "<", ">")
        }
        val popup = JBPopupFactory.getInstance().createPopupChooserBuilder(possiblePredicates)
            .setItemChosenCallback { predicate ->
                getReturnValue(validationType, predicate, e)
            }
            //.setItemSelectedCallback { println("ItemSelectedCallback was called!") }
            .setResizable(false)
            .setMovable(false)
            .setTitle("Select Predicate")
            .setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
            .setCloseOnEnter(true)
            .createPopup()
        popup.showInBestPositionFor(e.dataContext)
    }

    companion object {
        val defaultReturnValues = mapOf(
            ValidationType.INT8_T to "0",
            ValidationType.INT16_T to "0",
            ValidationType.INT32_T to "0",
            ValidationType.INT64_T to "0",
            ValidationType.UINT8_T to "0",
            ValidationType.UINT16_T to "0",
            ValidationType.UINT32_T to "0",
            ValidationType.UINT64_T to "0",
            ValidationType.CHAR to "a",
            ValidationType.FLOAT to "1.0",
            ValidationType.STRING to "default str",
        )

        private fun isIntegerInBounds(value: String, low: BigInteger?, high: BigInteger?): Boolean {
           if (low == null || high == null) {
               return false
           }
            return value.toBigInteger() in low..high
        }

        private fun intBoundsBySize(size: Int, signed: Boolean): Pair<BigInteger, BigInteger> {
            if (!signed) {
                return Pair((0).toBigInteger(), (2).toBigInteger().pow(size).dec())
            }
            return Pair((2).toBigInteger().pow(size-1).unaryMinus(), (2).toBigInteger().pow(size-1).dec())
        }

        private val validationTypeName = mapOf(
            ValidationType.INT8_T to "int8_t",
            ValidationType.INT16_T to "int16_t",
            ValidationType.INT32_T to "int32_t",
            ValidationType.INT64_T to "int64_t",
            ValidationType.UINT8_T to "uint8_t",
            ValidationType.UINT16_T to "uint16_t",
            ValidationType.UINT32_T to "uint32_t",
            ValidationType.UINT64_T to "uint64_t"
        )

        private val integerBounds = mapOf(
            ValidationType.INT8_T to intBoundsBySize(8, false),
            ValidationType.INT16_T to intBoundsBySize(16, false),
            ValidationType.INT32_T to intBoundsBySize(32, false),
            ValidationType.INT64_T to intBoundsBySize(64, false),
            ValidationType.UINT8_T to intBoundsBySize(8, true),
            ValidationType.UINT16_T to intBoundsBySize(16, true),
            ValidationType.UINT32_T to intBoundsBySize(32, true),
            ValidationType.UINT64_T to intBoundsBySize(64, true)
        )

        private fun intValidationFunc(validationType: ValidationType): (String) -> String?  {
            return fun(value: String): String? {
                println("In In validation func ${validationTypeName[validationType]}")
                return if ("""^-?(([1-9][0-9]*)|0)$""".toRegex().matches(value)) {
                    if (isIntegerInBounds(value, integerBounds[validationType]?.first, integerBounds[validationType]?.second)) {
                        null
                    } else {
                        "Value does not fit into C  ${validationTypeName[validationType]} type"
                    }
                } else {
                    "Value is not an integer"
                }
            }
        }

        val returnValueValidators = mapOf(
            ValidationType.INT8_T to intValidationFunc(ValidationType.INT8_T),
            ValidationType.INT16_T to intValidationFunc(ValidationType.INT16_T),
            ValidationType.INT32_T to intValidationFunc(ValidationType.INT32_T),
            ValidationType.INT64_T to intValidationFunc(ValidationType.INT64_T),
            ValidationType.UINT8_T to intValidationFunc(ValidationType.UINT8_T),
            ValidationType.UINT16_T to intValidationFunc(ValidationType.UINT16_T),
            ValidationType.UINT32_T to intValidationFunc(ValidationType.UINT32_T),
            ValidationType.UINT64_T to intValidationFunc(ValidationType.UINT64_T),
            ValidationType.CHAR to fun(value: String): String? {
            if (value.length == 1) {
                return null
            } else {
                val escapeSequences = listOf(
                        "\\\'",
                        "\"",
                        "\\?",
                        "\\\\",
                        "\\a",
                        "\\b",
                        "\\f",
                        "\\n",
                        "\\r",
                        "\\t",
                        "\\v"
                )
                return if (!escapeSequences.contains(value)) {
                    "Value is not a character"
                } else {
                    null
                }
            }
        },
        ValidationType.FLOAT to fun(value: String): String? {
            return if ("""^-?([1-9][0-9]*)[.]([0-9]*)$""".toRegex().matches(value)) {
                if (value.length < 15) {
                    null
                } else {
                    "Value does not fit into C float type."
                }
            } else {
                "Value is not floating-point"
            }
        },
        ValidationType.STRING to fun(value: String): String? {
            return if (value.length > 32) {
                "String is too long"
            } else {
                null
            }
        })
    }
}

