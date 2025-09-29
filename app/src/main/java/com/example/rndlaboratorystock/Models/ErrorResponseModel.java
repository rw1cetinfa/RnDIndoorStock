package com.example.rndlaboratorystock.Models;

import com.google.gson.annotations.SerializedName;

public class ErrorResponseModel {

    @SerializedName("errorCode")
    public int ErrorCode;
    @SerializedName("errorDesc")
    public String ErrorDesc;


}
