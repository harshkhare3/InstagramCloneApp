package com.example.instagramcloneapp

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.icu.util.ULocale.getName
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import androidx.fragment.app.FragmentManager
import com.example.instagramcloneapp.Fragments.ProfileFragment
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.android.material.bottomnavigation.BottomNavigationItemView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.StorageTask
import com.google.firebase.storage.UploadTask
import com.theartofdev.edmodo.cropper.CropImage
import kotlinx.android.synthetic.main.activity_account_settings.*
import kotlinx.android.synthetic.main.activity_add_post.*
import java.lang.Character.getName

class AddPostActivity : AppCompatActivity() {
    private var myUrl = ""
    private var imageUri: Uri? = null
    private var storagePostPictureRef: StorageReference? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_post)

        //Firebase storage reference
        storagePostPictureRef = FirebaseStorage.getInstance().reference.child("Posts Pictures")

        save_new_post_btn.setOnClickListener {
            uploadImage()
        }

        image_post.setOnClickListener {
            CropImage.activity()
                .setInitialCropWindowPaddingRatio(0F)
                .setMaxCropResultSize(10000,7000)
                .start(this@AddPostActivity)
        }

        CropImage.activity()
            .setInitialCropWindowPaddingRatio(0F)
            .setMaxCropResultSize(10000,7000)
            .start(this@AddPostActivity)
    }

    override fun onBackPressed() {
        finish()
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE && resultCode == Activity.RESULT_OK && data!=null){
            val result = CropImage.getActivityResult(data)
            imageUri = result.uri
            image_post.setImageURI(imageUri)
        }
        else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE){
            Toast.makeText(this, "Please select a correct image of jpg/jpeg format", Toast.LENGTH_LONG).show()
        }
        else{
            super.onBackPressed()
            val intent = Intent(this@AddPostActivity, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun uploadImage() {
        when{
            imageUri == null -> { Toast.makeText(this, "Please select an image first! ", Toast.LENGTH_SHORT).show() }
            TextUtils.isEmpty(description_post.text.toString()) -> { Toast.makeText(this, "Please enter a caption with this image! ", Toast.LENGTH_SHORT).show() }

            else ->{
                val progressDialog = ProgressDialog(this)
                progressDialog.setTitle("Adding")
                progressDialog.setMessage("Please wait a moment...")
                progressDialog.show()

                //A user can upload many images so we store those images base on unique id's. Here we use System time as an id.
                val fileRef = storagePostPictureRef!!.child(System.currentTimeMillis().toString() + ".jpg")

                //Uploading image to FireBase Storage and getting a download url for it.
                var uploadTask: StorageTask<*>
                uploadTask = fileRef.putFile(imageUri!!)

                uploadTask.continueWithTask(Continuation <UploadTask.TaskSnapshot, Task<Uri>>{ task->
                    if(!task.isSuccessful){
                        task.exception?.let{
                            throw it
                            progressDialog.dismiss()
                        }
                    }
                    return@Continuation fileRef.downloadUrl
                }).addOnCompleteListener (OnCompleteListener<Uri>{ task->

                    //Adding picture to firebase database so that we can retrieve it and display it.
                    if(task.isSuccessful){
                        val downloadURL = task.result
                        myUrl = downloadURL.toString()

                        val ref = FirebaseDatabase.getInstance().reference.child("Posts")

                        //Creating a random key to act as postId
                        var postId = ref.push().key

                        val postMap = HashMap<String, Any>()
                        postMap["postid"] = postId!!
                        postMap["postimage"] = myUrl
                        postMap["description"] = description_post.text.toString()
                        postMap["publisher"] = FirebaseAuth.getInstance().currentUser!!.uid

                        ref.child(postId).updateChildren(postMap)
                        Toast.makeText(this, "Post has been uploaded successfully.", Toast.LENGTH_LONG).show()

                        //Finish the current activity and return to Home page.
                        val intent = Intent(this@AddPostActivity, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                        progressDialog.dismiss()
                    }
                    else{
                        progressDialog.dismiss()
                    }
                })

            }
        }
    }
}