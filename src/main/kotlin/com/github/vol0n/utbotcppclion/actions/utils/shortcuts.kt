package com.github.vol0n.utbotcppclion.actions.utils

import com.github.vol0n.utbotcppclion.services.GeneratorSettings
import com.intellij.openapi.components.service

val client
    get() = service<GeneratorSettings>().client

val coroutinesScopeForGrpc
    get() = service<GeneratorSettings>().grpcCoroutineScope
