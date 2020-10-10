package com.example.instagramcloneapp.Adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat.startActivity
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.instagramcloneapp.CommentsActivity
import com.example.instagramcloneapp.Fragments.PostDetailsFragment
import com.example.instagramcloneapp.Fragments.ProfileFragment
import com.example.instagramcloneapp.MainActivity
import com.example.instagramcloneapp.Model.Post
import com.example.instagramcloneapp.Model.User
import com.example.instagramcloneapp.R
import com.example.instagramcloneapp.ShowUsersActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.android.synthetic.main.activity_comments.*
import org.jetbrains.annotations.NotNull

class PostAdapter
    (private val mContext: Context,
     private val mPost: List<Post>) : RecyclerView.Adapter<PostAdapter.ViewHolder>()
{
    private var firebaseUser: FirebaseUser? = null

    //Passes the layout to inner class so that it can access all the variables from posts_layout file.
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(mContext).inflate(R.layout.posts_layout, parent,false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return mPost.size
    }

    //When user clicks on profile name, then it redirects it to the clicked profile. If we click on comments, then it gets redirected to all comments.
    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        firebaseUser = FirebaseAuth.getInstance().currentUser
        val post = mPost[position]

        //Loads Post image.
        Picasso.get().load(post.getPostimage()).into(holder.postImage)

        holder.postImage.setOnClickListener(object : DoubleClickListener(){
            override fun onDoubleClick(v: View) {
                FirebaseDatabase.getInstance().reference
                    .child("Likes")
                    .child(post.getPostid())
                    .child(firebaseUser!!.uid).setValue(true)
            }
        })

        //Displays publisher information
        publisherInfo(holder.profileImage, holder.userName,  post.getPublisher())

        //Loads post description/ Caption
        if(post.getDescription() == ""){
            holder.description.visibility = View.GONE
        }
        else{
            holder.description.visibility = View.VISIBLE
            holder.description.text =  post.getDescription()
        }

        //Check if a post is liked or not.
        isLikes(post.getPostid(), holder.likeButton)

        //Get the number of likes
        numberOfLikes(post.getPostid(), holder.likes)

        //Get the number of comments
        getTotalComments(post.getPostid(), holder.comments)

        //Check if a post is saved or unsaved
        checkedSavedStatus(post.getPostid(), holder.saveButton)

        holder.likeButton.setOnClickListener {
            if (holder.likeButton.tag == "Like"){
                FirebaseDatabase.getInstance().reference
                    .child("Likes")
                    .child(post.getPostid())
                    .child(firebaseUser!!.uid).setValue(true)

                //Add notification
                addNotification(post.getPublisher(), post.getPostid())
            }
            else{
                FirebaseDatabase.getInstance().reference
                    .child("Likes")
                    .child(post.getPostid())
                    .child(firebaseUser!!.uid).removeValue()

//                val intent = Intent(mContext, MainActivity::class.java)
//                mContext.startActivity(intent)
            }
        }

        //Send user to Comment activity
        holder.commentButton.setOnClickListener {
            val intentComment = Intent(mContext, CommentsActivity::class.java)
            intentComment.putExtra("postId", post.getPostid())
            intentComment.putExtra("publisherId", post.getPublisher())
            mContext.startActivity(intentComment)
        }

        holder.comments.setOnClickListener {
            val intentComment = Intent(mContext, CommentsActivity::class.java)
            intentComment.putExtra("postId", post.getPostid())
            intentComment.putExtra("publisherId", post.getPublisher())
            mContext.startActivity(intentComment)
        }

        //Save or unSave a photo based on weather it is saved or not.
        holder.saveButton.setOnClickListener {
           if(holder.saveButton.tag == "Save"){
               FirebaseDatabase.getInstance().reference
                   .child("Saves")
                   .child(firebaseUser!!.uid)
                   .child(post.getPostid()).setValue(true)
           }
            else if(holder.saveButton.tag == "Saved"){
               FirebaseDatabase.getInstance().reference
                   .child("Saves")
                   .child(firebaseUser!!.uid)
                   .child(post.getPostid()).removeValue()
           }
        }

        //List of people who liked the post
        holder.likes.setOnClickListener{
            val intent = Intent(mContext, ShowUsersActivity::class.java)
            intent.putExtra("id", post.getPostid())
            intent.putExtra("title", "Likes")
            mContext.startActivity(intent)
        }

        //GO to post Details
        holder.postImage.setOnClickListener {
            val editor = mContext.getSharedPreferences("PREFS", Context.MODE_PRIVATE).edit()
            editor.putString("postId", post.getPostid())
            editor.apply()
            (mContext as FragmentActivity).supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, PostDetailsFragment()).addToBackStack("postDetails").commit()
        }

        //Redirect to user's profile by clicking on its name/profile pic
        holder.publisher.setOnClickListener {
            val editor = mContext.getSharedPreferences("PREFS", Context.MODE_PRIVATE).edit()
            editor.putString("profileId", post.getPublisher())
            editor.apply()
            (mContext as FragmentActivity).supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ProfileFragment()).addToBackStack("profile ${post.getPublisher()}").commit()
        }
        holder.profileImage.setOnClickListener {
            val editor = mContext.getSharedPreferences("PREFS", Context.MODE_PRIVATE).edit()
            editor.putString("profileId", post.getPublisher())
            editor.apply()
            (mContext as FragmentActivity).supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ProfileFragment()).addToBackStack("profile ${post.getPublisher()}").commit()
        }

    }

    inner class ViewHolder(@NotNull itemView: View) : RecyclerView.ViewHolder(itemView){

        var profileImage: CircleImageView
        var postImage : ImageView
        var likeButton : ImageView
        var commentButton : ImageView
        var saveButton : ImageView

        var userName: TextView
        var likes: TextView
        var publisher: TextView
        var description: TextView
        var comments: TextView

        init{
            profileImage = itemView.findViewById(R.id.user_profile_image_post)
            postImage = itemView.findViewById(R.id.post_image_home)
            likeButton = itemView.findViewById(R.id.post_image_like_btn)
            commentButton = itemView.findViewById(R.id.post_image_comment_btn)
            saveButton = itemView.findViewById(R.id.post_save_btn)
            userName = itemView.findViewById(R.id.user_name_post)
            likes = itemView.findViewById(R.id.likes)
            publisher = itemView.findViewById(R.id.publisher)
            description = itemView.findViewById(R.id.description)
            comments = itemView.findViewById(R.id.comments)

        }
    }

    private fun publisherInfo(
        profileImage: CircleImageView,
        userName: TextView,
        publisherId: String, ) {
        val usersRef = FirebaseDatabase.getInstance().reference.child("Users").child(publisherId)

        usersRef.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.exists()){
                    val user = snapshot.getValue<User>(User::class.java)
                    Picasso.get().load(user!!.getImage()).placeholder(R.drawable.profile).into(profileImage)
                    userName.setText(user!!.getUserName())

//                    Capitalize firstletter of first name and last name
                    var fullname = user!!.getFullName()
                    val str = fullname.split(" ").map { it.trim() }

                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    private fun isLikes(postid: String, likeButton: ImageView) {
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        val LikesRef = FirebaseDatabase.getInstance().reference
            .child("Likes")
            .child(postid)

        LikesRef.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.child(firebaseUser!!.uid).exists()){
                    likeButton.setImageResource(R.drawable.heart_clicked)
                    likeButton.tag = "Liked"
                }
                else{
                    likeButton.setImageResource(R.drawable.heart_not_clicked)
                    likeButton.tag = "Like"
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })

    }

    private fun numberOfLikes(postid: String, likes: TextView) {
        val LikesRef = FirebaseDatabase.getInstance().reference
            .child("Likes")
            .child(postid)

        LikesRef.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.exists()){
                    likes.text = snapshot.childrenCount.toString() + " Likes"
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    private fun getTotalComments(postid: String, comments: TextView) {
        val CommentsRef = FirebaseDatabase.getInstance().reference
            .child("Comments")
            .child(postid)

        CommentsRef.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.exists()){
                    comments.text = "View all "+ snapshot.childrenCount.toString() + " Comments"
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    abstract class DoubleClickListener : View.OnClickListener {
        private var lastClickTime: Long = 0
        override fun onClick(v: View) {
            val clickTime = System.currentTimeMillis()
            if (clickTime - lastClickTime < DOUBLE_CLICK_TIME_DELTA) {
                onDoubleClick(v)
                lastClickTime = 0
            }
            lastClickTime = clickTime
        }
        abstract fun onDoubleClick(v: View)
        companion object {
            private const val DOUBLE_CLICK_TIME_DELTA: Long = 300 //milliseconds
        }
    }

    private fun checkedSavedStatus(postid: String, imageView: ImageView){
        val savesRef = FirebaseDatabase.getInstance().reference
            .child("Saves")
            .child(firebaseUser!!.uid)

        savesRef.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {

                //If the image is already saved by a user in database
                if(snapshot.child(postid).exists()){
                    imageView.setImageResource(R.drawable.save_fill)
                    imageView.tag = "Saved"
                }
                else{
                    imageView.setImageResource(R.drawable.save_empty)
                    imageView.tag = "Save"
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    private fun addNotification(userId:String, postId:String){
        val notiRef = FirebaseDatabase.getInstance().reference
            .child("Notifications")
            .child(userId)

        val notiMap = HashMap<String, Any>()
        notiMap["userid"] = firebaseUser!!.uid
        notiMap["text"] = "liked your post"
        notiMap["postid"] = postId
        notiMap["ispost"] = true

        notiRef.push().setValue(notiMap)
    }
}