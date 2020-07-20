package com.pratik.here_tta_demo

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.pratik.here_tta_demo.navigation.NavigationActivity
import com.pratik.here_tta_demo.tta.RouteTTAActivity

class MenuActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choose_main)
    }

    fun launchRouteTTAActivity(view: View) {
        startActivity(Intent(this,RouteTTAActivity::class.java))
    }
    fun launchRouteNavigationActivity(view: View) {
        startActivity(Intent(this, NavigationActivity::class.java))
    }
}