package edu.cs.skku.autosen_server

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import javax.servlet.ServletRequest

class IP {
    companion object {
        fun getIpAddress(): String {
            val url = URL("http://checkip.amazonaws.com")
            val br = BufferedReader(InputStreamReader(url.openStream()))
            val ip = br.readLine()

            if (ip != null) return ip
            else return ""
        }
    }
}