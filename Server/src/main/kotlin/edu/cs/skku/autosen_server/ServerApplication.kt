package edu.cs.skku.autosen_server

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
open class ServerApplication

fun main() {
    runApplication<ServerApplication>()
}