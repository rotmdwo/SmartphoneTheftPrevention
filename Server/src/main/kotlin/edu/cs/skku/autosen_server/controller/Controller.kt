package edu.cs.skku.autosen_server.controller

import edu.cs.skku.autosen_server.Data
import edu.cs.skku.autosen_server.common.ApiResponse
import org.springframework.web.bind.annotation.*
import java.io.*
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

@RestController
@RequestMapping("/api/v1")
class Controller {
    @PostMapping("/getSecs")
    fun getSecs(@RequestBody userId: String): ApiResponse {
        val id = userId.substring(1, userId.lastIndex)
        val secsPath = "/Users/sungjaelee/Desktop/Android/SmartphoneTheftPrevention/data/secs_data/" + id + ".txt"
        val file = File(secsPath)
        if (!file.exists()) {
            return ApiResponse.ok("0")
        } else {
            val secs = FileReader(secsPath).readText()
            return ApiResponse.ok(secs)
        }
    }


    @PostMapping("/saveData") // 클라이언트에서 해당 링크를 실행하면 아래의 메서드가 실행됨
    fun saveData(@RequestBody incomingData: Data): ApiResponse {
        val userId = incomingData.userId
        val secs = incomingData.secs
        val data = incomingData.data

        val dataPath = "/Users/sungjaelee/Desktop/Android/SmartphoneTheftPrevention/data/user_data/" + userId + ".txt"
        val secsPath = "/Users/sungjaelee/Desktop/Android/SmartphoneTheftPrevention/data/secs_data/" + userId + ".txt"
        var file = File(dataPath)
        if (!file.exists()) file.createNewFile()
        file = File(secsPath)
        if (!file.exists()) file.createNewFile()


        val fiveSecsDataIterator= data.iterator()

        try {
            while (fiveSecsDataIterator.hasNext()) {
                val secData = fiveSecsDataIterator.next().value
                val secDataIterator = secData.iterator()

                while (secDataIterator.hasNext()) {
                    val oneOver64HzData = secDataIterator.next().value
                    val addedLine = "${oneOver64HzData["AccX"]} ${oneOver64HzData["AccY"]} ${oneOver64HzData["AccZ"]} " +
                            "${oneOver64HzData["MagX"]} ${oneOver64HzData["MagY"]} ${oneOver64HzData["MagZ"]} " +
                            "${oneOver64HzData["GyrX"]} ${oneOver64HzData["GyrY"]} ${oneOver64HzData["GyrZ"]}\n"

                    Files.write(Paths.get(dataPath), addedLine.toByteArray(), StandardOpenOption.APPEND)
                }
            }

            Files.write(Paths.get(secsPath), "$secs".toByteArray(), StandardOpenOption.WRITE)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return ApiResponse.ok("Uploaded Successfully")
    }


    fun writeFloatToBinaryFile(dataPath: String, num: Float) {
        val writer = DataOutputStream(BufferedOutputStream(FileOutputStream(dataPath, true)))
        writer.writeFloat(num)
        writer.close()
    }

    fun writeFloatToBinaryFile(dataPath: String, nums: FloatArray) {
        val writer = DataOutputStream(BufferedOutputStream(FileOutputStream(dataPath, true)))
        for (i in 0 until 9) {
            writer.writeFloat(nums[i])
        }
        writer.flush()
        writer.close()
    }

    fun readFloatFromBinaryFile(dataPath: String): Float {
        val reader = DataInputStream(FileInputStream(dataPath))
        return reader.readFloat()
    }
}
