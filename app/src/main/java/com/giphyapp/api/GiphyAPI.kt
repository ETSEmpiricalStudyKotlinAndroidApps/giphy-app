package com.giphyapp.api

import com.giphyapp.BuildConfig
import com.giphyapp.models.GiphyResponse
import com.giphyapp.util.Constants.Companion.NUMBER_OF_GIFS_ON_PAGE
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface GiphyAPI {
    @GET("/v1/gifs/trending")
    suspend fun getTrendingGifs(
        @Query("offset")
        offset: Int = 0,
        @Query("api_key")
        apiKey: String = BuildConfig.GiphySecAPIKey,
        @Query("limit")
        limit: Int = NUMBER_OF_GIFS_ON_PAGE
    ): Response<GiphyResponse>

    @GET("v1/gifs/search")
    suspend fun searchGifs(
        @Query("q")
        query: String,
        @Query("offset")
        offset: Int = 0,
        @Query("api_key")
        apiKey: String = BuildConfig.GiphySecAPIKey,
        @Query("limit")
        limit: Int = NUMBER_OF_GIFS_ON_PAGE
    ): Response<GiphyResponse>

    @Multipart
    @Headers("Content-Type:multipart/form-data")
    @POST("v1/gifs")
    suspend fun uploadGif(
        @Part
        file: MultipartBody.Part,
        @Part("tags")
        tags: RequestBody, // = "cat,meow,huso,emir",
        @Part("api_key")
        apiKeyBody: RequestBody, // = BuildConfig.GiphySecAPIKey,
        @Query("api_key")
        apiKey: String = BuildConfig.GiphySecAPIKey,
    ): Response<GiphyResponse>
}