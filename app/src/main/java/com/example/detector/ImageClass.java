package com.example.detector;

import com.google.gson.annotations.SerializedName;

public class ImageClass {

    @SerializedName("image")
    private String Image;

    @SerializedName("response")
    private String Response;

    public String getResponse() {
        return Response;
    }
}
