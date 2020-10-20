package edu.skku.cs.autosen.login

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import edu.skku.cs.autosen.MainActivity
import edu.skku.cs.autosen.R
import edu.skku.cs.autosen.utility.loadID
import edu.skku.cs.autosen.utility.saveID
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        var userId = loadID(applicationContext)

        if (userId != "") {
            val intent = Intent(baseContext, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        loginButton.setOnClickListener {
            userId = idText.text.toString()
            if (userId == "") Toast.makeText(applicationContext, "아이디를 입력해주세요.",Toast.LENGTH_SHORT).show()
            else {
                saveID(userId, applicationContext)
                val intent = Intent(baseContext, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }
}