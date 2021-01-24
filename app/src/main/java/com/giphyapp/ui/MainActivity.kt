package com.giphyapp.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AbsListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.giphyapp.R
import com.giphyapp.adapters.GifAdapter
import com.giphyapp.databinding.ActivityMainBinding
import com.giphyapp.db.GifDatabase
import com.giphyapp.models.Data
import com.giphyapp.models.Gif
import com.giphyapp.repository.GifsRepository
import com.giphyapp.util.Constants.Companion.EMPTY_LAST_PAGE_LOSS
import com.giphyapp.util.Constants.Companion.GIF_PICK_CODE
import com.giphyapp.util.Constants.Companion.INTEGER_DIVISION_PAGE_LOSS
import com.giphyapp.util.Constants.Companion.NUMBER_OF_COLUMNS
import com.giphyapp.util.Constants.Companion.NUMBER_OF_GIFS_ON_PAGE
import com.giphyapp.util.Constants.Companion.PERMISSION_CODE_READ_EXTERNAL
import com.giphyapp.util.Constants.Companion.PERMISSION_CODE_WRITE_EXTERNAL
import com.giphyapp.util.Resource
import kotlinx.coroutines.*
import java.io.*


class MainActivity : AppCompatActivity() {

    private var trendingGifsDisplayed = true
    private lateinit var lastSearch: String
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
        setGifOnClickListener()
        setupPullToRefresh()
    }

    private fun setGifOnClickListener() {
        gifAdapter.setOnItemClickListener {

            Log.e("LISTENER", "YEESSSSSSSS")

            val i: Intent = Intent(this, FullscreenActivity::class.java)
            i.putExtra("url",it.images.downsized.url)
            i.putExtra("thumbnail", it.images.original_still.url)
            startActivity(i)
        }
    }

    private fun hideProgressBar() {
        binding.progressBar.visibility = View.INVISIBLE
        isLoading = false
    }

    private fun showProgressBar() {
        binding.progressBar.visibility = View.VISIBLE
        isLoading = true
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
                        restartPagination()
                        trendingGifsDisplayed = false
                        lastSearch = query.toString()
                        viewModel.searchGifs(query.toString())
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
                    restartPagination()
                    viewModel.getTrendingGifs()
                }else{
                    /*
                    val lista = viewModel.getSavedGifs()

                    if(lista!=null){
                        for(list in lista){
                            Log.e("LISTAA", list.id!!.toString() + " " + list.pathToGif)
                        }
                    }

                    */
                }
                return true
            }
        })

        return true
    }

    private fun setupPullToRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener{

            restartPagination()

            if(trendingGifsDisplayed == true){
                viewModel.getTrendingGifs()
            }else{
                viewModel.searchGifs(lastSearch)
            }

            binding.swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun setFABListener() {
        binding.fab.setOnClickListener { view ->

            if(checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                            == PackageManager.PERMISSION_DENIED){
                        var permissions: Array<String> = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                        requestPermissions(permissions, PERMISSION_CODE_READ_EXTERNAL)
                        Log.e("MAIN ACATIVIY", "PERMISSION REQUESTED")
                    }else{
                        pickGifFromGallery()
            }

        }
    }

    private fun pickGifFromGallery() {
        // Intent to pick gif from gallery
        var intent: Intent = Intent(Intent.ACTION_PICK)
        intent.setType("image/*")

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
                        gifAdapter.differ.submitList(giphyResponse.data.toList())

                        val totalPages = giphyResponse.pagination.total_count / NUMBER_OF_GIFS_ON_PAGE +
                                INTEGER_DIVISION_PAGE_LOSS + EMPTY_LAST_PAGE_LOSS

                        isLastPage = viewModel.gifsPage == totalPages

                        checkExternalWritePermissions()
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

    private fun saveListOfGifsInDB(data: List<Data>) {

        //Save gifs in DB only one time
        if(viewModel.firstTimeSavingGifs == true){
            viewModel.firstTimeSavingGifs = false
            for(gif in data){
                saveGifInDB(gif.images.downsized
                        .url)
            }
        }
    }

    private fun setupRecyclerView() = binding.rvGifs.apply {
        gifAdapter = GifAdapter(this@MainActivity)
        adapter = gifAdapter
        layoutManager = GridLayoutManager(this@MainActivity, NUMBER_OF_COLUMNS)
        addOnScrollListener(this@MainActivity.scrollListener)
    }

    // Checking permissions needed for writing on external storage
    private fun checkExternalWritePermissions(){

        if(checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_DENIED){
            var permissions: Array<String> = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            //TODO("IF SAVING TO EXTERNAL STORAGE DOESNT WORK, CHECK ACCES NETWORK PERMISSION MAYBE")
            requestPermissions(permissions, PERMISSION_CODE_WRITE_EXTERNAL)
            Log.e("MAIN ACATIVIY", "PERMISSION REQUESTED")
        }else{
            saveListOfGifsInDB(gifAdapter.gifs)
        }
    }

    private fun saveGifInDB(url: String) = CoroutineScope(Dispatchers.IO).launch {
        saveGifOnStorage(Glide.with(this@MainActivity)
                .asBitmap()
                .load(url)
                .submit()
                .get())
    }

    fun saveGifOnStorage(image: Bitmap) : String? {
        var savedImagePath: String? = null
        val imageFileName = System.currentTimeMillis().toString() + ".gif"
        val storageDir = File(this.getExternalFilesDir(null), "Giphy")
        var success = true
        if (!storageDir.exists()) {
            success = storageDir.mkdirs()
        }
        if (success) {
            val imageFile = File(storageDir, imageFileName)
            savedImagePath = imageFile.getAbsolutePath()
            try {
                val fOut: OutputStream = FileOutputStream(imageFile)
                image.compress(Bitmap.CompressFormat.JPEG, 100, fOut)
                fOut.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Save in DB
            val gif = Gif(pathToGif = savedImagePath)
            viewModel.saveGif(gif)

            // Add the image to the system gallery
            // galleryAddPic(savedImagePath)
            //Toast.makeText(this, "IMAGE SAVED", Toast.LENGTH_LONG).show() // to make this working, need to manage coroutine, as this execution is something off the main thread
            Log.e("VIEWMODEL", savedImagePath)
        }
        return savedImagePath
    }

    private fun galleryAddPic(imagePath: String?) {
        imagePath?.let { path ->
            val file = File(path)
            MediaScannerConnection.scanFile(this, arrayOf(file.toString()),
                    arrayOf(file.getName()), MediaScannerConnection.OnScanCompletedListener { path, uri ->
                Log.e("EXTERNAL PATH ", path!!)
                Log.e("EXTERNAL URI", uri!!.toString())
            })
        }
    }

    // Handle permission result
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {

        when(requestCode){
            PERMISSION_CODE_READ_EXTERNAL -> {
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    pickGifFromGallery()
                } else {
                    Toast
                            .makeText(this@MainActivity, "I don't have the permission to access your gallery", Toast.LENGTH_SHORT)
                            .show()
                }
            }
            PERMISSION_CODE_WRITE_EXTERNAL -> {
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    saveListOfGifsInDB(gifAdapter.gifs)
                } else {
                    Toast
                            .makeText(this@MainActivity, "I don't have the permission to access your storage", Toast.LENGTH_SHORT)
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

            /*val i:Intent = Intent(this, FullscreenActivity::class.java)
            i.putExtra("uri",uri)
            startActivity(i)*/

            /*val iStream: InputStream? = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(iStream)
            iStream?.close()*/


            val inputStream = this@MainActivity?.contentResolver.openInputStream(uri)
            val fileBinary = inputStream?.readBytes()


            /*val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream)
            val fileBinary = stream.toByteArray()*/

            /*val file: File = File(uri.path)
            val fileBinary = file.readBytes()*/

            //val file = File(uri.path!!)

            //Log.e("PLEASEEEE",uri.path!!)

            //val fileBinary: RequestBody = file.asRequestBody("application/octet-stream;charset=utf-8".toMediaTypeOrNull())

            // Upload to giphy
            viewModel.uploadGif(fileBinary!!)
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    // Pagination
    var isLoading = false
    var isLastPage = false
    var isScrolling = false
    val scrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            if(newState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL){
                isScrolling = true
            }
        }

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)

            val layoutManager = recyclerView.layoutManager as GridLayoutManager
            val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
            val visibleItemCount = layoutManager.childCount
            val totalItemCount = layoutManager.itemCount

            val isNotLoadingAndNotAtTheLastPage = !isLoading && !isLastPage
            val isAtLastItem = firstVisibleItemPosition + visibleItemCount >= totalItemCount
            val isNotAtBeginning = firstVisibleItemPosition >= 0
            val isTotalMoreThanVisible = totalItemCount >= NUMBER_OF_GIFS_ON_PAGE
            val shouldPaginate = isNotLoadingAndNotAtTheLastPage && isAtLastItem && isNotAtBeginning &&
                    isTotalMoreThanVisible && isScrolling

            if(shouldPaginate) {
                if(trendingGifsDisplayed){
                    viewModel.getTrendingGifs()
                }else{
                    viewModel.searchGifs(lastSearch)
                }
                isScrolling = false
            }
        }
    }

    private fun restartPagination(){
        viewModel.gifsPage = 1
        viewModel.gifsResponse = null
    }
}