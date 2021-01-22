package com.giphyapp.api

import com.giphyapp.util.Constants
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object UploadRetrofitInstance {
    val retrofit  by lazy {
        val logging = HttpLoggingInterceptor()
        logging.setLevel(HttpLoggingInterceptor.Level.BODY)
        val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .build()
        Retrofit.Builder()
                .baseUrl(Constants.UPLOAD_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build()
    }

    val api by lazy {
        retrofit.create(GiphyAPI::class.java)
    }
}