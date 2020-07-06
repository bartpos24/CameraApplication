package com.example.myphotostock.login

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.example.myphotostock.MainActivity
import com.example.myphotostock.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class LoginActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    private lateinit var loginButton: Button
    private lateinit var registerButton: Button

    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        loginButton = findViewById(R.id.button)
        registerButton = findViewById(R.id.signUp_button)
        emailEditText = findViewById(R.id.email_editText)
        passwordEditText = findViewById(R.id.password_editText)

        registerButton.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        loginButton.setOnClickListener {
            doLogin()
        }


    }

    private fun doLogin() {
        if (emailEditText.text.toString().isEmpty()) {
            emailEditText.error = resources.getString(R.string.e_enter_mail)
            emailEditText.requestFocus()
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(emailEditText.text.toString()).matches()) {
            emailEditText.error = resources.getString(R.string.e_enter_valid_mail)
            emailEditText.requestFocus()
            return
        }
        if (passwordEditText.text.toString().isEmpty()) {
            passwordEditText.error = resources.getString(R.string.e_enter_password)
            passwordEditText.requestFocus()
            return
        }
        auth.signInWithEmailAndPassword(emailEditText.text.toString(), passwordEditText.text.toString())
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    updateUI(user)
                } else {
                    Toast.makeText(baseContext, resources.getString(R.string.e_login_fail), Toast.LENGTH_SHORT).show()
                    updateUI(null)
                }
            }
    }

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        updateUI(currentUser)
    }
    private fun updateUI(currentUser: FirebaseUser?) {
        if (currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
        else {
            Toast.makeText(baseContext, resources.getString(R.string.e_login_fail), Toast.LENGTH_SHORT).show()
        }

    }
}