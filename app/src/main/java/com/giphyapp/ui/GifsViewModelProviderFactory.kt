package com.giphyapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.giphyapp.repository.GifsRepository

class GifsViewModelProviderFactory(
        val gifsRepository: GifsRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return GifsViewModel(gifsRepository) as T
    }
}