package com.example.instagramcloneapp.Adapter

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.instagramcloneapp.AddStoryActivity
import com.example.instagramcloneapp.Model.Story
import com.example.instagramcloneapp.Model.User
import com.example.instagramcloneapp.R
import com.example.instagramcloneapp.StoryActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView
import org.jetbrains.annotations.NotNull

class StoryAdapter (private val mContext: Context, private val mStory: List<Story>)
    : RecyclerView.Adapter<StoryAdapter.ViewHolder>(){

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return if(viewType == 0){
            val view = LayoutInflater.from(mContext).inflate(R.layout.add_story_item, parent,false)
            ViewHolder(view)
        } else{
            val view = LayoutInflater.from(mContext).inflate(R.layout.story_item, parent,false)
            ViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val story = mStory[position]

        userInfo(holder, story.getUserId(), position)

        //adapterPosition = 0 means online user and !=0 means other users.
        if(holder.adapterPosition !== 0){
            seenStory(holder, story.getUserId())
        }
        if(holder.adapterPosition === 0){
            myStories(holder.addStory_text!!, holder.story_plus_btn!!, false)
        }

        holder.itemView.setOnClickListener {
            if(holder.adapterPosition ===0){
                myStories(holder.addStory_text!!, holder.story_plus_btn!!, true)
            }
            else{
                val intent = Intent(mContext, StoryActivity::class.java)
                intent.putExtra("userId", story.getUserId())
                mContext.startActivity(intent)
            }
        }
    }

    override fun getItemCount(): Int {
        return mStory.size
    }

    inner class ViewHolder(@NotNull itemView: View) : RecyclerView.ViewHolder(itemView){

        //Story Item View
        var story_image_seen : CircleImageView? = itemView.findViewById(R.id.story_image_seen)
        var story_image : CircleImageView? = itemView.findViewById(R.id.story_image)
        var story_username: TextView? = itemView.findViewById(R.id.story_username)

        //Add story item
        var story_plus_btn : ImageView? = itemView.findViewById(R.id.story_add)
        var addStory_text: TextView? = itemView.findViewById(R.id.add_story_text)

//        init{
//            //Story Item
//            story_image_seen = itemView.findViewById(R.id.story_image_seen)
//            story_image = itemView.findViewById(R.id.story_image)
//            story_username = itemView.findViewById(R.id.story_username)
//
//            //Add story item
//            story_plus_btn = itemView.findViewById(R.id.story_add)
//            addStory_text = itemView.findViewById(R.id.add_story_text)
//        }
    }

    private fun userInfo(viewHolder: ViewHolder, userId: String, position: Int)
    {
        val usersRef = FirebaseDatabase.getInstance().reference.child("Users").child(userId)

        usersRef.addListenerForSingleValueEvent(object : ValueEventListener
        {
            override fun onDataChange(p0: DataSnapshot)
            {
                if (p0.exists())
                {
                    val user = p0.getValue<User>(User::class.java)

                    Picasso.get().load(user!!.getImage()).placeholder(R.drawable.profile).into(viewHolder.story_image)

                    if(position !=0 ){
                        Picasso.get().load(user!!.getImage()).placeholder(R.drawable.profile).into(viewHolder.story_image_seen)
                        viewHolder.story_username!!.text = user.getUserName()
                    }
                }
            }

            override fun onCancelled(p0: DatabaseError) {

            }
        })
    }

    override fun getItemViewType(position: Int): Int {
        // Return 0 means Story item view will be returned. It will only be shown if there is a story of our friend to see.
        if(position == 0){
            return 0
        }

        //Return 1 is add_story layout which will always appear.
        return 1
    }

    //Differentiate online user's story with other user's story
    private fun myStories(textView: TextView, imageView: ImageView, click:Boolean){
        val storyRef = FirebaseDatabase.getInstance().reference
            .child("Story").child(FirebaseAuth.getInstance().currentUser!!.uid)

        storyRef.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                var counter = 0
                val timeCurrent = System.currentTimeMillis()

                for(p0 in snapshot.children){
                    val story = p0.getValue(Story::class.java)

                    if(timeCurrent > story!!.getTimeStart() && timeCurrent<story!!.getTimeEnd()){
                        counter++
                    }
                }
                if(click){
                    //Counter represents the number of stories uploaded by online user
                    if(counter>0){
                        val alertDialog = AlertDialog.Builder(mContext).create()

                        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "View Story"){
                            dialogInterface, which->

                            val intent = Intent(mContext, StoryActivity::class.java)
                            intent.putExtra("userId",FirebaseAuth.getInstance().currentUser!!.uid)
                            mContext.startActivity(intent)

                            dialogInterface.dismiss()
                        }
                        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Add Story"){
                                dialogInterface, which->

                            val intent = Intent(mContext, AddStoryActivity::class.java)
                            intent.putExtra("userId",FirebaseAuth.getInstance().currentUser!!.uid)
                            mContext.startActivity(intent)

                            dialogInterface.dismiss()
                        }
                        alertDialog.show()
                    }
                    else{
                        val intent = Intent(mContext, AddStoryActivity::class.java)
                        intent.putExtra("userId",FirebaseAuth.getInstance().currentUser!!.uid)
                        mContext.startActivity(intent)
                    }
                }
                else{
                    if(counter>0){
                        textView.text = "My Story"
                        imageView.visibility = View.GONE
                    }
                    else{
                        textView.text = "Add Story"
                        imageView.visibility = View.GONE
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })
    }

    //Differentiate between already seen and not seen story.
    private fun seenStory(viewHolder: ViewHolder, userId: String){
        val storyRef = FirebaseDatabase.getInstance().reference
            .child("Story")
            .child(userId)

        storyRef.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                var i=0
                for(p0 in snapshot.children){
                    if(!p0.child("views")
                            .child(FirebaseAuth.getInstance().currentUser!!.uid).exists()
                        && System.currentTimeMillis() < p0.getValue((Story::class.java))!!.getTimeEnd()){

                        i = i+1
                    }
                }

                if(i>0){
                    viewHolder.story_image!!.visibility = View.VISIBLE
                    viewHolder.story_image_seen!!.visibility = View.GONE
                }
                else{
                    viewHolder.story_image!!.visibility = View.GONE
                    viewHolder.story_image_seen!!.visibility = View.VISIBLE
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })

    }
}