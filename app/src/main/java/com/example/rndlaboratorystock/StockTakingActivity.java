package com.example.rndlaboratorystock;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
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
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;
import com.example.rndlaboratorystock.Classes.APICallAynscTask;
import com.example.rndlaboratorystock.Classes.APIClient;
import com.example.rndlaboratorystock.Classes.NumberPickerDialogFragment;
import com.example.rndlaboratorystock.Helpers.StringHelper;
import com.example.rndlaboratorystock.Interfaces.APIInterface;
import com.example.rndlaboratorystock.Models.BlankModel;
import com.example.rndlaboratorystock.Models.DateAndWeekResponseModel;
import com.example.rndlaboratorystock.Models.IncreaseShelfPostModel;
import com.example.rndlaboratorystock.Models.ResponseModel;
import com.example.rndlaboratorystock.Models.RndIndoorLaboratorySession;
import com.example.rndlaboratorystock.Models.RndIndoorLaboratoryRssiFilter;
import com.example.rndlaboratorystock.Models.SessionPostModel;
import com.example.rndlaboratorystock.Models.UserResponseModel;
import com.google.android.material.navigation.NavigationView;
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
    TextView txtSelfNumber;
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
    private String previousCabinetNumber;
    private String currentCabinetNumber;

    List<String> EPCs;

    Dialog scannerDialog;

    Button btnBack;
    Button btnStart;
    Button btnNextShelf;
    Button btnEnd;
    Button btnReset;
    Button btnCancel;

    FrameLayout failOverlay;
    FrameLayout successOverlay;

    private LottieAnimationView confettiView;

    Dialog loadingDialog;
    Dialog loadingDialogRFID;

    ImageView imgNav;
    DrawerLayout drawerLayout;
    NavigationView navigationView;

    int rssiFilter = 0;

    Spinner spinnerCabinet;

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
        btnReset = findViewById(R.id.btnReset);
        btnCancel = findViewById(R.id.btnCancel);

        txtWorkerNameSurname = findViewById(R.id.txtWorkerNameSurname);
        failOverlay = findViewById(R.id.failOverlay);
        successOverlay = findViewById(R.id.successOverlay);

        confettiView = findViewById(R.id.confettiView);

        txtFail = findViewById(R.id.txtFail);
        txtSuccess = findViewById(R.id.txtSuccess);
        txtDate = findViewById(R.id.txtDate);
        txtWeek = findViewById(R.id.txtWeek);
        txtSelfNumber = findViewById(R.id.txtSelfNumber);

        imgNav = findViewById(R.id.imgNav);
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigationView);

        imgNav.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    drawerLayout.openDrawer(GravityCompat.START);
                }
            }
        });

        Call<RndIndoorLaboratoryRssiFilter> rssiFilterCall = apiInterface.GetRSSI();
        ResponseModel<RndIndoorLaboratoryRssiFilter> rssiFilterCallResponse = null;
        try {
            rssiFilterCallResponse = new APICallAynscTask<RndIndoorLaboratoryRssiFilter>().execute(rssiFilterCall).get();
            if (rssiFilterCallResponse.Error == null && rssiFilterCallResponse.Content.ResponseCode == 200) {
                RndIndoorLaboratoryRssiFilter.Data response = rssiFilterCallResponse.Content.Data.get(0);

                rssiFilter = response.Rssi;

            } else {

                ResponseModel<RndIndoorLaboratoryRssiFilter> finalDateAndWeekResponseModel = rssiFilterCallResponse;
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

        navigationView.setNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.nav_rssi:

                    showNumberPicker(rssiFilter, -80, 0, 1, "RSSI Seçim Ekranı");
                    break;

            }
            drawerLayout.closeDrawers();
            return true;
        });

        spinnerCabinet = findViewById(R.id.cupboardIdentifier);
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

                        Call<RndIndoorLaboratorySession> sessionCall = apiInterface.StartSession(session);
                        ResponseModel<RndIndoorLaboratorySession> sessionCallResponse = null;
                        try {
                            sessionCallResponse = new APICallAynscTask<RndIndoorLaboratorySession>().execute(sessionCall).get();

                            ResponseModel<RndIndoorLaboratorySession> finalSessionCallResponse = sessionCallResponse;
                            runOnUiThread(() -> {
                            if (finalSessionCallResponse.Error == null && finalSessionCallResponse.Content.ResponseCode == 200) {
                                txtSuccess.setText("Stok sayımı başlatıldı. Tarama yapabilirsiniz.");
                                successOverlay.setVisibility(View.VISIBLE);
                                new Handler().postDelayed(() -> {
                                    successOverlay.setVisibility(View.GONE);
                                }, 2000);
                                sessionId = finalSessionCallResponse.Content.Data.get(0).Id;
                                selfNumber = finalSessionCallResponse.Content.Data.get(0).LatestShelfNumber;
                                selfNumber = selfNumber + 1;
                                txtSelfNumber.setText(""+selfNumber);
                                cabinetNumber = finalSessionCallResponse.Content.Data.get(0).LatestCabinetNumber;
                                btnStart.setVisibility(View.INVISIBLE);
                                runOnUiThread(() -> loadingDialog.dismiss());
                                btnNextShelf.setVisibility(View.VISIBLE);
                                btnEnd.setVisibility(View.VISIBLE);
                                btnReset.setVisibility(View.VISIBLE);
                                btnCancel.setVisibility(View.VISIBLE);
                            } else {
                                ResponseModel<RndIndoorLaboratorySession> finalsessionCallResponse = finalSessionCallResponse;
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

                if(EPCs.size() > 0){
                    loadingDialog.show();
                    //selfNumber += 1;
                    txtSelfNumber.setText(""+selfNumber);
                    IncreaseShelfPostModel session = new IncreaseShelfPostModel(sessionId, selfNumber, spinnerCabinet.getSelectedItem().toString(), EPCs);

                    new Thread(() -> {
                        try {
                            Call<BlankModel> sessionCall = apiInterface.IncreaseSession(session);
                            ResponseModel<BlankModel> sessionCallResponse = null;
                            sessionCallResponse = new APICallAynscTask<BlankModel>().execute(sessionCall).get();
                            ResponseModel<BlankModel> finalSessionCallResponse = sessionCallResponse;
                            runOnUiThread(() -> {
                                if (finalSessionCallResponse.Error == null && finalSessionCallResponse.Content.ResponseCode == 200) {
                                    selfNumber += 1;
                                    txtSuccess.setText(finalSessionCallResponse.Content.ResponseDesc+" Raf numarası:" + (selfNumber));
                                    successOverlay.setVisibility(View.VISIBLE);
                                    new Handler().postDelayed(() -> {
                                        successOverlay.setVisibility(View.GONE);
                                    }, 3000);

                                    clearTable();

                                    txtSelfNumber.setText(""+selfNumber);
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

                                                    txtSelfNumber.setText(""+selfNumber);
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

                                    txtSelfNumber.setText(""+selfNumber);
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
                else{
                    Toast.makeText(StockTakingActivity.this, "Önce tarama yapınız.", Toast.LENGTH_SHORT).show();
                }


            }
        });

        btnEnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (EPCs.size() == 0){
                    AlertDialog.Builder builder = new AlertDialog.Builder(StockTakingActivity.this);

                    builder.setTitle("Stok Sayım Bitirme Onayı");
                    builder.setMessage("Herhangi bir RFID okutulmadı. Bu rafı hariç tutup sayımı bitirmek istiyor musunuz?");

                    builder.setPositiveButton("Evet", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            StockTakingEnd();
                        }
                    });

                    builder.setNegativeButton("Hayır", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss(); // Pencereyi kapatır
                        }
                    });

                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
                else{
                    StockTakingEnd();
                }
            }
        });

        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                clearTable();
                Toast.makeText(StockTakingActivity.this, "Tarama sıfırlandı...", Toast.LENGTH_SHORT).show();

            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                new AlertDialog.Builder(StockTakingActivity.this)
                        .setTitle("UYARI")
                        .setMessage("Sayım iptal edilecek. Onaylıyor musunuz?")
                        .setCancelable(false) // dışarı tıklanınca kapanmaz
                        .setPositiveButton("Evet", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // Evet seçildiğinde yapılacaklar
                                loadingDialog.show();
                                new Thread(() -> {
                                    try {
                                        Call<BlankModel> sessionCall = apiInterface.EndSession(sessionId, false);
                                        ResponseModel<BlankModel> sessionCallResponse = null;
                                        try {
                                            sessionCallResponse = new APICallAynscTask<BlankModel>().execute(sessionCall).get();

                                            ResponseModel<BlankModel> finalSessionCallResponse = sessionCallResponse;
                                            runOnUiThread(() -> {
                                                if (finalSessionCallResponse.Error == null &&
                                                        (finalSessionCallResponse.Content.ResponseCode == 200 || finalSessionCallResponse.Content.ResponseCode == 202)) {

                                                    txtSuccess.setText("Sayım iptal edildi.");
                                                    successOverlay.setVisibility(View.VISIBLE);

                                                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                                        successOverlay.setVisibility(View.GONE);
                                                    }, 3000);

                                                    btnStart.setVisibility(View.VISIBLE);
                                                    spinnerCabinet.setSelection(0);
                                                    cabinetNumber = null;
                                                    sessionId = 0;
                                                    btnNextShelf.setVisibility(View.INVISIBLE);
                                                    btnEnd.setVisibility(View.INVISIBLE);
                                                    btnReset.setVisibility(View.INVISIBLE);
                                                    btnCancel.setVisibility(View.INVISIBLE);

                                                    txtScannerAnimationNumber.setText("0");
                                                    txtSelfNumber.setText("1");
                                                    clearTable();
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
                        })
                        .setNegativeButton("Hayır", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // Hayır seçildiğinde yapılacaklar
                                dialog.dismiss();
                            }
                        })
                        .show();
            }
        });

        spinnerCabinet.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Seçilen elemanı al
                previousCabinetNumber = currentCabinetNumber;

                currentCabinetNumber = parent.getItemAtPosition(position).toString();

                if (!currentCabinetNumber.equals(cabinetNumber) && cabinetNumber != null) {
                    new AlertDialog.Builder(StockTakingActivity.this)
                            .setTitle("UYARI")
                            .setMessage("Farklı bir dolaba geçilecek onaylıyor musunuz?")
                            .setCancelable(false) // dışarı tıklanınca kapanmaz
                            .setPositiveButton("Evet", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // Evet seçildiğinde yapılacaklar

                                    loadingDialog.show();

                                    new Thread(() -> {

                                        IncreaseShelfPostModel session2 = new IncreaseShelfPostModel(sessionId, selfNumber, previousCabinetNumber, EPCs);

                                        new Thread(() -> {
                                            try {
                                                Call<BlankModel> sessionCall2 = apiInterface.IncreaseSession(session2);
                                                ResponseModel<BlankModel> sessionCallResponse2 = null;
                                                sessionCallResponse2 = new APICallAynscTask<BlankModel>().execute(sessionCall2).get();
                                                ResponseModel<BlankModel> finalSessionCallResponse2 = sessionCallResponse2;
                                                runOnUiThread(() -> {
                                                    if (finalSessionCallResponse2.Error == null && finalSessionCallResponse2.Content.ResponseCode == 200) {

                                                        IncreaseShelfPostModel session = new IncreaseShelfPostModel(sessionId, 1, spinnerCabinet.getSelectedItem().toString(), EPCs);

                                                        Call<BlankModel> sessionCall = apiInterface.UpdateSession(session);
                                                        ResponseModel<BlankModel> sessionCallResponse = null;
                                                        try {
                                                            sessionCallResponse = new APICallAynscTask<BlankModel>().execute(sessionCall).get();

                                                            if (sessionCallResponse.Error == null && sessionCallResponse.Content.ResponseCode == 200) {
                                                                runOnUiThread(() -> loadingDialog.dismiss());
                                                                selfNumber = 1;
                                                                txtSelfNumber.setText(""+selfNumber);
                                                                txtSuccess.setText(spinnerCabinet.getSelectedItem().toString() + " dolabına geçildi. Raf numarası:" + selfNumber);
                                                                successOverlay.setVisibility(View.VISIBLE);
                                                                new Handler().postDelayed(() -> {
                                                                    successOverlay.setVisibility(View.GONE);
                                                                }, 2000);

                                                                btnStart.setVisibility(View.INVISIBLE); // görünmez yapar
                                                                cabinetNumber = spinnerCabinet.getSelectedItem().toString();

                                                                clearTable();

                                                            } else {
                                                                runOnUiThread(() -> loadingDialog.dismiss());
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
                                                            runOnUiThread(() -> loadingDialog.dismiss());
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
                                                            runOnUiThread(() -> loadingDialog.dismiss());
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
                                                    else if (finalSessionCallResponse2.Error != null && finalSessionCallResponse2.Error.ErrorCode == 406){
                                                        new Thread(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                runOnUiThread(new Runnable() {
                                                                    @Override
                                                                    public void run() {
                                                                        // update UI
                                                                        System.out.println(finalSessionCallResponse2.ResponseCode);
                                                                        txtFail.setText("Malzemeler daha önceden kaydedilmiş. Tekrar tarama yapınız.");
                                                                        failOverlay.setVisibility(View.VISIBLE);
                                                                        new Handler().postDelayed(() -> {
                                                                            failOverlay.setVisibility(View.GONE);
                                                                        }, 3000);
                                                                        selfNumber -= 1;
                                                                        txtSelfNumber.setText(""+selfNumber);
                                                                    }
                                                                });
                                                            }
                                                        }).start();
                                                        runOnUiThread(() -> loadingDialog.dismiss());
                                                    }
                                                    else {
                                                        ResponseModel<BlankModel> finalsessionCallResponse2 = finalSessionCallResponse2;
                                                        new Thread(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                runOnUiThread(new Runnable() {
                                                                    @Override
                                                                    public void run() {
                                                                        // update UI
                                                                        txtFail.setText(finalsessionCallResponse2.Error.ErrorDesc);
                                                                        failOverlay.setVisibility(View.VISIBLE);
                                                                        new Handler().postDelayed(() -> {
                                                                            failOverlay.setVisibility(View.GONE);
                                                                        }, 3000);
                                                                    }
                                                                });
                                                            }
                                                        }).start();
                                                        runOnUiThread(() -> loadingDialog.dismiss());
                                                        selfNumber -= 1;
                                                        txtSelfNumber.setText(""+selfNumber);
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


                                    }).start();



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

        Call<RndIndoorLaboratorySession> sessionCheckCall = apiInterface.CheckSession(wmCode);
        ResponseModel<RndIndoorLaboratorySession> sessionCheckCallResponse = null;
        try {
            sessionCheckCallResponse = new APICallAynscTask<RndIndoorLaboratorySession>().execute(sessionCheckCall).get();


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
                System.out.println("Self Number:"+ selfNumber);
                selfNumber = selfNumber + 1;
                txtSelfNumber.setText(""+selfNumber);
                cabinetNumber = sessionCheckCallResponse.Content.Data.get(0).LatestCabinetNumber;
                runOnUiThread(() -> loadingDialog.dismiss());
            } else {
                runOnUiThread(() -> loadingDialog.dismiss());
                btnNextShelf.setVisibility(View.INVISIBLE);
                btnEnd.setVisibility(View.INVISIBLE);
                btnReset.setVisibility(View.INVISIBLE);
                btnCancel.setVisibility(View.INVISIBLE);
                sessionId = 0;
                selfNumber = 1;
                txtSelfNumber.setText("1");
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

    private void showNumberPicker(int initial, int min, int max, int step, String title) {
        // offset ile negatif değerleri yönetiyoruz
        NumberPickerDialogFragment dialog = new NumberPickerDialogFragment(
                initial, min, max, step, title,
                value -> {
                    // Seçilen değer burada gelir (-80 ile 0 arası)
                    rssiFilter = value;


                    new Thread(() -> {
                        Call<BlankModel> sessionCall = apiInterface.UpdateRSSI(rssiFilter);
                        ResponseModel<BlankModel> sessionCallResponse = null;
                        try {
                                sessionCallResponse = new APICallAynscTask<BlankModel>().execute(sessionCall).get();

                                ResponseModel<BlankModel> finalSessionCallResponse = sessionCallResponse;
                                runOnUiThread(() -> {
                                    if (finalSessionCallResponse.Error == null && finalSessionCallResponse.Content.ResponseCode == 200) {
                                        txtSuccess.setText("RSSI değeri güncellendi.");
                                        successOverlay.setVisibility(View.VISIBLE);
                                        new Handler().postDelayed(() -> {
                                            successOverlay.setVisibility(View.GONE);
                                        }, 2000);
                                    } else {
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

                });

        dialog.show(getSupportFragmentManager(), "numberPicker");
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        Toast.makeText(this, "Lütfen ekrandaki geri butonunu kullanınız...", Toast.LENGTH_SHORT).show();
        // super.onBackPressed(); // Çağırma
    }

    private void StockTakingEnd(){
        loadingDialog.show();

        new Thread(() -> {

            IncreaseShelfPostModel session = new IncreaseShelfPostModel(sessionId, selfNumber, spinnerCabinet.getSelectedItem().toString(), EPCs);

            new Thread(() -> {
                try {
                    Call<BlankModel> sessionCall = apiInterface.IncreaseSession(session);
                    ResponseModel<BlankModel> sessionCallResponse = null;
                    sessionCallResponse = new APICallAynscTask<BlankModel>().execute(sessionCall).get();
                    ResponseModel<BlankModel> finalSessionCallResponse = sessionCallResponse;
                    runOnUiThread(() -> {
                        if (finalSessionCallResponse.Error == null && finalSessionCallResponse.Content.ResponseCode == 200) {

                            try {
                                Call<BlankModel> sessionCall2 = apiInterface.EndSession(sessionId, true);
                                ResponseModel<BlankModel> sessionCallResponse2 = null;
                                try {
                                    sessionCallResponse2 = new APICallAynscTask<BlankModel>().execute(sessionCall2).get();

                                    ResponseModel<BlankModel> finalSessionCallResponse2 = sessionCallResponse2;
                                    runOnUiThread(() -> {
                                        if (finalSessionCallResponse2.Error == null &&
                                                (finalSessionCallResponse2.Content.ResponseCode == 200 || finalSessionCallResponse2.Content.ResponseCode == 202)) {

                                            if (finalSessionCallResponse2.Content.ResponseCode == 200) {
                                                txtSuccess.setText("Sayım tamamlandı. Rapor gönderildi.");
                                            } else if (finalSessionCallResponse2.Content.ResponseCode == 202) {
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
                                            txtSelfNumber.setText("1");
                                            btnReset.setVisibility(View.INVISIBLE);
                                            btnCancel.setVisibility(View.INVISIBLE);
                                            clearTable();
                                            loadingDialog.dismiss();
                                        } else {
                                            loadingDialog.dismiss();
                                            txtFail.setText(finalSessionCallResponse2.Error.ErrorDesc);
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
                                            selfNumber -= 1;
                                            txtSelfNumber.setText(""+selfNumber);
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
                            selfNumber -= 1;
                            txtSelfNumber.setText(""+selfNumber);
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


        }).start();
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

            char thirdFromEnd = epc.charAt(epc.length() - 3);
            int shelfN = Character.getNumericValue(thirdFromEnd);
            String lastTwo = epc.substring(epc.length() - 5, epc.length() - 3);


            System.out.println("Shelf number: " + shelfN);

            Typeface customFont = ResourcesCompat.getFont(this, R.font.sans);

            int startIndex = tableData.getChildCount() + 1;

            TableRow row = new TableRow(this);
            row.setBackgroundColor(EPCs.size() % 2 == 0 ? 0xFFFFFFFF : 0xFFEFEFEF);
            //row.setGravity(Gravity.CENTER);

            TextView col0 = new TextView(this);
            col0.setText(String.valueOf(lastTwo));
            //col1.setGravity(Gravity.CENTER);
            col0.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            col0.setTypeface(customFont, Typeface.BOLD);
            col0.setPadding(8, 8, 8, 8);

            TextView col1 = new TextView(this);
            col1.setText(String.valueOf(shelfN));
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



            if (shelfN != selfNumber || (!lastTwo.equals(spinnerCabinet.getSelectedItem().toString()) && spinnerCabinet.getSelectedItemPosition() != 0)){
                row.setBackgroundColor(Color.parseColor("#E57373"));
                col0.setTextColor(Color.WHITE);
                col1.setTextColor(Color.WHITE);
                col2.setTextColor(Color.WHITE);
                row.addView(col0);
                row.addView(col1);
                row.addView(col2);

                tableData.addView(row,0);

                EPCs.add(0,epc);
            }
            else{
                row.addView(col0);
                row.addView(col1);
                row.addView(col2);

                tableData.addView(row);

                EPCs.add(epc);
            }



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
                txtScannerAnimationNumber.setText(String.valueOf(EPCs.size()));
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

                                System.out.println("Reader is connected:"+reader.isConnected());
                                if (!reader.isConnected()) {
                                    // Establish connection to the RFID Reader
                                    System.out.println("Reader connecting");
                                    reader.connect();
                                    System.out.println("Reader connected");
                                    //reader.Config.setTriggerMode(ENUM_TRIGGER_MODE.RFID_MODE, true);
                                    ConfigureReader();
                                    // tag event with tag data
                                    System.out.println("Reader configured");
                                    runOnUiThread(() -> loadingDialogRFID.dismiss());
                                    return true;
                                }
                                else{
                                    //reader.Config.setTriggerMode(ENUM_TRIGGER_MODE.RFID_MODE, true);
                                    ConfigureReader();
                                    runOnUiThread(() -> loadingDialogRFID.dismiss());
                                }
                            }
                        }
                    }
                } catch (InvalidUsageException e) {
                    System.out.println(e.getVendorMessage());
                    e.printStackTrace();
                    runOnUiThread(() -> loadingDialogRFID.dismiss());
                } catch (OperationFailureException e) {
                    System.out.println(e.getVendorMessage());
                    e.printStackTrace();
                    runOnUiThread(() -> loadingDialogRFID.dismiss());
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
                    System.out.println("rssiFilter "+ rssiFilter);

                    if (rssi > rssiFilter) {
                        System.out.println("OK");
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

        }

        // Status Event Notification
        public void eventStatusNotify(RfidStatusEvents rfidStatusEvents) {

            System.out.println("RFID_Event_State: " + rfidStatusEvents.StatusEventData.HandheldTriggerEventData.getHandheldEvent());
            if (rfidStatusEvents.StatusEventData.getStatusEventType() == STATUS_EVENT_TYPE.HANDHELD_TRIGGER_EVENT) {
                if (rfidStatusEvents.StatusEventData.HandheldTriggerEventData.getHandheldEvent() == HANDHELD_TRIGGER_EVENT_TYPE.HANDHELD_TRIGGER_PRESSED) {
                    //SetTextView("Butona basıldı");
                    System.out.println("BUTONA BASILDI!!!");
                    System.out.println("isRFIDPerforming:"+isRFIDPerforming);


                    try {

                        if (!isRFIDPerforming)
                        {

                            reader.Actions.Inventory.perform();

                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            // update UI
                                            if (scannerDialog  != null && !scannerDialog.isShowing() && !isDestroyed() && !isFinishing()) {
                                                scannerDialog.show();
                                                isRFIDPerforming = true;
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
                    System.out.println("isRFIDPerforming: " + isRFIDPerforming);

                    try {


                        if (isRFIDPerforming){
                            reader.Actions.Inventory.stop();

                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            // update UI
                                            if (scannerDialog.isShowing()){
                                                scannerDialog.dismiss();
                                                isRFIDPerforming = false;
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