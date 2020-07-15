package edu.skku.cs.autosen

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

import edu.skku.cs.autosen.sensor.SensorMeasurementService.Companion.SECONDS
import edu.skku.cs.autosen.sensor.MyReceiver
import edu.skku.cs.autosen.sensor.SensorMeasurementService
import edu.skku.cs.autosen.utility.checkDataRetrievalIfSuccessful
import edu.skku.cs.autosen.utility.normalizeData
import edu.skku.cs.autosen.utility.sampleData
import edu.skku.cs.autosen.utility.uploadData

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
                val accelerometerData = resultData.getFloatArray("accelerometerData")
                val magnetometerData = resultData.getFloatArray("magnetometerData")
                val gyroscopeData = resultData.getFloatArray("gyroscopeData")
                val numOfAccelerometerData = resultData.getIntArray("numOfAccelerometerData")
                val numOfMagnetometerData = resultData.getIntArray("numOfMagnetometerData")
                val numOfGyroscopeData = resultData.getIntArray("numOfGyroscopeData")


                var isProcessSuccessful = false

                // 데이터 정규화. 5초 마다 구간 설정.
                isProcessSuccessful = normalizeData(accelerometerData, numOfAccelerometerData, applicationContext)
                if (!isProcessSuccessful) error("데이터 정규화 오류")
                isProcessSuccessful = normalizeData(magnetometerData, numOfMagnetometerData, applicationContext)
                if (!isProcessSuccessful) error("데이터 정규화 오류")
                isProcessSuccessful = normalizeData(gyroscopeData, numOfGyroscopeData, applicationContext)
                if (!isProcessSuccessful) error("데이터 정규화 오류")


                val accX = ArrayList<Float>(SAMPLING_RATE)
                val accY = ArrayList<Float>(SAMPLING_RATE)
                val accZ = ArrayList<Float>(SAMPLING_RATE)
                val magX = ArrayList<Float>(SAMPLING_RATE)
                val magY = ArrayList<Float>(SAMPLING_RATE)
                val magZ = ArrayList<Float>(SAMPLING_RATE)
                val gyrX = ArrayList<Float>(SAMPLING_RATE)
                val gyrY = ArrayList<Float>(SAMPLING_RATE)
                val gyrZ = ArrayList<Float>(SAMPLING_RATE)


                sampleData(accelerometerData, numOfAccelerometerData, SAMPLING_RATE,
                    accX, accY, accZ)
                sampleData(magnetometerData, numOfMagnetometerData, SAMPLING_RATE,
                    magX, magY, magZ)
                sampleData(gyroscopeData, numOfGyroscopeData, SAMPLING_RATE,
                    gyrX, gyrY, gyrZ)


                // 데이터가 제대로 수집 되었는 지 확인
                isProcessSuccessful = checkDataRetrievalIfSuccessful(accX, accY, accZ, magX, magY, magZ, gyrX, gyrY, gyrZ, applicationContext)
                if (!isProcessSuccessful) error("데이터 수집 오류")


                // 데이터 업로드
                uploadData(SAMPLING_RATE, accX, accY, accZ, magX, magY, magZ, gyrX, gyrY, gyrZ, userId)


                button.isClickable = true
            }
        }
    }
}
