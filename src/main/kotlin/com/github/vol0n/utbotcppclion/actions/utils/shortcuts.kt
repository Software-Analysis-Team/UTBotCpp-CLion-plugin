package com.github.vol0n.utbotcppclion.actions.utils

import com.github.vol0n.utbotcppclion.services.Client
import com.github.vol0n.utbotcppclion.services.GeneratorSettings
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

val AnActionEvent.client: Client
    get() = this.getRequiredData(CommonDataKeys.PROJECT).service()
