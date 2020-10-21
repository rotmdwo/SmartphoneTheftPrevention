package edu.cs.skku.autosen_server.utility

import java.io.*
import java.net.URL

fun writeFloatToBinaryFile(dataPath: String, nums: FloatArray) {
    val writer = DataOutputStream(BufferedOutputStream(FileOutputStream(dataPath, true)))
    for (i in 0 until 9) {
        writer.writeFloat(nums[i])
    }
    writer.flush()
    writer.close()
}

fun writePicToBinaryFile(dataPath: String, pic: ByteArray) {
    val writer = DataOutputStream(BufferedOutputStream(FileOutputStream(dataPath)))
    writer.write(pic)
    writer.close()
}

fun readFloatFromBinaryFile(dataPath: String): Float {
    val reader = DataInputStream(FileInputStream(dataPath))
    return reader.readFloat()
}

fun getIpAddress(): String {
    val url = URL("http://checkip.amazonaws.com")
    val br = BufferedReader(InputStreamReader(url.openStream()))
    val ip = br.readLine()

    if (ip != null) return ip
    else return ""
}

class Utility {}