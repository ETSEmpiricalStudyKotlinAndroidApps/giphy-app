package com.giphyapp.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.giphyapp.R
import com.giphyapp.adapters.GifAdapter
import com.giphyapp.databinding.ActivityMainBinding
import com.giphyapp.db.GifDatabase
import com.giphyapp.repository.GifsRepository
import com.giphyapp.util.Constants.Companion.GIF_PICK_CODE
import com.giphyapp.util.Constants.Companion.NUMBER_OF_COLUMNS
import com.giphyapp.util.Constants.Companion.PERMISION_CODE
import com.giphyapp.util.Resource
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.experimental.and


class MainActivity : AppCompatActivity() {

    private var trendingGifsDisplayed = true
    private lateinit var binding: ActivityMainBinding
    lateinit var viewModel: GifsViewModel
    lateinit var gifAdapter: GifAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        setupViewModel()
        setFABListener()
        setupRecyclerView()
    }

    private fun hideProgressBar() {
        binding.progressBar.visibility = View.INVISIBLE
    }

    private fun showProgressBar() {
        binding.progressBar.visibility = View.VISIBLE
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)

        val searchItem: MenuItem = menu.findItem(R.id.search)
        val searchView : SearchView = searchItem.actionView as SearchView

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String?): Boolean {
                return true
            }

            override fun onQueryTextSubmit(query: String?): Boolean {

                hideKeyboard()

                query?.let {
                    if (query.toString().isNotEmpty()) {
                        trendingGifsDisplayed = false
                        viewModel.searchNews(query.toString())
                    }
                }

                searchItem.collapseActionView()

                return true
            }

            private fun hideKeyboard() {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                imm?.hideSoftInputFromWindow(searchView.windowToken, 0)
            }
        })

        val trendingItem: MenuItem = menu.findItem(R.id.trendingIcon)
        trendingItem.setOnMenuItemClickListener(object : MenuItem.OnMenuItemClickListener {
            override fun onMenuItemClick(item: MenuItem?): Boolean {
                if (trendingGifsDisplayed == false) {
                    trendingGifsDisplayed = true
                    viewModel.getTrendingGifs()
                }
                return true
            }
        })

        return true
    }

    private fun setFABListener() {
        binding.fab.setOnClickListener { view ->

            if(checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                            == PackageManager.PERMISSION_DENIED){
                        var permissions: Array<String> = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                        requestPermissions(permissions, PERMISION_CODE)
                        Log.e("MAIN ACATIVIY", "PERMISSION REQUESTED")
                    }else{
                        pickGifFromGallery()
            }

        }
    }

    private fun pickGifFromGallery() {
        // Intent to pick gif from gallery
        var intent: Intent = Intent(Intent.ACTION_PICK)
        intent.setType("image/* video/*")

        startActivityForResult(intent, GIF_PICK_CODE)
    }

    private fun setupViewModel() {
        val gifsRepository = GifsRepository(GifDatabase(this))
        val viewModelProviderFactory = GifsViewModelProviderFactory(gifsRepository)
        viewModel = ViewModelProvider(this, viewModelProviderFactory).get(GifsViewModel::class.java)

        viewModel.gifs.observe(this, Observer { response ->
            when (response) {
                is Resource.Succes -> {
                    hideProgressBar()
                    response.data?.let { giphyResponse ->
                        gifAdapter.differ.submitList(giphyResponse.data)
                    }
                }

                is Resource.Error -> {
                    hideProgressBar()
                    response.message?.let { message ->
                        Log.e("Main Activity", "An error occured: $message")
                    }
                }

                is Resource.Loading -> {
                    showProgressBar()
                }
            }
        })
    }

    private fun setupRecyclerView() = binding.rvGifs.apply {
        gifAdapter = GifAdapter(this@MainActivity)
        adapter = gifAdapter
        layoutManager = GridLayoutManager(this@MainActivity, NUMBER_OF_COLUMNS)
    }


    // Handle permission result
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {

        when(requestCode){
            PERMISION_CODE -> {
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    pickGifFromGallery()
                } else {
                    Toast
                            .makeText(this@MainActivity, "I don't have the permission to access your gallery", Toast.LENGTH_SHORT)
                            .show()
                }
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    // Handle gif pick result
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if(resultCode == RESULT_OK && requestCode == GIF_PICK_CODE && data!=null && data.data!=null){

            Log.e("MAIN ACTIVITY", data.data.toString())





            // Convert gif to binary
            val uri: Uri = data.data!!

            val iStream: InputStream? = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(iStream)
            iStream?.close()

            /*var outputStream =  ByteArrayOutputStream()
            var inputStream = this@MainActivity?.contentResolver.openInputStream(uri)
            var fileBinary = inputStream?.readBytes()*/


            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream)
            val fileBinary = stream.toByteArray()

            Log.e("MAIN ACTIVITY", fileBinary.toString())
            Log.e("MAIN ACTIVITY", fileBinary.toString())

            var s: String = ""

            Log.e("MAIN ACTIVITY BEGIN", fileBinary?.size.toString())

            val sb = StringBuilder()

            for(byte in fileBinary!!){
                sb.append(String.format("%8s", Integer.toBinaryString(((byte and 0xFF.toByte()).toInt()))).replace(' ', '0'))
            }

            Log.e("MAIN ACTIVITY FINAL", sb.toString())

            // Upload to giphy
            viewModel.uploadGif(sb.toString())
        }

        super.onActivityResult(requestCode, resultCode, data)
    }
}