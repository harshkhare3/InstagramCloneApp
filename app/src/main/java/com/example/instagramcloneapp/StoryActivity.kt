package com.example.instagramcloneapp

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import com.example.instagramcloneapp.Adapter.StoryAdapter
import com.example.instagramcloneapp.Model.Story
import com.example.instagramcloneapp.Model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso
import jp.shts.android.storiesprogressview.StoriesProgressView
import kotlinx.android.synthetic.main.activity_story.*

class StoryActivity : AppCompatActivity(), StoriesProgressView.StoriesListener {
    var currentUserId:String = ""
    var userId: String = ""
    var counter = 0

    var pressTime = 0L
    var limit = 500L

    var imagesList: List<String>? = null
    var storyIdsList: List<String>? = null
    var storiesProgressView: StoriesProgressView? = null

    //On TOuch event works when we touch a story and the story progress bar stops moving.
    @SuppressLint("ClickableViewAccessibility")
    private val onTouchListener = View.OnTouchListener { view, motionEvent ->
        when(motionEvent.action){
            MotionEvent.ACTION_DOWN ->{
                pressTime = System.currentTimeMillis()
                storiesProgressView!!.pause()
                return@OnTouchListener false
            }
            MotionEvent.ACTION_UP ->{
                val now = System.currentTimeMillis()
                storiesProgressView!!.resume()
                return@OnTouchListener limit< now-pressTime
            }
        }
        false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_story)

        currentUserId = FirebaseAuth.getInstance().currentUser!!.uid
        userId = intent.getStringExtra("userId").toString()

        storiesProgressView = findViewById(R.id.stories_progress)

        layout_seen.visibility = View.GONE
        story_delete.visibility = View.GONE

        if(userId == currentUserId){
            layout_seen.visibility = View.VISIBLE
            story_delete.visibility = View.VISIBLE
        }

        //Get all the stories and userInfo
        getStories(userId!!)
        userInfo(userId!!)

        val reverse: View = findViewById(R.id.reverse)
        reverse.setOnClickListener{ storiesProgressView!!.reverse() }
        reverse.setOnTouchListener (onTouchListener)

        val skip: View = findViewById(R.id.skip)
        skip.setOnClickListener{ storiesProgressView!!.skip() }
        skip.setOnTouchListener (onTouchListener)

        //Show list of people who see the story.
        layout_seen.setOnClickListener {
            val intent = Intent(this@StoryActivity, ShowUsersActivity::class.java)
            intent.putExtra("id", userId)
            intent.putExtra("storyid", storyIdsList!![counter])
            intent.putExtra("title", "views" )
            startActivity(intent)
        }

        //Delete Story
        story_delete.setOnClickListener {
            val ref = FirebaseDatabase.getInstance().reference
                .child("Story")
                .child(userId)
                .child(storyIdsList!![counter])

            ref.removeValue().addOnCompleteListener {
                if(it.isSuccessful){
                    Toast.makeText(this@StoryActivity, "Deleted successfully...", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun getStories(userId: String){
        imagesList = ArrayList()
        storyIdsList = ArrayList()

        val ref = FirebaseDatabase.getInstance().reference
            .child("Story")
            .child(userId!!)

        ref.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                (imagesList as ArrayList<String>).clear()
                (storyIdsList as ArrayList<String>).clear()

                for(p0 in snapshot.children){
                    val story: Story? = p0.getValue<Story>(Story::class.java)
                    val timeCurrent = System.currentTimeMillis()

                    if(timeCurrent > story!!.getTimeStart() && timeCurrent < story!!.getTimeEnd()){
                        (imagesList as ArrayList<String>).add(story.getImageUrl())
                        (storyIdsList as ArrayList<String>).add(story.getStoryId())
                    }
                }

                //Set the length of Story Progress View Bar.
                storiesProgressView!!.setStoriesCount((imagesList as ArrayList<String>).size)
                storiesProgressView!!.setStoryDuration(6000L) // 6 seconds

                storiesProgressView!!.setStoriesListener(this@StoryActivity)
                storiesProgressView!!.startStories(counter)

                Picasso.get().load(imagesList!!.get(counter)).into(image_story)

                addViewToStory(storyIdsList!!.get(counter))
                seenNumber(storyIdsList!!.get(counter))
            }


            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })
    }

    //Add a view of online user once he sees a story by adding his user id to views node.
    private fun addViewToStory(storyId: String){
        FirebaseDatabase.getInstance().reference
            .child("Story")
            .child(userId)
            .child(storyId)
            .child("views")
            .child(currentUserId)
            .setValue(true)
    }

    private fun seenNumber(storyId:String){
        val ref = FirebaseDatabase.getInstance().reference
            .child("Story")
            .child(userId!!)
            .child(storyId)
            .child("views")

        ref.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                seen_number.text = "" + snapshot.childrenCount
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    private fun userInfo(userId: String)
    {
        val usersRef = FirebaseDatabase.getInstance().reference.child("Users").child(userId)

        usersRef.addValueEventListener(object : ValueEventListener
        {
            override fun onDataChange(p0: DataSnapshot)
            {
                if (p0.exists())
                {
                    val user = p0.getValue<User>(User::class.java)

                    Picasso.get().load(user!!.getImage()).placeholder(R.drawable.profile).into(story_profile_image)
                    story_username!!.text = user.getUserName()
                }
            }

            override fun onCancelled(p0: DatabaseError) {

            }
        })
    }

    //By viewing next story of same person, we increment counter variable.
    override fun onNext() {
        Picasso.get().load(imagesList!![++counter]).into(image_story)
        addViewToStory(storyIdsList!![counter])
        seenNumber(storyIdsList!![counter])
    }

    //On going to a previous story, we can decrement counter but no need to add a view as we have seen it earlier.
    override fun onPrev() {

        if(counter -1 < 0){
            return
        }
        Picasso.get().load(imagesList!![--counter]).into(image_story)
        seenNumber(storyIdsList!![counter])
    }

    override fun onComplete() {
        finish()
    }

    //If the user close the app
    override fun onDestroy() {
        super.onDestroy()
        storiesProgressView!!.destroy()
    }

    //If we minimize the app
    override fun onPause() {
        super.onPause()
        storiesProgressView!!.pause()
    }

    //after re opening the app
    override fun onResume() {
        super.onResume()
        storiesProgressView!!.resume()

    }
}