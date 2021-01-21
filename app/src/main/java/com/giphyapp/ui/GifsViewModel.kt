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

    val trendingGifs: MutableLiveData<Resource<GiphyResponse>> = MutableLiveData()

    val trendingGifsPage = 1

    init {
        getTrendingGifs();
    }

    fun getTrendingGifs() = viewModelScope.launch {
        trendingGifs.postValue(Resource.Loading())
        val response = gifsRepository.getTrendingGifs(trendingGifsPage)
        trendingGifs.postValue(handleTrendingGifsResponse(response))
    }

    private fun handleTrendingGifsResponse(response: Response<GiphyResponse>): Resource<GiphyResponse> {
        if(response.isSuccessful){
            response.body()?.let{ resultResponse ->
                return Resource.Succes(resultResponse)
            }
        }
        return Resource.Error(response.message())
    }
}