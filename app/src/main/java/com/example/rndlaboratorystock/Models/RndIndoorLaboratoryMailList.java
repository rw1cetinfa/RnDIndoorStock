package com.example.rndlaboratorystock.Models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class RndIndoorLaboratoryMailList {
    @SerializedName("responseCode")
    public int ResponseCode;
    @SerializedName("data")
    public List<Data> Data = null;

    public class Data {
        @SerializedName("id")
        public int Id;
        @SerializedName("email")
        public String Email;
        @SerializedName("insertedAt")
        public String InsertedAt;
    }
}
