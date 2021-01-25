package com.giphyapp.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.giphyapp.repository.GifsRepository

class GifsViewModelProviderFactory(
        val app: Application,
        val gifsRepository: GifsRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return GifsViewModel(app, gifsRepository) as T
    }
}