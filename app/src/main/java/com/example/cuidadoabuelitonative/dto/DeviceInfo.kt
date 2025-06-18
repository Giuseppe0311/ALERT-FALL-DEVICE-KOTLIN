package com.example.cuidadoabuelitonative.dto

import com.google.gson.annotations.SerializedName

data class DeviceInfo(

    @SerializedName("device_id")
    val deviceId: String,
    
    val ip: String,

    @SerializedName("last_seen")
    val lastSeen: String
)