package com.example.detector;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface ApiInterface {

    @FormUrlEncoded
    @POST("img/")
    Call <ImageClass> uploadImage(@Field("image") String image);

}
