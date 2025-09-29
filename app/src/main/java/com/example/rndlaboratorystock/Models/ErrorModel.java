package com.example.rndlaboratorystock.Models;

import com.google.gson.annotations.SerializedName;

public class ErrorModel {

    @SerializedName("errorCode")
    public int ErrorCode;
    @SerializedName("errorDesc")
    public String ErrorDesc;
    public ErrorModel() {

    }

    public ErrorModel(int code, String desc) {
        this.ErrorCode = code;
        this.ErrorDesc = desc;
    }

}
