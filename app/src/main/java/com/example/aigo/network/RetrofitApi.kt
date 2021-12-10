package com.example.aigo.network

import com.example.aigo.model.googlePlaceModel.GoogleResponseModel
import com.example.aigo.model.googlePlaceModel.directionPlaceModel.DirectionResponseModel
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Url

interface RetrofitApi {

    @GET
    suspend fun getNearByPlaces(@Url url: String): Response<GoogleResponseModel>

    @GET
    suspend fun getDirection(@Url url: String): Response<DirectionResponseModel>

}