package com.example.instagramcloneapp.Fragments

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.instagramcloneapp.AccountSettingsActivity
import com.example.instagramcloneapp.Adapter.MyImagesAdapter
import com.example.instagramcloneapp.MainActivity
import com.example.instagramcloneapp.Model.Post
import com.example.instagramcloneapp.Model.User
import com.example.instagramcloneapp.R
import com.example.instagramcloneapp.ShowUsersActivity
import com.google.android.material.bottomnavigation.BottomNavigationItemView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_profile.*
import kotlinx.android.synthetic.main.fragment_profile.view.*
import java.util.*
import kotlin.collections.ArrayList

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [ProfileFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ProfileFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private lateinit var profileId: String
    private lateinit var firebaseUser: FirebaseUser

    private lateinit var progressDialog: ProgressDialog

    private var postList: List<Post>? = null
    private var myImagesAdapter : MyImagesAdapter? = null

    private var postListSaved: List<Post>? = null
    private var myImagesAdapterSavedImages : MyImagesAdapter? = null
    private var mySavesImg : List<String>? = null       //This will store data of all the images saved by user on basis of postId. Then we'll put all those post in postListSaved array.

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        progressDialog = ProgressDialog(context)
        progressDialog.show()
        progressDialog.setContentView(R.layout.progress_bar)

        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        firebaseUser = FirebaseAuth.getInstance().currentUser!!


        val pref = context?.getSharedPreferences("PREFS", Context.MODE_PRIVATE)
        if (pref != null)
        {
            this.profileId = pref.getString("profileId", "none").toString()
        }

        //profileId == firebaseUser.uid means that a user is viewing his own profile.
        if (profileId == firebaseUser.uid)
        {
            view.edit_account_settings_btn.text = "Edit Profile"
        }
        else if (profileId != firebaseUser.uid)
        {
            checkFollowAndFollowingButtonStatus()
        }

        //Code to display all the uploaded images on user's profile in grid/horizontal way.
        val recyclerViewUploadedImages : RecyclerView
        recyclerViewUploadedImages = view.findViewById(R.id.recycler_view_uploaded_pic)
        recyclerViewUploadedImages.setHasFixedSize(true)
        val linearLayoutManager:LinearLayoutManager = GridLayoutManager(context, 3)
        recyclerViewUploadedImages.layoutManager = linearLayoutManager

        postList = ArrayList()
        myImagesAdapter = context?.let { MyImagesAdapter(it, postList as ArrayList<Post>, profileId) }
        recyclerViewUploadedImages.adapter = myImagesAdapter


        //Code to display all the saved images
        val recyclerViewSavedImages : RecyclerView
        recyclerViewSavedImages = view.findViewById(R.id.recycler_view_saved_pic)
        recyclerViewSavedImages.setHasFixedSize(true)
        val linearLayoutManager2:LinearLayoutManager = GridLayoutManager(context, 3)
        recyclerViewSavedImages.layoutManager = linearLayoutManager2

        postListSaved = ArrayList()
        myImagesAdapterSavedImages = context?.let { MyImagesAdapter(it, postListSaved as ArrayList<Post>, profileId) }
        recyclerViewSavedImages.adapter = myImagesAdapterSavedImages

        //By default user always sees his Uploaded images.
        recyclerViewSavedImages.visibility = View.GONE
        recyclerViewUploadedImages.visibility = View.VISIBLE

        //This piece of code decides weather to show uploaded images or saved images.
        val uploadedImagesBtn: ImageButton = view.findViewById(R.id.image_grid_view_btn)
        uploadedImagesBtn.setOnClickListener{
            recyclerViewSavedImages.visibility = View.GONE
            recyclerViewUploadedImages.visibility = View.VISIBLE
        }

        val savedImagesBtn: ImageButton = view.findViewById(R.id.image_save_btn)
        savedImagesBtn.setOnClickListener{
            recyclerViewSavedImages.visibility = View.VISIBLE
            recyclerViewUploadedImages.visibility = View.GONE
        }

        //Display Followers
        view.Followers.setOnClickListener{
            val intent = Intent(context, ShowUsersActivity::class.java)
            intent.putExtra("id", profileId)
            intent.putExtra("title", "Followers")
            startActivity(intent)
        }

        //Display Following
        view.Following.setOnClickListener{
            val intent = Intent(context, ShowUsersActivity::class.java)
            intent.putExtra("id", profileId)
            intent.putExtra("title", "Following")
            startActivity(intent)
        }

        view.edit_account_settings_btn.setOnClickListener {
            val getButtonText = view.edit_account_settings_btn.text.toString()

            when
            {
                getButtonText == "Edit Profile" -> startActivity(Intent(context, AccountSettingsActivity::class.java))

                getButtonText == "Follow" -> {

                    firebaseUser?.uid.let { it1 ->
                        FirebaseDatabase.getInstance().reference
                            .child("Follow").child(it1.toString())
                            .child("Following").child(profileId)
                            .setValue(true)
                    }

                    firebaseUser?.uid.let { it1 ->
                        FirebaseDatabase.getInstance().reference
                            .child("Follow").child(profileId)
                            .child("Followers").child(it1.toString())
                            .setValue(true)
                    }

                    //Send notification for follow
                    addNotification()
                }

                getButtonText == "Following" -> {

                    firebaseUser?.uid.let { it1 ->
                        FirebaseDatabase.getInstance().reference
                            .child("Follow").child(it1.toString())
                            .child("Following").child(profileId)
                            .removeValue()
                    }

                    firebaseUser?.uid.let { it1 ->
                        FirebaseDatabase.getInstance().reference
                            .child("Follow").child(profileId)
                            .child("Followers").child(it1.toString())
                            .removeValue()
                    }

                }
            }

        }

        //Get the number of followers
        getFollowers()

        //Get the number of following
        getFollowings()

        //Display ALl the User information on the profile
        userInfo()

        //Pass a list of all the uploaded images
        myPhotos()

        //Get total number of posts
        getTotalNumberOfPosts()

        //Get postId of all the saved post by online user
        mySaves()

        return view
    }

    private fun checkFollowAndFollowingButtonStatus()
    {
        val followingRef = firebaseUser?.uid.let { it1 ->
            FirebaseDatabase.getInstance().reference
                .child("Follow").child(it1.toString())
                .child("Following")
        }

        if (followingRef != null)
        {
            followingRef.addValueEventListener(object : ValueEventListener
            {
                override fun onDataChange(p0: DataSnapshot)
                {
                    if (p0.child(profileId).exists())
                    {
                        view?.edit_account_settings_btn?.text = "Following"
                    }
                    else
                    {
                        view?.edit_account_settings_btn?.text = "Follow"
                    }
                }

                override fun onCancelled(p0: DatabaseError) {

                }
            })
        }
    }


    private fun getFollowers()
    {
        val followersRef = FirebaseDatabase.getInstance().reference
            .child("Follow").child(profileId)
            .child("Followers")

        followersRef.addValueEventListener(object : ValueEventListener
        {
            override fun onDataChange(p0: DataSnapshot)
            {
                if (p0.exists())
                {
                    view?.total_followers?.text = p0.childrenCount.toString()
                }
            }

            override fun onCancelled(p0: DatabaseError) {

            }
        })
    }


    private fun getFollowings()
    {
        val followersRef = FirebaseDatabase.getInstance().reference
            .child("Follow").child(profileId)
            .child("Following")


        followersRef.addValueEventListener(object : ValueEventListener
        {
            override fun onDataChange(p0: DataSnapshot)
            {
                if (p0.exists())
                {
                    view?.total_following?.text = p0.childrenCount.toString()
                }
            }

            override fun onCancelled(p0: DatabaseError) {

            }
        })
    }

    private fun userInfo()
    {
        val usersRef = FirebaseDatabase.getInstance().reference.child("Users").child(profileId)

        usersRef.addValueEventListener(object : ValueEventListener
        {
            override fun onDataChange(p0: DataSnapshot)
            {
                if (p0.exists())
                {
                    val user = p0.getValue<User>(User::class.java)

                    Picasso.get().load(user!!.getImage()).placeholder(R.drawable.profile).into(view?.profile_image_profile_frag)
                    view?.profile_fragment_username?.text = user!!.getUserName()

                    //Capitalize firstletter of first name and last name
                    var fullname = user!!.getFullName()
                    val str = fullname.split(" ").map { it.trim() }

                    view?.full_name?.text = str[0].capitalize()+ " "+str[1].capitalize()
                    view?.bio_profile_frag?.text = user!!.getBio()

                    progressDialog.dismiss()
                }
            }

            override fun onCancelled(p0: DatabaseError) {

            }
        })
    }

    private fun myPhotos(){
        val postsRef = FirebaseDatabase.getInstance().reference.child("Posts")

        postsRef.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.exists()){
                    (postList as ArrayList<Post>).clear()

                    for(p0 in snapshot.children){
                        val post = p0.getValue(Post::class.java)!!

                        if(post.getPublisher() == profileId){
                            (postList as ArrayList<Post>).add(post)
                        }

                        Collections.reverse(postList)
                        myImagesAdapter?.notifyDataSetChanged()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    private fun getTotalNumberOfPosts(){
        val postRef = FirebaseDatabase.getInstance().reference.child("Posts")

        postRef.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.exists()){
                    var postCounter = 0

                    //In all the posts, if online user's id equals to the publisher id, that means that that post is created by online user.
                    for(p0 in snapshot.children){
                        val post = p0.getValue(Post::class.java)
                        if(post!!.getPublisher() == profileId){
                            postCounter++
                        }
                    }

                    total_posts.text = " " + postCounter.toString()
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    private fun mySaves(){
        mySavesImg = ArrayList()

        val savesRef = FirebaseDatabase.getInstance().reference
            .child("Saves").child(firebaseUser.uid)

        savesRef.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.exists()){
                    for(p0 in snapshot.children){
                        (mySavesImg as ArrayList<String>).add(p0.key!!)
                    }

                    //Get all the post with the help of these keys.
                    readSavedImagesData()
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    private fun readSavedImagesData(){
        val postRef = FirebaseDatabase.getInstance().reference.child("Posts")

        postRef.addValueEventListener(object :ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.exists()){
                    (postListSaved as ArrayList<Post>).clear()

                    for(p0 in snapshot.children){
                        val post = p0.getValue(Post::class.java)

                        for(key in mySavesImg!!){
                            if(post!!.getPostid() == key){
                                (postListSaved as ArrayList<Post>).add(post)
                                break
                            }
                        }
                    }

                    myImagesAdapterSavedImages!!.notifyDataSetChanged()
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    private fun addNotification(){
        val notiRef = FirebaseDatabase.getInstance().reference
            .child("Notifications")
            .child(profileId)

        val notiMap = HashMap<String, Any>()
        notiMap["userid"] = firebaseUser!!.uid
        notiMap["text"] = "liked your post"
        notiMap["postid"] = ""
        notiMap["ispost"] = true

        notiRef.push().setValue(notiMap)
    }

    override fun onStop() {
        super.onStop()

        val pref = context?.getSharedPreferences("PREFS", Context.MODE_PRIVATE)?.edit()
        pref?.putString("profileId", firebaseUser.uid)
        pref?.apply()
    }

    override fun onPause() {
        super.onPause()

        val pref = context?.getSharedPreferences("PREFS", Context.MODE_PRIVATE)?.edit()
        pref?.putString("profileId", firebaseUser.uid)
        pref?.apply()
    }

    override fun onDestroy() {
        super.onDestroy()

        val pref = context?.getSharedPreferences("PREFS", Context.MODE_PRIVATE)?.edit()
        pref?.putString("profileId", firebaseUser.uid)
        pref?.apply()
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment ProfileFragment.
         */
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ProfileFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}

