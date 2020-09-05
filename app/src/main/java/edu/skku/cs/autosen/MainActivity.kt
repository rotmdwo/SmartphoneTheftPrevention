package edu.skku.cs.autosen

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_main.*

import edu.skku.cs.autosen.sensor.MyReceiver
import edu.skku.cs.autosen.sensor.SensorMeasurementService
import edu.skku.cs.autosen.utility.*
import java.util.*
import kotlin.collections.ArrayList

const val RESULT_CODE = 101

class MainActivity : AppCompatActivity() {
    //private var userId = ""
    //private val SAMPLING_RATE: Int = 64

    companion object {
        var LANGUAGE = "KOREAN"
        var userId = ""
    }

    private val possibleTestIdSet = hashSetOf("sungjae","heidi","chettem","wiu",
    "seongjeong","yeongho", "yeongho_family1", "yeongho_family2","jinsol", "jinsol_family1",
        "jinsol_family2", "hanjoon", "kan", "chanhee", "yewon")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (Locale.getDefault().language != "ko") {
            LANGUAGE = "OTHERS"
            button.text = "Start"
        }

        val receiver = MyReceiver(Handler())
        receiver.setReceiver(obj)

        button.setOnClickListener {
            button.isClickable = false

            userId = ID.text.toString()

            if (userId.equals("")) {
                if (LANGUAGE == "KOREAN")
                    Toast.makeText(applicationContext, "ID를 입력해주세요.", Toast.LENGTH_SHORT).show()
                else Toast.makeText(applicationContext, "Please enter ID.", Toast.LENGTH_SHORT).show()
                button.isClickable = true
            } else if (!possibleTestIdSet.contains(userId)) {
                if (LANGUAGE == "KOREAN")
                    Toast.makeText(applicationContext, "허가되지 않은 ID입니다.", Toast.LENGTH_SHORT).show()
                else Toast.makeText(applicationContext, "Not Allowed ID", Toast.LENGTH_SHORT).show()
                button.isClickable = true
            } else {
                val reference = FirebaseDatabase.getInstance().getReference().child("Users")
                val query = reference.orderByKey()
                val singleValueEventListener = object : ValueEventListener {
                    override fun onCancelled(error: DatabaseError) {
                        Log.e("asdf", "checkIdIfDuplicated 메서드 오류")
                        if (LANGUAGE == "KOREAN")
                            Toast.makeText(applicationContext, "인터넷 연결 오류", Toast.LENGTH_LONG).show()
                        else Toast.makeText(applicationContext, "Internet Access Error", Toast.LENGTH_LONG).show()
                        button.isClickable = true
                    }

                    override fun onDataChange(snapshot: DataSnapshot) {
                        val userIds = snapshot.children

                        for (i in userIds) {
                            val id = i.key

                            if (userId.equals(id)) {
                                if (LANGUAGE == "KOREAN")
                                    Toast.makeText(applicationContext, "이미 데이터가 등록되어 있습니다.", Toast.LENGTH_LONG).show()
                                else Toast.makeText(applicationContext, "Data are already registered with your ID", Toast.LENGTH_LONG).show()
                                button.isClickable = true

                                return
                            }
                        }

                        // Start Service
                        val intent = Intent(applicationContext, SensorMeasurementService::class.java)
                        intent.putExtra("receiver",receiver )
                        startService(intent)
                    }

                }

                query.addListenerForSingleValueEvent(singleValueEventListener)
            }
        }
    }

    private val obj = object: MyReceiver.Receiver {
        override fun onReceiverResult(resultCode: Int, resultData: Bundle){
            if (resultCode == RESULT_CODE) {
                /*
                // 서비스로부터 데이터 받아옴
                val accelerometerData = resultData.getFloatArray("accelerometerData")
                val magnetometerData = resultData.getFloatArray("magnetometerData")
                val gyroscopeData = resultData.getFloatArray("gyroscopeData")
                val numOfAccelerometerData = resultData.getIntArray("numOfAccelerometerData")
                val numOfMagnetometerData = resultData.getIntArray("numOfMagnetometerData")
                val numOfGyroscopeData = resultData.getIntArray("numOfGyroscopeData")


                var isProcessSuccessful = false

                // 데이터 정규화. 5초 마다 구간 설정.
                if (accelerometerData != null && numOfAccelerometerData != null) {
                    isProcessSuccessful = normalizeData(accelerometerData, numOfAccelerometerData, applicationContext)
                    if (!isProcessSuccessful) error("데이터 정규화 오류")
                } else {
                    error("데이터 수집 오류")
                }
                if (magnetometerData != null && numOfMagnetometerData != null) {
                    isProcessSuccessful = normalizeData(magnetometerData, numOfMagnetometerData, applicationContext)
                    if (!isProcessSuccessful) error("데이터 정규화 오류")
                } else {
                    error("데이터 수집 오류")
                }
                if (gyroscopeData != null && numOfGyroscopeData != null) {
                    isProcessSuccessful = normalizeData(gyroscopeData, numOfGyroscopeData, applicationContext)
                    if (!isProcessSuccessful) error("데이터 정규화 오류")
                } else {
                    error("데이터 수집 오류")
                }


                // 파이어베이스에 업로드할 데이터들
                val accX = ArrayList<Float>(SAMPLING_RATE)
                val accY = ArrayList<Float>(SAMPLING_RATE)
                val accZ = ArrayList<Float>(SAMPLING_RATE)
                val magX = ArrayList<Float>(SAMPLING_RATE)
                val magY = ArrayList<Float>(SAMPLING_RATE)
                val magZ = ArrayList<Float>(SAMPLING_RATE)
                val gyrX = ArrayList<Float>(SAMPLING_RATE)
                val gyrY = ArrayList<Float>(SAMPLING_RATE)
                val gyrZ = ArrayList<Float>(SAMPLING_RATE)


                // 1초 당 64개의 데이터만 뽑아내 사용
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


                 */

                button.isClickable = true
            }
        }
    }
}
