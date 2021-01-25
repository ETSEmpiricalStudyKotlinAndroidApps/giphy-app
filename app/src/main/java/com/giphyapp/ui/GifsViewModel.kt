package com.giphyapp.ui

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities.*
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giphyapp.BuildConfig
import com.giphyapp.GiphyApplication
import com.giphyapp.models.Gif
import com.giphyapp.models.GiphyResponse
import com.giphyapp.repository.GifsRepository
import com.giphyapp.util.Resource
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import java.io.File
import java.util.concurrent.TimeUnit


class GifsViewModel(
        app: Application,
        val gifsRepository: GifsRepository
) : AndroidViewModel(app) {

    val gifs: MutableLiveData<Resource<GiphyResponse>> = MutableLiveData()

    var gifsPage = 1
    var firstTimeSavingGifs = true
    var firstTimeLoadingTrending = true
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