package com.example.favoriterestaurant

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import android.util.Log


class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("SplashActivity", "✅ SplashActivity loaded")
        setContentView(R.layout.activity_splash_1)

        Handler(Looper.getMainLooper()).postDelayed({

            startActivity(Intent(this, MainActivity::class.java))
            overridePendingTransition(0, 0)  // 전환 애니메이션 제거

            finish()
        }, 3000)
    }

}