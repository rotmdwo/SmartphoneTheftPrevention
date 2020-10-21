package edu.skku.cs.autosen.utility

import android.app.*
import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import android.widget.Toast
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import edu.skku.cs.autosen.dataType.SensorData
import edu.skku.cs.autosen.MainActivity
import edu.skku.cs.autosen.MainActivity.Companion.authentication
import edu.skku.cs.autosen.api.ServerApi
import edu.skku.cs.autosen.api.response.ApiResponse
import edu.skku.cs.autosen.dataType.PictureData
import kotlinx.coroutines.runBlocking


fun normalizeData(data: FloatArray, numOfData: IntArray, context: Context): Boolean {
    for (i in 0 until (numOfData.size - 1) / 5) {

        // 기존 구간들의 (X, Y, Z) 세트의 데이터 수
        var base = 0
        for (k in 0 until i * 5) {
            base += numOfData[k]
        }

        // 해당 5초 구간 안에 있는 (X, Y, Z) 세트의 데이터 수
        val totalNumOfDataFor5Secs = numOfData[i * 5] + numOfData[i * 5 + 1] +
                numOfData[i * 5 + 2] + numOfData[i * 5 + 3] + numOfData[i * 5 + 4]

        // 해당 5초 구간의 데이터 추출
        val arrayListX = ArrayList<Float>(totalNumOfDataFor5Secs)
        val arrayListY = ArrayList<Float>(totalNumOfDataFor5Secs)
        val arrayListZ = ArrayList<Float>(totalNumOfDataFor5Secs)
        for (j in base until base + totalNumOfDataFor5Secs) {
            arrayListX.add(data[j * 3])
            arrayListY.add(data[j * 3 + 1])
            arrayListZ.add(data[j * 3 + 2])
        }

        // 데이터의 Min, Max
        val tempMaxX = arrayListX.max()
        val tempMinX = arrayListX.min()
        val tempMaxY = arrayListY.max()
        val tempMinY = arrayListY.min()
        val tempMaxZ = arrayListZ.max()
        val tempMinZ = arrayListZ.min()

        val maxX = if (tempMaxX != null) {
            tempMaxX
        } else {
            Toast.makeText(context, "데이터 처리 중에 문제가 발생하였습니다.", Toast.LENGTH_LONG).show()
            return false
        }
        val minX = if (tempMinX != null) {
            tempMinX
        } else {
            Toast.makeText(context, "데이터 처리 중에 문제가 발생하였습니다.", Toast.LENGTH_LONG).show()
            return false
        }
        val maxY = if (tempMaxY != null) {
            tempMaxY
        } else {
            Toast.makeText(context, "데이터 처리 중에 문제가 발생하였습니다.", Toast.LENGTH_LONG).show()
            return false
        }
        val minY = if (tempMinY != null) {
            tempMinY
        } else {
            Toast.makeText(context, "데이터 처리 중에 문제가 발생하였습니다.", Toast.LENGTH_LONG).show()
            return false
        }
        val maxZ = if (tempMaxZ != null) {
            tempMaxZ
        } else {
            Toast.makeText(context, "데이터 처리 중에 문제가 발생하였습니다.", Toast.LENGTH_LONG).show()
            return false
        }
        val minZ = if (tempMinZ != null) {
            tempMinZ
        } else {
            Toast.makeText(context, "데이터 처리 중에 문제가 발생하였습니다.", Toast.LENGTH_LONG).show()
            return false
        }

        // 구간 길이
        val lengthX = maxX - minX
        val lengthY = maxY - minY
        val lengthZ = maxZ - minZ

        for (j in base until base + totalNumOfDataFor5Secs) {
            data[j * 3] = (data[j * 3] - minX) / lengthX
            data[j * 3 + 1] = (data[j * 3 + 1] - minY) / lengthY
            data[j * 3 + 2] = (data[j * 3 + 2] - minZ) / lengthZ
        }
    }

    return true
}

fun sampleData(fullData: FloatArray, numOfData: IntArray, samplingRate: Int,
    dataX: ArrayList<Float>, dataY: ArrayList<Float>, dataZ: ArrayList<Float>) {
    var baseNum = 0
    var lastSuccessfulRetrieval = 0

    for (i in 0 until numOfData.size - 1) {
        if (numOfData[i] < samplingRate) { // 데이터 수집이 제대로 안 된 경우
            if (i == 0) { // 가장 처음부터 데이터 수집이 안 되었다면 가장 미래에 수집인 잘 된 데이터 가져옴
                var futureSuccessfulRetrieval = 1
                var tempBaseNum = numOfData[0]
                while (numOfData[futureSuccessfulRetrieval] < samplingRate) {
                    tempBaseNum += numOfData[futureSuccessfulRetrieval]
                    futureSuccessfulRetrieval++
                }

                val step: Float = numOfData[futureSuccessfulRetrieval].toFloat() / samplingRate

                for (j in 0 until samplingRate) {
                    dataX.add(fullData[(tempBaseNum + (step * j).toInt()) * 3])
                    dataY.add(fullData[(tempBaseNum + (step * j).toInt()) * 3 + 1])
                    dataZ.add(fullData[(tempBaseNum + (step * j).toInt()) * 3 + 2])
                }
            } else { // 가장 최근에 데이터 수집이 잘 된 데이터 복사
                for (j in 0 until samplingRate) {
                    dataX.add(dataX[lastSuccessfulRetrieval * samplingRate + j])
                    dataY.add(dataY[lastSuccessfulRetrieval * samplingRate + j])
                    dataZ.add(dataZ[lastSuccessfulRetrieval * samplingRate + j])
                }
            }

            baseNum += numOfData[i]
        } else { // 데이터 수집이 제대로 된 경우

            val step: Float = numOfData[i].toFloat() / samplingRate

            for (j in 0 until samplingRate) {
                dataX.add(fullData[(baseNum + (step * j).toInt()) * 3])
                dataY.add(fullData[(baseNum + (step * j).toInt()) * 3 + 1])
                dataZ.add(fullData[(baseNum + (step * j).toInt()) * 3 + 2])
            }

            baseNum += numOfData[i]
            lastSuccessfulRetrieval = i
        }
    }
}

fun checkDataRetrievalIfSuccessful(accX: ArrayList<Float>, accY: ArrayList<Float>, accZ: ArrayList<Float>,
magX: ArrayList<Float>, magY: ArrayList<Float>, magZ: ArrayList<Float>,
gyrX: ArrayList<Float>, gyrY: ArrayList<Float>, gyrZ: ArrayList<Float>, context: Context): Boolean {
    val retrievedNum = accX.size
    if (accY.size != retrievedNum || accZ.size != retrievedNum || magX.size != retrievedNum || magY.size != retrievedNum ||
        magZ.size != retrievedNum || gyrX.size != retrievedNum || gyrY.size != retrievedNum || gyrZ.size != retrievedNum) {
        Toast.makeText(context, "데이터 처리 중에 문제가 발생하였습니다. (수집오류)", Toast.LENGTH_LONG).show()
        return false
    }

    return true
}

fun uploadData(samplingRate: Int, accX: ArrayList<Float>, accY: ArrayList<Float>, accZ: ArrayList<Float>,
               magX: ArrayList<Float>, magY: ArrayList<Float>, magZ: ArrayList<Float>, gyrX: ArrayList<Float>,
               gyrY: ArrayList<Float>, gyrZ: ArrayList<Float>, userId: String, SECONDS: Int) {
    val totalData = HashMap<String, Any>()

    for (i in 1 ..SECONDS) {
        val secondData = HashMap<String, Any>()

        for (j in 1 .. samplingRate) {
            val oneOver64HzData = HashMap<String, Any>()

            oneOver64HzData.put("AccX", accX[((i - 1) * samplingRate + (j - 1)).toInt()])
            oneOver64HzData.put("AccY", accY[((i - 1) * samplingRate + (j - 1)).toInt()])
            oneOver64HzData.put("AccZ", accZ[((i - 1) * samplingRate + (j - 1)).toInt()])

            oneOver64HzData.put("MagX", magX[((i - 1) * samplingRate + (j - 1)).toInt()])
            oneOver64HzData.put("MagY", magY[((i - 1) * samplingRate + (j - 1)).toInt()])
            oneOver64HzData.put("MagZ", magZ[((i - 1) * samplingRate + (j - 1)).toInt()])

            oneOver64HzData.put("GyrX", gyrX[((i - 1) * samplingRate + (j - 1)).toInt()])
            oneOver64HzData.put("GyrY", gyrY[((i - 1) * samplingRate + (j - 1)).toInt()])
            oneOver64HzData.put("GyrZ", gyrZ[((i - 1) * samplingRate + (j - 1)).toInt()])

            secondData.put("data" + j , oneOver64HzData)
        }

        totalData.put(userId + "/sec" + i, secondData)
    }

    var reference = FirebaseDatabase.getInstance().getReference().child("Sensor_Data")
    reference.updateChildren(totalData)

    val idData = HashMap<String, Any>()
    idData.put(userId, userId)
    reference = FirebaseDatabase.getInstance().getReference().child("Users")
    reference.updateChildren(idData)
}

fun removeDatabaseItem(name1: String, name2: String? = null, name3: String? = null, name4: String? = null) {
    var reference: DatabaseReference

    if (name4 != null && name3 != null && name2 != null) {
        reference = FirebaseDatabase.getInstance().reference.child(name1).child(name2).child(name3).child(name4)
    } else {
        if (name3 != null && name2 != null) {
            reference = FirebaseDatabase.getInstance().reference.child(name1).child(name2).child(name3)
        } else {
            if (name2 != null) {
                reference = FirebaseDatabase.getInstance().reference.child(name1).child(name2)
            } else {
                reference = FirebaseDatabase.getInstance().reference.child(name1)
            }
        }
    }

    reference?.ref.removeValue()
}

fun checkData(accelerometerData: Array<ArrayList<FloatArray>>, magnetometerData: Array<ArrayList<FloatArray>>,
              gyroscopeData: Array<ArrayList<FloatArray>>, SAMPLING_RATE: Int): Boolean {
    var numOfTotalData = 0
    var numOfInactiveData = 0

    for (i in 0..4) {
        numOfTotalData += accelerometerData[i].size

        if (accelerometerData[i].size < SAMPLING_RATE || magnetometerData[i].size < SAMPLING_RATE || gyroscopeData[i].size < SAMPLING_RATE) {
            return false
        }

        for (j in 0 until accelerometerData[i].size) {
            if (accelerometerData[i][j][3] == 0.0f) numOfInactiveData++
        }
    }

    if ((numOfInactiveData.toFloat() / numOfTotalData) >= 0.99f) return false
    return true
}

fun normalizeData(data: Array<ArrayList<FloatArray>>) {
    // 데이터의 Min, Max
    val maxX = findMax(data, 0)
    val minX = findMin(data, 0)
    val maxY = findMax(data, 1)
    val minY = findMin(data, 1)
    val maxZ = findMax(data, 2)
    val minZ = findMin(data, 2)

    // 구간 길이
    val lengthX = maxX - minX
    val lengthY = maxY - minY
    val lengthZ = maxZ - minZ

    for (i in 0..4) {
        for (j in 0 until data[i].size) {
            data[i][j][0] = (data[i][j][0] - minX) / lengthX
            data[i][j][1] = (data[i][j][1] - minY) / lengthY
            data[i][j][2] = (data[i][j][2] - minZ) / lengthZ
        }
    }
}

fun decreaseIndex(index: Int): Int {
    return index - 5
}

// index 0: X, index 1: Y, index 2: Z
fun findMax(data: Array<ArrayList<FloatArray>>, index: Int): Float {
    var max = Float.MIN_VALUE

    for (i in 0..4) {
        for (j in 0 until data[i].size) {
            if (data[i][j][index] > max) max = data[i][j][index]
        }
    }

    return max
}

fun findMin(data: Array<ArrayList<FloatArray>>, index: Int): Float {
    var min = Float.MAX_VALUE

    for (i in 0..4) {
        for (j in 0 until data[i].size) {
            if (data[i][j][index] < min) min = data[i][j][index]
        }
    }

    return min
}

fun sampleData(data: Array<ArrayList<FloatArray>>, SAMPLING_RATE: Int): ArrayList<ArrayList<FloatArray>> {
    val sampledData = ArrayList<ArrayList<FloatArray>>(5)
    for (i in 0..4) sampledData.add(ArrayList())

    for (i in 0..4) {
        val step: Float = data[i].size.toFloat() / SAMPLING_RATE

        for (j in 0 until SAMPLING_RATE) {
            sampledData[i].add(data[i][(step * j).toInt()])
        }
    }
    return sampledData
}

fun authenticateData(accelerometerData:  ArrayList<ArrayList<FloatArray>>, magnetometerData:  ArrayList<ArrayList<FloatArray>>,
    gyroscopeData:  ArrayList<ArrayList<FloatArray>>, userId: String, SAMPLING_RATE: Int) {
    val totalData = HashMap<String, HashMap<String, HashMap<String, Float>>>()

    for (i in 0 until 1) {
        val secondData = HashMap<String, HashMap<String, Float>>()

        for (j in 0 until SAMPLING_RATE) {
            val oneOver64HzData = HashMap<String, Float>()

            oneOver64HzData.put("AccX", accelerometerData[i][j][0])
            oneOver64HzData.put("AccY", accelerometerData[i][j][1])
            oneOver64HzData.put("AccZ", accelerometerData[i][j][2])

            oneOver64HzData.put("MagX", magnetometerData[i][j][0])
            oneOver64HzData.put("MagY", magnetometerData[i][j][1])
            oneOver64HzData.put("MagZ", magnetometerData[i][j][2])

            oneOver64HzData.put("GyrX", gyroscopeData[i][j][0])
            oneOver64HzData.put("GyrY", gyroscopeData[i][j][1])
            oneOver64HzData.put("GyrZ", gyroscopeData[i][j][2])

            secondData.put("data" + (j + 1) , oneOver64HzData)
        }

        totalData.put("sec" + i, secondData)
    }

    val data = SensorData(userId, 0, totalData)

    runBlocking {
        try {
            val response = ServerApi.instance.predict(data).data

            if (response.equals("true")) {
                authentication = "true " + System.currentTimeMillis()
            } else if (response.equals("false")) {
                authentication = "false " + System.currentTimeMillis()
            } else {
                authentication = "error " + System.currentTimeMillis()
            }
        } catch (e: Exception) {
            Log.e("asdf", "sendData API 호출 오류", e)
        }
    }
}


fun uploadData(accelerometerData:  ArrayList<ArrayList<FloatArray>>, magnetometerData:  ArrayList<ArrayList<FloatArray>>,
               gyroscopeData:  ArrayList<ArrayList<FloatArray>>, userId: String, SAMPLING_RATE: Int, secsUploaded: Int) {
    val totalData = HashMap<String, HashMap<String, HashMap<String, Float>>>()

    for (i in 0..4) {
        val secondData = HashMap<String, HashMap<String, Float>>()

        for (j in 0 until SAMPLING_RATE) {
            val oneOver64HzData = HashMap<String, Float>()

            oneOver64HzData.put("AccX", accelerometerData[i][j][0])
            oneOver64HzData.put("AccY", accelerometerData[i][j][1])
            oneOver64HzData.put("AccZ", accelerometerData[i][j][2])

            oneOver64HzData.put("MagX", magnetometerData[i][j][0])
            oneOver64HzData.put("MagY", magnetometerData[i][j][1])
            oneOver64HzData.put("MagZ", magnetometerData[i][j][2])

            oneOver64HzData.put("GyrX", gyroscopeData[i][j][0])
            oneOver64HzData.put("GyrY", gyroscopeData[i][j][1])
            oneOver64HzData.put("GyrZ", gyroscopeData[i][j][2])

            secondData.put("data" + (j + 1) , oneOver64HzData)
        }

        totalData.put("sec" + (secsUploaded + i + 1), secondData)
    }

    val data =
        SensorData(userId, secsUploaded + 5, totalData)

    runBlocking {
        try {
            val response = ServerApi.instance.sendData(data)

            if (response.data.equals("Uploaded Successfully")) {
                MainActivity.secsUploaded += 5
            } else {

            }
        } catch (e: Exception) {
            Log.e("asdf", "sendData API 호출 오류", e)
        }
    }
    /*
    for (i in 0..4) {
        val secondData = HashMap<String, Any>()

        for (j in 0 until SAMPLING_RATE) {
            val oneOver64HzData = HashMap<String, Any>()

            oneOver64HzData.put("AccX", accelerometerData[i][j][0])
            oneOver64HzData.put("AccY", accelerometerData[i][j][1])
            oneOver64HzData.put("AccZ", accelerometerData[i][j][2])

            oneOver64HzData.put("MagX", magnetometerData[i][j][0])
            oneOver64HzData.put("MagY", magnetometerData[i][j][1])
            oneOver64HzData.put("MagZ", magnetometerData[i][j][2])

            oneOver64HzData.put("GyrX", gyroscopeData[i][j][0])
            oneOver64HzData.put("GyrY", gyroscopeData[i][j][1])
            oneOver64HzData.put("GyrZ", gyroscopeData[i][j][2])

            secondData.put("data" + (j + 1) , oneOver64HzData)
        }

        totalData.put(userId + "/sec" + (secsUploaded + i + 1), secondData)
    }

    var reference = FirebaseDatabase.getInstance().getReference().child("Sensor_Data")
    reference.updateChildren(totalData)

    val idData = HashMap<String, Any>()
    idData.put(userId, secsUploaded + 5)
    reference = FirebaseDatabase.getInstance().getReference().child("Users")
    reference.updateChildren(idData)

     */
}


fun removeUploadedData(accelerometerData:  ArrayList<ArrayList<FloatArray>>, magnetometerData:  ArrayList<ArrayList<FloatArray>>,
                       gyroscopeData:  ArrayList<ArrayList<FloatArray>>) {
    for (i in 0..4) {
        accelerometerData.removeAt(0)
        magnetometerData.removeAt(0)
        gyroscopeData.removeAt(0)
    }
}

// 인터넷이 연결상태 확인
fun checkInternetStatus(context: Context): Boolean {
    val connectivityManager: ConnectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    return connectivityManager.activeNetworkInfo != null && connectivityManager.activeNetworkInfo.isConnected
}

// 앱 재시작으로 인해 ID가 null이 아닌지 확인
fun checkIfIdAvailable(userId: String, service: Service): Boolean {
    if (userId == "") {
        service.stopForeground(true)
        service.stopSelf()
        return false
    }
    return true
}

// 화면이 꺼진 상태에서 돌아왔는지 확인
fun checkIfRestedForLong(accelerometerData: ArrayList<ArrayList<FloatArray>>, magnetometerData: ArrayList<ArrayList<FloatArray>>,
                         gyroscopeData: ArrayList<ArrayList<FloatArray>>, previousTime: Long): Long {
    if (System.currentTimeMillis() - previousTime > 8000) {
        accelerometerData.clear()
        magnetometerData.clear()
        gyroscopeData.clear()
        return System.currentTimeMillis()
    }

    return previousTime
}

fun saveID(id: String, context: Context) {
    val pref = context.getSharedPreferences("id", Activity.MODE_PRIVATE)
    val editor = pref.edit()
    editor.clear()
    editor.putString("id", id)
    editor.commit()
}

fun loadID(context: Context): String {
    val pref = context.getSharedPreferences("id", Activity.MODE_PRIVATE)
    val id = pref.getString("id", "")
    if (id != null) return id
    else return ""
}

fun saveModelAvailability(id: String, context: Context) {
    val pref = context.getSharedPreferences("model", Activity.MODE_PRIVATE)
    val editor = pref.edit()
    editor.clear()
    editor.putBoolean(id, true)
    editor.commit()
}

fun loadModelAvailability(id: String, context: Context): Boolean {
    val pref = context.getSharedPreferences("model", Activity.MODE_PRIVATE)
    return pref.getBoolean(id, false)
}

fun sendPicture(userId: String, pic: ByteArray) {
    runBlocking {
        try {
            val response = ServerApi.instance.savePicture(PictureData(userId, pic)).data
        } catch (e: Exception) {
            Log.e("asdf", "sendData API 호출 오류", e)
        }
    }
}

class Utility {

}