package com.giphyapp.api

import com.giphyapp.BuildConfig
import com.giphyapp.models.GiphyResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface GiphyAPI {
    @GET("/v1/gifs/trending")
    suspend fun getTrendingGifs(
        @Query("offset")
        offset: Int = 0,
        @Query("api_key")
        apiKey: String = BuildConfig.GiphySecAPIKey
    ): Response<GiphyResponse>

    @GET("v1/gifs/search")
    suspend fun searchGifs(
        @Query("q")
        query: String,
        @Query("offset")
        offset: Int = 0,
        @Query("api_key")
        apiKey: String = BuildConfig.GiphySecAPIKey
    ): Response<GiphyResponse>
}