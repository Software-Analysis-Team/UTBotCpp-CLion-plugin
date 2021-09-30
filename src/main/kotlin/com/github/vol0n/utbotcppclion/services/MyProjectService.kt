package com.github.vol0n.utbotcppclion.services

import com.intellij.openapi.project.Project
import com.github.vol0n.utbotcppclion.MyBundle

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
