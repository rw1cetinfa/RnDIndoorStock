package com.example.rndlaboratorystock;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MaterialInOutActivity extends AppCompatActivity {

    Button btnIn;
    Button btnOut;
    Button btnBack;

    private String wmCode;
    private String wmName;
    private String wmSurname;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_material_in_out);

        btnBack = findViewById(R.id.btnMaterialMenuBack);
        btnIn = findViewById(R.id.btnIn);
        btnOut = findViewById(R.id.btnOut);

        Intent intent = getIntent();
        wmCode = intent.getStringExtra("wmCode");
        wmName = intent.getStringExtra("wmName");
        wmSurname = intent.getStringExtra("wmSurname");

        btnIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OpenMaterialInPage();
            }
        });

        btnOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OpenMaterialOutPage();
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GoToMainMenu();
            }
        });

    }

    private void OpenMaterialInPage(){
        Intent intent = new Intent(getApplicationContext(), RfidWriteActivity.class);
        //intent.putExtra("supplier", supplier);
        intent.putExtra("wmCode", wmCode);
        intent.putExtra("wmName", wmName);
        intent.putExtra("wmSurname", wmSurname);
        startActivity(intent);
    }

    private void OpenMaterialOutPage(){
        Intent intent = new Intent(getApplicationContext(), MaterialOutActivity.class);
        //intent.putExtra("supplier", supplier);
        intent.putExtra("wmCode", wmCode);
        intent.putExtra("wmName", wmName);
        intent.putExtra("wmSurname", wmSurname);
        startActivity(intent);
    }

    private void GoToMainMenu(){
        Intent intent = new Intent(getApplicationContext(), MenuActivity.class);
        //intent.putExtra("supplier", supplier);
        intent.putExtra("wmCode", wmCode);
        intent.putExtra("wmName", wmName);
        intent.putExtra("wmSurname", wmSurname);
        startActivity(intent);
    }


}