package com.github.vol0n.utbotcppclion.services

import com.github.vol0n.utbotcppclion.client.Client
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity

class UTBotStartupActivity: StartupActivity {
    override fun runActivity(project: Project) {
        project.service<Client>()
    }
}