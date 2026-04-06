package com.example.rndlaboratorystock.Models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class RndIndoorLaboratoryAndroidVersion {
    @SerializedName("responseCode")
    public int ResponseCode;
    @SerializedName("data")
    public List<Data> Data = null;

    public class Data {
        @SerializedName("id")
        public int Id;
        @SerializedName("appVersion")
        public String AppVersion;
        @SerializedName("isActive")
        public boolean IsActive;
        @SerializedName("insertedAt")
        public String InsertedAt;
    }
}
