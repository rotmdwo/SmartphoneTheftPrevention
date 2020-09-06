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
import java.util.*

const val RESULT_CODE = 101

class MainActivity : AppCompatActivity() {
    companion object {
        var LANGUAGE = "KOREAN"
        var userId = ""
    }

    private val possibleTestIdSet = hashSetOf("sungjae","heidi","chettem","wiu",
    "seongjeong","youngoh", "youngoh_family1", "youngoh_family2","jinsol", "jinsol_family1",
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
                        var secsUploaded = 0

                        for (i in userIds) {
                            val id = i.key

                            if (userId.equals(id)) {
                                secsUploaded = (i.value as Long).toInt()

                                if (secsUploaded >= 60 * 60 * 10) {
                                    if (LANGUAGE == "KOREAN")
                                        Toast.makeText(applicationContext, "이미 충분한 데이터가 등록되어 있습니다.", Toast.LENGTH_LONG).show()
                                    else Toast.makeText(applicationContext, "Enough data are already registered with your ID", Toast.LENGTH_LONG).show()
                                    button.isClickable = true

                                    return
                                } else {
                                    break
                                }
                            }
                        }

                        // Start Service
                        val intent = Intent(applicationContext, SensorMeasurementService::class.java)
                        intent.putExtra("receiver",receiver )
                        intent.putExtra("secsUploaded", secsUploaded)
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

                button.isClickable = true
            }
        }
    }
}
