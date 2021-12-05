package com.example.aigo

import androidx.lifecycle.ViewModel
import com.example.aigo.repo.AppRepo

class LocationViewModel : ViewModel() {
    private val repo = AppRepo()

    fun getNearByPlace(url: String) = repo.getPlaces(url)
}