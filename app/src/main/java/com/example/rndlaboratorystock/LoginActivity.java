package com.example.rndlaboratorystock;

import androidx.appcompat.app.AppCompatActivity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;
import com.example.rndlaboratorystock.Classes.APICallAynscTask;
import com.example.rndlaboratorystock.Classes.APIClient;
import com.example.rndlaboratorystock.Interfaces.APIInterface;
import com.example.rndlaboratorystock.Models.BlankModel;
import com.example.rndlaboratorystock.Models.ResponseModel;
import com.example.rndlaboratorystock.Models.UserResponseModel;

import java.util.concurrent.ExecutionException;

import es.dmoral.toasty.Toasty;
import retrofit2.Call;

public class LoginActivity extends AppCompatActivity {

    private Button btnLogin;

    APIInterface apiInterface;

    EditText editTextWorkerCode;

    TextView txtFail;

    FrameLayout failOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        apiInterface = APIClient.getClient().create(APIInterface.class);

        btnLogin = findViewById(R.id.btnLogin);
        editTextWorkerCode = findViewById(R.id.editTextWorkerCode);
        failOverlay = findViewById(R.id.failOverlay);
        txtFail = findViewById(R.id.txtFail);

        /*Spinner spinner = findViewById(R.id.spinnerEleman);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"Üretici seçiniz","Eleman 1", "Eleman 2", "Eleman 3"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

*/

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Dialog loadingDialog = new Dialog(LoginActivity.this);
                loadingDialog.setContentView(R.layout.loading_popup_layout);  // Popup layout'unu belirtiyoruz
                loadingDialog.setCancelable(false);  // Kullanıcının popup'ı kapatmasını engelliyoruz
                loadingDialog.getWindow().setBackgroundDrawableResource(R.drawable.loading_popup_background);

                // Lottie animasyonunu popup'ta başlatıyoruz
                LottieAnimationView popupAnimation = loadingDialog.findViewById(R.id.loadingAnimation);
                popupAnimation.playAnimation();  // Animasyonu başlat

                // Popup'ı gösteriyoruz
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // update UI
                                loadingDialog.show();
                            }
                        });
                    }
                }).start();

                Runnable afterPopupShown = () -> {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Call<UserResponseModel> userCall = apiInterface.UserCheck(editTextWorkerCode.getText().toString());
                            ResponseModel<UserResponseModel> userCallResponse = null;
                            try {
                                userCallResponse = new APICallAynscTask<UserResponseModel>().execute(userCall).get();
                                if (userCallResponse.Error == null && userCallResponse.Content.ResponseCode == 200){
                                    OpenMenu(userCallResponse.Content.Data.get(0));
                                }
                                else{

                                    ResponseModel<UserResponseModel> finalUserCallResponse = userCallResponse;
                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    // update UI
                                                    loadingDialog.dismiss();
                                                    txtFail.setText(finalUserCallResponse.Error.ErrorDesc);
                                                    failOverlay.setVisibility(View.VISIBLE);
                                                    new Handler().postDelayed(() -> {
                                                        failOverlay.setVisibility(View.GONE);
                                                    }, 3000);
                                                    editTextWorkerCode.setText("");
                                                }
                                            });
                                        }
                                    }).start();
                                }
                            } catch (ExecutionException e) {
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                // update UI
                                                loadingDialog.dismiss();
                                                txtFail.setText(e.getLocalizedMessage());
                                                failOverlay.setVisibility(View.VISIBLE);
                                                new Handler().postDelayed(() -> {
                                                    failOverlay.setVisibility(View.GONE);
                                                }, 3000);
                                                editTextWorkerCode.setText("");
                                            }
                                        });
                                    }
                                }).start();
                            } catch (InterruptedException e) {
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                // update UI
                                                loadingDialog.dismiss();
                                                txtFail.setText(e.getLocalizedMessage());
                                                failOverlay.setVisibility(View.VISIBLE);
                                                new Handler().postDelayed(() -> {
                                                    failOverlay.setVisibility(View.GONE);
                                                }, 3000);
                                                editTextWorkerCode.setText("");
                                            }
                                        });
                                    }
                                }).start();
                            }

                        }
                    }, 2000);
                };

                loadingDialog.setOnShowListener(dialog -> {
                    // Dialog görünür olduğunda bu tetiklenir
                    afterPopupShown.run();
                });

                runOnUiThread(() -> loadingDialog.show());


                //OpenMenu();


            }
        });
    }



    private void OpenMenu(UserResponseModel.Data userModel){
        Intent intent = new Intent(getApplicationContext(), MenuActivity.class);
        System.out.println("WmCode:" + userModel.wmCode);
        intent.putExtra("wmCode", String.valueOf(userModel.wmCode));
        intent.putExtra("wmName", userModel.wmName);
        intent.putExtra("wmSurname", userModel.wmSurname);
        startActivity(intent);
    }
}