package com.example.rndlaboratorystock.Models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class RndIndoorLaboratoryMasterDeleted {
    @SerializedName("responseCode")
    public int ResponseCode;
    @SerializedName("data")
    public List<Data> Data = null;

    public class Data {
        @SerializedName("id")
        public int Id;
        @SerializedName("oldId")
        public int OldId;
        @SerializedName("epc")
        public String Epc;
        @SerializedName("productNumber")
        public String ProductNumber;
        @SerializedName("expiredDate")
        public String ExpiredDate;
        @SerializedName("cabinetNumber")
        public String CabinetNumber;
        @SerializedName("shelf")
        public int Shelf;
        @SerializedName("quantity")
        public String Quantity;
        @SerializedName("brand")
        public String Brand;
        @SerializedName("code")
        public String Code;
        @SerializedName("packageQuantity")
        public String PackageQuantity;
        @SerializedName("unit")
        public String Unit;
        @SerializedName("chemicalName")
        public String ChemicalName;
        @SerializedName("supplierName")
        public String SupplierName;
        @SerializedName("quantityNumber")
        public String QuantityNumber;
        @SerializedName("limit")
        public int Limit;
        @SerializedName("expireLimit")
        public int ExpireLimit;
        @SerializedName("insertedAt")
        public String InsertedAt;
        @SerializedName("deletedAt")
        public String DeletedAt;
        @SerializedName("wmCode")
        public String WmCode;
    }
}
