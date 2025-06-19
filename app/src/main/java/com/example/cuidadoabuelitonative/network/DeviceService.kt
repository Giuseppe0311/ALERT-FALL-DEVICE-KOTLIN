package com.example.cuidadoabuelitonative.network

import com.example.cuidadoabuelitonative.dto.DeviceInfo
import com.example.cuidadoabuelitonative.dto.FallInfo
import retrofit2.http.GET
import retrofit2.http.Query

interface DeviceService {
    @GET("device-status")
    suspend fun getDeviceStatus(
        @Query("device_id") deviceId: String
    ): DeviceInfo

    @GET("fall-detected")
    suspend fun getFallDetected(
        @Query("device_id") deviceId: String
    ): List<FallInfo>
}