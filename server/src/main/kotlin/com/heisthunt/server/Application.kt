package com.heisthunt.server

import com.heisthunt.server.database.DatabaseFactory
import com.heisthunt.server.plugins.*
import io.ktor.server.application.*
import io.ktor.server.netty.*

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    val config = environment.config

    DatabaseFactory.init(config)
    configureSerialization()
    configureSecurity(config)
    configureCors()
    configureMonitoring()
    configureStatusPages()
    configureWebSockets()
    configureRouting()
}
