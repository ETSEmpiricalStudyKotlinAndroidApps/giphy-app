package com.giphyapp

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "gifs"
)

data class Gif (
    @PrimaryKey(autoGenerate = true)
    var id: Int? = null,
    val pathToGif: String,
    val createdAt: String
)