package com.giphyapp.ui

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giphyapp.models.GiphyResponse
import com.giphyapp.repository.GifsRepository
import com.giphyapp.util.Resource
import kotlinx.coroutines.launch
import retrofit2.Response


class GifsViewModel(

        val gifsRepository: GifsRepository
) : ViewModel() {

    val gifs: MutableLiveData<Resource<GiphyResponse>> = MutableLiveData()

    var gifsPage = 1
    var firstTimeSavingGifs = true;
    var gifsResponse: GiphyResponse? = null

    init {
        getTrendingGifs();
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

    fun uploadGif(fileBinary: String) = viewModelScope.launch{
        val response = gifsRepository.uploadGif(fileBinary)
        //TODO("Must somehow show response")
        Log.e("VIEWMODEL", response.message())
    }

    fun getSavedGifs() = gifsRepository.getSavedGifs()

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
}