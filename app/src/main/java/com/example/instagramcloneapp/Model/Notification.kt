package com.example.instagramcloneapp.Model

class Notification {
    private var userid: String = ""
    private var text: String = ""
    private var postid: String = ""
    private var ispost: Boolean = false

    constructor()

    constructor(userid: String, text: String, postid:String, ispost: Boolean){
        this.userid = userid
        this.postid = postid
        this.text = text
        this.ispost = ispost
    }

    fun getUserId(): String{
        return userid
    }

    fun getText(): String{
        return text
    }

    fun getPostId(): String{
        return postid
    }

    fun isIsPost(): Boolean{
        return ispost
    }

    fun setUserId(userid: String){
        this.userid = userid
    }

    fun setText(text: String){
        this.text = text
    }

    fun setPostId(postid: String){
        this.postid = postid
    }

    fun setIsPost(ispost: Boolean){
        this.ispost = ispost
    }

}