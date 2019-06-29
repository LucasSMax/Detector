package com.example.detector;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    private static final String _baseUrl = "http://192.168.25.195:8000/admin/";
    private static Retrofit retrofit;

    public static Retrofit getApiClient()
    {

        if(retrofit==null) {
            retrofit = new Retrofit.Builder().baseUrl(_baseUrl).
                    addConverterFactory(GsonConverterFactory.create()).build();
        }
        return  retrofit;
    }

}
