package com.example.rndlaboratorystock.Models;

import com.google.gson.annotations.SerializedName;

public class RndLaboratoryEpcDetail {

    @SerializedName("epc")
    public String Epc;
    @SerializedName("CabinetNumber")
    public String Cabinet;
    @SerializedName("shelf")
    public int Shelf;
    @SerializedName("quantity")
    public String Quantity;
    @SerializedName("ProductNumber")
    public String ProductNumber;
    @SerializedName("ExpiredDate")
    public String ExpiredDate;

    @SerializedName("Code")
    public String Code;
    @SerializedName("Brand")
    public String Brand;
    @SerializedName("PackageQuantity")
    public String PackageQuantity;

}
