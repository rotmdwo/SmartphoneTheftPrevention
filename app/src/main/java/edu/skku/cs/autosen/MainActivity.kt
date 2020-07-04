package edu.skku.cs.autosen

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*

const val RESULT_CODE = 101

class MainActivity : AppCompatActivity() {
    private var userId = ""
    private val SAMPLING_RATE: Int = 64

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val receiver = MyReceiver(Handler())
        receiver.setReceiver(obj)

        button.setOnClickListener {
            button.isClickable = false

            userId = ID.text.toString()

            if (userId.equals("")) {
                Toast.makeText(applicationContext, "ID를 입력해주세요.", Toast.LENGTH_SHORT).show()
                button.isClickable = true
            } else {
                // Start Service
                val intent = Intent(applicationContext, SensorMeasurementService::class.java)
                intent.putExtra("receiver",receiver )
                startService(intent)
            }
        }
    }

    private val obj = object: MyReceiver.Receiver {
        override fun onReceiverResult(resultCode: Int, resultData: Bundle){
            if (resultCode == RESULT_CODE) {
                //val string = resultData.getString("str")
                val accelerometerData = resultData.getFloatArray("accelerometerData")
                val magnetometerData = resultData.getFloatArray("magnetometerData")
                val gyroscopeData = resultData.getFloatArray("gyroscopeData")
                val numOfAccelerometerData = resultData.getIntArray("numOfAccelerometerData")
                val numOfMagnetometerData = resultData.getIntArray("numOfMagnetometerData")
                val numOfGyroscopeData = resultData.getIntArray("numOfGyroscopeData")

                // 데이터 정규화. 5초 마다 구간 설정.
                for (i in 0 until (numOfAccelerometerData.size - 1) / 5) {

                    // 기존 구간들의 (X, Y, Z) 세트의 데이터 수
                    var base = 0
                    for (k in 0 until i * 5) {
                        base += numOfAccelerometerData[k]
                    }

                    // 해당 5초 구간 안에 있는 (X, Y, Z) 세트의 데이터 수
                    val totalNumOfDataFor5Secs = numOfAccelerometerData[i * 5] + numOfAccelerometerData[i * 5 + 1] +
                            numOfAccelerometerData[i * 5 + 2] + numOfAccelerometerData[i * 5 + 3] + numOfAccelerometerData[i * 5 + 4]

                    // 해당 5초 구간의 데이터 추출
                    val arrayListX = ArrayList<Float>(totalNumOfDataFor5Secs)
                    val arrayListY = ArrayList<Float>(totalNumOfDataFor5Secs)
                    val arrayListZ = ArrayList<Float>(totalNumOfDataFor5Secs)
                    for (j in base until base + totalNumOfDataFor5Secs) {
                        arrayListX.add(accelerometerData[j * 3])
                        arrayListY.add(accelerometerData[j * 3 + 1])
                        arrayListZ.add(accelerometerData[j * 3 + 2])
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
                        Toast.makeText(applicationContext, "데이터 처리 중에 문제가 발생하였습니다.", Toast.LENGTH_LONG).show()
                        return
                    }
                    val minX = if (tempMinX != null) {
                        tempMinX
                    } else {
                        Toast.makeText(applicationContext, "데이터 처리 중에 문제가 발생하였습니다.", Toast.LENGTH_LONG).show()
                        return
                    }
                    val maxY = if (tempMaxY != null) {
                        tempMaxY
                    } else {
                        Toast.makeText(applicationContext, "데이터 처리 중에 문제가 발생하였습니다.", Toast.LENGTH_LONG).show()
                        return
                    }
                    val minY = if (tempMinY != null) {
                        tempMinY
                    } else {
                        Toast.makeText(applicationContext, "데이터 처리 중에 문제가 발생하였습니다.", Toast.LENGTH_LONG).show()
                        return
                    }
                    val maxZ = if (tempMaxZ != null) {
                        tempMaxZ
                    } else {
                        Toast.makeText(applicationContext, "데이터 처리 중에 문제가 발생하였습니다.", Toast.LENGTH_LONG).show()
                        return
                    }
                    val minZ = if (tempMinZ != null) {
                        tempMinZ
                    } else {
                        Toast.makeText(applicationContext, "데이터 처리 중에 문제가 발생하였습니다.", Toast.LENGTH_LONG).show()
                        return
                    }

                    // 구간 길이
                    val lengthX = maxX - minX
                    val lengthY = maxY - minY
                    val lengthZ = maxZ - minZ

                    for (j in base until base + totalNumOfDataFor5Secs) {
                        accelerometerData[j * 3] = (accelerometerData[j * 3] - minX) / lengthX
                        accelerometerData[j * 3 + 1] = (accelerometerData[j * 3 + 1] - minY) / lengthY
                        accelerometerData[j * 3 + 2] = (accelerometerData[j * 3 + 2] - minZ) / lengthZ
                    }
                }

                for (i in 0 until (numOfMagnetometerData.size - 1) / 5) {

                    // 기존 구간들의 (X, Y, Z) 데이터 수
                    var base = 0
                    for (k in 0 until i * 5) {
                        base += numOfMagnetometerData[k]
                    }

                    // 해당 5초 구간 안에 있는 (X, Y, Z) 데이터 수
                    val totalNumOfDataFor5Secs = numOfMagnetometerData[i * 5] + numOfMagnetometerData[i * 5 + 1] +
                            numOfMagnetometerData[i * 5 + 2] + numOfMagnetometerData[i * 5 + 3] + numOfMagnetometerData[i * 5 + 4]

                    // 해당 5초 구간의 데이터 추출
                    val arrayListX = ArrayList<Float>(totalNumOfDataFor5Secs)
                    val arrayListY = ArrayList<Float>(totalNumOfDataFor5Secs)
                    val arrayListZ = ArrayList<Float>(totalNumOfDataFor5Secs)
                    for (j in base until base + totalNumOfDataFor5Secs) {
                        arrayListX.add(magnetometerData[j * 3])
                        arrayListY.add(magnetometerData[j * 3 + 1])
                        arrayListZ.add(magnetometerData[j * 3 + 2])
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
                        Toast.makeText(applicationContext, "데이터 처리 중에 문제가 발생하였습니다.", Toast.LENGTH_LONG).show()
                        return
                    }
                    val minX = if (tempMinX != null) {
                        tempMinX
                    } else {
                        Toast.makeText(applicationContext, "데이터 처리 중에 문제가 발생하였습니다.", Toast.LENGTH_LONG).show()
                        return
                    }
                    val maxY = if (tempMaxY != null) {
                        tempMaxY
                    } else {
                        Toast.makeText(applicationContext, "데이터 처리 중에 문제가 발생하였습니다.", Toast.LENGTH_LONG).show()
                        return
                    }
                    val minY = if (tempMinY != null) {
                        tempMinY
                    } else {
                        Toast.makeText(applicationContext, "데이터 처리 중에 문제가 발생하였습니다.", Toast.LENGTH_LONG).show()
                        return
                    }
                    val maxZ = if (tempMaxZ != null) {
                        tempMaxZ
                    } else {
                        Toast.makeText(applicationContext, "데이터 처리 중에 문제가 발생하였습니다.", Toast.LENGTH_LONG).show()
                        return
                    }
                    val minZ = if (tempMinZ != null) {
                        tempMinZ
                    } else {
                        Toast.makeText(applicationContext, "데이터 처리 중에 문제가 발생하였습니다.", Toast.LENGTH_LONG).show()
                        return
                    }

                    // 구간 길이
                    val lengthX = maxX - minX
                    val lengthY = maxY - minY
                    val lengthZ = maxZ - minZ

                    for (j in base until base + totalNumOfDataFor5Secs) {
                        magnetometerData[j * 3] = (magnetometerData[j * 3] - minX) / lengthX
                        magnetometerData[j * 3 + 1] = (magnetometerData[j * 3 + 1] - minY) / lengthY
                        magnetometerData[j * 3 + 2] = (magnetometerData[j * 3 + 2] - minZ) / lengthZ
                    }
                }

                for (i in 0 until (numOfGyroscopeData.size - 1) / 5) {

                    // 기존 구간들의 (X, Y, Z) 데이터 수
                    var base = 0
                    for (k in 0 until i * 5) {
                        base += numOfGyroscopeData[k]
                    }

                    // 해당 5초 구간 안에 있는 (X, Y, Z) 데이터 수
                    val totalNumOfDataFor5Secs = numOfGyroscopeData[i * 5] + numOfGyroscopeData[i * 5 + 1] +
                            numOfGyroscopeData[i * 5 + 2] + numOfGyroscopeData[i * 5 + 3] + numOfGyroscopeData[i * 5 + 4]

                    // 해당 5초 구간의 데이터 추출
                    val arrayListX = ArrayList<Float>(totalNumOfDataFor5Secs)
                    val arrayListY = ArrayList<Float>(totalNumOfDataFor5Secs)
                    val arrayListZ = ArrayList<Float>(totalNumOfDataFor5Secs)
                    for (j in base until base + totalNumOfDataFor5Secs) {
                        arrayListX.add(gyroscopeData[j * 3])
                        arrayListY.add(gyroscopeData[j * 3 + 1])
                        arrayListZ.add(gyroscopeData[j * 3 + 2])
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
                        Toast.makeText(applicationContext, "데이터 처리 중에 문제가 발생하였습니다.", Toast.LENGTH_LONG).show()
                        return
                    }
                    val minX = if (tempMinX != null) {
                        tempMinX
                    } else {
                        Toast.makeText(applicationContext, "데이터 처리 중에 문제가 발생하였습니다.", Toast.LENGTH_LONG).show()
                        return
                    }
                    val maxY = if (tempMaxY != null) {
                        tempMaxY
                    } else {
                        Toast.makeText(applicationContext, "데이터 처리 중에 문제가 발생하였습니다.", Toast.LENGTH_LONG).show()
                        return
                    }
                    val minY = if (tempMinY != null) {
                        tempMinY
                    } else {
                        Toast.makeText(applicationContext, "데이터 처리 중에 문제가 발생하였습니다.", Toast.LENGTH_LONG).show()
                        return
                    }
                    val maxZ = if (tempMaxZ != null) {
                        tempMaxZ
                    } else {
                        Toast.makeText(applicationContext, "데이터 처리 중에 문제가 발생하였습니다.", Toast.LENGTH_LONG).show()
                        return
                    }
                    val minZ = if (tempMinZ != null) {
                        tempMinZ
                    } else {
                        Toast.makeText(applicationContext, "데이터 처리 중에 문제가 발생하였습니다.", Toast.LENGTH_LONG).show()
                        return
                    }

                    // 구간 길이
                    val lengthX = maxX - minX
                    val lengthY = maxY - minY
                    val lengthZ = maxZ - minZ

                    for (j in base until base + totalNumOfDataFor5Secs) {
                        gyroscopeData[j * 3] = (gyroscopeData[j * 3] - minX) / lengthX
                        gyroscopeData[j * 3 + 1] = (gyroscopeData[j * 3 + 1] - minY) / lengthY
                        gyroscopeData[j * 3 + 2] = (gyroscopeData[j * 3 + 2] - minZ) / lengthZ
                    }
                }

                var baseNumOfAc = 0
                var baseNumOfMa = 0
                var baseNumOfGr = 0

                for (i in 0 until numOfAccelerometerData.size - 1) {
                    if (numOfAccelerometerData[i] < SAMPLING_RATE) { // 데이터 수집이 제대로 안 된 경우
                        baseNumOfAc += numOfAccelerometerData[i]
                        continue
                    } else { // 데이터 수집이 제대로 된 경우
                        val arrayListX = ArrayList<Float>(SAMPLING_RATE)
                        val arrayListY = ArrayList<Float>(SAMPLING_RATE)
                        val arrayListZ = ArrayList<Float>(SAMPLING_RATE)
                        val step: Float = numOfAccelerometerData[i].toFloat() / SAMPLING_RATE

                        for (j in 0 until SAMPLING_RATE) {
                            arrayListX.add(accelerometerData[(baseNumOfAc + (step * i).toInt()) * 3])
                            arrayListY.add(accelerometerData[(baseNumOfAc + (step * i).toInt()) * 3 + 1])
                            arrayListZ.add(accelerometerData[(baseNumOfAc + (step * i).toInt()) * 3 + 2])
                        }

                        //TODO arrayList에 넣은 64개의 데이터를 파이어베이스에 업로드 하는 코드


                        baseNumOfAc += numOfAccelerometerData[i]
                    }
                }
                for (i in 0 until numOfMagnetometerData.size - 1) {
                    if (numOfMagnetometerData[i] < SAMPLING_RATE) { // 데이터 수집이 제대로 안 된 경우
                        baseNumOfMa += numOfMagnetometerData[i]
                        continue
                    } else { // 데이터 수집이 제대로 된 경우
                        val arrayListX = ArrayList<Float>(SAMPLING_RATE)
                        val arrayListY = ArrayList<Float>(SAMPLING_RATE)
                        val arrayListZ = ArrayList<Float>(SAMPLING_RATE)
                        val step: Float = numOfMagnetometerData[i].toFloat() / SAMPLING_RATE

                        for (j in 0 until SAMPLING_RATE) {
                            arrayListX.add(magnetometerData[(baseNumOfAc + (step * i).toInt()) * 3])
                            arrayListY.add(magnetometerData[(baseNumOfAc + (step * i).toInt()) * 3 + 1])
                            arrayListZ.add(magnetometerData[(baseNumOfAc + (step * i).toInt()) * 3 + 2])
                        }

                        //TODO arrayList에 넣은 64개의 데이터를 파이어베이스에 업로드 하는 코드

                        baseNumOfMa += numOfMagnetometerData[i]
                    }
                }
                for (i in 0 until numOfGyroscopeData.size - 1) {
                    if (numOfGyroscopeData[i] < SAMPLING_RATE) { // 데이터 수집이 제대로 안 된 경우
                        baseNumOfGr += numOfGyroscopeData[i]
                        continue
                    } else { // 데이터 수집이 제대로 된 경우
                        val arrayListX = ArrayList<Float>(SAMPLING_RATE)
                        val arrayListY = ArrayList<Float>(SAMPLING_RATE)
                        val arrayListZ = ArrayList<Float>(SAMPLING_RATE)
                        val step: Float = numOfGyroscopeData[i].toFloat() / SAMPLING_RATE

                        for (j in 0 until SAMPLING_RATE) {
                            arrayListX.add(gyroscopeData[(baseNumOfAc + (step * i).toInt()) * 3])
                            arrayListY.add(gyroscopeData[(baseNumOfAc + (step * i).toInt()) * 3])
                            arrayListZ.add(gyroscopeData[(baseNumOfAc + (step * i).toInt()) * 3])
                        }

                        //TODO arrayList에 넣은 64개의 데이터를 파이어베이스에 업로드 하는 코드

                        baseNumOfGr += numOfGyroscopeData[i]
                    }
                }

                button.isClickable = true
            }
        }
    }
    /*
    override fun onNewIntent(intent: Intent) {
        processIntent(intent)
        super.onNewIntent(intent)
    }

    fun processIntent(intent: Intent) {
        if (intent != null) {
            textView.text = intent.getStringExtra("string")
            button.isClickable = true
        }
    }
    */
}
