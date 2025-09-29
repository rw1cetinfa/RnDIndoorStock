package com.example.rndlaboratorystock.Models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class DatetimeResponseModel {
    @SerializedName("responseCode")
    public int ResponseCode;
    @SerializedName("data")
    public List<Data> Data = null;

    public class Data {
        @SerializedName("currentDatetime")
        public String CurrentDatetime;
    }
}
