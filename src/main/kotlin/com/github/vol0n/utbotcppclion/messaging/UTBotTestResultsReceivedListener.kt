package com.github.vol0n.utbotcppclion.messaging

import com.intellij.util.messages.Topic
import testsgen.Testgen

fun interface UTBotTestResultsReceivedListener {
    companion object {
        val TOPIC = Topic.create(
            "UTBot settings changed",
            UTBotTestResultsReceivedListener::class.java
        )
    }

    fun testResultsReceived(results: List<Testgen.TestResultObject>)
}