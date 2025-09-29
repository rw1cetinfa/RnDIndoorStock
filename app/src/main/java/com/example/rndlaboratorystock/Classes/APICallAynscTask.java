package com.example.rndlaboratorystock.Classes;

import android.os.AsyncTask;

import com.example.rndlaboratorystock.Models.ErrorModel;
import com.example.rndlaboratorystock.Models.ErrorResponseModel;
import com.example.rndlaboratorystock.Models.ResponseModel;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.SocketTimeoutException;

import retrofit2.Call;
import retrofit2.Response;

public class APICallAynscTask<T> extends AsyncTask <Call<T>, Void, ResponseModel<T>> {

    @Override
    protected ResponseModel<T> doInBackground(Call<T>... call) {
        ResponseModel<T> responseModel = new ResponseModel<T>();
        T dataModel = null;

        try {
            Response<T> response = call[0].execute();
            if (response.isSuccessful())
            {
                dataModel = response.body();

                responseModel.Content = dataModel;
            }
            else
            {

                String errorBody = null;
                try {
                    errorBody = response.errorBody().string();

                    JSONObject jsonObject = new JSONObject(errorBody.trim());


                    if(jsonObject.has("error") && !jsonObject.isNull("error") ){
                        jsonObject = jsonObject.getJSONObject("error");
                        Gson gson = new Gson();
                        Type cls = new TypeToken<ErrorResponseModel>() {}.getType();
                        ErrorResponseModel error = gson.fromJson(String.valueOf(jsonObject),cls);
                        responseModel.Error = new ErrorModel(error.ErrorCode, error.ErrorDesc);
                    }else{
                        int responseCode = jsonObject.getInt("responseCode");
                        responseModel.Error = new ErrorModel(responseCode, "Error body is null. Errorcode is "+ responseCode);
                    }

                } catch (IOException e) {
                    ErrorModel error = new ErrorModel(500, e.getLocalizedMessage());

                    responseModel.Error = error;
                } catch (JSONException e) {
                    ErrorModel error = new ErrorModel(500, e.getLocalizedMessage());

                    responseModel.Error = error;
                }

            }

        } catch (SocketTimeoutException ex){
            //System.out.println("Timeout error: " + ex.getLocalizedMessage());
            ErrorModel error = new ErrorModel(403, ex.getLocalizedMessage());

            responseModel.Error = error;
        } catch (IOException e) {

            //System.out.println("Call error: " + e.getCause());
;
            ErrorModel error = new ErrorModel(500, e.getLocalizedMessage());

            responseModel.Error = error;

        }


        return responseModel;
    }

    @Override
    protected void onPostExecute(ResponseModel<T> result) {
        super.onPostExecute(result);
        //how i will pass this result where i called this task?
    }

}
