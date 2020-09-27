package edu.cs.skku.autosen_server

import com.fasterxml.jackson.core.io.UTF8Writer
import edu.cs.skku.autosen_server.IP.Companion.getIpAddress
import org.apache.tomcat.util.buf.Utf8Decoder
import org.apache.tomcat.util.buf.Utf8Encoder
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import java.io.*

@SpringBootApplication
open class ServerApplication

fun main() {
    //print(getIpAddress())
    runApplication<ServerApplication>()
}