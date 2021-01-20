package com.giphyapp.api

data class GiphyResponse(
    val `data`: List<Data>,
    val meta: Meta,
    val pagination: Pagination
)