package com.github.vol0n.utbotcppclion.messaging

import com.github.vol0n.utbotcppclion.services.ProjectSettings
import com.intellij.util.messages.Topic

fun interface UTBotSettingsChangedListener {
    companion object {
        val TOPIC = Topic.create(
            "UTBot settings changed",
            UTBotSettingsChangedListener::class.java
        )
    }

    fun settingsChanged(settings: ProjectSettings)
}
