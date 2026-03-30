package com.example.rndlaboratorystock;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;
import com.example.rndlaboratorystock.Classes.APICallAynscTask;
import com.example.rndlaboratorystock.Classes.APIClient;
import com.example.rndlaboratorystock.Interfaces.APIInterface;
import com.example.rndlaboratorystock.Models.BlankModel;
import com.example.rndlaboratorystock.Models.ResponseModel;
import com.example.rndlaboratorystock.Models.RndLaboratoryRssiFilter;
import com.zebra.rfid.api3.Antennas;
import com.zebra.rfid.api3.ENUM_TRANSPORT;
import com.zebra.rfid.api3.ENUM_TRIGGER_MODE;
import com.zebra.rfid.api3.HANDHELD_TRIGGER_EVENT_TYPE;
import com.zebra.rfid.api3.INVENTORY_STATE;
import com.zebra.rfid.api3.InvalidUsageException;
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
import com.zebra.rfid.api3.TagData;
import com.zebra.rfid.api3.TriggerInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import retrofit2.Call;

public class MaterialOutActivity extends AppCompatActivity {
    private static Readers readers;
    private static ArrayList<ReaderDevice> availableRFIDReaderList;
    private static ReaderDevice readerDevice;
    private static RFIDReader reader;
    private EventHandler eventHandler;
    private Boolean isRFIDPerforming = false;

    List<String> EPCs;

    Dialog scannerDialog;

    TextView txtScannerAnimationNumber;

    Button btnBack;
    Button btnReset;
    Button btnDelete;

    private String wmCode;
    private String wmName;
    private String wmSurname;

    Dialog loadingDialog;
    Dialog loadingDialogRFID;

    int rssiFilter = -50;

    TableLayout tableData;

    APIInterface apiInterface;

    TextView emptyView;
    TextView txtEpcCount;
    TextView txtFail;
    TextView txtSuccess;

    FrameLayout failOverlay;
    FrameLayout successOverlay;
    private LottieAnimationView confettiView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_material_out);

        apiInterface = APIClient.getClient().create(APIInterface.class);

        btnBack = findViewById(R.id.btnMaterialMenuBack);
        btnReset = findViewById(R.id.btnReset);
        btnDelete = findViewById(R.id.btnDelete);

        tableData = findViewById(R.id.tableData2);
        txtEpcCount = findViewById(R.id.txtEpcCount);
        txtFail = findViewById(R.id.txtFail);
        txtSuccess = findViewById(R.id.txtSuccess);

        failOverlay = findViewById(R.id.failOverlay);
        successOverlay = findViewById(R.id.successOverlay);

        confettiView = findViewById(R.id.confettiView);

        Intent intent = getIntent();
        wmCode = intent.getStringExtra("wmCode");
        wmName = intent.getStringExtra("wmName");
        wmSurname = intent.getStringExtra("wmSurname");

        EPCs = new ArrayList<>();

        scannerDialog = new Dialog(MaterialOutActivity.this);
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

        loadingDialogRFID = new Dialog(MaterialOutActivity.this);
        loadingDialogRFID.setContentView(R.layout.rfid_loading_popup_layout);  // Popup layout'unu belirtiyoruz
        loadingDialogRFID.setCancelable(false);  // Kullanıcının popup'ı kapatmasını engelliyoruz
        loadingDialogRFID.getWindow().setBackgroundDrawableResource(R.drawable.loading_popup_background);

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

        Call<RndLaboratoryRssiFilter> rssiFilterCall = apiInterface.GetRSSI();
        ResponseModel<RndLaboratoryRssiFilter> rssiFilterCallResponse = null;
        try {
            rssiFilterCallResponse = new APICallAynscTask<RndLaboratoryRssiFilter>().execute(rssiFilterCall).get();
            if (rssiFilterCallResponse.Error == null && rssiFilterCallResponse.Content.ResponseCode == 200) {
                RndLaboratoryRssiFilter.Data response = rssiFilterCallResponse.Content.Data.get(0);

                rssiFilter = response.Rssi;

            } else {

                ResponseModel<RndLaboratoryRssiFilter> finalDateAndWeekResponseModel = rssiFilterCallResponse;
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
                GoToMenu();
            }
        });

        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                new AlertDialog.Builder(MaterialOutActivity.this)
                        .setTitle("UYARI")
                        .setMessage("Tarama sıfırlanacak. Onaylıyor musunuz?")
                        .setCancelable(false) // dışarı tıklanınca kapanmaz
                        .setPositiveButton("Evet", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // Evet seçildiğinde yapılacaklar
                                clearTable();
                                Toast.makeText(MaterialOutActivity.this, "Tarama sıfırlandı...", Toast.LENGTH_SHORT).show();
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

        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AlertDialog.Builder(MaterialOutActivity.this)
                        .setTitle("UYARI")
                        .setMessage("Taranan RFID'ler sistemden kaldırılacak. Onaylıyor musunuz?")
                        .setCancelable(false) // dışarı tıklanınca kapanmaz
                        .setPositiveButton("Evet", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // Evet seçildiğinde yapılacaklar

                                Call<BlankModel> rssiFilterCall = apiInterface.DeleteByEpcs(EPCs,wmCode);
                                ResponseModel<BlankModel> rssiFilterCallResponse = null;
                                try {
                                    rssiFilterCallResponse = new APICallAynscTask<BlankModel>().execute(rssiFilterCall).get();
                                    if (rssiFilterCallResponse.Error == null && rssiFilterCallResponse.Content.ResponseCode == 200) {
                                        txtSuccess.setText("İşlem başarılı");
                                        successOverlay.setVisibility(View.VISIBLE);
                                        new Handler().postDelayed(() -> {
                                            successOverlay.setVisibility(View.GONE);
                                        }, 2000);

                                    } else {

                                        ResponseModel<BlankModel> finalDateAndWeekResponseModel = rssiFilterCallResponse;
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

                                clearTable();
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

        if (readers == null) {
            readers = new Readers(MaterialOutActivity.this, ENUM_TRANSPORT.SERVICE_SERIAL);

            ConnectRFIDReader();
        }
        else{

            ConnectRFIDReader();
        }

    }
    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        Toast.makeText(this, "Lütfen ekrandaki geri butonunu kullanınız...", Toast.LENGTH_SHORT).show();
        // super.onBackPressed(); // Çağırma
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

    private void GoToMenu(){
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
        Intent intent = new Intent(getApplicationContext(), MaterialInOutActivity.class);
        //intent.putExtra("supplier", supplier);
        intent.putExtra("wmCode", wmCode);
        intent.putExtra("wmName", wmName);
        intent.putExtra("wmSurname", wmSurname);
        startActivity(intent);
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


            row.addView(col0);
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
                                        //EPCs.add(tag.getTagID());
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
}