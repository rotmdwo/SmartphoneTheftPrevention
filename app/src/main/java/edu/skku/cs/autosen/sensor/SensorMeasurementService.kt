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
import edu.skku.cs.autosen.MainActivity.Companion.userId
import edu.skku.cs.autosen.R
import edu.skku.cs.autosen.RESULT_CODE
import edu.skku.cs.autosen.utility.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs

class SensorMeasurementService : Service() {
    var accZPrevious = 0.0f

    // 시간 설정
    companion object {
        val MINUTES: Long = 15
        val SECONDS: Long = MINUTES * 60
    }
    var previousTime = 0L
    var elapsedTime = 0L
    //var secIndex = 0
    var secIndex = AtomicInteger(0)

    // 각 센서 데이터. FloatArray - index 0: X, index 1: Y, index 2: Z, index 3: Active/Inactive
    val accelerometerData = ArrayList<ArrayList<FloatArray>>(5)
    val magnetometerData = ArrayList<ArrayList<FloatArray>>(5)
    val gyroscopeData = ArrayList<ArrayList<FloatArray>>(5)

    val ANDROID_CHANNNEL_ID = "edu.skku.cs.autosen"
    val NOTIFICATION_ID = 5534
    val SAMPLING_RATE: Int = 64

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
        var secsUploaded = intent.getIntExtra("secsUploaded", 0)
        var bundle = Bundle()

        previousTime = System.currentTimeMillis()

        Thread(Runnable {
            while (secsUploaded < 60 * 60 * 10) {
                if (accelerometerData.size > 5) {
                    if (checkData(accelerometerData, magnetometerData, gyroscopeData, SAMPLING_RATE)) {
                        normalizeData(accelerometerData)
                        normalizeData(magnetometerData)
                        normalizeData(gyroscopeData)

                        val sampledAccelerometerData = sampleData(accelerometerData, SAMPLING_RATE)
                        val sampledMagnetometerData = sampleData(magnetometerData, SAMPLING_RATE)
                        val sampledGyroscopeData = sampleData(gyroscopeData, SAMPLING_RATE)

                        if (!checkInternetStatus(applicationContext)) {
                            sensorManager.unregisterListener(acceleroLis)
                            sensorManager.unregisterListener(gyroLis)
                            sensorManager.unregisterListener(magneticLis)

                            resultReceiver.send(RESULT_CODE, bundle)
                        }

                        uploadData(sampledAccelerometerData, sampledMagnetometerData, sampledGyroscopeData,
                            userId, SAMPLING_RATE, secsUploaded)

                        secsUploaded += 5
                        //secIndex = decreaseIndex(secIndex)
                        previousTime += 5000
                        secIndex.addAndGet(-5)

                        removeUploadedData(accelerometerData, magnetometerData, gyroscopeData)
                    } else {
                        //secIndex = decreaseIndex(secIndex)
                        previousTime += 5000
                        secIndex.addAndGet(-5)

                        removeUploadedData(accelerometerData, magnetometerData, gyroscopeData)
                    }
                }
            }

            sensorManager.unregisterListener(acceleroLis)
            sensorManager.unregisterListener(gyroLis)
            sensorManager.unregisterListener(magneticLis)

            resultReceiver.send(RESULT_CODE, bundle)
        }).start()

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

                elapsedTime = System.currentTimeMillis() - previousTime
                secIndex = AtomicInteger((elapsedTime / 1000).toInt())

                // 0.5%의 변화 미만은 바닥에 둔 걸로 판단 가능. 스마트폰을 들고 있을 때는 5초 이상 0.5% 이상 안 바뀌기 쉽지 않음.
                var isActive = 1.0f
                if (abs((p0.values[2] - accZPrevious) / accZPrevious) < 0.005) isActive = 0.0f

                accZPrevious = p0.values[2]

                if (accelerometerData.getOrNull(secIndex.toInt()) != null)
                    accelerometerData[secIndex.toInt()].add(floatArrayOf(p0.values[0], p0.values[1], p0.values[2], isActive))
                else {
                    accelerometerData.add(ArrayList())
                    accelerometerData[secIndex.toInt()].add(floatArrayOf(p0.values[0], p0.values[1], p0.values[2], isActive))
                }
            }
        }
    }

    inner class GyrometerListener : SensorEventListener {

        override fun onAccuracyChanged(p0: Sensor?, p1: Int) {

        }

        override fun onSensorChanged(p0: SensorEvent?) {
            if (p0!!.sensor.type == Sensor.TYPE_GYROSCOPE) {
                elapsedTime = System.currentTimeMillis() - previousTime
                secIndex = AtomicInteger((elapsedTime / 1000).toInt())

                if (gyroscopeData.getOrNull(secIndex.toInt()) != null)
                    gyroscopeData[secIndex.toInt()].add(floatArrayOf(p0.values[0], p0.values[1], p0.values[2]))
                else {
                    gyroscopeData.add(ArrayList())
                    gyroscopeData[secIndex.toInt()].add(floatArrayOf(p0.values[0], p0.values[1], p0.values[2]))
                }
            }
        }
    }

    inner class MagneticListener : SensorEventListener {

        override fun onAccuracyChanged(p0: Sensor?, p1: Int) {

        }

        override fun onSensorChanged(p0: SensorEvent?) {
            if (p0!!.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                elapsedTime = System.currentTimeMillis() - previousTime
                secIndex = AtomicInteger((elapsedTime / 1000).toInt())

                if (magnetometerData.getOrNull(secIndex.toInt()) != null)
                    magnetometerData[secIndex.toInt()].add(floatArrayOf(p0.values[0], p0.values[1], p0.values[2]))
                else {
                    magnetometerData.add(ArrayList())
                    magnetometerData[secIndex.toInt()].add(floatArrayOf(p0.values[0], p0.values[1], p0.values[2]))
                }
            }
        }
    }
}
