package edu.skku.cs.autosen

import kotlinx.coroutines.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
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
import java.util.*
import kotlin.concurrent.timer
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import edu.skku.cs.autosen.utility.*
import java.util.concurrent.Executor

const val RESULT_CODE = 101

class MainActivity : AppCompatActivity() {
    companion object {
        var LANGUAGE = "KOREAN"
        var userId = ""
        var secsUploaded = 0
        var isStopped = false
        var isPredictionStopped = false
        var isServiceDestroyed =false
        var isPredictionServiceDestroyed = false
        var authentication = ""
        var hasModel = false

        var isDataSwitchOn = false
        var isPredictionSwitchOn = false

        lateinit var pic: Bitmap
        lateinit var picByteArray: ByteArray
        lateinit var executer: Executor
        lateinit var biometricPrompt: BiometricPrompt
        lateinit var promptInfo: BiometricPrompt.PromptInfo
        lateinit var camera: android.hardware.Camera
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (intent.hasExtra("pic")) {
            imageView.setImageBitmap(pic)
            // Biometric Authentication
            executer = ContextCompat.getMainExecutor(applicationContext)
            biometricPrompt = BiometricPrompt(this, executer, object: BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int,
                                                   errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(applicationContext,
                        "인증에러. 서버에 사진이 전송됩니다.", Toast.LENGTH_SHORT).show()

                    sendPicture(userId, picByteArray)
                }

                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Toast.makeText(applicationContext,
                        "인증성공", Toast.LENGTH_SHORT)
                        .show()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(applicationContext, "인증실패. 서버에 사진이 전송됩니다.",
                        Toast.LENGTH_SHORT).show()

                    sendPicture(userId, picByteArray)
                }
            })
            promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("생체인증 필요")
                .setSubtitle("비정상적인 사용을 감지했습니다")
                //.setNegativeButtonText("cancel")
                .setDeviceCredentialAllowed(true)
                .build()

            biometricPrompt.authenticate(promptInfo)
        }

        dataSwitch.isChecked = isDataSwitchOn
        predictSwitch.isChecked = isPredictionSwitchOn
        if (isDataSwitchOn) dataSwitch.text = "데이터 수집 종료"
        if (isPredictionSwitchOn) predictSwitch.text = "보안기능 끄기"

        if (Locale.getDefault().language != "ko") {
            LANGUAGE = "OTHERS"
            dataSwitch.text = "Start data retrieval"
            predictSwitch.text = "Turn on authentication mode"
        }

        //userId = ID.text.toString()
        userId = loadID(applicationContext)

        if (LANGUAGE == "KOREAN") {
            infoText.text = "어서오세요, ${userId}님!"
        } else infoText.text = "Welcome, ${userId}!"

        if (!hasModel) {
            if (loadModelAvailability(userId, applicationContext)) hasModel = true;
            else {
                runBlocking {
                    try {
                        val response = ServerApi.instance.checkIfModelExists(userId).data
                        if (response != null && response == "Exists") {
                            hasModel = true;
                            saveModelAvailability(userId, applicationContext)
                        } else {

                        }
                    } catch (e: Exception) {
                        Log.e("asdf", "sendData API 호출 오류", e)
                    }
                }
            }
        }

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
            isPredictionStopped = true

            saveID("", applicationContext)
            val intent = Intent(baseContext, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        dataSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) { // 데이터 수집 시작
                buttonView.text = "데이터 수집 종료"
                isStopped = false
                isDataSwitchOn = true

                if (userId.equals("")) {
                    if (LANGUAGE == "KOREAN")
                        Toast.makeText(applicationContext, "ID 불러오기에 문제가 생겼습니다. 로그아웃 후 재접속해주세요.", Toast.LENGTH_SHORT).show()
                    else Toast.makeText(applicationContext, "An error occured with ID. Please sign in again.", Toast.LENGTH_SHORT).show()
                }  else {
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
                            }

                            // Start Service
                            val intent = Intent(baseContext, SensorMeasurementService::class.java)
                            //intent.putExtra("receiver",receiver )
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent)
                            else startService(intent)
                        } catch (e: Exception) {
                            buttonView.isChecked = false
                            isDataSwitchOn = false
                            buttonView.text = "데이터 수집하기"
                            Log.e("asdf", "getSecs API 호출 오류", e)
                            if (LANGUAGE == "KOREAN")
                                Toast.makeText(applicationContext, "인터넷 연결 오류", Toast.LENGTH_LONG).show()
                            else Toast.makeText(applicationContext, "Internet Access Error", Toast.LENGTH_LONG).show()
                        }
                    }
                }

            } else { // 데이터 수집 종료
                buttonView.isClickable = false
                buttonView.text = "데이터 수집하기"
                isStopped = true
                isDataSwitchOn = false
            }
        }

        trainButton.setOnClickListener {
            runBlocking {
                try {
                    val response = ServerApi.instance.buildModel(userId).data

                    if (response.equals("No Data")) {
                        Toast.makeText(applicationContext,
                            "데이터를 먼저 수집해주세요.", Toast.LENGTH_SHORT).show()
                    } else if (response.equals("Already in Queue")) {
                        Toast.makeText(applicationContext,
                            "이미 모델을 만들고 있습니다.", Toast.LENGTH_SHORT).show()
                    } else {
                        val num = response!!.toInt()
                        Toast.makeText(applicationContext,
                            "모델생성을 시작합니다. ${num * 10}분 정도 소요될 예정입니다.",
                            Toast.LENGTH_SHORT).show()
                        timer(period = 60000L) {
                            if (hasModel) this.cancel()
                            runBlocking {
                                val response2 = ServerApi.instance.checkIfModelExists(userId).data
                                if (response2 != null && response2 == "Exists") {
                                    hasModel = true;
                                    saveModelAvailability(userId, applicationContext)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("asdf", "sendData API 호출 오류", e)
                }
            }
        }




        val receiver = MyReceiver(Handler())
        receiver.setReceiver(obj)

        predictSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                if (!hasModel) {
                    Toast.makeText(applicationContext,
                        "아직 만들어진 모델이 없습니다.", Toast.LENGTH_SHORT).show()
                } else {
                    buttonView.text = "보안기능 끄기"
                    isPredictionStopped = false
                    isPredictionSwitchOn = true

                    val intent = Intent(baseContext, AuthenticationService::class.java)
                    intent.putExtra("receiver",receiver )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent)
                    else startService(intent)
                }

            } else {
                buttonView.isClickable = false
                buttonView.text = "보안기능 켜기"
                isPredictionStopped = true
                isPredictionSwitchOn = false
            }
        }

        timer(period = 1000L) {
            runOnUiThread {
                if (isServiceDestroyed) {
                    isServiceDestroyed = false
                    dataSwitch.isClickable = true
                }
                if (isPredictionServiceDestroyed) {
                    isPredictionServiceDestroyed = false
                    predictSwitch.isClickable = true
                }
                textView.text = "현재까지 수집한 데이터: ${secsUploaded} / 21600"
                if (hasModel) trainText.text = "등록된 모델이 있습니다."
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
