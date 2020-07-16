package edu.skku.cs.autosen.utility

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import edu.skku.cs.autosen.MainActivity
import edu.skku.cs.autosen.sensor.SensorMeasurementService

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

    for (i in 0 until numOfData.size - 1) {
        if (numOfData[i] < samplingRate) { // 데이터 수집이 제대로 안 된 경우
            baseNum += numOfData[i]
            continue
        } else { // 데이터 수집이 제대로 된 경우

            val step: Float = numOfData[i].toFloat() / samplingRate

            for (j in 0 until samplingRate) {
                dataX.add(fullData[(baseNum + (step * i).toInt()) * 3])
                dataY.add(fullData[(baseNum + (step * i).toInt()) * 3 + 1])
                dataZ.add(fullData[(baseNum + (step * i).toInt()) * 3 + 2])
            }

            baseNum += numOfData[i]
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
               gyrY: ArrayList<Float>, gyrZ: ArrayList<Float>, userId: String) {
    val totalData = HashMap<String, Any>()

    for (i in 1 ..SensorMeasurementService.SECONDS) {
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

    val reference = FirebaseDatabase.getInstance().getReference().child("Sensor_Data")
    reference.updateChildren(totalData)
}

fun checkIdIfDuplicated(userId: String, context: Context): Boolean {
    var isDuplicated = false
    var isThreadDone = false

    val reference = FirebaseDatabase.getInstance().getReference().child("Users")
    val query = reference.orderByKey()
    val singleValueEventListener = object : ValueEventListener {
        override fun onCancelled(error: DatabaseError) {
            Log.e("asdf", "checkIdIfDuplicated 메서드 오류")
            Toast.makeText(context, "인터넷 연결 오류", Toast.LENGTH_LONG).show()
            isThreadDone = true
            isDuplicated = true
        }

        override fun onDataChange(snapshot: DataSnapshot) {
            val userIds = snapshot.children

            for (i in userIds) {
                val id = i.key

                if (userId.equals(id)) {
                    Toast.makeText(context, "이미 데이터가 등록되어 있습니다.", Toast.LENGTH_LONG).show()
                    isDuplicated = true
                    isThreadDone = true
                    return
                }
            }

            isThreadDone = true
        }

    }
    query.addListenerForSingleValueEvent(singleValueEventListener)

    while (!isThreadDone) {
        Thread.sleep(50)
    }

    return isDuplicated
}

class Utility {

}