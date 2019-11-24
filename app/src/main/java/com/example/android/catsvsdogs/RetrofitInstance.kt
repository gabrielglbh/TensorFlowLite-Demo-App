package com.example.android.catsvsdogs

import com.example.android.catsvsdogs.model.ModelResponse
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Headers

class RetrofitInstance(private val BASE_URL: String) {

    private var retrofit: Retrofit? = null

    interface GetService {
        @Headers("x-api-key: API-KEY-FOR-DOG-API")
        @GET("/v1/images/search")
        fun getDogPhotos(): Call<List<ModelResponse>>

        @Headers("x-api-key: API-KEY-FOR-CAT-API")
        @GET("/v1/images/search")
        fun getCatPhotos(): Call<List<ModelResponse>>
    }

    fun getRetrofitInstance(): Retrofit {
        if (retrofit == null) {
            retrofit = retrofit2.Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return retrofit!!
    }
}