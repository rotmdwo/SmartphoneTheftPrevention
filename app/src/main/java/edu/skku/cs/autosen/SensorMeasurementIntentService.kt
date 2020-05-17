package edu.skku.cs.autosen

import android.app.IntentService
import android.content.Intent
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.ResultReceiver
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*
import java.text.SimpleDateFormat

class SensorMeasurementIntentService : IntentService("SensorMeasurementIntentService") {
    val timeFormat = SimpleDateFormat("mm:ss:SSS")
    var str = ""
    val accelerometerData = FloatArray(1000, {0.0f})
    val magnetometerData = FloatArray(1000, {0.0f})
    val gyroscopeData = FloatArray(1000, {0.0f})
    var numOfAccelerometerData: Int = 0
    var numOfMagnetometerData: Int = 0
    var numOfGyroscopeData: Int = 0


    override fun onHandleIntent(intent: Intent?) {
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


        val seconds = 1L
        val delayedTime : Long = 1000 * seconds

        // 레지스터 등록. 마지막 Parameter로 수집 속도 결정 10^-6초 단위
        sensorManager.registerListener(acceleroLis, acceleroSensor, 10000)
        sensorManager.registerListener(gyroLis, gyroSensor, 10000)
        sensorManager.registerListener(magneticLis, magneticSensor, 10000)


        val resultReceiver = intent!!.getParcelableExtra<ResultReceiver>("receiver")
        var bundle = Bundle()


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

            bundle.putString("str",str)
            bundle.putInt("numOfAccelerometerData", numOfAccelerometerData)
            bundle.putInt("numOfMagnetometerData", numOfMagnetometerData)
            bundle.putInt("numOfGyroscopeData", numOfGyroscopeData)
            resultReceiver.send(RESULT_CODE, bundle)



        }, delayedTime)
    }

    inner class AccelerometerListener : SensorEventListener {

        override fun onAccuracyChanged(p0: Sensor?, p1: Int) {

        }

        override fun onSensorChanged(p0: SensorEvent?) {
            if (p0!!.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                //textView.text = "${textView.text}${timeFormat.format(System.currentTimeMillis())} accX: ${p0!!.values[0]} accY: ${p0!!.values[1]} accZ: ${p0!!.values[2]}\n"
                str += "${timeFormat.format(System.currentTimeMillis())} accX: ${p0!!.values[0]} accY: ${p0!!.values[1]} accZ: ${p0!!.values[2]}\n"
                accelerometerData[numOfAccelerometerData * 3 + 0] = p0.values[0]
                accelerometerData[numOfAccelerometerData * 3 + 1] = p0.values[1]
                accelerometerData[numOfAccelerometerData * 3 + 2] = p0.values[2]
                numOfAccelerometerData++
            }
        }
    }

    inner class GyrometerListener : SensorEventListener {

        override fun onAccuracyChanged(p0: Sensor?, p1: Int) {

        }

        override fun onSensorChanged(p0: SensorEvent?) {
            if (p0!!.sensor.type == Sensor.TYPE_GYROSCOPE) {
                //textView.text = "${textView.text}${timeFormat.format(System.currentTimeMillis())} gyroX: ${p0!!.values[0]} gyroY: ${p0!!.values[1]} gyroZ: ${p0!!.values[2]}\n"
                str += "${timeFormat.format(System.currentTimeMillis())} gyroX: ${p0!!.values[0]} gyroY: ${p0!!.values[1]} gyroZ: ${p0!!.values[2]}\n"
                gyroscopeData[numOfGyroscopeData * 3 + 0] = p0.values[0]
                gyroscopeData[numOfGyroscopeData * 3 + 1] = p0.values[1]
                gyroscopeData[numOfGyroscopeData * 3 + 2] = p0.values[2]
                numOfGyroscopeData++
            }
        }
    }

    inner class MagneticListener : SensorEventListener {

        override fun onAccuracyChanged(p0: Sensor?, p1: Int) {

        }

        override fun onSensorChanged(p0: SensorEvent?) {
            if (p0!!.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                //textView.text = "${textView.text}${timeFormat.format(System.currentTimeMillis())} magX: ${p0!!.values[0]} magY: ${p0!!.values[1]} magZ: ${p0!!.values[2]}\n"
                str += "${timeFormat.format(System.currentTimeMillis())} magX: ${p0!!.values[0]} magY: ${p0!!.values[1]} magZ: ${p0!!.values[2]}\n"
                magnetometerData[numOfMagnetometerData * 3 + 0] = p0.values[0]
                magnetometerData[numOfMagnetometerData * 3 + 1] = p0.values[1]
                magnetometerData[numOfMagnetometerData * 3 + 2] = p0.values[2]
                numOfMagnetometerData++
            }
        }
    }
}
