package com.example.android.catsvsdogs.models

import com.google.gson.annotations.SerializedName

class DogModel(message: String, status: String) {

    @SerializedName("message")
    var message: String? = null

    @SerializedName("status")
    var status: String? = null

    init {
        this.message = message
        this.status = status
    }
}

