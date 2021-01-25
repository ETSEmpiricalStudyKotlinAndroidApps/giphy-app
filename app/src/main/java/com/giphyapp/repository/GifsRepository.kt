package com.giphyapp.repository

import com.giphyapp.BuildConfig
import com.giphyapp.api.RetrofitInstance
import com.giphyapp.models.GiphyResponse
import com.giphyapp.util.Constants.Companion.NUMBER_OF_GIFS_ON_PAGE
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import java.io.File
import java.util.concurrent.TimeUnit

class GifsRepository(
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

    fun uploadGif(file: File): okhttp3.Response {
        val logging = HttpLoggingInterceptor()
        logging.setLevel(HttpLoggingInterceptor.Level.BODY)

        val okHttpClient = OkHttpClient()
            .newBuilder()
            .connectTimeout(1, TimeUnit.MINUTES)
            .readTimeout(1, TimeUnit.MINUTES)
            .writeTimeout(1, TimeUnit.MINUTES)
            .addInterceptor(logging)
            .build()

        val body = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("api_key", BuildConfig.GiphySecAPIKey)
            .addFormDataPart("tags", "MOP,Task,Upload")
            .addFormDataPart("file", file.name,
                file.asRequestBody("application/octet-stream".toMediaTypeOrNull()))
            .build()

        val request = Request.Builder()
            .url("https://upload.giphy.com/v1/gifs?api_key=" + BuildConfig.GiphySecAPIKey)
            .method("POST", body)
            .build()

        return okHttpClient.newCall(request).execute()
    }
}