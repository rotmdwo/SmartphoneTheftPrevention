package edu.skku.cs.autosen

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.ResultReceiver
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*
import java.text.SimpleDateFormat

const val RESULT_CODE = 101

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val obj = object: MyReceiver.Receiver {
            override fun onReceiverResult(resultCode: Int, resultData: Bundle){
                if (resultCode == RESULT_CODE) {
                    val string = resultData.getString("str")
                    val numOfAccelerometerData = resultData.getInt("numOfAccelerometerData")
                    val numOfMagnetometerData = resultData.getInt("numOfMagnetometerData")
                    val numOfGyroscopeData = resultData.getInt("numOfGyroscopeData")
                    textView.text = "acc: ${numOfAccelerometerData}개  mag: ${numOfMagnetometerData}개  gyro: ${numOfGyroscopeData}개"
                    button.isClickable = true
                }
            }
        }
        val reciever = MyReceiver(Handler())
        reciever.setReceiver(obj)

        button.setOnClickListener {
            button.isClickable = false

            // Start Service
            val intent = Intent(applicationContext, SensorMeasurementIntentService::class.java)
            intent.putExtra("receiver",reciever )
            startService(intent)
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
