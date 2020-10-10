package com.example.instagramcloneapp.Fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.instagramcloneapp.Adapter.PostAdapter
import com.example.instagramcloneapp.Adapter.StoryAdapter
import com.example.instagramcloneapp.Model.Post
import com.example.instagramcloneapp.Model.Story
import com.example.instagramcloneapp.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [homeFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class homeFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    //For Posts
    private var postAdapter: PostAdapter? = null
    private var postList : MutableList<Post>? = null
    private var followingList: MutableList<String> = mutableListOf(String())

    //For Stories
    private var storyAdapter: StoryAdapter? = null
    private var storyList : MutableList<Story>? = null

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
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        //Recycler view for posts
        var recyclerView: RecyclerView? = null
        recyclerView = view.findViewById(R.id.recycler_view_home)

        //Posts will be displayed in a linear layout fashion on home screen.
        val linearLayoutManager = LinearLayoutManager(context)
        //reverse layout means the latest post will be shown on top.
        linearLayoutManager.reverseLayout = true
        linearLayoutManager.stackFromEnd = true

        recyclerView.layoutManager = linearLayoutManager

        postList = ArrayList()
        postAdapter = context?.let { PostAdapter(it, postList as ArrayList<Post>) }
        recyclerView.adapter = postAdapter

        //Recycler view for stories
        var recyclerViewStory:RecyclerView? = null
        recyclerViewStory = view.findViewById(R.id.recycler_view_story)
        recyclerViewStory.setHasFixedSize(true)
        val linearLayoutManager2 = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        recyclerViewStory.layoutManager = linearLayoutManager2

        storyList = ArrayList()
        storyAdapter = context?.let { StoryAdapter(it, storyList as ArrayList<Story>) }
        recyclerViewStory.adapter = storyAdapter

        //Check the following list of a user and shows the post of the people who they follow.
        checkfollowing()

        return view
    }

    private fun checkfollowing() {
        val followingRef = FirebaseDatabase.getInstance().reference
                .child("Follow").child(FirebaseAuth.getInstance().currentUser!!.uid)
                .child("Following")

        followingRef.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(p0: DataSnapshot) {
                if(p0.exists()){
                    (followingList as ArrayList<String>).clear()

                    for(snapshot in p0.children){
                        snapshot.key?.let{ (followingList as ArrayList<String>).add(it) }
                    }

                    //Add our own id (Online user's id) to the followingList
                    (followingList as ArrayList<String>).add(FirebaseAuth.getInstance().currentUser!!.uid)

                    //Retrieve all the posts and stories of users present in our following list.
                    retrievePosts()
                    retrieveStories()
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    private fun retrievePosts(){
        val postsRef = FirebaseDatabase.getInstance().reference.child("Posts")
        postsRef.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                postList?.clear()
                for(p0 in snapshot.children){
                    //Make a post based on model class
                    val post = p0.getValue(Post::class.java)

                    //Add that post to postList if its id is present in followingList
                    for(userId in (followingList as ArrayList<String>)){
                         if(post!!.getPublisher() == userId){
                             postList!!.add(post)
                         }
                        postAdapter!!.notifyDataSetChanged()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    private fun retrieveStories(){
        val storyRef = FirebaseDatabase.getInstance().reference.child("Story")

        storyRef.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val timeCurrent = System.currentTimeMillis()
                (storyList as ArrayList<Story>).clear()

                (storyList as ArrayList<Story>).add(Story("", 0, 0, "", FirebaseAuth.getInstance().currentUser!!.uid))

                for(id in followingList){
                    var countStory = 0
                    var story: Story? = null

                    for(p0 in snapshot.child(id).children){
                        story = p0.getValue(Story::class.java)

                        if(timeCurrent > story!!.getTimeStart() && timeCurrent<story!!.getTimeEnd()){
                            countStory++
                        }
                    }

                    if(countStory>0){
                        (storyList as ArrayList<Story>).add(story!!)
                    }
                }
                storyAdapter!!.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment homeFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            homeFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}