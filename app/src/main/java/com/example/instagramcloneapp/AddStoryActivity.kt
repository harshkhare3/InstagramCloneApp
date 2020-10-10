package com.example.instagramcloneapp

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.StorageTask
import com.google.firebase.storage.UploadTask
import com.theartofdev.edmodo.cropper.CropImage
import kotlinx.android.synthetic.main.activity_add_post.*

class AddStoryActivity : AppCompatActivity() {

    private var myUrl = ""
    private var imageUri: Uri? = null
    private var storageStoryRef: StorageReference? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_story)

        //Firebase storage reference
        storageStoryRef = FirebaseStorage.getInstance().reference.child("Story Pictures")

        CropImage.activity()
            .setAspectRatio(9, 16)
            .start(this@AddStoryActivity)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE && resultCode == Activity.RESULT_OK && data!=null){
            val result = CropImage.getActivityResult(data)
            imageUri = result.uri

            uploadStory()
        }
        //If back button is pressed, return back to home activity.
        else{
            super.onBackPressed()
            val intent = Intent(this@AddStoryActivity, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun uploadStory(){
        when{
            imageUri == null -> { Toast.makeText(this, "Please select an image first! ", Toast.LENGTH_SHORT).show() }

            else ->{
                val progressDialog = ProgressDialog(this)
                progressDialog.setTitle("Adding")
                progressDialog.setMessage("Please wait a moment...")
                progressDialog.show()

                //A user can upload many images so we store those images base on unique id's. Here we use System time as an id.
                val fileRef = storageStoryRef!!.child(System.currentTimeMillis().toString() + ".jpg")

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

                    //Adding story to firebase database so that we can retrieve it and display it.
                    if(task.isSuccessful){
                        val downloadURL = task.result
                        myUrl = downloadURL.toString()

                        val ref = FirebaseDatabase.getInstance().reference
                            .child("Story")
                            .child(FirebaseAuth.getInstance().currentUser!!.uid)

                        //Creating a random key to act as storyId
                        val storyId = ref.push().key

                        val timeEnd = System.currentTimeMillis() + 86400000 // One day later

                        val storyMap = HashMap<String, Any>()
                        storyMap["userid"] = FirebaseAuth.getInstance().currentUser!!.uid
                        storyMap["timestart"] = ServerValue.TIMESTAMP
                        storyMap["timeend"] = timeEnd
                        storyMap["imageurl"] = myUrl
                        storyMap["storyid"] = storyId.toString()

                        ref.child(storyId.toString()).updateChildren(storyMap)
                        Toast.makeText(this, "Story has been uploaded successfully.", Toast.LENGTH_LONG).show()

                        //Finish the current activity and return to Home page.
                        val intent = Intent(this@AddStoryActivity, MainActivity::class.java)
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