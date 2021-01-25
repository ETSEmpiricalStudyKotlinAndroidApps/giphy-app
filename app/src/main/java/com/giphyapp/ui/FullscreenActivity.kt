package com.giphyapp.ui

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.giphyapp.databinding.ActivityFullscreenBinding
import com.giphyapp.db.GifDatabase
import com.giphyapp.repository.GifsRepository

class FullscreenActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFullscreenBinding
    private lateinit var viewModel: GifsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityFullscreenBinding.inflate(layoutInflater)

        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        setupViewModel()

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        if(intent.extras!!["url"] != null){
            binding.btnUpload.visibility = View.INVISIBLE
            Glide.with(this)
                    .load(intent.extras!!["url"])
                    .thumbnail(Glide.with(this).load(intent.extras!!["thumbnail"]))
                    .into(binding.ivGif)
        }else{
            Log.e("DOBRO JE", "DOBRO JE")
            binding.ivGif.setImageURI(intent.extras!!["uri"] as Uri?)
            binding.btnUpload.setOnClickListener {
                //TODO("CALL UPLOAD")
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean{
        onBackPressed()
        return true
    }

    private fun setupViewModel() {
        val gifsRepository = GifsRepository(GifDatabase(this))
        val viewModelProviderFactory = GifsViewModelProviderFactory(application, gifsRepository)
        viewModel = ViewModelProvider(this, viewModelProviderFactory).get(GifsViewModel::class.java)
    }

}