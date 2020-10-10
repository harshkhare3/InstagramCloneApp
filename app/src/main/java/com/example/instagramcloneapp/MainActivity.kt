package com.example.instagramcloneapp

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.instagramcloneapp.Fragments.NotificationsFragment
import com.example.instagramcloneapp.Fragments.ProfileFragment
import com.example.instagramcloneapp.Fragments.SearchFragment
import com.example.instagramcloneapp.Fragments.homeFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private var firstTime = 0
    private var num = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navView: BottomNavigationView = findViewById(R.id.nav_view)

        navView.setOnNavigationItemSelectedListener(onNavigationItemSelectedListener)

        //After starting the application, Home Fragment should be selected
        moveToFragment(homeFragment(), "home")
        firstTime++
    }

    private val onNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.nav_home -> {
                moveToFragment(homeFragment(), "home")

                return@OnNavigationItemSelectedListener true
            }
            R.id.nav_search -> {
                moveToFragment(SearchFragment(), "search")
                return@OnNavigationItemSelectedListener true
            }
            R.id.nav_add_post -> {
                item.isChecked = false
                startActivity(Intent(this@MainActivity, AddPostActivity::class.java))
                return@OnNavigationItemSelectedListener true
            }
            R.id.nav_notifications -> {
                moveToFragment(NotificationsFragment(), "notification")
                return@OnNavigationItemSelectedListener true
            }
            R.id.nav_profile -> {
                num=1
                var userId = FirebaseAuth.getInstance().currentUser!!.uid
                moveToFragment(ProfileFragment(), "profile ${userId}")
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    private fun moveToFragment(fragment: Fragment, name:String){
        val fragmentTrans = supportFragmentManager.beginTransaction()
        if(firstTime > 0){
            fragmentTrans.addToBackStack(name);
        }

        if(num==1){
            val pref = this.getSharedPreferences("PREFS", Context.MODE_PRIVATE)?.edit()
            pref?.putString("profileId", FirebaseAuth.getInstance().currentUser!!.uid)
            pref?.apply()
        }

        fragmentTrans.replace(R.id.fragment_container, fragment)
        fragmentTrans.commit()

    }

}