package edu.skku.cs.autosen

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*

const val RESULT_CODE = 101

class MainActivity : AppCompatActivity() {
    private var userId = ""
    private val SAMPLING_RATE: Int = 64

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val obj = object: MyReceiver.Receiver {
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
                        // 기존 구간들의 (X, Y, Z) 데이터 수
                        var base = 0
                        for (k in 0 until i) {
                            base += numOfAccelerometerData[k * 5] + numOfAccelerometerData[k * 5 + 1] +
                                    numOfAccelerometerData[k * 5 + 2] + numOfAccelerometerData[k * 5 + 3] + numOfAccelerometerData[k * 5 + 4]
                        }

                        // 해당 5초 구간 안에 있는 (X, Y, Z) 데이터 수
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
                        val maxX = arrayListX.max()
                        val minX = arrayListX.min()
                        val maxY = arrayListY.max()
                        val minY = arrayListY.min()
                        val maxZ = arrayListZ.max()
                        val minZ = arrayListZ.min()

                        // 구간 길이
                        val lengthX = maxX?.minus(minX!!)
                        val lengthY = maxY?.minus(minY!!)
                        val lengthZ = maxZ?.minus(minZ!!)

                        for (j in base until base + totalNumOfDataFor5Secs) {
                            accelerometerData[j * 3] = (accelerometerData[j * 3] - minX!!) / lengthX!!
                            accelerometerData[j * 3 + 1] = (accelerometerData[j * 3 + 1] - minY!!) / lengthY!!
                            accelerometerData[j * 3 + 2] = (accelerometerData[j * 3 + 2] - minZ!!) / lengthZ!!
                        }
                    }

                    for (i in 0 until (numOfMagnetometerData.size - 1) / 5) {
                        // 기존 구간들의 (X, Y, Z) 데이터 수
                        var base = 0
                        for (k in 0 until i) {
                            base += numOfMagnetometerData[k * 5] + numOfMagnetometerData[k * 5 + 1] +
                                    numOfMagnetometerData[k * 5 + 2] + numOfMagnetometerData[k * 5 + 3] + numOfMagnetometerData[k * 5 + 4]
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
                        val maxX = arrayListX.max()
                        val minX = arrayListX.min()
                        val maxY = arrayListY.max()
                        val minY = arrayListY.min()
                        val maxZ = arrayListZ.max()
                        val minZ = arrayListZ.min()

                        // 구간 길이
                        val lengthX = maxX?.minus(minX!!)
                        val lengthY = maxY?.minus(minY!!)
                        val lengthZ = maxZ?.minus(minZ!!)

                        for (j in base until base + totalNumOfDataFor5Secs) {
                            magnetometerData[j * 3] = (magnetometerData[j * 3] - minX!!) / lengthX!!
                            magnetometerData[j * 3 + 1] = (magnetometerData[j * 3 + 1] - minY!!) / lengthY!!
                            magnetometerData[j * 3 + 2] = (magnetometerData[j * 3 + 2] - minZ!!) / lengthZ!!
                        }
                    }

                    for (i in 0 until (numOfGyroscopeData.size - 1) / 5) {
                        // 기존 구간들의 (X, Y, Z) 데이터 수
                        var base = 0
                        for (k in 0 until i) {
                            base += numOfGyroscopeData[k * 5] + numOfGyroscopeData[k * 5 + 1] +
                                    numOfGyroscopeData[k * 5 + 2] + numOfGyroscopeData[k * 5 + 3] + numOfGyroscopeData[k * 5 + 4]
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
                        val maxX = arrayListX.max()
                        val minX = arrayListX.min()
                        val maxY = arrayListY.max()
                        val minY = arrayListY.min()
                        val maxZ = arrayListZ.max()
                        val minZ = arrayListZ.min()

                        // 구간 길이
                        val lengthX = maxX?.minus(minX!!)
                        val lengthY = maxY?.minus(minY!!)
                        val lengthZ = maxZ?.minus(minZ!!)

                        for (j in base until base + totalNumOfDataFor5Secs) {
                            gyroscopeData[j * 3] = (gyroscopeData[j * 3] - minX!!) / lengthX!!
                            gyroscopeData[j * 3 + 1] = (gyroscopeData[j * 3 + 1] - minY!!) / lengthY!!
                            gyroscopeData[j * 3 + 2] = (gyroscopeData[j * 3 + 2] - minZ!!) / lengthZ!!
                        }
                    }


                    var baseNumOfAc = 0
                    var baseNumOfMa = 0
                    var baseNumOfGr = 0

                    for (i in 0 until numOfAccelerometerData.size - 1) {
                        if (numOfAccelerometerData[i] < 64) { // 데이터 수집이 제대로 안 된 경우
                            baseNumOfAc += numOfAccelerometerData[i]
                            continue
                        } else { // 데이터 수집이 제대로 된 경우
                            val arrayListX = ArrayList<Float>(SAMPLING_RATE)
                            val arrayListY = ArrayList<Float>(SAMPLING_RATE)
                            val arrayListZ = ArrayList<Float>(SAMPLING_RATE)
                            val step: Float = numOfAccelerometerData[i].toFloat() / SAMPLING_RATE

                            for (j in 0 until SAMPLING_RATE) {
                                arrayListX.add(accelerometerData[baseNumOfAc * 3 + (step * i).toInt() * 3])
                                arrayListY.add(accelerometerData[baseNumOfAc * 3 + (step * i).toInt() * 3 + 1])
                                arrayListZ.add(accelerometerData[baseNumOfAc * 3 + (step * i).toInt() * 3 + 2])
                            }

                            //TODO arrayList에 넣은 64개의 데이터를 파이어베이스에 업로드 하는 코드

                            baseNumOfAc += numOfAccelerometerData[i]
                        }
                    }
                    for (i in 0 until numOfMagnetometerData.size - 1) {
                        if (numOfMagnetometerData[i] < 64) { // 데이터 수집이 제대로 안 된 경우
                            baseNumOfMa += numOfMagnetometerData[i]
                            continue
                        } else { // 데이터 수집이 제대로 된 경우
                            val arrayListX = ArrayList<Float>(SAMPLING_RATE)
                            val arrayListY = ArrayList<Float>(SAMPLING_RATE)
                            val arrayListZ = ArrayList<Float>(SAMPLING_RATE)
                            val step: Float = numOfMagnetometerData[i].toFloat() / SAMPLING_RATE

                            for (j in 0 until SAMPLING_RATE) {
                                arrayListX.add(magnetometerData[baseNumOfMa * 3 + (step * i).toInt() * 3])
                                arrayListY.add(magnetometerData[baseNumOfMa * 3 + (step * i).toInt() * 3 + 1])
                                arrayListZ.add(magnetometerData[baseNumOfMa * 3 + (step * i).toInt() * 3 + 2])
                            }

                            //TODO arrayList에 넣은 64개의 데이터를 파이어베이스에 업로드 하는 코드

                            baseNumOfMa += numOfMagnetometerData[i]
                        }
                    }
                    for (i in 0 until numOfGyroscopeData.size - 1) {
                        if (numOfGyroscopeData[i] < 64) { // 데이터 수집이 제대로 안 된 경우
                            baseNumOfGr += numOfGyroscopeData[i]
                            continue
                        } else { // 데이터 수집이 제대로 된 경우
                            val arrayListX = ArrayList<Float>(SAMPLING_RATE)
                            val arrayListY = ArrayList<Float>(SAMPLING_RATE)
                            val arrayListZ = ArrayList<Float>(SAMPLING_RATE)
                            val step: Float = numOfGyroscopeData[i].toFloat() / SAMPLING_RATE

                            for (j in 0 until SAMPLING_RATE) {
                                arrayListX.add(gyroscopeData[baseNumOfGr * 3 + (step * i).toInt() * 3])
                                arrayListY.add(gyroscopeData[baseNumOfGr * 3 + (step * i).toInt() * 3 + 1])
                                arrayListZ.add(gyroscopeData[baseNumOfGr * 3 + (step * i).toInt() * 3 + 2])
                            }

                            //TODO arrayList에 넣은 64개의 데이터를 파이어베이스에 업로드 하는 코드

                            baseNumOfGr += numOfGyroscopeData[i]
                        }
                    }

                    button.isClickable = true
                }
            }
        }
        val reciever = MyReceiver(Handler())
        reciever.setReceiver(obj)

        button.setOnClickListener {
            button.isClickable = false

            userId = ID.text.toString()

            if (userId.equals("")) {
                Toast.makeText(applicationContext, "ID를 입력해주세요.", Toast.LENGTH_SHORT).show()
                button.isClickable = true
            } else {
                // Start Service
                val intent = Intent(applicationContext, SensorMeasurementService::class.java)
                intent.putExtra("receiver",reciever )
                startService(intent)
            }
        }

        //processIntent(intent)
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
