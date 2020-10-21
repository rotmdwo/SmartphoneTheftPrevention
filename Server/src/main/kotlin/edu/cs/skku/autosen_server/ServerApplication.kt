package edu.cs.skku.autosen_server

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import java.io.*
import java.lang.StringBuilder
import kotlin.concurrent.timer

@SpringBootApplication
open class ServerApplication

fun main() {
    runApplication<ServerApplication>()
}