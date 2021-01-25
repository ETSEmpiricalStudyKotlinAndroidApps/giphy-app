package com.giphyapp.ui

import RealPathUtil.getRealPath
import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AbsListView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.giphyapp.R
import com.giphyapp.adapters.GifAdapter
import com.giphyapp.databinding.ActivityMainBinding
import com.giphyapp.models.Data
import com.giphyapp.repository.GifsRepository
import com.giphyapp.util.Constants.Companion.EMPTY_LAST_PAGE_LOSS
import com.giphyapp.util.Constants.Companion.GIF_PICK_CODE
import com.giphyapp.util.Constants.Companion.INTEGER_DIVISION_PAGE_LOSS
import com.giphyapp.util.Constants.Companion.NUMBER_OF_COLUMNS
import com.giphyapp.util.Constants.Companion.NUMBER_OF_GIFS_ON_PAGE
import com.giphyapp.util.Constants.Companion.PERMISSION_CODE_READ_EXTERNAL
import com.giphyapp.util.Constants.Companion.PERMISSION_CODE_WRITE_EXTERNAL
import com.giphyapp.util.Resource
import com.google.android.material.snackbar.Snackbar
import java.io.File
import com.bumptech.glide.request.target.Target
import com.giphyapp.adapters.FileAdapter
import kotlinx.coroutines.*
import okhttp3.Response
import org.json.JSONObject
import java.io.FileOutputStream
import java.lang.Exception
import java.net.SocketTimeoutException


class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"
    private var trendingGifsDisplayed = true
    private lateinit var lastSearch: String
    private lateinit var binding: ActivityMainBinding
    private  var hadConnection: Boolean? = null
    lateinit var viewModel: GifsViewModel
    lateinit var gifAdapter: GifAdapter
    lateinit var fileAdapter: FileAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        runBlocking {
            delay(500L)
        }

        setTheme(R.style.Theme_GiphyApp_NoActionBar)

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

        //Different listeners for online and offline mode
        if(viewModel.hasInternetConnection()==true){
            gifAdapter.setOnItemClickListener {
                val i: Intent = Intent(this, FullscreenActivity::class.java)
                i.putExtra("url",it.images.downsized.url)
                i.putExtra("thumbnail", it.images.original_still.url)
                startActivity(i)
            }
        }else{
            fileAdapter.setOnItemClickListener {
                val i: Intent = Intent(this, FullscreenActivity::class.java)
                i.putExtra("file",it)
                startActivity(i)
            }
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
                        checkConnectionChangeAndGetGifs(query.toString())
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
                    checkConnectionChangeAndGetGifs()
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
                checkConnectionChangeAndGetGifs()
            }else{
                checkConnectionChangeAndGetGifs(lastSearch)
            }

            binding.swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun setFABListener() {
        binding.fab.setOnClickListener { view ->

            if(viewModel.hasInternetConnection() == true){
                if(checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_DENIED){
                    var permissions: Array<String> = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                    requestPermissions(permissions, PERMISSION_CODE_READ_EXTERNAL)
                    Log.e("MAIN ACATIVIY", "PERMISSION REQUESTED")
                }else{
                    pickGifFromGallery()
                }
            }else{
                Toast.makeText(this@MainActivity, "NO INTERNET CONNECTION", Toast.LENGTH_SHORT).show()
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
        val gifsRepository = GifsRepository()
        val viewModelProviderFactory = GifsViewModelProviderFactory(application, gifsRepository)
        viewModel = ViewModelProvider(this, viewModelProviderFactory).get(GifsViewModel::class.java)

        viewModel.gifs.observe(this, Observer { response ->
            when (response) {
                is Resource.Succes -> {
                    hideProgressBar()
                    response.data?.let { giphyResponse ->
                        gifAdapter.differ.submitList(giphyResponse.data.toList())

                        if(giphyResponse.pagination.count == 0){
                            binding.tvError.visibility = View.VISIBLE
                        }else{
                            binding.tvError.visibility = View.INVISIBLE
                        }

                        val totalPages = giphyResponse.pagination.total_count / NUMBER_OF_GIFS_ON_PAGE +
                                INTEGER_DIVISION_PAGE_LOSS + EMPTY_LAST_PAGE_LOSS

                        isLastPage = viewModel.gifsPage == totalPages

                        if(viewModel.trendingSaved == false){
                            viewModel.firstTimeLoadingTrending = true
                            checkExternalWritePermissions()
                        }
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

    private fun saveListOfGifsOnStorage(data: List<Data>) {

        //Save gifs in DB only one time
        if(viewModel.firstTimeSavingGifs == true){
            viewModel.firstTimeSavingGifs = false
            for(gif in data){
                saveGifOnStorage(gif.images.downsized
                        .url)
            }
        }
    }

    private fun setupRecyclerView() = binding.rvGifs.apply {

        layoutManager = GridLayoutManager(this@MainActivity, NUMBER_OF_COLUMNS)

        if(viewModel.hasInternetConnection()==true){
            hadConnection = true
            gifAdapter = GifAdapter(this@MainActivity)
            adapter = gifAdapter
            addOnScrollListener(this@MainActivity.scrollListener)
        }else{
            hadConnection = false
            fileAdapter = FileAdapter(this@MainActivity)
            adapter = fileAdapter
            fileAdapter.differ.submitList(viewModel.getSavedGifs())

            // Check if should display error
            checkForEmptyListOffline()
        }
    }

    // Checking permissions needed for writing on external storage
    private fun checkExternalWritePermissions(){

        if(checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_DENIED){
            var permissions: Array<String> = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            requestPermissions(permissions, PERMISSION_CODE_WRITE_EXTERNAL)
        }else{
            saveListOfGifsOnStorage(gifAdapter.gifs)
        }
    }

    private fun saveGifOnStorage(url: String) = CoroutineScope(Dispatchers.IO).launch {

        // Getting File object from url
        Glide.with(this@MainActivity).asFile()
            .load(url)
            .apply(
                RequestOptions()
                    .diskCacheStrategy(DiskCacheStrategy.DATA)
                    .format(DecodeFormat.PREFER_ARGB_8888)
                    .override(Target.SIZE_ORIGINAL)
            )
            .into(object : CustomTarget<File?>() {
                override fun onResourceReady(
                    resource: File,
                    transition: com.bumptech.glide.request.transition.Transition<in File?>?
                ) {
                    storeImage(resource)
                }

                override fun onLoadCleared(placeholder: Drawable?) {

                }
            })
     }

    private fun storeImage(to: File) {

        // Saving image on storage
        val dir = File(getExternalFilesDir(null), "Saved_gifs")
            if(!dir.isDirectory){
                dir.mkdir()
            }else if(viewModel.firstTimeLoadingTrending == true){
                // If the directory exists, delete the directory
                viewModel.firstTimeLoadingTrending = false
                deleteRecursive(dir)
                dir.mkdir()
            }

            val gifFile = File(dir, System.currentTimeMillis().toString() + "test.gif")


        //This point and below is responsible for the write operation
        var outputStream: FileOutputStream?
        try {
            gifFile.createNewFile()
            //second argument of FileOutputStream constructor indicates whether
            //to append or create new file if one exists
            outputStream = FileOutputStream(gifFile, true);

            outputStream.write(to.readBytes());
            outputStream.flush();
            outputStream.close();

        } catch (e: Exception){
            Log.e(TAG, "Something went wrong while saving a gif")
        }
    }

    private fun deleteRecursive(storageDir: File) {

        if(storageDir.isDirectory){
            for(child in storageDir.listFiles()!!){
                deleteRecursive(child)
            }
        }

        storageDir.delete()
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
                    saveListOfGifsOnStorage(gifAdapter.gifs)
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

        if(resultCode == RESULT_OK && requestCode == GIF_PICK_CODE  && data?.data!=null){

            val uri: Uri = data.data!!

            val uriString = getRealPath(this, uri)

            val file = File(uriString!!)

            if(contentResolver.getType(uri) != "image/gif"){
                Snackbar.make(binding.root, "Please pick a gif", Snackbar.LENGTH_SHORT).show()
                return
            }


            GlobalScope.launch(Dispatchers.IO) {

                Snackbar.make(binding.root, "UPLOAD STARTED", Snackbar.LENGTH_SHORT).show()

                var response: Response? = null
                try{
                    // Upload to giphy
                    response = viewModel.uploadGif(file)
                }catch(e: SocketTimeoutException){
                    withContext(Dispatchers.Main){
                        val builder = AlertDialog.Builder(this@MainActivity, R.style.AlertDialogTheme)
                        builder.setTitle("Upload failed")
                        builder.setMessage("Bad connection")
                        builder.setCancelable(false)
                        builder.setPositiveButton("OK", DialogInterface.OnClickListener(function = { _, _ ->
                            // Just close the alert
                        }))
                        builder.create().show()
                    }
                }


                if(response != null){
                    Snackbar.make(binding.root, "UPLOAD FINISHED", Snackbar.LENGTH_SHORT).show()

                    val jsonData = response.body?.string()!!
                    val jObject = JSONObject(jsonData)
                    val dataObject = jObject.get("data") as JSONObject
                    val gifId = dataObject.getString("id")

                    withContext(Dispatchers.Main){
                        val builder = AlertDialog.Builder(this@MainActivity, R.style.AlertDialogTheme)
                        builder.setTitle("Upload finished")
                        builder.setMessage("Uploaded GIF ID: $gifId")
                        builder.setCancelable(false)
                        builder.setPositiveButton("OK", DialogInterface.OnClickListener(function = { _, _ ->
                            // Just close the alert
                        }))
                        builder.create().show()
                    }
                }

            }

        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun checkConnectionChangeAndGetGifs(searchQuery: String? = null){

        if(hadConnection == true && viewModel.hasInternetConnection() == false){
            fileAdapter = FileAdapter(this)
            binding.rvGifs.adapter = fileAdapter
            fileAdapter.differ.submitList(viewModel.getSavedGifs())
            checkForEmptyListOffline()
            hadConnection = false
            setGifOnClickListener()
        }else if(hadConnection == false && viewModel.hasInternetConnection() == true){
            gifAdapter = GifAdapter(this)
            binding.rvGifs.adapter = gifAdapter
            if(searchQuery == null){
                viewModel.getTrendingGifs()
            }else{
                viewModel.searchGifs(searchQuery)
            }
            binding.rvGifs.addOnScrollListener(this@MainActivity.scrollListener)
            setGifOnClickListener()
            hadConnection = true
        }else{
            if(searchQuery == null){
                viewModel.getTrendingGifs()
            }else{
                viewModel.searchGifs(searchQuery)
            }

            if(viewModel.hasInternetConnection()==false){
                checkForEmptyListOffline()
            }
        }
    }


    private fun checkForEmptyListOffline(){
        if(fileAdapter.files.size == 0){
            binding.tvError.visibility = View.VISIBLE
        }else{
            binding.tvError.visibility = View.INVISIBLE
        }
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
                    checkConnectionChangeAndGetGifs()
                }else{
                    checkConnectionChangeAndGetGifs(lastSearch)
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