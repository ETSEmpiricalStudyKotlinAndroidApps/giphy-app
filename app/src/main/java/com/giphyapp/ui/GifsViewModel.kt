package com.giphyapp.ui

import androidx.lifecycle.ViewModel
import com.giphyapp.repository.GifsRepository

class GifsViewModel(
        val  gifsRepository: GifsRepository
) : ViewModel() {
}