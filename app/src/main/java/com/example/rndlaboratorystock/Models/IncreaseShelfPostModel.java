package com.example.rndlaboratorystock.Models;

import java.util.List;

public class IncreaseShelfPostModel {
    public int sessionId;
    public int shelfNumber;
    public String cabinetNumber;
    public List<String> epcList;


    public IncreaseShelfPostModel(int sessionId, int shelfNumber, String cabinetNumber,List<String> epcList){
        this.sessionId = sessionId;
        this.shelfNumber = shelfNumber;
        this.cabinetNumber = cabinetNumber;
        this.epcList = epcList;
    }
}
