package com.example.rndlaboratorystock;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
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
import com.example.rndlaboratorystock.Models.UpdateResponse;
import com.example.rndlaboratorystock.Models.UserResponseModel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import es.dmoral.toasty.Toasty;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class LoginActivity extends AppCompatActivity {

    private Button btnLogin;

    APIInterface apiInterface;

    EditText editTextWorkerCode;

    TextView txtFail;
    TextView txtLoadingMessage;
    TextView txtVersion;

    FrameLayout failOverlay;

    Dialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        apiInterface = APIClient.getClient().create(APIInterface.class);

        btnLogin = findViewById(R.id.btnLogin);
        editTextWorkerCode = findViewById(R.id.editTextWorkerCode);
        failOverlay = findViewById(R.id.failOverlay);
        txtFail = findViewById(R.id.txtFail);
        txtVersion = findViewById(R.id.txtVersion);

        loadingDialog = new Dialog(LoginActivity.this);
        loadingDialog.setContentView(R.layout.loading_popup_layout);  // Popup layout'unu belirtiyoruz
        loadingDialog.setCancelable(false);  // Kullanıcının popup'ı kapatmasını engelliyoruz
        loadingDialog.getWindow().setBackgroundDrawableResource(R.drawable.loading_popup_background);

        txtLoadingMessage = loadingDialog.findViewById(R.id.txtToBeWritten);
        // Lottie animasyonunu popup'ta başlatıyoruz
        LottieAnimationView popupAnimation = loadingDialog.findViewById(R.id.loadingAnimation);
        popupAnimation.playAnimation();  // Animasyonu başlat

        /*Spinner spinner = findViewById(R.id.spinnerEleman);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"Üretici seçiniz","Eleman 1", "Eleman 2", "Eleman 3"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

*/

        String currentVersion = BuildConfig.VERSION_NAME;
        txtVersion.setText("v."+currentVersion);
        apiInterface.CheckUpdate().enqueue(new Callback<UpdateResponse>() {
            @Override
            public void onResponse(Call<UpdateResponse> call, Response<UpdateResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String latestVersion = response.body().getVersion();
                    String apkUrl = response.body().getApkUrl();
                    if (!currentVersion.equals(latestVersion)) {
                        showUpdateDialog(apkUrl);
                    }
                }
            }

            @Override
            public void onFailure(Call<UpdateResponse> call, Throwable t) {
                t.printStackTrace();
            }
        });

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                txtLoadingMessage.setText("Yükleniyor...");

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

            }
        });
    }

    private void showUpdateDialog(String apkUrl) {
        new AlertDialog.Builder(this)
                .setTitle("Yeni Güncelleme Mevcut")
                .setCancelable(false)
                .setMessage("Yeni bir versiyon mevcut.")
                .setPositiveButton("Yükle", (dialog, which) -> downloadApk(apkUrl))
                //.setNegativeButton("Hayır", null)
                .show();
    }

    private void downloadApk(String apkUrl) {
        txtLoadingMessage.setText("Yeni versiyon kuruluyor lütfen bekleyiniz...");
        runOnUiThread(() -> loadingDialog.show());
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                        }

                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }
                    }
            };

            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

            HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            OkHttpClient client = new OkHttpClient.Builder()
                    .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustAllCerts[0])
                    .hostnameVerifier((hostname, session) -> true)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .addInterceptor(interceptor)
                    .build();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(apkUrl.substring(0, apkUrl.lastIndexOf('/') + 1)) // Temel URL'yi ayır
                    .client(client)
                    .build();

            APIInterface apiInterface2 = retrofit.create(APIInterface.class);

            String filePath = apkUrl.substring(apkUrl.lastIndexOf('/') + 1);

            apiInterface2.DownloadApk(filePath).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

                    if (response.isSuccessful() && response.body() != null) {


                        boolean writtenToDisk = saveApkToDisk(response.body());

                        if (writtenToDisk) {
                            installApk();
                        } else {
                            runOnUiThread(() -> loadingDialog.dismiss());
                            Log.e("Download", "Failed to save the file!");
                        }
                    } else {
                        runOnUiThread(() -> loadingDialog.dismiss());
                        Log.e("Download", "Server response error!");
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    t.printStackTrace();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean saveApkToDisk(ResponseBody body) {
        try {

            File apkFile = new File(getExternalFilesDir(null), "LaboratoryStock.apk");
            InputStream inputStream = body.byteStream();
            OutputStream outputStream = new FileOutputStream(apkFile);

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.close();
            inputStream.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void installApk() {
        Toasty.success(LoginActivity.this, "started",
                Toast.LENGTH_LONG, true).show();
        File apkFile = new File(getExternalFilesDir(null), "LaboratoryStock.apk");

        if (apkFile.exists()) {
            Toasty.success(LoginActivity.this, "exists",
                    Toast.LENGTH_LONG, true).show();
        } else {
            Toasty.success(LoginActivity.this, "not exists",
                    Toast.LENGTH_LONG, true).show();

        }
        apkFile.setReadable(true, false);

        Uri apkUri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", apkFile);
        //apkFile.delete();

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // Gerekli bayraklar
        startActivity(intent);


        Toasty.success(LoginActivity.this, "finished",
                Toast.LENGTH_LONG, true).show();
        runOnUiThread(() -> loadingDialog.dismiss());
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        //Toast.makeText(this, "Lütfen ekrandaki çıkış butonunu kullanınız...", Toast.LENGTH_SHORT).show();
        // super.onBackPressed(); // Çağırma
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