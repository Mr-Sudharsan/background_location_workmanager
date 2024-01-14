package com.kttelematictask.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.kttelematictask.databinding.ActivitySignUpBinding
import com.kttelematictask.realm.Database
import com.kttelematictask.realm.User
import io.realm.kotlin.ext.query

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setListeners()
    }

    private fun setListeners() {
        binding.textSignIn.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        binding.buttonSignUp.setOnClickListener {
            if (isValidSignUpDetails()) {
                signUp()
            }
        }


    }

    private fun signUp() {
        loading(true)
            try {
                checkUser(binding.inputUserName.text.toString().trim())
            }catch (e:Exception){
                e.printStackTrace()
            }
    }

    private fun checkUser(userName : String){
        // Search equality on the primary key field name
        val user: User? = Database.usersRealmOpen.query<User>("userId == $0", userName).first().find()
        if(user==null){
            write()
        }else{
            showToast("User name is already present")
            loading(false)
        }
    }


    private fun write(){
        try {
            val userModel = User().apply {
                userId = binding.inputUserName.text.toString().trim()
                password = binding.inputPassword.text.toString().trim()
                email = binding.inputEmail.text.toString().trim()
            }
            Database.usersRealmOpen.writeBlocking {
                copyToRealm(userModel)
            }
            showToast("Signup Successful")
            Log.d("","Write operation completed successfully")
        } catch (e: Exception) {
            Log.d("","Error during write operation: $e")
            showToast(e.message.toString())
            loading(false)
        } finally {
            loading(false)
            val intent = Intent(this@SignUpActivity,SignInActivity::class.java)
            startActivity(intent)
        }

    }


    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun isValidSignUpDetails(): Boolean {
        if (binding.inputUserName.text.toString().trim().isEmpty()) {
            showToast("Enter your username")
            return false
        } else if (binding.inputEmail.text.toString().trim().isEmpty()) {
            showToast("Enter your email")
            return false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(binding.inputEmail.text.toString()).matches()) {
            showToast("Enter a valid email")
            return false
        } else if (binding.inputPassword.text.toString().trim().isEmpty()) {
            showToast("Enter your password")
            return false
        } else if (binding.inputConfirmPassword.text.toString().trim().isEmpty()) {
            showToast("Confirm your password")
            return false
        } else if (binding.inputPassword.text.toString() != binding.inputConfirmPassword.text.toString()) {
            showToast("Password & confirm password must be same");
            return false;
        } else {
            return true;
        }
    }

    private fun loading(isLoading: Boolean) {
        if (isLoading) {
            binding.buttonSignUp.visibility = View.INVISIBLE
            binding.progressBar.visibility = View.VISIBLE
        } else {
            binding.buttonSignUp.visibility = View.VISIBLE
            binding.progressBar.visibility = View.INVISIBLE
        }
    }

}