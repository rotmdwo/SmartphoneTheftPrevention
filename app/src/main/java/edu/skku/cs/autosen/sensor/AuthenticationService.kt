package edu.skku.cs.autosen.sensor

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.hardware.*
import android.hardware.Camera
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.ResultReceiver
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import edu.skku.cs.autosen.MainActivity
import edu.skku.cs.autosen.MainActivity.Companion.isPredictionServiceDestroyed
import edu.skku.cs.autosen.MainActivity.Companion.isPredictionStopped
import edu.skku.cs.autosen.MainActivity.Companion.pic
import edu.skku.cs.autosen.MainActivity.Companion.picByteArray
import edu.skku.cs.autosen.R
import edu.skku.cs.autosen.utility.*
import kotlinx.coroutines.runBlocking
import kotlin.concurrent.timer
import kotlin.math.abs

class AuthenticationService : Service() {
    val SAMPLING_RATE: Int = 64
    val service = this

    val ANDROID_CHANNNEL_ID = "edu.skku.cs.autosen"
    val NOTIFICATION_ID = 5534

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // For foreground service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(ANDROID_CHANNNEL_ID,
                "Retrieve Sensor Data", NotificationManager.IMPORTANCE_DEFAULT)

            notificationChannel.lightColor = Color.RED
            notificationChannel.description ="데이터 수집중"
            //notificationChannel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            notificationChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(notificationChannel)

            var contentText = "센서 데이터를 수집중입니다."
            if (MainActivity.LANGUAGE == "OTHERS") contentText = "Retrieving Data"

            val notificationBuilder = Notification.Builder(this, ANDROID_CHANNNEL_ID)
                .setContentTitle(getString(R.string.app_name))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentText(contentText)
                .setAutoCancel(false)
                .setOngoing(true)

            val notification = notificationBuilder.build()

            startForeground(NOTIFICATION_ID, notification)
        }

        // Sensors
        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val acceleroSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val gyroSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        val magneticSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        val resultReceiver = intent!!.getParcelableExtra<ResultReceiver>("receiver")
        val bundle = Bundle()

        timer(period = 600000L) {
            if (isPredictionStopped) {
                stopService(intent)
                //sensorManager.unregisterListener(sensorListener)
                isPredictionServiceDestroyed = true
                this.cancel()
            }
            /*
            val surfaceTexture = SurfaceTexture(10)
            MainActivity.camera = Camera.open(1)
            MainActivity.camera.parameters.setPreviewSize(1,1)
            MainActivity.camera.setPreviewTexture(surfaceTexture)
            MainActivity.camera.startPreview()
            MainActivity.camera.takePicture(null, null, Camera.PictureCallback { data, camera ->
                bundle.putByteArray("pic", data)
                resultReceiver.send(101, bundle)

                picByteArray = data
                val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
                val matrix = Matrix()
                matrix.setRotate(270.0f, (bitmap.width / 2).toFloat(), (bitmap.height / 2).toFloat())
                pic = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

                // intent에 500kb 이상 정도의 데이터를 넣으면 전달이 안 됨
                val newIntent = Intent(applicationContext, MainActivity::class.java)
                newIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                newIntent.putExtra("pic", true)
                val pendingIntent = PendingIntent.getActivity(applicationContext, 0, newIntent, PendingIntent.FLAG_UPDATE_CURRENT)


                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val notificationChannel = NotificationChannel(
                        "Warning",
                        "Unauthorized User", NotificationManager.IMPORTANCE_HIGH
                    )

                    notificationChannel.lightColor = Color.RED
                    notificationChannel.description = "허가되지 않은 사용자"
                    notificationChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC

                    val notificationManager =
                        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.createNotificationChannel(notificationChannel)

                    var contentText = "허가되지 않은 사용자"
                    if (MainActivity.LANGUAGE == "OTHERS") contentText = "Unauthorized User"

                    val notificationBuilder = NotificationCompat.Builder(applicationContext, "Warning")
                        .setContentTitle("Warning!")
                        .setSmallIcon(R.drawable.common_full_open_on_phone)
                        .setLargeIcon(pic)
                        //.setSmallIcon(R.mipmap.ic_launcher)
                        .setContentText(contentText)
                        .setStyle(NotificationCompat.BigPictureStyle().bigPicture(pic))
                        .setAutoCancel(true)
                        .setOngoing(true)
                        .setContentIntent(pendingIntent)

                    //val notification = notificationBuilder.build()
                    with(NotificationManagerCompat.from(applicationContext)) {
                        notify(0, notificationBuilder.build())
                    }
                }


            })

             */

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
                                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                                authenticateData(sampledAccelerometerData, sampledMagnetometerData, sampledGyroscopeData,
                                    MainActivity.userId, SAMPLING_RATE, applicationContext, notificationManager)

                                uploaded = true

                                // timer안의 timer 명시적으로 종료하지 않으면 계속 살아있음
                                this.cancel()
                            }
                        } else {
                            this.cancel()
                        }
                    }
                } else {
                    isPredictionStopped = true
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