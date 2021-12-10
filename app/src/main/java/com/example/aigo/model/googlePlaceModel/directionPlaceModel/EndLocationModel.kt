package com.example.aigo.model.googlePlaceModel.directionPlaceModel

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.squareup.moshi.Json

data class EndLocationModel(
    @field:Json(name = "lat")
    var lat: Double? = null,

    @field:Json(name = "lng")
    var lng: Double? = null
) {

}