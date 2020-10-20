package edu.skku.cs.autosen

import kotlinx.coroutines.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import edu.skku.cs.autosen.api.ServerApi
import edu.skku.cs.autosen.login.LoginActivity
import edu.skku.cs.autosen.sensor.AuthenticationService
import kotlinx.android.synthetic.main.activity_main.*

import edu.skku.cs.autosen.sensor.MyReceiver
import edu.skku.cs.autosen.sensor.SensorMeasurementService
import edu.skku.cs.autosen.utility.loadID
import edu.skku.cs.autosen.utility.saveID
import java.util.*
import kotlin.concurrent.timer
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import java.util.concurrent.Executor

const val RESULT_CODE = 101

class MainActivity : AppCompatActivity() {
    companion object {
        var LANGUAGE = "KOREAN"
        var userId = ""
        var secsUploaded = 0
        var isStopped = false
        var isServiceDestroyed =false
        var authentication = ""

        lateinit var executer: Executor
        lateinit var biometricPrompt: BiometricPrompt
        lateinit var promptInfo: BiometricPrompt.PromptInfo
        lateinit var camera: android.hardware.Camera
    }

    private val possibleTestIdSet = hashSetOf("sungjae","heidi","chettem","wiu",
    "seongjeong","youngoh","jinsol", "hanjun", "kan", "chanhee", "yewon", "fah", "other1", "other2", "other3")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (Locale.getDefault().language != "ko") {
            LANGUAGE = "OTHERS"
            button.text = "Start"
        }

        //userId = ID.text.toString()
        userId = loadID(applicationContext)

        if (LANGUAGE == "KOREAN") {
            infoText.text = "어서오세요, ${userId}님!"
        } else infoText.text = "Welcome, ${userId}!"

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        @RequiresApi(Build.VERSION_CODES.M)
        if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
            val intent = Intent()
            intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.setData(Uri.parse("package:" + packageName))
            startActivityForResult(intent, 0)
        }

        logoutButton.setOnClickListener {
            isStopped = true
            saveID("", applicationContext)
            val intent = Intent(baseContext, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        button.setOnClickListener {
            button.isClickable = false
            isStopped = false

            if (userId.equals("")) {
                if (LANGUAGE == "KOREAN")
                    //Toast.makeText(applicationContext, "ID를 입력해주세요.", Toast.LENGTH_SHORT).show()
                    Toast.makeText(applicationContext, "ID 불러오기에 문제가 생겼습니다. 로그아웃 후 재접속해주세요.", Toast.LENGTH_SHORT).show()
                //else Toast.makeText(applicationContext, "Please enter ID.", Toast.LENGTH_SHORT).show()
                else Toast.makeText(applicationContext, "An error occured with ID. Please sign in again.", Toast.LENGTH_SHORT).show()
                button.isClickable = true
            } else if (!possibleTestIdSet.contains(userId)) {
                if (LANGUAGE == "KOREAN")
                    Toast.makeText(applicationContext, "허가되지 않은 ID입니다.", Toast.LENGTH_SHORT).show()
                else Toast.makeText(applicationContext, "Not Allowed ID", Toast.LENGTH_SHORT).show()
                button.isClickable = true
            } else {
                runBlocking {
                    try {
                        val response = ServerApi.instance.getSecs(userId)
                        if (response.data != null) {
                            secsUploaded = response.data.toInt()
                            textView.text = "${secsUploaded} / 21600"
                        }

                        if (secsUploaded >= 60 * 60 * 6) {
                            if (LANGUAGE == "KOREAN")
                                Toast.makeText(applicationContext, "이미 충분한 데이터가 등록되어 있습니다.", Toast.LENGTH_LONG).show()
                            else Toast.makeText(applicationContext, "Enough data are already registered with your ID", Toast.LENGTH_LONG).show()
                            button.isClickable = true
                        }

                        // Start Service
                        val intent = Intent(baseContext, SensorMeasurementService::class.java)
                        //intent.putExtra("receiver",receiver )
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent)
                        else startService(intent)
                    } catch (e: Exception) {
                        Log.e("asdf", "getSecs API 호출 오류", e)
                        if (LANGUAGE == "KOREAN")
                            Toast.makeText(applicationContext, "인터넷 연결 오류", Toast.LENGTH_LONG).show()
                        else Toast.makeText(applicationContext, "Internet Access Error", Toast.LENGTH_LONG).show()
                        button.isClickable = true
                    }
                }
            }
        }

        stopButton.setOnClickListener {
            isStopped = true
        }



        // Biometric Authentication
        executer = ContextCompat.getMainExecutor(applicationContext)
        biometricPrompt = BiometricPrompt(this, executer, object: BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int,
                                               errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                Toast.makeText(applicationContext,
                    "Authentication error: $errString", Toast.LENGTH_SHORT)
                    .show()
            }

            override fun onAuthenticationSucceeded(
                result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                Toast.makeText(applicationContext,
                    "Authentication succeeded!", Toast.LENGTH_SHORT)
                    .show()
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Toast.makeText(applicationContext, "Authentication failed",
                    Toast.LENGTH_SHORT)
                    .show()
            }
        })
        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("생체인증 필요")
            .setSubtitle("비정상적인 사용을 감지했습니다")
            //.setNegativeButtonText("cancel")
            .setDeviceCredentialAllowed(true)
            .build()
        //biometricPrompt.authenticate(promptInfo)


        val receiver = MyReceiver(Handler())
        receiver.setReceiver(obj)

        predictButton.setOnClickListener {

            button.isClickable = false
            //userId = ID.text.toString()
            if (userId.equals("sungjae")) {
                // Start Service
                val intent = Intent(baseContext, AuthenticationService::class.java)
                intent.putExtra("receiver",receiver )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent)
                else startService(intent)
            }
            button.isClickable = true





        }

        timer(period = 1000L) {
            runOnUiThread {
                //predictText.text = authentication


            }
        }

        timer(period = 1000L) {
            runOnUiThread {
                if (isServiceDestroyed) {
                    isServiceDestroyed = false
                    button.isClickable = true
                }
                textView.text = "현재까지 수집한 데이터: ${secsUploaded} / 21600"
            }
        }
    }

    private val obj = object: MyReceiver.Receiver {
        override fun onReceiverResult(resultCode: Int, resultData: Bundle){
            if (resultCode == RESULT_CODE) {
                val data = resultData.getByteArray("pic")
                if (data != null) {
                    val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
                    val matrix = Matrix()
                    matrix.setRotate(270.0f, (bitmap.width / 2).toFloat(), (bitmap.height / 2).toFloat())

                    val converted = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                    imageView.setImageBitmap(converted)
                }

            }
        }
    }
}
