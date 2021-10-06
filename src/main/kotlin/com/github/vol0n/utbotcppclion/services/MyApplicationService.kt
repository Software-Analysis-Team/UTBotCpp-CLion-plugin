package com.github.vol0n.utbotcppclion.services

import com.github.vol0n.utbotcppclion.client.GrpcStarter

class MyApplicationService {
    val client = GrpcStarter.startClient()
}
