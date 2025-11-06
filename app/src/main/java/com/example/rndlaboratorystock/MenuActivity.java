package com.example.rndlaboratorystock;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;

public class MenuActivity extends AppCompatActivity {

    private Button btnStockTaking;
    private Button btnMaterialDetail;
    private Button btnRfidWrite;
    private Button btnBack;

    private String wmCode;
    private String wmName;
    private String wmSurname;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        Intent intent = getIntent();
        wmCode = intent.getStringExtra("wmCode");
        wmName = intent.getStringExtra("wmName");
        wmSurname = intent.getStringExtra("wmSurname");

        btnStockTaking = findViewById(R.id.btnStockTaking);
        btnMaterialDetail = findViewById(R.id.btnMaterialDetail);
        btnRfidWrite = findViewById(R.id.btnRfidWrite);
        btnBack = findViewById(R.id.btnBack);

        btnStockTaking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OpenStockTakingPage();
            }
        });

        btnMaterialDetail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OpenMaterialDetailPage();
            }
        });

        btnRfidWrite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OpenRfidWritePage();
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GoToLogIn();
            }
        });
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        Toast.makeText(this, "Lütfen ekrandaki çıkış butonunu kullanınız...", Toast.LENGTH_SHORT).show();
        // super.onBackPressed(); // Çağırma
    }

    private void OpenStockTakingPage(){
        Intent intent = new Intent(getApplicationContext(), StockTakingActivity.class);
        //intent.putExtra("supplier", supplier);
        intent.putExtra("wmCode", wmCode);
        intent.putExtra("wmName", wmName);
        intent.putExtra("wmSurname", wmSurname);
        startActivity(intent);
    }

    private void OpenMaterialDetailPage(){
        Intent intent = new Intent(getApplicationContext(), MaterialDetailActivity.class);
        //intent.putExtra("supplier", supplier);
        intent.putExtra("wmCode", wmCode);
        intent.putExtra("wmName", wmName);
        intent.putExtra("wmSurname", wmSurname);
        startActivity(intent);
    }

    private void OpenRfidWritePage(){
        Intent intent = new Intent(getApplicationContext(), RfidWriteActivity.class);
        //intent.putExtra("supplier", supplier);
        intent.putExtra("wmCode", wmCode);
        intent.putExtra("wmName", wmName);
        intent.putExtra("wmSurname", wmSurname);
        startActivity(intent);
    }

    private void GoToLogIn(){
        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
        //intent.putExtra("supplier", supplier);
        intent.putExtra("wmCode", wmCode);
        intent.putExtra("wmName", wmName);
        intent.putExtra("wmSurname", wmSurname);
        startActivity(intent);
    }


}