package com.example.rndlaboratorystock.Models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class UserResponseModel {
    @SerializedName("responseCode")
    public int ResponseCode;
    @SerializedName("data")
    public List<Data> Data = null;

    public class Data {
        @SerializedName("wmCode")
        public int wmCode;
        @SerializedName("wmName")
        public String wmName;
        @SerializedName("wmSurname")
        public String wmSurname;
    }
}
