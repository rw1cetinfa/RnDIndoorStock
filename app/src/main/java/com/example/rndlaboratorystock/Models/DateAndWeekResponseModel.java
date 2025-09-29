package com.example.rndlaboratorystock.Models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class DateAndWeekResponseModel {
    @SerializedName("responseCode")
    public int ResponseCode;
    @SerializedName("data")
    public List<Data> Data = null;

    public class Data {
        @SerializedName("date")
        public String Date;
        @SerializedName("week")
        public int Week;
    }
}
