package com.example.googlepaydemo

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        findViewById<Button>(R.id.btn_go).setOnClickListener {
            this@MainActivity.startActivity(Intent(this@MainActivity, PayActivity::class.java))
        }
    }

}