package edu.cs.skku.autosen_server.controller

import edu.cs.skku.autosen_server.dataType.SensorData
import edu.cs.skku.autosen_server.common.ApiResponse
import edu.cs.skku.autosen_server.dataType.PictureData
import edu.cs.skku.autosen_server.train.TrainProcess.Companion.trainingQueue
import edu.cs.skku.autosen_server.utility.writeFloatToBinaryFile
import edu.cs.skku.autosen_server.utility.writePicToBinaryFile
import org.springframework.web.bind.annotation.*
import java.io.*
import java.lang.StringBuilder
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import kotlin.collections.ArrayList

@RestController
@RequestMapping("/api/v1")
class Controller {
    @PostMapping("/getSecs")
    fun getSecs(@RequestBody userId: String): ApiResponse {
        val id = userId.substring(1, userId.lastIndex)
        //val secsPath = "/Users/sungjaelee/Desktop/Android/SmartphoneTheftPrevention/data/secs_data/" + id + ".txt"
        val secsPath = "D:/Android/AndroidStudioProjects/AUToSen/data/secs_data/${id}.txt"
        val file = File(secsPath)
        if (!file.exists()) {
            return ApiResponse.ok("0")
        } else {
            val secs = FileReader(secsPath).readText()
            return ApiResponse.ok(secs)
        }
    }


    @PostMapping("/saveData") // 클라이언트에서 해당 링크를 실행하면 아래의 메서드가 실행됨
    fun saveData(@RequestBody incomingSensorData: SensorData): ApiResponse {
        val userId = incomingSensorData.userId
        val secs = incomingSensorData.secs
        val data = incomingSensorData.data

        //val dataPath = "/Users/sungjaelee/Desktop/Android/SmartphoneTheftPrevention/data/user_data/" + userId + ".txt"
        //val secsPath = "/Users/sungjaelee/Desktop/Android/SmartphoneTheftPrevention/data/secs_data/" + userId + ".txt"
        val dataPath = "D:/Android/AndroidStudioProjects/AUToSen/data/user_data/" + userId + ".txt"
        val secsPath = "D:/Android/AndroidStudioProjects/AUToSen/data/secs_data/" + userId + ".txt"
        //var file = File(dataPath)
        //if (!file.exists()) file.createNewFile()
        val file = File(secsPath)
        if (!file.exists()) file.createNewFile()


        val fiveSecsDataIterator= data.iterator()

        try {
            while (fiveSecsDataIterator.hasNext()) {
                val secData = fiveSecsDataIterator.next().value
                val secDataIterator = secData.iterator()

                while (secDataIterator.hasNext()) {
                    val oneOver64HzData = secDataIterator.next().value
                    writeFloatToBinaryFile(dataPath, floatArrayOf(oneOver64HzData["AccX"]!!, oneOver64HzData["AccY"]!!, oneOver64HzData["AccZ"]!!,
                        oneOver64HzData["MagX"]!!, oneOver64HzData["MagY"]!!, oneOver64HzData["MagZ"]!!, oneOver64HzData["GyrX"]!!, oneOver64HzData["GyrY"]!!, oneOver64HzData["GyrZ"]!!))

                }
            }

            Files.write(Paths.get(secsPath), "$secs".toByteArray(), StandardOpenOption.WRITE)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return ApiResponse.ok("Uploaded Successfully")
    }

    @PostMapping("/predict")
    fun predict(@RequestBody incomingSensorData: SensorData): ApiResponse {
        val userId = incomingSensorData.userId
        val data = incomingSensorData.data
        val dataArray = ArrayList<Float>(9 * 64 * 1)

        val fiveSecsDataIterator = data.iterator()

        while (fiveSecsDataIterator.hasNext()) {
            val secData = fiveSecsDataIterator.next().value
            val secDataIterator = secData.iterator()

            while (secDataIterator.hasNext()) {
                val oneOver64HzData = secDataIterator.next().value
                dataArray.add(oneOver64HzData["AccX"]!!)
                dataArray.add(oneOver64HzData["AccY"]!!)
                dataArray.add(oneOver64HzData["AccZ"]!!)
                dataArray.add(oneOver64HzData["MagX"]!!)
                dataArray.add(oneOver64HzData["MagY"]!!)
                dataArray.add(oneOver64HzData["MagZ"]!!)
                dataArray.add(oneOver64HzData["GyrX"]!!)
                dataArray.add(oneOver64HzData["GyrY"]!!)
                dataArray.add(oneOver64HzData["GyrZ"]!!)
            }
        }

        val runtime = Runtime.getRuntime()
        val stringBuilder = StringBuilder("python D:/Android/AndroidStudioProjects/AUToSen/model/LoadModel.py")

        for (i in 0 until 1) {
            for (j in 0 until 64) {
                for (k in 0 until 9) {
                    stringBuilder.append(" ${dataArray[k + j * 9 + i * 9 * 64]}")
                }
            }
        }

        println("predict 시작")

        stringBuilder.append(" ${userId}")
        val command = stringBuilder.toString()
        val process = runtime.exec(command)

        val br = BufferedReader(InputStreamReader(process.inputStream))
        var textFromPython = br.readLine()

        println(textFromPython)
        textFromPython = br.readLine()

        process.waitFor()
        process.destroy()

        if (textFromPython != null) {
            if (textFromPython.equals("true")) {
                return ApiResponse.ok("true")
            } else {
                return ApiResponse.ok("false")
            }
        } else {
            //return ApiResponse.ok("error")
            return ApiResponse.error("error")
        }
    }

    @PostMapping("/buildModel")
    fun buildModel(@RequestBody userId: String): ApiResponse {
        val id = userId.substring(1, userId.lastIndex)
        val secsPath = "D:/Android/AndroidStudioProjects/AUToSen/data/secs_data/${id}.txt"
        val file = File(secsPath)
        if (!file.exists()) {
            return ApiResponse.error("No Data")
        } else if (trainingQueue.contains(id)) {
            return ApiResponse.error("Already in Queue")
        } else {
            trainingQueue.add(id)
            return ApiResponse.ok("${trainingQueue.size}")
        }
    }

    @PostMapping("/checkModel")
    fun checkIfModelExists(@RequestBody userId: String): ApiResponse {
        val id = userId.substring(1, userId.lastIndex)
        val secsPath = "D:/Android/AndroidStudioProjects/AUToSen/model/models/$id.h5"
        val file = File(secsPath)

        if (file.exists()) return ApiResponse.ok("Exists")
        else return ApiResponse.ok("Not Exists")
    }

    @PostMapping("/savePic")
    fun savePicture(@RequestBody data: PictureData): ApiResponse {
        val id = data.userId
        val pic = data.picture
        val path = "D:/Android/AndroidStudioProjects/AUToSen/UnauthorizedPics/${id}_${System.currentTimeMillis()}.bin"
        writePicToBinaryFile(path, pic)

        return ApiResponse.ok("Saved")
    }
}
