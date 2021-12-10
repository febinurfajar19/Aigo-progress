package com.example.aigo.interfaces

import com.example.aigo.model.googlePlaceModel.GooglePlaceModel

interface NearLocationInterface {

    fun onSaveClick(googlePlaceModel: GooglePlaceModel)

    fun onDirectionClick(googlePlaceModel: GooglePlaceModel)

}