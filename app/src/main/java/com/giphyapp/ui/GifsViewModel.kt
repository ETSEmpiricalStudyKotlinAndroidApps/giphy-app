package com.giphyapp.ui

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giphyapp.models.GiphyResponse
import com.giphyapp.repository.GifsRepository
import com.giphyapp.util.Resource
import kotlinx.coroutines.launch
import retrofit2.Response

class GifsViewModel(
        val  gifsRepository: GifsRepository
) : ViewModel() {

    val gifs: MutableLiveData<Resource<GiphyResponse>> = MutableLiveData()

    val gifsPage = 1

    init {
        getTrendingGifs();
    }

    fun getTrendingGifs() = viewModelScope.launch {
        gifs.postValue(Resource.Loading())
        val response = gifsRepository.getTrendingGifs(gifsPage)
        gifs.postValue(handleGifsResponse(response))
    }

    fun searchNews(searchQuery: String) = viewModelScope.launch{
        gifs.postValue(Resource.Loading())
        val response = gifsRepository.searchGifs(searchQuery, gifsPage)
        gifs.postValue(handleGifsResponse(response))
    }

    private fun handleGifsResponse(response: Response<GiphyResponse>): Resource<GiphyResponse> {
        if(response.isSuccessful){
            response.body()?.let{ resultResponse ->
                return Resource.Succes(resultResponse)
            }
        }
        return Resource.Error(response.message())
    }
}