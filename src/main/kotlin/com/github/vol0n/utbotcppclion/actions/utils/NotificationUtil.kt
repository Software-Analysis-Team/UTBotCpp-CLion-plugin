package com.github.vol0n.utbotcppclion.actions.utils

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project

fun notifyError(content: String, project: Project? = null) {
    NotificationGroupManager.getInstance().getNotificationGroup("Custom Notification Group")
        .createNotification(content, NotificationType.ERROR)
        .notify(project)
}
