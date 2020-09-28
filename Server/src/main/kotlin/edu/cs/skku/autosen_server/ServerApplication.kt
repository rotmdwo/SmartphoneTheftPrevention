package edu.cs.skku.autosen_server

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import java.io.BufferedOutputStream
import java.io.DataOutputStream
import java.io.FileOutputStream

@SpringBootApplication
open class ServerApplication

fun main() {
    //print(getIpAddress())
    runApplication<ServerApplication>()
}