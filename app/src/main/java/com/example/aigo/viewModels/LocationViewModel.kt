package com.example.aigo

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aigo.model.googlePlaceModel.GooglePlaceModel
import com.example.aigo.repo.AppRepo
import com.google.firebase.auth.AuthCredential
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LocationViewModel : ViewModel() {
    private val repo = AppRepo()

    fun getNearByPlace(url: String) = repo.getPlaces(url)

//    fun removePlace(userSavedLocationId: ArrayList<String>) = repo.removePlace(userSavedLocationId)
//
//    fun addUserPlace(googlePlaceModel: GooglePlaceModel, userSavedLocationId: ArrayList<String>) =
//        repo.addUserPlace(googlePlaceModel, userSavedLocationId)
//
//    fun getUserLocationId(): ArrayList<String>{
//        var data: ArrayList<String> = ArrayList()
//        viewModelScope.launch {
//            data = withContext(Dispatchers.Default){ repo.getUserLocationId()}
//        }
//
//        return data
//    }
    fun getDirection(url: String) = repo.getDirection(url)

    fun getUserLocations() = repo.getUserLocations()

    fun updateName(name: String)=repo.updateName(name)

    fun updateImage(image: Uri) = repo.updateImage(image)

    fun confirmEmail(authCredential: AuthCredential) = repo.confirmEmail(authCredential)

    fun updateEmail(email: String) = repo.updateEmail(email)

    fun updatePassword(password: String) = repo.updatePassword(password)
}

