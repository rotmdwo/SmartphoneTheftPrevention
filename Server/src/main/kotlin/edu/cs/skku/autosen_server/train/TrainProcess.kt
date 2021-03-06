package edu.cs.skku.autosen_server.train

import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*
import kotlin.concurrent.timer


class TrainProcess {
    companion object {
        val trainingQueue = LinkedList<String>();
        var currentID = ""

        fun process() {
            timer(period = 1000L) {
                if (trainingQueue.isNotEmpty()) {
                    println("START")
                    val id = trainingQueue.peek()
                    val runtime = Runtime.getRuntime()
                    val command = "python D:/Android/AndroidStudioProjects/AUToSen/model/TrainModel.py $id"
                    val process = runtime.exec(command)

                    val br = BufferedReader(InputStreamReader(process.inputStream))
                    var textFromPython = br.readLine()
                    println(textFromPython)
                    while (textFromPython != null) {
                        println(textFromPython)
                        textFromPython = br.readLine()
                    }

                    //if (textFromPython == "OK") println(textFromPython)
                    //else println("Training Model Failed: ${trainingQueue.peek()}")

                    trainingQueue.poll()
                    println("END")
                }
            }
        }
    }
}