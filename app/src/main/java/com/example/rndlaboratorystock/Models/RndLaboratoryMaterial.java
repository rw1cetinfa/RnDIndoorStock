package com.example.rndlaboratorystock.Models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class RndLaboratoryMaterial {
    @SerializedName("responseCode")
    public int ResponseCode;
    @SerializedName("data")
    public List<Data> Data = null;

    public class Data {
        @SerializedName("id")
        public int Id;
        @SerializedName("epc")
        public String Epc;
        @SerializedName("brand")
        public String Brand;
        @SerializedName("brandCode")
        public String BrandCode;
        @SerializedName("amount")
        public double Amount;
        @SerializedName("unit")
        public String Unit;
        @SerializedName("chemicalName")
        public String ChemicalName;
        @SerializedName("companyName")
        public String CompanyName;
        @SerializedName("itemNumber")
        public int ItemNumber;
        @SerializedName("limit")
        public int Limit;
        @SerializedName("exp")
        public int Exp;
        @SerializedName("remain")
        public int Remain;
        @SerializedName("insertedAt")
        public String InsertedAt;
    }
}
