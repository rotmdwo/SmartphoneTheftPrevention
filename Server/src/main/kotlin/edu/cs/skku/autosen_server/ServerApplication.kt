package edu.cs.skku.autosen_server

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import java.io.*
import java.lang.StringBuilder

@SpringBootApplication
open class ServerApplication

fun main() {
    //print(getIpAddress())
    runApplication<ServerApplication>()
}