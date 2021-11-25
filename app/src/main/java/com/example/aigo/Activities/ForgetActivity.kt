package com.example.aigo.Activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.aigo.databinding.ActivityForgetBinding

class ForgetActivity : AppCompatActivity() {
    private lateinit var binding: ActivityForgetBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgetBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { onBackPressed() }
    }
}