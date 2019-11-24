package com.example.android.catsvsdogs.models

import com.google.gson.annotations.SerializedName

class CatModel(breeds: List<String>, height: String, id: String, url: String, width: String) {
    @SerializedName("breeds")
    var breeds: List<String>? = null

    @SerializedName("height")
    var height: String? = null

    @SerializedName("id")
    var id: String? = null

    @SerializedName("url")
    var url: String? = null

    @SerializedName("width")
    var width: String? = null

    init {
        this.breeds = breeds
        this.height = height
        this.id = id
        this.url = url
        this.width = width
    }
}
