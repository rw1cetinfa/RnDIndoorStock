package com.example.rndlaboratorystock.Models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class RndIndoorLaboratoryStocktaking {
    @SerializedName("responseCode")
    public int ResponseCode;
    @SerializedName("data")
    public List<Data> Data = null;

    public class Data {
        @SerializedName("id")
        public int Id;
        @SerializedName("sessionId")
        public int SessionId;
        @SerializedName("cabinet")
        public String Cabinet;
        @SerializedName("shelf")
        public int Shelf;
        @SerializedName("epcCount")
        public int EpcCount;
        @SerializedName("insertedAt")
        public String InsertedAt;
    }
}
