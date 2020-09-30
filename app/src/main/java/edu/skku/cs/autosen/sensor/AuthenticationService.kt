package edu.skku.cs.autosen.sensor

import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import edu.skku.cs.autosen.MainActivity
import edu.skku.cs.autosen.utility.*
import kotlin.concurrent.timer
import kotlin.math.abs

class AuthenticationService : Service() {
    val SAMPLING_RATE: Int = 64
    val service = this

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        //TODO: Notification Build
        Log.d("asdf","4")
        // Sensors
        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val acceleroSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val gyroSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        val magneticSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        timer(period = 15000L) {
            // 각 센서 데이터. FloatArray - index 0: X, index 1: Y, index 2: Z, index 3: Active/Inactive
            val accelerometerData = Array(6, {ArrayList<FloatArray>()})
            val magnetometerData = Array(6, {ArrayList<FloatArray>()})
            val gyroscopeData = Array(6, {ArrayList<FloatArray>()})

            var uploaded = false
            var accZPrevious = 0.0f
            var checkedStartTime = false
            var startTime = System.currentTimeMillis()

            class SensorListener: SensorEventListener {
                override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
                }

                override fun onSensorChanged(event: SensorEvent) {
                    if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                        if (!checkedStartTime) {
                            startTime = System.currentTimeMillis()
                            checkedStartTime = true
                        }
                        val elapsedTime = System.currentTimeMillis() - startTime
                        val secIndex = (elapsedTime / 1000).toInt()

                        // 0.5%의 변화 미만은 바닥에 둔 걸로 판단 가능. 스마트폰을 들고 있을 때는 5초 이상 0.5% 이상 안 바뀌기 쉽지 않음.
                        var isActive = 1.0f
                        if (abs((event.values[2] - accZPrevious) / accZPrevious) < 0.005) isActive = 0.0f

                        accZPrevious = event.values[2]

                        if (secIndex < 6)
                            accelerometerData[secIndex].add(floatArrayOf(event.values[0], event.values[1], event.values[2], isActive))
                    } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                        if (!checkedStartTime) {
                            startTime = System.currentTimeMillis()
                            checkedStartTime = true
                        }
                        val elapsedTime = System.currentTimeMillis() - startTime
                        val secIndex = (elapsedTime / 1000).toInt()

                        if (secIndex < 6)
                            magnetometerData[secIndex].add(floatArrayOf(event.values[0], event.values[1], event.values[2]))
                    } else if (event.sensor.type == Sensor.TYPE_GYROSCOPE) {
                        if (!checkedStartTime) {
                            startTime = System.currentTimeMillis()
                            checkedStartTime = true
                        }
                        val elapsedTime = System.currentTimeMillis() - startTime
                        val secIndex = (elapsedTime / 1000).toInt()

                        if (secIndex < 6)
                            gyroscopeData[secIndex].add(floatArrayOf(event.values[0], event.values[1], event.values[2]))
                    }
                }
            }

            val sensorListener = SensorListener()

            sensorManager.registerListener(sensorListener, acceleroSensor, 8000)
            sensorManager.registerListener(sensorListener, magneticSensor, 8000)
            sensorManager.registerListener(sensorListener, gyroSensor, 8000)

            timer(period = 100L) {
                if (MainActivity.secsUploaded < 60 * 60 * 6) {
                    if (accelerometerData[5].size > 0 && !uploaded && checkInternetStatus(applicationContext)) {
                        sensorManager.unregisterListener(sensorListener)

                        if (checkData(accelerometerData, magnetometerData, gyroscopeData, SAMPLING_RATE)) {
                            normalizeData(accelerometerData)
                            normalizeData(magnetometerData)
                            normalizeData(gyroscopeData)

                            val sampledAccelerometerData = sampleData(accelerometerData, SAMPLING_RATE)
                            val sampledMagnetometerData = sampleData(magnetometerData, SAMPLING_RATE)
                            val sampledGyroscopeData = sampleData(gyroscopeData, SAMPLING_RATE)

                            if (checkIfIdAvailable(MainActivity.userId, service)) {
                                authenticateData(sampledAccelerometerData, sampledMagnetometerData, sampledGyroscopeData,
                                    MainActivity.userId, SAMPLING_RATE)

                                uploaded = true

                                // timer안의 timer 명시적으로 종료하지 않으면 계속 살아있음
                                this.cancel()
                            }
                        } else {
                            this.cancel()
                        }
                    }
                } else {
                    MainActivity.isStopped = true
                    Toast.makeText(service, "Data Retrieving Ended.", Toast.LENGTH_LONG).show()
                }
            }
        }

        //return Service.START_REDELIVER_INTENT
        return START_NOT_STICKY
        }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }
}