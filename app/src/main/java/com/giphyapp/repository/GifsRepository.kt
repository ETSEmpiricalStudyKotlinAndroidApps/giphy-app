package com.giphyapp.repository

import com.giphyapp.api.RetrofitInstance
import com.giphyapp.api.UploadRetrofitInstance
import com.giphyapp.db.GifDatabase
import com.giphyapp.models.Gif
import com.giphyapp.models.GiphyResponse
import com.giphyapp.util.Constants.Companion.NUMBER_OF_GIFS_ON_PAGE
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import java.io.File

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

    suspend fun uploadGif(fileBinary: MultipartBody.Part, tags: RequestBody, apiKey: RequestBody) = UploadRetrofitInstance.api.uploadGif(fileBinary, tags, apiKey)

    suspend fun upsert(gif: Gif) = db.getGifDao().upsert(gif)

    fun getSavedGifs(){

    } //= db.getGifDao().getGifs()

    suspend fun deleteAll() = db.getGifDao().deleteAll()
}