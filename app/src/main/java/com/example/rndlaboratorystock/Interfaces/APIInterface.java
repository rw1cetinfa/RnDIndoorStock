package com.example.rndlaboratorystock.Interfaces;


import com.example.rndlaboratorystock.Models.AddEventModel;
import com.example.rndlaboratorystock.Models.BlankModel;
import com.example.rndlaboratorystock.Models.DateAndWeekResponseModel;
import com.example.rndlaboratorystock.Models.DatetimeResponseModel;
import com.example.rndlaboratorystock.Models.IncreaseShelfPostModel;
import com.example.rndlaboratorystock.Models.RndLaboratoryMaterial;
import com.example.rndlaboratorystock.Models.RndLaboratoryResponseSession;
import com.example.rndlaboratorystock.Models.RndLaboratoryRssiFilter;
import com.example.rndlaboratorystock.Models.SessionPostModel;
import com.example.rndlaboratorystock.Models.UpdateResponse;
import com.example.rndlaboratorystock.Models.UserResponseModel;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Query;
import retrofit2.http.Url;

public interface APIInterface {

    // Rim Size Endpoint
    @GET("Laboratory/UserCheck")
    Call<UserResponseModel> UserCheck(@Query("workerCode") String workerCode);

    @GET("General/GetDateAndWeek")
    Call<DateAndWeekResponseModel> GetDateAndWeek();

    @GET("General/GetDateTime")
    Call<DatetimeResponseModel> GetDateTime();


    @POST("Laboratory/StartSession")
    Call<RndLaboratoryResponseSession> StartSession(@Body SessionPostModel postModel);

    @GET("Laboratory/CheckSession")
    Call<RndLaboratoryResponseSession> CheckSession(@Query("wmCode") String workerCode);

    @PUT("Laboratory/IncreaseSession")
    Call<BlankModel> IncreaseSession(@Body IncreaseShelfPostModel postModel);

    @PUT("Laboratory/UpdateSession")
    Call<BlankModel> UpdateSession(@Body IncreaseShelfPostModel postModel);

    @GET("Laboratory/EndSession")
    Call<BlankModel> EndSession(@Query("sessionId") int sessionId, @Query("flag") Boolean flag);

    @GET("Laboratory/CheckRFIDAvailability")
    Call<BlankModel> CheckRFIDAvailability(@Query("epc") String epc);

    @POST("Laboratory/AddEvent")
    Call<BlankModel> AddEvent(@Body AddEventModel model);

    @GET("Laboratory/GetMaterialDetailsByRFID")
    Call<RndLaboratoryMaterial> GetMaterialDetailsByRFID(@Query("epc") String epc);


    //Automatic Update
    @GET("AndroidApk/CheckUpdate")
    Call<UpdateResponse> CheckUpdate();

    @GET
    Call<ResponseBody> DownloadApk(@Url String fileUrl); // Dinamik URL

    @PUT("Laboratory/UpdateRSSI")
    Call<BlankModel> UpdateRSSI(@Query("rssi") int rssi);
    @GET("Laboratory/GetRSSI")
    Call<RndLaboratoryRssiFilter> GetRSSI();
}
