package com.example.rndlaboratorystock.Models;

public class AddEventModel {
    public String readPoint;
    public String supplier;
    public String lotNumber;

    public String epcread;

    public String epcwrite;

    public String eventState;

    public String errorLog;

    public AddEventModel(){}

    public AddEventModel(String readPoint,String supplier,String lotNumber,String epcread,String epcwrite,String eventState,String errorLog){
        this.readPoint = readPoint;
        this.supplier = supplier;
        this.lotNumber = lotNumber;
        this.epcread = epcread;
        this.epcwrite = epcwrite;
        this.eventState = eventState;
        this.errorLog = errorLog;
    }
}
