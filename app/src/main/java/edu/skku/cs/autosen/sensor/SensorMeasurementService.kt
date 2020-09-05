package edu.skku.cs.autosen.sensor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.*
import android.util.Log
import android.widget.Toast
import edu.skku.cs.autosen.MainActivity.Companion.userId
import edu.skku.cs.autosen.R
import edu.skku.cs.autosen.RESULT_CODE
import edu.skku.cs.autosen.utility.checkDataRetrievalIfSuccessful
import edu.skku.cs.autosen.utility.normalizeData
import edu.skku.cs.autosen.utility.sampleData
import edu.skku.cs.autosen.utility.uploadData
import java.text.SimpleDateFormat

class SensorMeasurementService : Service() {
    val timeFormat = SimpleDateFormat("mm:ss:SSS")
    var str = ""

    // 시간 설정
    companion object {
        val MINUTES: Long = 15
        val SECONDS: Long = MINUTES * 60
    }
    val DELAYED_TIME : Long = 1000 * SECONDS
    var previousTime = 0L
    var elapsedTime = 0L

    // 각 초 마다의 수집된 데이터 개수
    var numOfAccelerometerData = IntArray((SECONDS + 1).toInt(), {0})
    var numOfMagnetometerData = IntArray((SECONDS + 1).toInt(), {0})
    var numOfGyroscopeData = IntArray((SECONDS + 1).toInt(), {0})

    // 센서 마다의 수집된 전체 데이터 개수
    var numOfAllAccelerometerData = 0
    var numOfAllMagnetometerData = 0
    var numOfAllGyroscopeData = 0

    // 각 센서 데이터. x, y, z 순서로 들어감.
    val DATA_ARRAY_SIZE = 100 * 3 * (SECONDS.toInt() + 60)
    val accelerometerData = FloatArray(DATA_ARRAY_SIZE, {0.0f})
    val magnetometerData = FloatArray(DATA_ARRAY_SIZE, {0.0f})
    val gyroscopeData = FloatArray(DATA_ARRAY_SIZE, {0.0f})

    val ANDROID_CHANNNEL_ID = "edu.skku.cs.autosen"
    val NOTIFICATION_ID = 5534
    private val SAMPLING_RATE: Int = 64

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // For foreground service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(ANDROID_CHANNNEL_ID,
                "Retrieve Sensor Data", NotificationManager.IMPORTANCE_NONE)

            notificationChannel.lightColor = Color.WHITE
            notificationChannel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(notificationChannel)

            val notificationBuilder = Notification.Builder(this, ANDROID_CHANNNEL_ID)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("센서 데이터를 수집중입니다.")

            val notification = notificationBuilder.build()

            startForeground(NOTIFICATION_ID, notification)
        }

        val handlerThread = HandlerThread("background-thread")
        handlerThread.start()
        val mHandler = Handler(handlerThread.looper)

        // Sensors
        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val acceleroSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val gyroSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        val magneticSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        // Listeners
        val acceleroLis = AccelerometerListener()
        val gyroLis = GyrometerListener()
        val magneticLis = MagneticListener()


        previousTime = System.currentTimeMillis()

        // 레지스터 등록. 마지막 Parameter로 수집 속도 결정 10^-6초 단위
        sensorManager.registerListener(acceleroLis, acceleroSensor, 10000)
        sensorManager.registerListener(gyroLis, gyroSensor, 10000)
        sensorManager.registerListener(magneticLis, magneticSensor, 10000)

        val resultReceiver = intent!!.getParcelableExtra<ResultReceiver>("receiver")
        var bundle = Bundle()

        previousTime = System.currentTimeMillis()

        mHandler.postDelayed({
            sensorManager.unregisterListener(acceleroLis)
            sensorManager.unregisterListener(gyroLis)
            sensorManager.unregisterListener(magneticLis)

            /*  startActivity로 서비스의 결과를 전달하면, 백그라운드 상태에서는 액티비티가 인텐트를 받지 못한다.
            val backIntent = Intent(applicationContext, MainActivity::class.java)
            backIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            backIntent.putExtra("string", str)
            startActivity(backIntent)
            */

            // 액티비티로 데이터 보내기
            //bundle.putString("str",str)
            //bundle.putInt("numOfAccelerometerData", numOfAccelerometerData)
            //bundle.putInt("numOfMagnetometerData", numOfMagnetometerData)
            //bundle.putInt("numOfGyroscopeData", numOfGyroscopeData)
            /*
            bundle.putIntArray("numOfAccelerometerData", numOfAccelerometerData)
            bundle.putIntArray("numOfMagnetometerData", numOfMagnetometerData)
            bundle.putIntArray("numOfGyroscopeData", numOfGyroscopeData)
            bundle.putFloatArray("accelerometerData", accelerometerData)
            bundle.putFloatArray("magnetometerData", magnetometerData)
            bundle.putFloatArray("gyroscopeData", gyroscopeData)
            resultReceiver.send(RESULT_CODE, bundle)

             */

            Toast.makeText(this, "Successfully Retrieved Data", Toast.LENGTH_SHORT).show()

            for (i in 0 until 100) {
                Log.d("asdf", "1: ${accelerometerData[i]}")
            }

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

            for (i in 0 until 100) {
                Log.d("asdf", "2: ${accelerometerData[i]}")
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

            for (i in 0 until 64) {
                Log.d("asdf", "3: ${accX[i]}")
            }


            // 데이터가 제대로 수집 되었는 지 확인
            isProcessSuccessful = checkDataRetrievalIfSuccessful(accX, accY, accZ, magX, magY, magZ, gyrX, gyrY, gyrZ, applicationContext)
            if (!isProcessSuccessful) error("데이터 수집 오류")


            // 데이터 업로드
            uploadData(SAMPLING_RATE, accX, accY, accZ, magX, magY, magZ, gyrX, gyrY, gyrZ, userId)

            resultReceiver.send(RESULT_CODE, bundle)

        }, DELAYED_TIME)

        return Service.START_REDELIVER_INTENT
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    inner class AccelerometerListener : SensorEventListener {

        override fun onAccuracyChanged(p0: Sensor?, p1: Int) {

        }

        override fun onSensorChanged(p0: SensorEvent?) {
            if (p0!!.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                //textView.text = "${textView.text}${timeFormat.format(System.currentTimeMillis())} accX: ${p0!!.values[0]} accY: ${p0!!.values[1]} accZ: ${p0!!.values[2]}\n"
                //str += "${timeFormat.format(System.currentTimeMillis())} accX: ${p0!!.values[0]} accY: ${p0!!.values[1]} accZ: ${p0!!.values[2]}\n"
                elapsedTime = System.currentTimeMillis() - previousTime
                val index = (elapsedTime / 1000).toInt()
                /*
                var base = 0
                for (i in 0..index) {
                    base += numOfAccelerometerData[i]
                }
                accelerometerData[base * 3 + 0] = p0.values[0]
                accelerometerData[base * 3 + 1] = p0.values[1]
                accelerometerData[base * 3 + 2] = p0.values[2]
                numOfAccelerometerData[index]++

                 */

                accelerometerData[numOfAllAccelerometerData * 3 + 0] = p0.values[0]
                accelerometerData[numOfAllAccelerometerData * 3 + 1] = p0.values[1]
                accelerometerData[numOfAllAccelerometerData * 3 + 2] = p0.values[2]
                numOfAllAccelerometerData++
                numOfAccelerometerData[index]++

                //Log.d("asdf","collecting: " + p0.values[0])
            }
        }
    }

    inner class GyrometerListener : SensorEventListener {

        override fun onAccuracyChanged(p0: Sensor?, p1: Int) {

        }

        override fun onSensorChanged(p0: SensorEvent?) {
            if (p0!!.sensor.type == Sensor.TYPE_GYROSCOPE) {
                //textView.text = "${textView.text}${timeFormat.format(System.currentTimeMillis())} gyroX: ${p0!!.values[0]} gyroY: ${p0!!.values[1]} gyroZ: ${p0!!.values[2]}\n"
                //str += "${timeFormat.format(System.currentTimeMillis())} gyroX: ${p0!!.values[0]} gyroY: ${p0!!.values[1]} gyroZ: ${p0!!.values[2]}\n"
                elapsedTime = System.currentTimeMillis() - previousTime
                val index = (elapsedTime / 1000).toInt()
                /*
                var base = 0
                for (i in 0..index) {
                    base += numOfGyroscopeData[i]
                }
                gyroscopeData[base * 3 + 0] = p0.values[0]
                gyroscopeData[base * 3 + 1] = p0.values[1]
                gyroscopeData[base * 3 + 2] = p0.values[2]
                numOfGyroscopeData[index]++

                 */


                gyroscopeData[numOfAllGyroscopeData * 3 + 0] = p0.values[0]
                gyroscopeData[numOfAllGyroscopeData * 3 + 1] = p0.values[1]
                gyroscopeData[numOfAllGyroscopeData * 3 + 2] = p0.values[2]
                numOfAllGyroscopeData++
                numOfGyroscopeData[index]++

            }
        }
    }

    inner class MagneticListener : SensorEventListener {

        override fun onAccuracyChanged(p0: Sensor?, p1: Int) {

        }

        override fun onSensorChanged(p0: SensorEvent?) {
            if (p0!!.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                //textView.text = "${textView.text}${timeFormat.format(System.currentTimeMillis())} magX: ${p0!!.values[0]} magY: ${p0!!.values[1]} magZ: ${p0!!.values[2]}\n"
                //str += "${timeFormat.format(System.currentTimeMillis())} magX: ${p0!!.values[0]} magY: ${p0!!.values[1]} magZ: ${p0!!.values[2]}\n"
                elapsedTime = System.currentTimeMillis() - previousTime
                val index = (elapsedTime / 1000).toInt()
                /*
                var base = 0
                for (i in 0..index) {
                    base += numOfMagnetometerData[i]
                }
                magnetometerData[base * 3 + 0] = p0.values[0]
                magnetometerData[base * 3 + 1] = p0.values[1]
                magnetometerData[base * 3 + 2] = p0.values[2]
                numOfMagnetometerData[index]++

                 */


                magnetometerData[numOfAllMagnetometerData * 3 + 0] = p0.values[0]
                magnetometerData[numOfAllMagnetometerData * 3 + 1] = p0.values[1]
                magnetometerData[numOfAllMagnetometerData * 3 + 2] = p0.values[2]
                numOfAllMagnetometerData++
                numOfMagnetometerData[index]++

            }
        }
    }
}
