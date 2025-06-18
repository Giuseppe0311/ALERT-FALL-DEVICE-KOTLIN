package com.example.cuidadoabuelitonative.network

import com.example.cuidadoabuelitonative.dto.DeviceInfo
import retrofit2.http.GET
import retrofit2.http.Query

interface DeviceService {
    @GET("device-status")
    suspend fun getDeviceStatus(
        @Query("device_id") deviceId: String
    ): DeviceInfo
}