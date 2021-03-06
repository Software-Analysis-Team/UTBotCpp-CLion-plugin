package com.github.vol0n.utbotcppclion.actions

import com.github.vol0n.utbotcppclion.actions.utils.client
import com.github.vol0n.utbotcppclion.actions.utils.getContainingFunction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.ComponentValidator
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.components.fields.ExtendableTextField
import javax.swing.ListSelectionModel
import javax.swing.event.DocumentEvent
import testsgen.Util.ValidationType
import java.awt.Dimension
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.math.BigInteger
import java.util.function.Supplier
import kotlinx.coroutines.launch

class GenerateForPredicateAction : GenerateTestsBaseAction() {
    override fun updateIfServerAvailable(e: AnActionEvent) {
        val containingFun = getContainingFunction(e)
        e.presentation.isEnabledAndVisible = (containingFun != null)
    }

    fun createListPopup(title: String, list: List<String>, onChoose: (String) -> Unit): JBPopup {
        return JBPopupFactory.getInstance().createPopupChooserBuilder(list)
            .setResizable(false)
            .setMovable(false)
            .setTitle(title)
            .setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
            .setCloseOnEnter(true)
            .setItemChosenCallback(onChoose)
            .createPopup()
    }

    fun createTrueFalsePopup(onChoose: (String) -> Unit) = createListPopup("Select bool value",
        listOf("true", "false")) { onChoose(it) }

    fun createTextFieldPopup(type: ValidationType, onChoose: (String) -> Unit): JBPopup {
        val textField = ExtendableTextField()
        textField.minimumSize = Dimension(100, textField.width)
        textField.text = defaultReturnValues[type]
        textField.selectAll()
        val popup = JBPopupFactory.getInstance().createComponentPopupBuilder(textField, null)
            .setFocusable(true)
            .setRequestFocus(true)
            .setTitle("Specify Return Value of type ${validationTypeName[type]}")
            .createPopup()

        var canClosePopup = true
        ComponentValidator(popup).withValidator(Supplier<ValidationInfo?> {
            val validationResult = returnValueValidators[type]?.let { it(textField.text) }
            if (validationResult == null) {
                canClosePopup = true
                null
            } else {
                canClosePopup = false
                ValidationInfo(validationResult, textField)
            }
        }).installOn(textField)

        textField.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(p0: DocumentEvent) {
                ComponentValidator.getInstance(textField).ifPresent { v ->
                    v.revalidate()
                }
            }
        })

        textField.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_ENTER) {
                    if (canClosePopup) {
                        popup.cancel()
                        onChoose(textField.text)
                    }
                }
            }
        })

        return popup
    }

    override fun actionPerformed(e: AnActionEvent) {

        fun sendPredicateToServer(validationType: ValidationType, valueToCompare: String, comparisonOperator: String) {
            val predicateRequest = getPredicateRequestMessage(validationType, valueToCompare, comparisonOperator, e)
            e.client.generateForPredicate(predicateRequest)
        }

        fun chooseComparisonOperator(type: ValidationType, proceedWithComparisonOperator: (comparisonOperator: String) -> Unit) {
            when (type) {
                ValidationType.STRING, ValidationType.BOOL -> {
                    proceedWithComparisonOperator("==")
                    return
                }
                else -> {
                    createListPopup("Select Predicate", listOf("==", "<=", "=>", "<", ">")) { comparisonOperator ->
                        proceedWithComparisonOperator(comparisonOperator)
                    }.showInBestPositionFor(e.dataContext)
                }
            }
        }

        fun chooseReturnValue(type: ValidationType, proceedWithValueToCompare: (valueToCompare: String) -> Unit) {
            val popup = if (type == ValidationType.BOOL) {
                createTrueFalsePopup { returnValue -> proceedWithValueToCompare(returnValue) }
            } else {
                createTextFieldPopup(type) { returnValue -> proceedWithValueToCompare(returnValue) }
            }
            popup.showInBestPositionFor(e.dataContext)
        }

        e.client.grpcCoroutineScope.launch {
            val type = e.client.getFunctionReturnType(getFunctionRequestMessage(e)).validationType
            chooseComparisonOperator(type) { comparisonOperator ->
                chooseReturnValue(type) { valueToCompare ->
                    sendPredicateToServer(type, valueToCompare, comparisonOperator)
                }
            }
        }
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
            ValidationType.STRING to "default",
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
            return Pair((2).toBigInteger().pow(size - 1).unaryMinus(), (2).toBigInteger().pow(size - 1).dec())
        }

        private val validationTypeName = mapOf(
            ValidationType.INT8_T to "int8_t",
            ValidationType.INT16_T to "int16_t",
            ValidationType.INT32_T to "int32_t",
            ValidationType.INT64_T to "int64_t",
            ValidationType.UINT8_T to "uint8_t",
            ValidationType.UINT16_T to "uint16_t",
            ValidationType.UINT32_T to "uint32_t",
            ValidationType.UINT64_T to "uint64_t",
            ValidationType.CHAR to "char",
            ValidationType.FLOAT to "float",
            ValidationType.STRING to "string",
            ValidationType.BOOL to "bool"
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

        private fun intValidationFunc(validationType: ValidationType): (String) -> String? {
            return fun(value: String): String? {
                return if ("""^-?(([1-9][0-9]*)|0)$""".toRegex().matches(value)) {
                    if (isIntegerInBounds(
                            value,
                            integerBounds[validationType]?.first,
                            integerBounds[validationType]?.second
                        )
                    ) {
                        null
                    } else {
                        "Value does not fit into C  ${validationTypeName[validationType]} type"
                    }
                } else {
                    "Value is not an integer"
                }
            }
        }

        private fun validateChar(value: String): String? {
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
        }

        private fun validateFloat(value: String): String? {
            return if ("""^-?([1-9][0-9]*)[.]([0-9]*)$""".toRegex().matches(value)) {
                if (value.length < 15) {
                    null
                } else {
                    "Value does not fit into C float type."
                }
            } else {
                "Value is not floating-point"
            }
        }

        private fun validateString(value: String): String? {
            return if (value.length > 32) {
                "String is too long"
            } else {
                null
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
            ValidationType.CHAR to this::validateChar,
            ValidationType.FLOAT to this::validateFloat,
            ValidationType.STRING to this::validateString,
        )
    }
}
