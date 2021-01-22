package com.giphyapp.api

import com.giphyapp.BuildConfig
import com.giphyapp.models.GiphyResponse
import com.giphyapp.util.Constants.Companion.NUMBER_OF_GIFS_ON_PAGE
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

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

    @POST("v1/gifs")
    suspend fun uploadGif(
        @Body
        file: String,
        @Query("api_key")
        apiKey: String = BuildConfig.GiphySecAPIKey
    ): Response<GiphyResponse>
}