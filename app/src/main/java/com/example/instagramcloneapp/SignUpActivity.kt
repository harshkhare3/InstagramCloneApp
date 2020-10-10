package com.example.instagramcloneapp

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_sign_up.*
import kotlin.collections.HashMap

class SignUpActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        signin_link_btn.setOnClickListener{
            startActivity(Intent(this, SignInActivity::class.java))
        }

        signup_btn.setOnClickListener{
            CreateAccount()
        }
    }

    private fun CreateAccount(){
        var fullName = fullname_signup.text.toString()
        var userName = username_signup.text.toString()
        var email = email_signup.text.toString()
        var password = password_signup.text.toString()

        when{
            TextUtils.isEmpty(fullName) -> Toast.makeText(this, "Your full name is required.", Toast.LENGTH_LONG).show()
            TextUtils.isEmpty(userName) -> Toast.makeText(this, "A username is required.", Toast.LENGTH_LONG).show()
            TextUtils.isEmpty(email) -> Toast.makeText(this, "Your email id is required.", Toast.LENGTH_LONG).show()
            TextUtils.isEmpty(password) -> Toast.makeText(this, "A password is required.", Toast.LENGTH_LONG).show()

            else -> {
                val progressDialog = ProgressDialog(this)
                progressDialog.setTitle("SignUp")
                progressDialog.setMessage("Please wait, this may take a while...")
                progressDialog.setCancelable(false)
                progressDialog.show()

                val mAuth:FirebaseAuth = FirebaseAuth.getInstance()

                mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener{task->
                    if(task.isSuccessful){
                        saveUserInfo(fullName, userName, email, progressDialog)
                    }
                    else{
                        val errorMessage = task.exception.toString()
                        Toast.makeText(this, "Error: ${errorMessage}", Toast.LENGTH_LONG).show()
                        mAuth.signOut()
                        progressDialog.dismiss()
                    }
                }
            }
        }
    }



    private fun saveUserInfo(fullName:String, userName:String, email:String, progressDialog: ProgressDialog){
        val currentUserID = FirebaseAuth.getInstance().currentUser!!.uid
        val usersRef: DatabaseReference = FirebaseDatabase.getInstance().reference.child("Users")

        val userMap = HashMap<String, Any>()
        userMap["uid"] = currentUserID
        userMap["fullName"] = fullName.toLowerCase()
        userMap["userName"] = userName
        userMap["email"] = email
        userMap["bio"] = "Hey I am using Instagram Clone App"
        userMap["image"] = "https://firebasestorage.googleapis.com/v0/b/instagram-clone-app-b20cb.appspot.com/o/Default%20Images%2Fprofile.png?alt=media&token=9555e373-fdc0-42dd-9db9-b21e27d5ddb1"

        usersRef.child(currentUserID).setValue(userMap).addOnCompleteListener {task->
            if(task.isSuccessful){
                progressDialog.dismiss()
                Toast.makeText(this, "Account has been created successfully.", Toast.LENGTH_LONG).show()

                val intent = Intent(this@SignUpActivity, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            }
            else{
                val errorMessage = task.exception.toString()
                Toast.makeText(this, "Error: ${errorMessage}", Toast.LENGTH_LONG).show()
                FirebaseAuth.getInstance().signOut()
                progressDialog.dismiss()
            }
        }
    }
}