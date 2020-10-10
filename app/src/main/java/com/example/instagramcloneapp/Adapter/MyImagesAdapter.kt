package com.example.instagramcloneapp.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.instagramcloneapp.Fragments.PostDetailsFragment
import com.example.instagramcloneapp.Model.Post
import com.example.instagramcloneapp.R
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.images_item_layout.view.*
import org.jetbrains.annotations.NotNull

class MyImagesAdapter(private val mContext:Context, private val mPost: List<Post>, private val profileId: String)
    : RecyclerView.Adapter<MyImagesAdapter.ViewHolder>()
{
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyImagesAdapter.ViewHolder {
        val view = LayoutInflater.from(mContext).inflate(R.layout.images_item_layout, parent,false)
        return ViewHolder(view)

    }

    override fun getItemCount(): Int {
        return mPost.size
    }

    override fun onBindViewHolder(holder: MyImagesAdapter.ViewHolder, position: Int) {
        val post: Post = mPost[position]

        //Load the images
        Picasso.get().load(post.getPostimage()).into(holder.postImage)

        //When we click on a specific image, we send its details to PostDetails fragment

        holder.postImage.setOnClickListener {
            val editor = mContext.getSharedPreferences("PREFS", Context.MODE_PRIVATE).edit()
            editor.putString("postId", post.getPostid())
            editor.putString("publisherId", profileId)    //This is the id of person whose profile is getting viewed.
            editor.apply()
            (mContext as FragmentActivity).supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, PostDetailsFragment()).addToBackStack("postDetails").commit()
        }
    }

    inner class ViewHolder(@NotNull itemView: View) : RecyclerView.ViewHolder(itemView){
        var postImage : ImageView
        init {
            postImage = itemView.findViewById(R.id.post_image)
        }
    }
}