package com.giphyapp.repository

import com.giphyapp.api.RetrofitInstance
import com.giphyapp.db.GifDatabase
import com.giphyapp.models.GiphyResponse
import com.giphyapp.util.Constants.Companion.NUMBER_OF_GIFS_ON_PAGE
import retrofit2.Response

class GifsRepository(
        val db: GifDatabase
) {
    suspend fun getTrendingGifs(page: Int = 1): Response<GiphyResponse> {

        val offset: Int = (page - 1) * NUMBER_OF_GIFS_ON_PAGE

        return RetrofitInstance.api
                .getTrendingGifs(offset)
    }

    suspend fun searchGifs(searchQuery: String, page:Int = 1): Response<GiphyResponse> {
        val offset: Int = (page - 1) * NUMBER_OF_GIFS_ON_PAGE

        return RetrofitInstance.api.searchGifs(searchQuery, offset)
    }
}