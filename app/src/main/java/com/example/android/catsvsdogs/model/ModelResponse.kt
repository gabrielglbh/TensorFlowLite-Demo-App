package com.example.android.catsvsdogs.model

import com.google.gson.annotations.SerializedName
import org.json.JSONObject

class ModelResponse(breeds: List<JSONObject>, height: Int, id: String, url: String, width: Int) {
    @SerializedName("breeds")
    var breeds: List<JSONObject>? = null

    @SerializedName("height")
    var height: Int? = null

    @SerializedName("id")
    var id: String? = null

    @SerializedName("url")
    var url: String? = null

    @SerializedName("width")
    var width: Int? = null

    init {
        this.breeds = breeds
        this.height = height
        this.id = id
        this.url = url
        this.width = width
    }
}
