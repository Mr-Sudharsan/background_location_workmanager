package com.kttelematictask.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.kttelematictask.databinding.ActivitySignInBinding
import com.kttelematictask.realm.Database
import com.kttelematictask.realm.User
import com.kttelematictask.utils.Constants
import com.kttelematictask.utils.PreferenceManager
import io.realm.kotlin.Realm
import io.realm.kotlin.ext.query

class SignInActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignInBinding
    private lateinit var preferenceManager: PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)
        preferenceManager = PreferenceManager(this)
        if(preferenceManager.getBoolean(Constants.IS_LOGGED_IN)){
            val intent = Intent(this@SignInActivity,UsersActivity::class.java)
            intent.putExtra("userId",preferenceManager.getString(Constants.KEY_USER_ID))
            startActivity(intent)
            finish()
        }
        binding.textCreateNewAccount.setOnClickListener {
            val intent = Intent(this@SignInActivity,SignUpActivity::class.java)
            startActivity(intent)
        }

        binding.buttonSignIn.setOnClickListener{
            if(isValidSignInDetails()){
                loading(true)
                try {
                    checkUser(binding.inputUserName.text.toString().trim(),binding.inputPassword.text.toString().trim())
                }catch (e:Exception){
                    e.printStackTrace()
                    loading(false)
                    showToast(e.message.toString())
                }
            }
        }

    }


     private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun isValidSignInDetails(): Boolean {
        return if (binding.inputUserName.text.toString().trim().isEmpty()) {
            showToast("Enter your username")
            false
        } else if (binding.inputPassword.text.toString().trim().isEmpty()) {
            showToast("Enter your password")
            false
        } else {
            true;
        }
    }

    private fun signIn(user: User) {
        preferenceManager.putString(Constants.KEY_USER_ID, user.userId)
        preferenceManager.putBoolean(Constants.IS_LOGGED_IN, true)
        val intent = Intent(this@SignInActivity,UsersActivity::class.java)
        intent.putExtra("userId",user.userId)
        startActivity(intent)
        finish()
    }

    private fun checkUser(userName : String,password : String){
        val user: User? = Database.usersRealmOpen.query<User>("userId == $0 AND password == $1", userName,password).first().find()
        if(user!=null){
            signIn(user)
            loading(false)
        }else{
            showToast("User name is not available")
            loading(false)
        }
    }

    private fun loading(isLoading: Boolean) {
        if (isLoading) {
            binding.buttonSignIn.visibility = View.INVISIBLE
            binding.progressBar.visibility = View.VISIBLE
        } else {
            binding.buttonSignIn.visibility = View.VISIBLE
            binding.progressBar.visibility = View.INVISIBLE
        }
    }


}