package com.example.cuidadoabuelitonative.dto

import com.google.gson.annotations.SerializedName

data class FallInfo(
    @SerializedName("device_id")
    val deviceId: String,


    @SerializedName("occurred_at")
    val occurredAt: String,
)