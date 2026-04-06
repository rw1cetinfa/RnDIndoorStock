package com.example.rndlaboratorystock.Models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class RndIndoorLaboratorySession {
    @SerializedName("responseCode")
    public int ResponseCode;
    @SerializedName("data")
    public List<Data> Data = null;

    public class Data {
        @SerializedName("id")
        public int Id;
        @SerializedName("wmCode")
        public String WmCode;
        @SerializedName("latestShelfNumber")
        public int LatestShelfNumber;
        @SerializedName("latestCabinetNumber")
        public String LatestCabinetNumber;
        @SerializedName("isActive")
        public boolean IsActive;
        @SerializedName("insertedAt")
        public String InsertedAt;
        @SerializedName("closedAt")
        public String ClosedAt;
    }
}
