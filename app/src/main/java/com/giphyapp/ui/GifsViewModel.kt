package com.giphyapp.ui

import android.app.Application
import android.content.Context
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities.*
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.Target
import com.giphyapp.GiphyApplication
import com.giphyapp.models.Data
import com.giphyapp.models.GiphyResponse
import com.giphyapp.repository.GifsRepository
import com.giphyapp.util.Resource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream


class GifsViewModel(
        app: Application,
        val gifsRepository: GifsRepository
) : AndroidViewModel(app) {

    private val TAG = "View Model"

    val gifs: MutableLiveData<Resource<GiphyResponse>> = MutableLiveData()

    var gifsPage = 1
    var firstTimeSavingGifs = true
    var firstTimeLoadingTrending = true
    var trendingSaved = false
    var gifsResponse: GiphyResponse? = null

    init {
        if(hasInternetConnection()){
            getTrendingGifs();
        }else{
            getSavedGifs()
            Toast.makeText(getApplication(), "NO INTERNET CONNECTION", Toast.LENGTH_SHORT).show()
        }

    }

    fun getTrendingGifs() = viewModelScope.launch {
        if(hasInternetConnection()){
            gifs.postValue(Resource.Loading())
            val response = gifsRepository.getTrendingGifs(gifsPage)
            gifs.postValue(handleGifsResponse(response))
        }else{
            Toast.makeText(getApplication(),"NO INTERNET CONNECTION", Toast.LENGTH_SHORT).show()
        }

    }

    fun searchGifs(searchQuery: String) = viewModelScope.launch{
        if(hasInternetConnection()){
            gifs.postValue(Resource.Loading())
            val response = gifsRepository.searchGifs(searchQuery, gifsPage)
            gifs.postValue(handleGifsResponse(response))
        }else{
            Toast.makeText(getApplication(),"NO INTERNET CONNECTION", Toast.LENGTH_SHORT).show()
        }

    }

    fun uploadGif(file: File) = gifsRepository.uploadGif(file)

    fun getSavedGifs(): List<File> {
        val folder = File(getApplication<GiphyApplication>().getExternalFilesDir(null), "Saved_gifs");

        return folder.listFiles().toList()
    }

    private fun handleGifsResponse(response: Response<GiphyResponse>): Resource<GiphyResponse> {
        if(response.isSuccessful){
            response.body()?.let{ resultResponse ->
                gifsPage++

                if(gifsResponse == null){
                    // If the first page is being loaded
                    gifsResponse = resultResponse
                }else{
                    // If any page is being loaded other than first - merging gifs from different pages
                    val oldGifs = gifsResponse?.data
                    val newGifs = resultResponse.data
                    oldGifs?.addAll(newGifs)
                }
                return Resource.Succes(gifsResponse?:resultResponse)
            }
        }
        return Resource.Error(response.message())
    }

    fun saveListOfGifsOnStorage(data: List<Data>) {

        //Save gifs in DB only one time
        if(firstTimeSavingGifs == true){
            firstTimeSavingGifs = false
            for(gif in data){
                saveGifOnStorage(gif.images.downsized
                        .url)
            }
        }
    }

    private fun saveGifOnStorage(url: String) = CoroutineScope(Dispatchers.IO).launch {

        // Getting File object from url
        Glide.with(getApplication<GiphyApplication>()).asFile()
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
        val dir = File(getApplication<GiphyApplication>().getExternalFilesDir(null), "Saved_gifs")
        if(!dir.isDirectory){
            dir.mkdir()
        }else if(firstTimeLoadingTrending){
            // If the directory exists, delete the directory
            firstTimeLoadingTrending = false
            deleteRecursive(dir)
            dir.mkdir()
        }

        val gifFile = File(dir, System.currentTimeMillis().toString() + "test.gif")


        //This point and below is responsible for the write operation
        val outputStream: FileOutputStream?
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


    fun hasInternetConnection(): Boolean {
        val connectivityManager = getApplication<GiphyApplication>().getSystemService(
            Context.CONNECTIVITY_SERVICE
        ) as ConnectivityManager

        val activeNetwork = connectivityManager.activeNetwork ?:return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return when {
            capabilities.hasTransport(TRANSPORT_WIFI) -> true
            capabilities.hasTransport(TRANSPORT_CELLULAR) -> true
            capabilities.hasTransport(TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }
}