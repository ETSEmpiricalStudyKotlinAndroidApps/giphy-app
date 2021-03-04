package com.giphyapp.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.giphyapp.databinding.ActivityFullscreenBinding
import java.io.File

class FullscreenActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFullscreenBinding
    private lateinit var viewModel: GifsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityFullscreenBinding.inflate(layoutInflater)

        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        if(intent.extras!!["url"] != null){
            Glide.with(this)
                    .load(intent.extras!!["url"])
                    .thumbnail(Glide.with(this).load(intent.extras!!["thumbnail"]))
                    .into(binding.ivGif)
        }else if(intent.extras!!["file"] != null){
            Glide.with(this)
                    .load(intent.extras!!["file"] as File)
                    .into(binding.ivGif)
        }
    }

    override fun onSupportNavigateUp(): Boolean{
        onBackPressed()
        return true
    }
}