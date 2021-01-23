package com.giphyapp.models

data class GiphyResponse(
    val `data`: MutableList<Data>,
    val meta: Meta,
    val pagination: Pagination
)