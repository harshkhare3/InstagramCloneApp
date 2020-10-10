package com.example.instagramcloneapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.instagramcloneapp.Adapter.CommentAdapter
import com.example.instagramcloneapp.Adapter.PostAdapter
import com.example.instagramcloneapp.Model.Comment
import com.example.instagramcloneapp.Model.Post
import com.example.instagramcloneapp.Model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_account_settings.*
import kotlinx.android.synthetic.main.activity_comments.*
import android.content.Intent as Intent1

class CommentsActivity : AppCompatActivity() {

    private  var postId = ""
    private var publisherId = ""
    private var firebaseUser: FirebaseUser? = null
    private var commentAdapter: CommentAdapter? = null
    private var commentList: MutableList<Comment>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comments)

        val intent = intent
        postId = intent.getStringExtra("postId")!!
        publisherId = intent.getStringExtra("publisherId")!!

        firebaseUser = FirebaseAuth.getInstance().currentUser

        val recyclerView : RecyclerView = findViewById(R.id.recycler_view_comments)
        val linearLayoutManager = LinearLayoutManager(this)
        linearLayoutManager.reverseLayout = true
        recyclerView.layoutManager = linearLayoutManager

        commentList = ArrayList()
        commentAdapter = this?.let { CommentAdapter(it, commentList as ArrayList<Comment>) }
        recyclerView.adapter = commentAdapter

        //Load user profile image
        userInfo()

        //Display all the comments
        readComments()

        //Display Post image
        getPostImage()

        post_comment.setOnClickListener{
            if(add_comment!!.text.toString() == ""){
                Toast.makeText(this@CommentsActivity, "Please write a comment first ", Toast.LENGTH_LONG).show()
            }
            else{
                //Add a comment to database
                addComment()
            }
        }
    }

    private fun userInfo(){
        val usersRef = FirebaseDatabase.getInstance().reference.child("Users").child(firebaseUser!!.uid)

        usersRef.addValueEventListener(object: ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.exists()){
                    val user = snapshot.getValue<User>(User::class.java)

                    Picasso.get().load(user!!.getImage()).placeholder(R.drawable.profile).into(profile_image_comment)
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    private fun addComment(){
        val commentsRef = FirebaseDatabase.getInstance().reference
            .child("Comments")
            .child(postId!!)

        val commentsMap = HashMap<String, Any>()
        commentsMap["comment"] = add_comment!!.text.toString()
        commentsMap["publisher"] = firebaseUser!!.uid

        commentsRef.push().setValue(commentsMap)

        //Send a notification after adding comment.
        addNotification()

        add_comment!!.text.clear()
    }

    private fun readComments(){
        val commentsRef = FirebaseDatabase.getInstance()
            .reference.child("Comments")
            .child(postId)

        commentsRef.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.exists()){
                    commentList!!.clear()

                    for(p0 in snapshot.children){
                        val comment = p0.getValue(Comment::class.java)
                        commentList!!.add(comment!!)
                    }

                    commentAdapter!!.notifyDataSetChanged()
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    private fun getPostImage(){
        val postRef = FirebaseDatabase.getInstance().reference
            .child("Posts").child(postId).child("postimage")

        postRef .addValueEventListener(object: ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.exists()){
                    val image = snapshot.value.toString()

                    Picasso.get().load(image).placeholder(R.drawable.profile).into(post_image_comment)
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    private fun addNotification(){
        val notiRef = FirebaseDatabase.getInstance().reference
            .child("Notifications")
            .child(publisherId!!)

        val notiMap = HashMap<String, Any>()
        notiMap["userid"] = firebaseUser!!.uid
        notiMap["text"] = "commented: " + add_comment!!.text.toString()
        notiMap["postid"] = postId
        notiMap["ispost"] = true

        notiRef.push().setValue(notiMap)
    }
}