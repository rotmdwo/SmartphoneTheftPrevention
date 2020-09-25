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

    @PostMapping("/data") // 클라이언트에서 해당 링크를 실행하면 아래의 메서드가 실행됨
    fun getID(@RequestBody incomingData: Data): ApiResponse {
        val userId = incomingData.userId
        val data = incomingData.data
        val path = "/Users/sungjaelee/Desktop/Android/SmartphoneTheftPrevention/user_data/" + userId + ".txt"
        val file = File(path)

        if (!file.exists()) file.createNewFile()

        val fiveSecsDataIterator= data.iterator()

        while (fiveSecsDataIterator.hasNext()) {
            val secData = fiveSecsDataIterator.next().value
            val secDataIterator = secData.iterator()

            while (secDataIterator.hasNext()) {
                val oneOver64HzData = secDataIterator.next().value
                val addedLine = "${oneOver64HzData["AccX"]} ${oneOver64HzData["AccY"]} ${oneOver64HzData["AccZ"]} " +
                        "${oneOver64HzData["MagX"]} ${oneOver64HzData["MagY"]} ${oneOver64HzData["MagZ"]} " +
                        "${oneOver64HzData["GyrX"]} ${oneOver64HzData["GyrY"]} ${oneOver64HzData["GyrZ"]}\n"
                try {
                    Files.write(Paths.get(path), addedLine.toByteArray(), StandardOpenOption.APPEND)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }

        return ApiResponse.ok("Uploaded Successfully")
    }
}
