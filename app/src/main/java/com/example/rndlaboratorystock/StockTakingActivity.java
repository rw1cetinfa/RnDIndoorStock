package com.example.rndlaboratorystock;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;
import com.example.rndlaboratorystock.Classes.APICallAynscTask;
import com.example.rndlaboratorystock.Classes.APIClient;
import com.example.rndlaboratorystock.Helpers.StringHelper;
import com.example.rndlaboratorystock.Interfaces.APIInterface;
import com.example.rndlaboratorystock.Models.BlankModel;
import com.example.rndlaboratorystock.Models.DateAndWeekResponseModel;
import com.example.rndlaboratorystock.Models.IncreaseShelfPostModel;
import com.example.rndlaboratorystock.Models.ResponseModel;
import com.example.rndlaboratorystock.Models.RndLaboratoryResponseSession;
import com.example.rndlaboratorystock.Models.SessionPostModel;
import com.example.rndlaboratorystock.Models.UserResponseModel;
import com.zebra.rfid.api3.Antennas;
import com.zebra.rfid.api3.ENUM_TRANSPORT;
import com.zebra.rfid.api3.ENUM_TRIGGER_MODE;
import com.zebra.rfid.api3.HANDHELD_TRIGGER_EVENT_TYPE;
import com.zebra.rfid.api3.INVENTORY_STATE;
import com.zebra.rfid.api3.InvalidUsageException;
import com.zebra.rfid.api3.MEMORY_BANK;
import com.zebra.rfid.api3.OperationFailureException;
import com.zebra.rfid.api3.POWER_EVENT;
import com.zebra.rfid.api3.RFIDReader;
import com.zebra.rfid.api3.ReaderDevice;
import com.zebra.rfid.api3.Readers;
import com.zebra.rfid.api3.RfidEventsListener;
import com.zebra.rfid.api3.RfidReadEvents;
import com.zebra.rfid.api3.RfidStatusEvents;
import com.zebra.rfid.api3.SESSION;
import com.zebra.rfid.api3.SL_FLAG;
import com.zebra.rfid.api3.START_TRIGGER_TYPE;
import com.zebra.rfid.api3.STATUS_EVENT_TYPE;
import com.zebra.rfid.api3.STOP_TRIGGER_TYPE;
import com.zebra.rfid.api3.TagAccess;
import com.zebra.rfid.api3.TagData;
import com.zebra.rfid.api3.TriggerInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

import retrofit2.Call;

public class StockTakingActivity extends AppCompatActivity {

    private static Readers readers;
    private static ArrayList<ReaderDevice> availableRFIDReaderList;
    private static ReaderDevice readerDevice;
    private static RFIDReader reader;
    private EventHandler eventHandler;
    private Boolean isRFIDPerforming = false;

    TableLayout tableData;

    APIInterface apiInterface;

    TextView emptyView;
    TextView txtEpcCount;
    TextView txtScannerAnimationNumber;
    TextView txtWorkerNameSurname;
    TextView txtFail;
    TextView txtSuccess;
    TextView txtDate;
    TextView txtWeek;

    private String wmCode;
    private String wmName;
    private String wmSurname;

    private int sessionId;
    private int selfNumber;
    private String cabinetNumber;

    List<String> EPCs;

    Dialog scannerDialog;

    Button btnBack;
    Button btnStart;
    Button btnNextShelf;
    Button btnEnd;

    FrameLayout failOverlay;
    FrameLayout successOverlay;

    private LottieAnimationView confettiView;

    Dialog loadingDialog;
    Dialog loadingDialogRFID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_taking);

        apiInterface = APIClient.getClient().create(APIInterface.class);

        Intent intent = getIntent();
        wmCode = intent.getStringExtra("wmCode");
        wmName = intent.getStringExtra("wmName");
        wmSurname = intent.getStringExtra("wmSurname");

        loadingDialog = new Dialog(StockTakingActivity.this);
        loadingDialog.setContentView(R.layout.loading_popup_layout);  // Popup layout'unu belirtiyoruz
        loadingDialog.setCancelable(false);  // Kullanıcının popup'ı kapatmasını engelliyoruz
        loadingDialog.getWindow().setBackgroundDrawableResource(R.drawable.loading_popup_background);

        LottieAnimationView popupAnimation = loadingDialog.findViewById(R.id.loadingAnimation);
        popupAnimation.playAnimation();  // Animasyonu başlat

        EPCs = new ArrayList<>();

        scannerDialog = new Dialog(StockTakingActivity.this);
        scannerDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        scannerDialog.setContentView(R.layout.popup_lottie);

        // Animasyon
        LottieAnimationView lottieAnimation = scannerDialog.findViewById(R.id.lottieAnimation);
        lottieAnimation.playAnimation();

        txtScannerAnimationNumber = scannerDialog.findViewById(R.id.txtScannerAnimationNumber);
        txtScannerAnimationNumber.setText(String.valueOf(EPCs.size()));
        // Dialog ayarları
        if (scannerDialog.getWindow() != null) {
            scannerDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            scannerDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        //showLottiePopup(this);
        //new Handler().postDelayed(() -> scannerDialog.dismiss(), 2000);

        //loadingDialog.show();

        // RFID conf. loading dialog
        loadingDialogRFID = new Dialog(StockTakingActivity.this);
        loadingDialogRFID.setContentView(R.layout.rfid_loading_popup_layout);  // Popup layout'unu belirtiyoruz
        loadingDialogRFID.setCancelable(false);  // Kullanıcının popup'ı kapatmasını engelliyoruz
        loadingDialogRFID.getWindow().setBackgroundDrawableResource(R.drawable.loading_popup_background);

        // Lottie animasyonunu popup'ta başlatıyoruz
        LottieAnimationView popupAnimation2 = loadingDialogRFID.findViewById(R.id.loadingAnimation);
        popupAnimation2.playAnimation();  // Animasyonu başlat

        // Popup'ı gösteriyoruz
        new Thread(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // update UI
                        loadingDialogRFID.show();
                    }
                });
            }
        }).start();

        tableData = findViewById(R.id.tableData);
        txtEpcCount = findViewById(R.id.txtEpcCount);

        btnBack = findViewById(R.id.btnBack);
        btnStart = findViewById(R.id.btnStart);
        btnNextShelf = findViewById(R.id.btnNextShelf);
        btnEnd = findViewById(R.id.btnEnd);

        txtWorkerNameSurname = findViewById(R.id.txtWorkerNameSurname);
        failOverlay = findViewById(R.id.failOverlay);
        successOverlay = findViewById(R.id.successOverlay);

        confettiView = findViewById(R.id.confettiView);

        txtFail = findViewById(R.id.txtFail);
        txtSuccess = findViewById(R.id.txtSuccess);
        txtDate = findViewById(R.id.txtDate);
        txtWeek = findViewById(R.id.txtWeek);


        Spinner spinnerCabinet = findViewById(R.id.cupboardIdentifier);
        ArrayAdapter<String> adapter2 = new ArrayAdapter<>(
                this,
                R.layout.spinner_item,
                new String[]{"Dolap numarası seçiniz",
                        "AA", "AB", "AC", "AD", "AE", "AF",
                        "BA", "BB", "BC", "BD", "BE", "BF",
                        "CA", "CB", "CC", "CD", "CE", "CF",
                        "DA", "DB", "DC", "DD", "DE", "DF",
                        "EA", "EB", "EC", "ED", "EE", "EF",
                        "FA", "FB", "FC", "FD", "FE", "FF"
                });
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCabinet.setAdapter(adapter2);


        txtWorkerNameSurname.setText(StringHelper.toTitleCase(wmName + " " + wmSurname));



        // Başlangıçta "veri yok" göster
        showEmptyMessage();

        //addRows("301512312312312312312312");
        //addRows("301512312312312312312312");
        //addRows("301512312312312312312313");

        Call<DateAndWeekResponseModel> dateAndTimeCall = apiInterface.GetDateAndWeek();
        ResponseModel<DateAndWeekResponseModel> dateAndTimeCallResponse = null;
        try {
            dateAndTimeCallResponse = new APICallAynscTask<DateAndWeekResponseModel>().execute(dateAndTimeCall).get();
            if (dateAndTimeCallResponse.Error == null && dateAndTimeCallResponse.Content.ResponseCode == 200) {
                DateAndWeekResponseModel.Data response = dateAndTimeCallResponse.Content.Data.get(0);

                txtDate.setText(response.Date);
                txtWeek.setText(String.valueOf(response.Week));
            } else {

                ResponseModel<DateAndWeekResponseModel> finalDateAndWeekResponseModel = dateAndTimeCallResponse;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // update UI
                                txtFail.setText(finalDateAndWeekResponseModel.Error.ErrorDesc);
                                failOverlay.setVisibility(View.VISIBLE);
                                new Handler().postDelayed(() -> {
                                    failOverlay.setVisibility(View.GONE);
                                }, 3000);
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
                            txtFail.setText(e.getLocalizedMessage());
                            failOverlay.setVisibility(View.VISIBLE);
                            new Handler().postDelayed(() -> {
                                failOverlay.setVisibility(View.GONE);
                            }, 3000);
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
                            txtFail.setText(e.getLocalizedMessage());
                            failOverlay.setVisibility(View.VISIBLE);
                            new Handler().postDelayed(() -> {
                                failOverlay.setVisibility(View.GONE);
                            }, 3000);
                        }
                    });
                }
            }).start();
        }

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (sessionId != 0) {
                    new AlertDialog.Builder(StockTakingActivity.this)
                            .setTitle("UYARI")
                            .setMessage("Sayım bitirilmedi. İşlemi yarıda bırakmak istiyor musunuz?")
                            .setCancelable(false) // dışarı tıklanınca kapanmaz
                            .setPositiveButton("Evet", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // Evet seçildiğinde yapılacaklar
                                    Intent intent = new Intent(getApplicationContext(), MenuActivity.class);
                                    //intent.putExtra("supplier", supplier);
                                    intent.putExtra("wmCode", wmCode);
                                    intent.putExtra("wmName", wmName);
                                    intent.putExtra("wmSurname", wmSurname);

                                    startActivity(intent);
                                }
                            })
                            .setNegativeButton("Hayır", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // Hayır seçildiğinde yapılacaklar
                                    dialog.dismiss();
                                }
                            })
                            .show();
                } else {
                    if (reader != null && reader.isConnected()){
                        try {
                            reader.Actions.Inventory.stop();
                            reader.Config.setTriggerMode(ENUM_TRIGGER_MODE.RFID_MODE, false);
                            reader.Config.setTriggerMode(ENUM_TRIGGER_MODE.BARCODE_MODE, true);
                            reader.disconnect();
                        } catch (InvalidUsageException e) {
                            throw new RuntimeException(e);
                        } catch (OperationFailureException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    Intent intent = new Intent(getApplicationContext(), MenuActivity.class);
                    //intent.putExtra("supplier", supplier);
                    intent.putExtra("wmCode", wmCode);
                    intent.putExtra("wmName", wmName);
                    intent.putExtra("wmSurname", wmSurname);

                    startActivity(intent);
                }

            }
        });

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                loadingDialog.show();

                if (spinnerCabinet.getSelectedItemPosition() == 0) {
                    txtFail.setText("Lütfen dolap seçiniz.");
                    failOverlay.setVisibility(View.VISIBLE);
                    new Handler().postDelayed(() -> {
                        failOverlay.setVisibility(View.GONE);
                    }, 3000);
                    runOnUiThread(() -> loadingDialog.dismiss());

                } else {
                    new Thread(() -> {
                        SessionPostModel session = new SessionPostModel(wmCode, spinnerCabinet.getSelectedItem().toString());

                        Call<RndLaboratoryResponseSession> sessionCall = apiInterface.StartSession(session);
                        ResponseModel<RndLaboratoryResponseSession> sessionCallResponse = null;
                        try {
                            sessionCallResponse = new APICallAynscTask<RndLaboratoryResponseSession>().execute(sessionCall).get();

                            ResponseModel<RndLaboratoryResponseSession> finalSessionCallResponse = sessionCallResponse;
                            runOnUiThread(() -> {
                            if (finalSessionCallResponse.Error == null && finalSessionCallResponse.Content.ResponseCode == 200) {
                                txtSuccess.setText("Stok sayımı başlatıldı. Tarama yapabilirsiniz.");
                                successOverlay.setVisibility(View.VISIBLE);
                                new Handler().postDelayed(() -> {
                                    successOverlay.setVisibility(View.GONE);
                                }, 2000);
                                sessionId = finalSessionCallResponse.Content.Data.get(0).Id;
                                selfNumber = finalSessionCallResponse.Content.Data.get(0).LatestShelfNumber;
                                cabinetNumber = finalSessionCallResponse.Content.Data.get(0).LatestCabinetNumber;
                                btnStart.setVisibility(View.INVISIBLE);
                                runOnUiThread(() -> loadingDialog.dismiss());
                                btnNextShelf.setVisibility(View.VISIBLE);
                                btnEnd.setVisibility(View.VISIBLE);
                            } else {
                                ResponseModel<RndLaboratoryResponseSession> finalsessionCallResponse = finalSessionCallResponse;
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                // update UI
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
                            }
                            });
                        } catch (ExecutionException e) {
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            // update UI
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
                                            // update UI
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

        btnNextShelf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                loadingDialog.show();
                selfNumber += 1;
                IncreaseShelfPostModel session = new IncreaseShelfPostModel(sessionId, selfNumber, spinnerCabinet.getSelectedItem().toString(), EPCs);


                new Thread(() -> {
                    try {
                        Call<BlankModel> sessionCall = apiInterface.IncreaseSession(session);
                        ResponseModel<BlankModel> sessionCallResponse = null;
                        sessionCallResponse = new APICallAynscTask<BlankModel>().execute(sessionCall).get();
                        ResponseModel<BlankModel> finalSessionCallResponse = sessionCallResponse;
                        runOnUiThread(() -> {
                            if (finalSessionCallResponse.Error == null && finalSessionCallResponse.Content.ResponseCode == 200) {

                                txtSuccess.setText(finalSessionCallResponse.Content.ResponseDesc+" Raf numarası:" + (selfNumber + 1));
                                successOverlay.setVisibility(View.VISIBLE);
                                new Handler().postDelayed(() -> {
                                    successOverlay.setVisibility(View.GONE);
                                }, 3000);

                                clearTable();



                                runOnUiThread(() -> loadingDialog.dismiss());
                            }
                            else if (finalSessionCallResponse.Error != null && finalSessionCallResponse.Error.ErrorCode == 406){
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                // update UI
                                                System.out.println(finalSessionCallResponse.ResponseCode);
                                                txtFail.setText("Malzemeler daha önceden kaydedilmiş. Tekrar tarama yapınız.");
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
                            else {
                                ResponseModel<BlankModel> finalsessionCallResponse = finalSessionCallResponse;
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                // update UI
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
                            }
                        });
                    } catch (ExecutionException e) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        // update UI
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
                                        // update UI
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
        });

        btnEnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                loadingDialog.show();
                new Thread(() -> {
                    try {
                        Call<BlankModel> sessionCall = apiInterface.EndSession(sessionId);
                        ResponseModel<BlankModel> sessionCallResponse = null;
                        try {
                            sessionCallResponse = new APICallAynscTask<BlankModel>().execute(sessionCall).get();

                            ResponseModel<BlankModel> finalSessionCallResponse = sessionCallResponse;
                            runOnUiThread(() -> {
                                if (finalSessionCallResponse.Error == null &&
                                        (finalSessionCallResponse.Content.ResponseCode == 200 || finalSessionCallResponse.Content.ResponseCode == 202)) {

                                    if (finalSessionCallResponse.Content.ResponseCode == 200) {
                                        txtSuccess.setText("Sayım tamamlandı. Rapor gönderildi.");
                                    } else if (finalSessionCallResponse.Content.ResponseCode == 202) {
                                        txtSuccess.setText("Sayım tamamlandı. Rapor gönderilemedi. IT'ye bilgi veriniz.");
                                    }

                                    confettiView.setVisibility(View.VISIBLE);
                                    confettiView.playAnimation();
                                    successOverlay.setVisibility(View.VISIBLE);

                                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                        successOverlay.setVisibility(View.GONE);
                                    }, 3000);

                                    confettiView.addAnimatorListener(new AnimatorListenerAdapter() {
                                        @Override
                                        public void onAnimationEnd(Animator animation) {
                                            confettiView.setVisibility(View.GONE);
                                        }
                                    });

                                    btnStart.setVisibility(View.VISIBLE);
                                    spinnerCabinet.setSelection(0);
                                    cabinetNumber = null;
                                    sessionId = 0;
                                    btnNextShelf.setVisibility(View.INVISIBLE);
                                    btnEnd.setVisibility(View.INVISIBLE);
                                    txtScannerAnimationNumber.setText("0");

                                    loadingDialog.dismiss();
                                } else {
                                    loadingDialog.dismiss();
                                    txtFail.setText(finalSessionCallResponse.Error.ErrorDesc);
                                    failOverlay.setVisibility(View.VISIBLE);

                                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                        failOverlay.setVisibility(View.GONE);
                                    }, 3000);
                                }
                            });

                        } catch (ExecutionException e) {
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            // update UI
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
                                            // update UI
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

                    } catch (Exception e) {
                        loadingDialog.dismiss();
                        System.out.println("HATA:" + e.getLocalizedMessage());
                    }
                }).start();


            }
        });

        spinnerCabinet.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Seçilen elemanı al
                String selectedCabinet = parent.getItemAtPosition(position).toString();

                if (!selectedCabinet.equals(cabinetNumber) && cabinetNumber != null) {
                    new AlertDialog.Builder(StockTakingActivity.this)
                            .setTitle("UYARI")
                            .setMessage("Farklı bir dolaba geçilecek onaylıyor musunuz?")
                            .setCancelable(false) // dışarı tıklanınca kapanmaz
                            .setPositiveButton("Evet", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // Evet seçildiğinde yapılacaklar
                                    IncreaseShelfPostModel session = new IncreaseShelfPostModel(sessionId, 1, spinnerCabinet.getSelectedItem().toString(), EPCs);

                                    Call<BlankModel> sessionCall = apiInterface.UpdateSession(session);
                                    ResponseModel<BlankModel> sessionCallResponse = null;
                                    try {
                                        sessionCallResponse = new APICallAynscTask<BlankModel>().execute(sessionCall).get();

                                        if (sessionCallResponse.Error == null && sessionCallResponse.Content.ResponseCode == 200) {
                                            selfNumber = 1;
                                            txtSuccess.setText(spinnerCabinet.getSelectedItem().toString() + " dolabına geçildi. Raf numarası:" + selfNumber);
                                            successOverlay.setVisibility(View.VISIBLE);
                                            new Handler().postDelayed(() -> {
                                                successOverlay.setVisibility(View.GONE);
                                            }, 2000);

                                            btnStart.setVisibility(View.INVISIBLE); // görünmez yapar
                                            cabinetNumber = spinnerCabinet.getSelectedItem().toString();

                                            clearTable();
                                            new Handler().postDelayed(() -> {
                                                addRows("301512312312312312312312");
                                            }, 2000);
                                        } else {
                                            ResponseModel<BlankModel> finalsessionCallResponse = sessionCallResponse;
                                            new Thread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            // update UI
                                                            txtFail.setText(finalsessionCallResponse.Error.ErrorDesc);
                                                            failOverlay.setVisibility(View.VISIBLE);
                                                            new Handler().postDelayed(() -> {
                                                                failOverlay.setVisibility(View.GONE);
                                                            }, 3000);

                                                            int position = adapter2.getPosition(cabinetNumber);
                                                            spinnerCabinet.setSelection(position);
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
                                                        txtFail.setText(e.getLocalizedMessage());
                                                        failOverlay.setVisibility(View.VISIBLE);
                                                        new Handler().postDelayed(() -> {
                                                            failOverlay.setVisibility(View.GONE);
                                                        }, 3000);
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
                                                        txtFail.setText(e.getLocalizedMessage());
                                                        failOverlay.setVisibility(View.VISIBLE);
                                                        new Handler().postDelayed(() -> {
                                                            failOverlay.setVisibility(View.GONE);
                                                        }, 3000);
                                                    }
                                                });
                                            }
                                        }).start();
                                    }
                                }
                            })
                            .setNegativeButton("Hayır", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // Hayır seçildiğinde yapılacaklar
                                    int position = adapter2.getPosition(cabinetNumber);
                                    spinnerCabinet.setSelection(position);
                                    dialog.dismiss();
                                }
                            })
                            .show();
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Hiçbir şey seçilmediğinde çalışır (opsiyonel)
            }
        });

        //------ Session Control --------

        Call<RndLaboratoryResponseSession> sessionCheckCall = apiInterface.CheckSession(wmCode);
        ResponseModel<RndLaboratoryResponseSession> sessionCheckCallResponse = null;
        try {
            sessionCheckCallResponse = new APICallAynscTask<RndLaboratoryResponseSession>().execute(sessionCheckCall).get();


            if (sessionCheckCallResponse.Error == null && sessionCheckCallResponse.Content.ResponseCode == 200) {
                txtSuccess.setText("Devam eden sayım mevcut. " +
                        "Dolap numarası : " + sessionCheckCallResponse.Content.Data.get(0).LatestCabinetNumber
                        + "  Taranan son raf numarası:" + (sessionCheckCallResponse.Content.Data.get(0).LatestShelfNumber));

                int position = adapter2.getPosition(sessionCheckCallResponse.Content.Data.get(0).LatestCabinetNumber);
                spinnerCabinet.setSelection(position);

                successOverlay.setVisibility(View.VISIBLE);
                new Handler().postDelayed(() -> {
                    successOverlay.setVisibility(View.GONE);
                }, 5000);
                btnStart.setVisibility(View.INVISIBLE); // görünmez yapar
                sessionId = sessionCheckCallResponse.Content.Data.get(0).Id;
                selfNumber = sessionCheckCallResponse.Content.Data.get(0).LatestShelfNumber;
                cabinetNumber = sessionCheckCallResponse.Content.Data.get(0).LatestCabinetNumber;
                runOnUiThread(() -> loadingDialog.dismiss());
            } else {
                runOnUiThread(() -> loadingDialog.dismiss());
                btnNextShelf.setVisibility(View.INVISIBLE);
                btnEnd.setVisibility(View.INVISIBLE);
                sessionId = 0;
            }
        } catch (ExecutionException e) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // update UI
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
                            // update UI
                            txtFail.setText(e.getLocalizedMessage());
                            failOverlay.setVisibility(View.VISIBLE);
                            new Handler().postDelayed(() -> {
                                failOverlay.setVisibility(View.GONE);
                            }, 3000);
                        }
                    });
                }
            }).start();

        }

        if (readers == null) {
            readers = new Readers(StockTakingActivity.this, ENUM_TRANSPORT.SERVICE_SERIAL);

            ConnectRFIDReader();
        }
        else{

            ConnectRFIDReader();
        }

    }

    private void showEmptyMessage() {
        // Zaten varsa tekrar ekleme
        if (emptyView != null && emptyView.getParent() != null) return;

        TableRow row = new TableRow(this);
        emptyView = new TextView(this);
        emptyView.setText("Veri yok");
        emptyView.setGravity(Gravity.CENTER);
        emptyView.setPadding(16, 16, 16, 16);

        // TableRow'a ekle
        row.addView(emptyView);

        // Tüm satırları temizle ve sadece mesajı göster
        tableData.removeAllViews();
        tableData.addView(row);
    }

    private void hideEmptyMessage() {
        if (emptyView != null) {
            tableData.removeView((TableRow) emptyView.getParent());
            emptyView = null;
        }
    }

    private void addRows(String epc) {
        // Eğer "Veri yok" görünüyorsa kaldır
        if (!EPCs.contains(epc))
        {
            hideEmptyMessage();

            Typeface customFont = ResourcesCompat.getFont(this, R.font.sans);

            int startIndex = tableData.getChildCount() + 1;

            TableRow row = new TableRow(this);
            row.setBackgroundColor(EPCs.size() % 2 == 0 ? 0xFFFFFFFF : 0xFFEFEFEF);
            //row.setGravity(Gravity.CENTER);

            TextView col1 = new TextView(this);
            col1.setText(String.valueOf(startIndex));
            //col1.setGravity(Gravity.CENTER);
            col1.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            col1.setTypeface(customFont, Typeface.BOLD);
            col1.setPadding(8, 8, 8, 8);

            TextView col2 = new TextView(this);
            col2.setText(epc);
            //col2.setGravity(Gravity.CENTER);
            col2.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            col2.setTypeface(customFont, Typeface.BOLD);
            col2.setPadding(8, 8, 8, 8);

            row.addView(col1);
            row.addView(col2);

            tableData.addView(row);
            EPCs.add(epc);

            new Thread(() -> {
                runOnUiThread(() -> {
                    txtEpcCount.setText(String.valueOf(EPCs.size()));
                    txtScannerAnimationNumber.setText(String.valueOf(EPCs.size()));
                });

            }).start();
        }

    }

    private void clearTable() {
        tableData.removeAllViews();
        EPCs.clear();
        showEmptyMessage();
        new Thread(() -> {
            runOnUiThread(() -> {
                txtEpcCount.setText(String.valueOf(EPCs.size()));
                //txtScannerAnimationNumber.setText(String.valueOf(EPCs.size()));
            });
        }).start();
    }

    private String writeTag(String sourceEPC, int Password, MEMORY_BANK memory_bank, String targetData, int offset) {

        TagData tagData = null;
        final OperationFailureException[] operationFailureExceptions = new OperationFailureException[1];
        final InvalidUsageException[] invalidUsageExceptions = new InvalidUsageException[1];
        String tagId = sourceEPC;
        TagAccess tagAccess = new TagAccess();
        TagAccess.WriteAccessParams writeAccessParams = tagAccess.new WriteAccessParams();
        String writeData = targetData; //write data in string
        writeAccessParams.setAccessPassword(Password);
        writeAccessParams.setMemoryBank(memory_bank);
        writeAccessParams.setOffset(offset); // start writing from word offset 0
        writeAccessParams.setWriteData(writeData);
// data length in words
        writeAccessParams.setWriteDataLength(writeData.length() / 4);
// antenna Info is null – performs on all antenna
        /*try {*/
        CountDownLatch latch = new CountDownLatch(1);
        new Thread(() -> {
            try {
                reader.Actions.TagAccess.writeWait(tagId, writeAccessParams, null, tagData);
            } catch (OperationFailureException e) {
                operationFailureExceptions[0] = e;
            } catch (InvalidUsageException e) {
                invalidUsageExceptions[0] = e;
            } finally {
                latch.countDown();  // yazma işlemi bitince latch serbest kalır
            }
        }).start();


        try {
            latch.await();  // burada bekler, işlem bitince devam eder
            // Yazma işlemi bitti, buraya gelir
            System.out.println("ISLEM YAPILDI");
            String message = null;
            if (operationFailureExceptions[0] != null)
            {
                message = operationFailureExceptions[0].getVendorMessage();
            }
            else if (invalidUsageExceptions[0] != null)
            {
                message = invalidUsageExceptions[0].getVendorMessage();
            }
            else{
                System.out.println("YAZILDI");
                message = "OK";
            }

            return message;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return e.getLocalizedMessage();
        }

      /*  } catch (InvalidUsageException e) {
            System.out.println(e.getVendorMessage());

            SetTextView(txtStatus,"YAZILAMADI");

        } catch (OperationFailureException e) {
            System.out.println(e.getVendorMessage());
            SetTextView(txtStatus,"YAZILAMADI");
            return e.getVendorMessage();
        }*/

    }

    public void ConnectRFIDReader(){
        new AsyncTask() {
            @Override
            protected Boolean doInBackground(Object[] objects) {
                try {
                    if (readers != null) {
                        if (readers.GetAvailableRFIDReaderList() != null) {
                            availableRFIDReaderList = readers.GetAvailableRFIDReaderList();
                            System.out.println("Available readers:"+availableRFIDReaderList.size());
                            if (availableRFIDReaderList.size() != 0) {
                                // get first reader from list
                                for(int i = 0; i< availableRFIDReaderList.size(); i++){
                                    System.out.println("Reader "+ i + ": "+ availableRFIDReaderList.get(0).getName());
                                }
                                readerDevice = availableRFIDReaderList.get(0);
                                reader = readerDevice.getRFIDReader();
                                reader.Config.setTriggerMode(ENUM_TRIGGER_MODE.RFID_MODE, true);
                                System.out.println("Reader is connected:"+reader.isConnected());
                                if (!reader.isConnected()) {
                                    // Establish connection to the RFID Reader
                                    System.out.println("Reader connecting");
                                    reader.connect();
                                    System.out.println("Reader connected");
                                    ConfigureReader();
                                    // tag event with tag data
                                    System.out.println("Reader configured");
                                    runOnUiThread(() -> loadingDialogRFID.dismiss());
                                    return true;
                                }
                                else{
                                    ConfigureReader();
                                }
                            }
                        }
                    }
                } catch (InvalidUsageException e) {
                    System.out.println(e.getVendorMessage());
                    e.printStackTrace();
                } catch (OperationFailureException e) {
                    System.out.println(e.getVendorMessage());
                    e.printStackTrace();
                }
                return false;
            }


        }.execute();
    }
    private void ConfigureReader() {
        if (reader.isConnected()) {

            TriggerInfo triggerInfo = new TriggerInfo();
            triggerInfo.StartTrigger.setTriggerType(START_TRIGGER_TYPE.START_TRIGGER_TYPE_IMMEDIATE);
            triggerInfo.StopTrigger.setTriggerType(STOP_TRIGGER_TYPE.STOP_TRIGGER_TYPE_IMMEDIATE);

            try {
                // receive events from reader
                if (eventHandler == null)
                    eventHandler = new EventHandler();
                reader.Events.addEventsListener(eventHandler);
                // HH event
                reader.Events.setHandheldEvent(true);
                // tag event with tag data
                reader.Events.setTagReadEvent(true);
                // application will collect tag using getReadTags API
                reader.Events.setAttachTagDataWithReadEvent(false);
                // set trigger mode as rfid so scanner beam will not come
                reader.Config.setTriggerMode(ENUM_TRIGGER_MODE.RFID_MODE, true);
                // set start and stop triggers
                reader.Config.setStartTrigger(triggerInfo.StartTrigger);
                reader.Config.setStopTrigger(triggerInfo.StopTrigger);

                POWER_EVENT power = new POWER_EVENT();
                power.setPower(500);


                float pwr= power.getPower();
                System.out.println("Power:"+ pwr);

                Antennas.AntennaRfConfig antennaRfConfig = reader.Config.Antennas.getAntennaRfConfig(1);
                antennaRfConfig.setrfModeTableIndex(0);
                antennaRfConfig.setTari(0);
                //antennaRfConfig.setTransmitPowerIndex(500);
                // reader.Config.setAccessOperationWaitTimeout(2000);

                reader.Config.Antennas.setAntennaRfConfig(1, antennaRfConfig);

                Antennas.SingulationControl s1_singulationControl = reader.Config.Antennas.getSingulationControl(1);
                s1_singulationControl.setSession(SESSION.SESSION_S1);
                //s1_singulationControl.setTagPopulation((short)1);
                s1_singulationControl.Action.setInventoryState(INVENTORY_STATE.INVENTORY_STATE_AB_FLIP);
                s1_singulationControl.Action.setSLFlag(SL_FLAG.SL_ALL);
                reader.Config.Antennas.setSingulationControl(1, s1_singulationControl);

                reader.Config.setAccessOperationWaitTimeout(100);

            } catch (InvalidUsageException e) {
            } catch (OperationFailureException e) {
            }
        }
    }

    int i = 0;
    public class EventHandler implements RfidEventsListener {
        // Read Event Notification
        public void eventReadNotify(RfidReadEvents e) {
            TagData[] myTags = reader.Actions.getReadTags(100);
            if (myTags != null) {
                for (TagData tag : myTags) {
                    String epc = tag.getTagID();
                    // Tag EPC bilgisini alırsın
                    int rssi = tag.getPeakRSSI(); // <<< Buradan RSSI değerini alıyorsun
                    System.out.println(tag.getTagID() + " / "+ rssi);

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    // update UI
                                    addRows(tag.getTagID());
                                }
                            });
                        }
                    }).start();


                }

            }

        }

        // Status Event Notification
        public void eventStatusNotify(RfidStatusEvents rfidStatusEvents) {

            if (rfidStatusEvents.StatusEventData.getStatusEventType() == STATUS_EVENT_TYPE.HANDHELD_TRIGGER_EVENT) {
                if (rfidStatusEvents.StatusEventData.HandheldTriggerEventData.getHandheldEvent() == HANDHELD_TRIGGER_EVENT_TYPE.HANDHELD_TRIGGER_PRESSED) {
                    //SetTextView("Butona basıldı");
                    System.out.println("BUTONA BASILDI!!!");


                    try {

                        if (!isRFIDPerforming)
                        {
                            reader.Actions.Inventory.perform();
                            isRFIDPerforming = true;
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            // update UI
                                            if (!scannerDialog.isShowing()) {
                                                scannerDialog.show();
                                            }
                                        }
                                    });
                                }
                            }).start();
                        }
                    } catch (InvalidUsageException e) {
                        e.printStackTrace();
                    } catch (OperationFailureException e) {
                        e.printStackTrace();
                    }

                }
                if (rfidStatusEvents.StatusEventData.HandheldTriggerEventData.getHandheldEvent() == HANDHELD_TRIGGER_EVENT_TYPE.HANDHELD_TRIGGER_RELEASED) {

                    System.out.println("BUTON BIRAKILDI!!!");

                    try {


                        if (isRFIDPerforming){
                            reader.Actions.Inventory.stop();
                            isRFIDPerforming = false;
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            // update UI
                                            if (scannerDialog.isShowing()){
                                                scannerDialog.dismiss();
                                            }
                                        }
                                    });
                                }
                            }).start();
                        }

                    } catch (InvalidUsageException e) {
                        e.printStackTrace();
                    } catch (OperationFailureException e) {
                        e.printStackTrace();
                    }

                }

            }
        }
    }

    public void SetTextView(TextView txtView,String tag){
        new Thread(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // update UI
                        txtView.setText(tag);
                    }
                });
            }
        }).start();
    }

   /*public void showLottiePopup(Context context) {

        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.popup_lottie);

        // Animasyon
        LottieAnimationView lottieAnimation = dialog.findViewById(R.id.lottieAnimation);
        lottieAnimation.playAnimation();

        scannerDialog = dialog;

        txtScannerAnimationNumber = dialog.findViewById(R.id.txtScannerAnimationNumber);
        txtScannerAnimationNumber.setText(String.valueOf(EPCs.size()));
        // Dialog ayarları
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }



    }*/

}