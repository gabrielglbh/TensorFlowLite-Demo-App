package com.example.android.catsvsdogs

import com.example.android.catsvsdogs.models.CatModel
import com.example.android.catsvsdogs.models.DogModel
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Headers

class RetrofitInstance(private val BASE_URL: String) {

    private var retrofit: Retrofit? = null

    interface GetService {
        @GET("/api/breeds/image/random")
        fun getDogPhotos(): Call<DogModel>

        @Headers("x-api-key: YOUR-CAT-API-KEY")
        @GET("/v1/images/search")
        fun getCatPhotos(): Call<List<CatModel>>
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