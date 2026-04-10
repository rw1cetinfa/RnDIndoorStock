package com.example.rndlaboratorystock;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;
import com.example.rndlaboratorystock.Classes.APICallAynscTask;
import com.example.rndlaboratorystock.Classes.APIClient;
import com.example.rndlaboratorystock.Interfaces.APIInterface;
import com.example.rndlaboratorystock.Models.BlankModel;
import com.example.rndlaboratorystock.Models.ResponseModel;
import com.example.rndlaboratorystock.Models.RndIndoorLaboratoryMaterial;

import java.util.concurrent.ExecutionException;

import retrofit2.Call;

public class MaterialSearchActivity extends AppCompatActivity {

    APIInterface apiInterface;

    Button btnSearch;
    Button btnBack;

    TextView txtMaterialName;
    TextView txtWarehouseCode;
    TextView txtLocation;
    TextView txtStock;
    TextView txtFail;
    TextView txtSuccess;

    EditText editTextProductNumber;

    FrameLayout failOverlay;
    FrameLayout successOverlay;

    private LottieAnimationView confettiView;

    Dialog loadingDialog;

    private String wmCode;
    private String wmName;
    private String wmSurname;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_material_search);

        apiInterface = APIClient.getClient().create(APIInterface.class);

        Intent intent = getIntent();
        wmCode = intent.getStringExtra("wmCode");
        wmName = intent.getStringExtra("wmName");
        wmSurname = intent.getStringExtra("wmSurname");

        btnSearch = findViewById(R.id.btnSearch);
        btnBack = findViewById(R.id.btnBack);

        txtMaterialName = findViewById(R.id.txtMaterialName);
        txtWarehouseCode = findViewById(R.id.txtWarehouseCode);
        txtLocation = findViewById(R.id.txtLocation);
        txtStock = findViewById(R.id.txtStock);
        txtFail = findViewById(R.id.txtFail);
        txtSuccess = findViewById(R.id.txtSuccess);

        editTextProductNumber = findViewById(R.id.editTextProductNumber);

        failOverlay = findViewById(R.id.failOverlay);
        successOverlay = findViewById(R.id.successOverlay);

        confettiView = findViewById(R.id.confettiView);

        loadingDialog = new Dialog(MaterialSearchActivity.this);
        loadingDialog.setContentView(R.layout.loading_popup_layout);
        loadingDialog.setCancelable(false);
        loadingDialog.getWindow().setBackgroundDrawableResource(R.drawable.loading_popup_background);

        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String productNumber = editTextProductNumber.getText().toString().trim();

                if (productNumber.isEmpty()) {
                    txtFail.setText("Lütfen ürün sıra numarası giriniz.");
                    failOverlay.setVisibility(View.VISIBLE);
                    new Handler().postDelayed(() -> {
                        failOverlay.setVisibility(View.GONE);
                    }, 2000);
                } else if (!productNumber.matches("\\d{1,3}") || Integer.parseInt(productNumber) > 999) {
                    txtFail.setText("Sıra numarası 0-999 arasında olmalıdır.");
                    failOverlay.setVisibility(View.VISIBLE);
                    new Handler().postDelayed(() -> {
                        failOverlay.setVisibility(View.GONE);
                    }, 2000);
                } else {
                    loadingDialog.show();

                    new Thread(() -> {
                        try {
                            Call<RndIndoorLaboratoryMaterial> sessionCall = apiInterface.GetMaterialByProductNumber(productNumber);
                            ResponseModel<RndIndoorLaboratoryMaterial> sessionCallResponse = null;
                            sessionCallResponse = new APICallAynscTask<RndIndoorLaboratoryMaterial>().execute(sessionCall).get();
                            ResponseModel<RndIndoorLaboratoryMaterial> finalSessionCallResponse = sessionCallResponse;

                            runOnUiThread(() -> {
                                if (finalSessionCallResponse.Error == null && finalSessionCallResponse.Content.ResponseCode == 200) {
                                    runOnUiThread(() -> loadingDialog.dismiss());
                                    RndIndoorLaboratoryMaterial.Data material = finalSessionCallResponse.Content.Data.get(0);

                                    txtMaterialName.setText(material.ChemicalName != null ? material.ChemicalName : "-");
                                    txtWarehouseCode.setText(material.BrandCode != null ? material.BrandCode : "-");
                                    txtLocation.setText(material.ItemNumber != 0 ? material.ItemNumber : 0);
                                    txtStock.setText("" + material.Amount);

                                    txtSuccess.setText("Malzeme bulundu.");
                                    successOverlay.setVisibility(View.VISIBLE);
                                    new Handler().postDelayed(() -> {
                                        successOverlay.setVisibility(View.GONE);
                                    }, 2000);
                                } else {
                                    ResponseModel<RndIndoorLaboratoryMaterial> finalsessionCallResponse = finalSessionCallResponse;
                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    txtFail.setText(finalsessionCallResponse.Error.ErrorDesc);
                                                    failOverlay.setVisibility(View.VISIBLE);
                                                    new Handler().postDelayed(() -> {
                                                        failOverlay.setVisibility(View.GONE);
                                                    }, 3000);
                                                }
                                            });
                                        }
                                    }).start();
                                    runOnUiThread(() -> loadingDialog.dismiss());
                                    txtMaterialName.setText("-");
                                    txtWarehouseCode.setText("-");
                                    txtLocation.setText("-");
                                    txtStock.setText("-");
                                }
                            });
                        } catch (ExecutionException e) {
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            txtFail.setText(e.getLocalizedMessage());
                                            failOverlay.setVisibility(View.VISIBLE);
                                            new Handler().postDelayed(() -> {
                                                failOverlay.setVisibility(View.GONE);
                                            }, 3000);
                                        }
                                    });
                                }
                            }).start();
                            runOnUiThread(() -> loadingDialog.dismiss());
                        } catch (InterruptedException e) {
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            txtFail.setText(e.getLocalizedMessage());
                                            failOverlay.setVisibility(View.VISIBLE);
                                            new Handler().postDelayed(() -> {
                                                failOverlay.setVisibility(View.GONE);
                                            }, 3000);
                                        }
                                    });
                                }
                            }).start();
                            runOnUiThread(() -> loadingDialog.dismiss());
                        }
                    }).start();
                }
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        Toast.makeText(this, "Lütfen ekrandaki geri butonunu kullanınız...", Toast.LENGTH_SHORT).show();
        // super.onBackPressed(); // Çağırma - Hardware back button disabled
    }
}
