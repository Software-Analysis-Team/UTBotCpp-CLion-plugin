package com.github.vol0n.utbotcppclion.messaging

import com.intellij.util.messages.Topic

enum class ConnectionStatus(val description: String) {
    CONNECTED("connected"),
    BROKEN("not connected"),
    INIT("not connected")
}

interface UTBotConnectionChangedNotifier {
    companion object {
        val CONNECTION_CHANGED_TOPIC = Topic.create(
            "Connection to UTBot server changed",
            UTBotConnectionChangedNotifier::class.java
        )
    }

    fun onChange(oldStatus: ConnectionStatus, newStatus: ConnectionStatus)
}