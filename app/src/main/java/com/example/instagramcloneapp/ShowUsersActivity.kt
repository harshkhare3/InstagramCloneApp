package com.example.instagramcloneapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.instagramcloneapp.Adapter.UserAdapter
import com.example.instagramcloneapp.Model.User
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_show_users.*
import kotlinx.android.synthetic.main.fragment_profile.view.*
import kotlinx.android.synthetic.main.fragment_search.view.*
import kotlinx.android.synthetic.main.posts_layout.view.*

//This Activity helps to show a list of people who likes a post/ list of followers & following.
class ShowUsersActivity : AppCompatActivity() {

    var id: String = ""
    var title:String = ""
    var storyId = ""
    private var userAdapter: UserAdapter? = null
    private var userList: List<User>? = null
    private var idList: List<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_users)

        val intnt = intent
        id = intent.getStringExtra("id").toString()
        title = intnt.getStringExtra("title").toString()
        storyId = intent.getStringExtra("storyid").toString()

        toolbar_text.text = title
        toolbar_back.setOnClickListener {
            finish()
        }

        var recyclerView: RecyclerView = findViewById(R.id.recycler_view)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(this)
        userList = ArrayList()
        userAdapter = UserAdapter(this, userList as ArrayList<User>, false)
        recyclerView.adapter = userAdapter

        idList = ArrayList()

        when(title){
            "Likes" -> getLikes()
            "Following" -> getFollowing()
            "Followers" -> getFollowers()
            "views" -> getViews()   //TO get the number of views on a user's story.
        }
    }

    override fun onBackPressed() {
        finish()
    }
    //getLikes() takes in a postId and finds all the users who liked that post and put their userIds in idList ArrayList.
    private fun getLikes() {
        val LikesRef = FirebaseDatabase.getInstance().reference
            .child("Likes")
            .child(id!!)

        LikesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.exists()){
                    (idList as ArrayList<String>).clear()

                    for(p0 in snapshot.children){
                        (idList as ArrayList<String>).add(p0.key!!)
                    }

                    //Get all the users whose userId is present in idList
                    showUsers()
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    //getFollowing() takes in a profileId and finds all the users whom that person follows and put their userIds in idList ArrayList.
    private fun getFollowing() {
        val followersRef = FirebaseDatabase.getInstance().reference
            .child("Follow").child(id)
            .child("Following")


        followersRef.addValueEventListener(object : ValueEventListener
        {
            override fun onDataChange(snapshot: DataSnapshot)
            {
                if (snapshot.exists())
                {
                    (idList as ArrayList<String>).clear()
                    for(p0 in snapshot.children){
                        (idList as ArrayList<String>).add(p0.key!!)
                    }

                    showUsers()
                }
            }

            override fun onCancelled(p0: DatabaseError) {

            }
        })
    }

    //getFollowing() takes in a profileId and finds all the users who follows that person and put their userIds in idList ArrayList.
    private fun getFollowers() {
        val followersRef = FirebaseDatabase.getInstance().reference
            .child("Follow").child(id!!)
            .child("Followers")

        followersRef.addValueEventListener(object : ValueEventListener
        {
            override fun onDataChange(snapshot: DataSnapshot)
            {
                if (snapshot.exists())
                {
                    (idList as ArrayList<String>).clear()
                    for(p0 in snapshot.children){
                        (idList as ArrayList<String>).add(p0.key!!)
                    }

                    showUsers()
                }
            }

            override fun onCancelled(p0: DatabaseError) {

            }
        })
    }

    private fun getViews() {
        val ref = FirebaseDatabase.getInstance().reference
            .child("Story")
            .child(id)
            .child(intent.getStringExtra("storyid").toString())
            .child("views")

        ref.addValueEventListener(object : ValueEventListener
        {
            override fun onDataChange(snapshot: DataSnapshot)
            {
                if (snapshot.exists())
                {
                    (idList as ArrayList<String>).clear()
                    for(p0 in snapshot.children){
                        (idList as ArrayList<String>).add(p0.key!!)
                    }

                    showUsers()
                }
            }

            override fun onCancelled(p0: DatabaseError) {

            }
        })
    }


    private fun showUsers() {
        val usersRef = FirebaseDatabase.getInstance().getReference().child("Users")
        usersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(datasnapshot: DataSnapshot) {

                (userList as ArrayList<User>).clear()

                for(snapshot in datasnapshot.children){
                    val user = snapshot.getValue(User::class.java)

                    for(id in idList!!){
                        if(user!!.getUID() == id){
                            (userList as ArrayList<User>).add(user!!)
                            break;
                        }
                    }
                    userAdapter?.notifyDataSetChanged()
                }

            }
            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

}