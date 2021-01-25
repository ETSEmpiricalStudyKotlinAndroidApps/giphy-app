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
import com.giphyapp.GiphyApplication
import com.giphyapp.models.Gif
import com.giphyapp.models.GiphyResponse
import com.giphyapp.repository.GifsRepository
import com.giphyapp.util.Resource
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import java.io.File


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
        gifs.postValue(Resource.Loading())
        val response = gifsRepository.getTrendingGifs(gifsPage)
        gifs.postValue(handleGifsResponse(response))
    }

    fun searchGifs(searchQuery: String) = viewModelScope.launch{
        gifs.postValue(Resource.Loading())
        val response = gifsRepository.searchGifs(searchQuery, gifsPage)
        gifs.postValue(handleGifsResponse(response))
    }

    fun uploadGif(fileBinary: MultipartBody.Part, tags: RequestBody, apiKey: RequestBody) = viewModelScope.launch{
        val response = gifsRepository.uploadGif(fileBinary, tags, apiKey)
        //TODO("Must somehow show response")
        Log.e("VIEWMODEL", response.message())
    }

    fun saveGif(gif: Gif) = viewModelScope.launch {
        gifsRepository.upsert(gif)
    }

    fun getSavedGifs() {
        val folder = File(getApplication<GiphyApplication>().getExternalFilesDir(null), "Saved_gifs");

        for (fileEntry in folder.listFiles()) {
            if (fileEntry.isFile()) {
                Log.e("ViewMODEL", fileEntry.path)
            }
        }

    }

    fun deleteAllGifs() = viewModelScope.launch {
        gifsRepository.deleteAll()
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