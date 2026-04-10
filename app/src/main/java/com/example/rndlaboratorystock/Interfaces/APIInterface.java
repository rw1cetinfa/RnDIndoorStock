package com.example.rndlaboratorystock.Interfaces;


import com.example.rndlaboratorystock.Models.AddEventModel;
import com.example.rndlaboratorystock.Models.BlankModel;
import com.example.rndlaboratorystock.Models.DateAndWeekResponseModel;
import com.example.rndlaboratorystock.Models.DatetimeResponseModel;
import com.example.rndlaboratorystock.Models.IncreaseShelfPostModel;
import com.example.rndlaboratorystock.Models.RndIndoorLaboratoryMaster;
import com.example.rndlaboratorystock.Models.RndIndoorLaboratoryMaterial;
import com.example.rndlaboratorystock.Models.RndIndoorLaboratoryRssiFilter;
import com.example.rndlaboratorystock.Models.RndIndoorLaboratorySession;
import com.example.rndlaboratorystock.Models.SessionPostModel;
import com.example.rndlaboratorystock.Models.UpdateResponse;
import com.example.rndlaboratorystock.Models.UserResponseModel;

import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Url;

public interface APIInterface {

    // Rim Size Endpoint
    @GET("IndoorLaboratory/UserCheck")
    Call<UserResponseModel> UserCheck(@Query("workerCode") String workerCode);

    @GET("General/GetDateAndWeek")
    Call<DateAndWeekResponseModel> GetDateAndWeek();

    @GET("General/GetDateTime")
    Call<DatetimeResponseModel> GetDateTime();


    @POST("IndoorLaboratory/StartSession")
    Call<RndIndoorLaboratorySession> StartSession(@Body SessionPostModel postModel);

    @GET("IndoorLaboratory/CheckSession")
    Call<RndIndoorLaboratorySession> CheckSession(@Query("wmCode") String workerCode);

    @PUT("IndoorLaboratory/IncreaseSession")
    Call<BlankModel> IncreaseSession(@Body IncreaseShelfPostModel postModel);

    @PUT("IndoorLaboratory/UpdateSession")
    Call<BlankModel> UpdateSession(@Body IncreaseShelfPostModel postModel);

    @GET("IndoorLaboratory/EndSession")
    Call<BlankModel> EndSession(@Query("sessionId") int sessionId, @Query("flag") Boolean flag);

    @GET("IndoorLaboratory/CheckRFIDAvailability")
    Call<BlankModel> CheckRFIDAvailability(@Query("epc") String epc);

    @POST("IndoorLaboratory/AddEvent")
    Call<BlankModel> AddEvent(@Body AddEventModel model);

    @GET("IndoorLaboratory/GetMaterialDetailsByRFID")
    Call<RndIndoorLaboratoryMaterial> GetMaterialDetailsByRFID(@Query("epc") String epc);

    @GET("IndoorLaboratory/GetMaterialByProductNumber")
    Call<RndIndoorLaboratoryMaterial> GetMaterialByProductNumber(@Query("productNumber") String productNumber);


    //Automatic Update
    @GET("AndroidApk/CheckUpdate")
    Call<UpdateResponse> CheckUpdate();

    @GET
    Call<ResponseBody> DownloadApk(@Url String fileUrl); // Dinamik URL

    @PUT("IndoorLaboratory/UpdateRSSI")
    Call<BlankModel> UpdateRSSI(@Query("rssi") int rssi);
    @GET("IndoorLaboratory/GetRSSI")
    Call<RndIndoorLaboratoryRssiFilter> GetRSSI();

    @POST("IndoorLaboratory/InsertEpcDetails")
    Call<BlankModel> InsertEpcDetails(@Body RndIndoorLaboratoryMaster.Data model);

    @PUT("IndoorLaboratory/DeleteByEpcs/{wm_code}")
    Call<BlankModel> DeleteByEpcs(@Body List<String> epcs,@Path("wm_code") String wmCode);

    @PUT("IndoorLaboratory/UpdateStockByEPC")
    Call<BlankModel> UpdateStockByEPC(@Query("epc") String epc, @Query("quantity") String quantity);
}
