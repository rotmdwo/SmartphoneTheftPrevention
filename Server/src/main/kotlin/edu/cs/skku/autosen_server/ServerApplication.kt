package edu.cs.skku.autosen_server

import edu.cs.skku.autosen_server.train.TrainProcess.Companion.process
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import java.io.*
import java.lang.StringBuilder
import kotlin.concurrent.timer

@SpringBootApplication
open class ServerApplication

fun main() {
    process()
    runApplication<ServerApplication>()
}