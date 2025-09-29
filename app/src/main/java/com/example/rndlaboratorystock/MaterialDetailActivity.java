package com.example.rndlaboratorystock;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.airbnb.lottie.LottieAnimationView;
import com.example.rndlaboratorystock.Classes.APICallAynscTask;
import com.example.rndlaboratorystock.Classes.APIClient;
import com.example.rndlaboratorystock.Interfaces.APIInterface;
import com.example.rndlaboratorystock.Models.BlankModel;
import com.example.rndlaboratorystock.Models.ResponseModel;
import com.example.rndlaboratorystock.Models.RndLaboratoryMaterial;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

import retrofit2.Call;

public class MaterialDetailActivity extends AppCompatActivity {
    APIInterface apiInterface;

    private static Readers readers;
    private static ArrayList<ReaderDevice> availableRFIDReaderList;
    private static ReaderDevice readerDevice;
    private static RFIDReader reader;
    private EventHandler eventHandler;
    private Boolean isRFIDPerforming = false;

    Button btnBack;

    EditText editTextRFID;

    TextView txtBrand;
    TextView txtBrandCode;
    TextView txtAmount;
    TextView txtUnit;
    TextView txtChemicalName;
    TextView txtCompanyName;
    TextView txtItemNumber;
    TextView txtRemain;
    TextView txtFail;
    TextView txtSuccess;

    TextView txtScannerAnimationNumber;

    FrameLayout failOverlay;
    FrameLayout successOverlay;

    Dialog scannerDialog;

    private String wmCode;
    private String wmName;
    private String wmSurname;

    Dialog loadingDialog;
    Dialog loadingDialogRFID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_material_detail);

        apiInterface = APIClient.getClient().create(APIInterface.class);

        Intent intent = getIntent();
        wmCode = intent.getStringExtra("wmCode");
        wmName = intent.getStringExtra("wmName");
        wmSurname = intent.getStringExtra("wmSurname");


        btnBack = findViewById(R.id.btnBack);

        txtBrand = findViewById(R.id.txtBrand);
        txtBrandCode = findViewById(R.id.txtBrandCode);
        txtAmount = findViewById(R.id.txtAmount);
        txtUnit = findViewById(R.id.txtUnit);
        txtChemicalName = findViewById(R.id.txtChemicalName);
        txtCompanyName = findViewById(R.id.txtCompanyName);
        txtItemNumber = findViewById(R.id.txtItemNumber);
        txtRemain = findViewById(R.id.txtRemain);
        txtFail = findViewById(R.id.txtFail);
        txtSuccess = findViewById(R.id.txtSuccess);

        failOverlay = findViewById(R.id.failOverlay);
        successOverlay = findViewById(R.id.successOverlay);

        editTextRFID = findViewById(R.id.editTextRFID);
        editTextRFID.requestFocus();


        loadingDialog = new Dialog(MaterialDetailActivity.this);
        loadingDialog.setContentView(R.layout.loading_popup_layout);  // Popup layout'unu belirtiyoruz
        loadingDialog.setCancelable(false);  // Kullanıcının popup'ı kapatmasını engelliyoruz
        loadingDialog.getWindow().setBackgroundDrawableResource(R.drawable.loading_popup_background);

        scannerDialog = new Dialog(MaterialDetailActivity.this);
        scannerDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        scannerDialog.setContentView(R.layout.popup_lottie);

        // Animasyon
        LottieAnimationView lottieAnimation = scannerDialog.findViewById(R.id.lottieAnimation);
        lottieAnimation.playAnimation();

        txtScannerAnimationNumber = scannerDialog.findViewById(R.id.txtScannerAnimationNumber);
        // Dialog ayarları
        if (scannerDialog.getWindow() != null) {
            scannerDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            scannerDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        // RFID conf. loading dialog
        loadingDialogRFID = new Dialog(MaterialDetailActivity.this);
        loadingDialogRFID.setContentView(R.layout.rfid_loading_popup_layout);  // Popup layout'unu belirtiyoruz
        loadingDialogRFID.setCancelable(false);  // Kullanıcının popup'ı kapatmasını engelliyoruz
        loadingDialogRFID.getWindow().setBackgroundDrawableResource(R.drawable.loading_popup_background);

        // Lottie animasyonunu popup'ta başlatıyoruz
        LottieAnimationView popupAnimation = loadingDialogRFID.findViewById(R.id.loadingAnimation);
        popupAnimation.playAnimation();  // Animasyonu başlat

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

        editTextRFID.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Metin değişmeden hemen önce tetiklenir
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                if (!s.toString().equals(""))
                {
                    loadingDialog.show();

                    new Thread(() -> {
                        try {
                            Call<RndLaboratoryMaterial> sessionCall = apiInterface.GetMaterialDetailsByRFID(editTextRFID.getText().toString().toUpperCase());
                            ResponseModel<RndLaboratoryMaterial> sessionCallResponse = null;
                            sessionCallResponse = new APICallAynscTask<RndLaboratoryMaterial>().execute(sessionCall).get();
                            ResponseModel<RndLaboratoryMaterial> finalSessionCallResponse = sessionCallResponse;
                            runOnUiThread(() -> {
                                if (finalSessionCallResponse.Error == null && finalSessionCallResponse.Content.ResponseCode == 200) {

                                    //DETAYLAR
                                    runOnUiThread(() -> loadingDialog.dismiss());
                                    RndLaboratoryMaterial.Data material = finalSessionCallResponse.Content.Data.get(0);

                                    txtBrand.setText(material.Brand);
                                    txtBrandCode.setText(material.BrandCode);
                                    txtAmount.setText(String.valueOf(material.Amount));
                                    txtUnit.setText(material.Unit);
                                    txtChemicalName.setText(material.ChemicalName);
                                    txtCompanyName.setText(material.CompanyName);
                                    txtItemNumber.setText(String.valueOf(material.ItemNumber));
                                    txtRemain.setText(String.valueOf(material.Remain));

                                    editTextRFID.setText("");
                                    editTextRFID.requestFocus();
                                }
                                else {
                                    ResponseModel<RndLaboratoryMaterial> finalsessionCallResponse = finalSessionCallResponse;
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
                                    editTextRFID.setText("");
                                    editTextRFID.requestFocus();
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

            @Override
            public void afterTextChanged(Editable s) {
                // Metin değiştikten sonra tetiklenir
            }
        });

        //editTextRFID.setText("0010000000000020925AA101");
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

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
        });

        if (readers == null) {
            readers = new Readers(this, ENUM_TRANSPORT.SERVICE_SERIAL);
            ConnectRFIDReader();
        }
        else{
            //reader.Config.setTriggerMode(ENUM_TRIGGER_MODE.RFID_MODE, true);
            ConnectRFIDReader();
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




    int i = 1;
    String previousEPC = "";
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

                    System.out.println("i:"+ i);

                    if (!previousEPC.equals(epc))
                    {
                        i = 1;
                    }

                    if (rssi > -45 && i > 1){
                        i++;

                        try {

                            if (isRFIDPerforming){
                                i = 1;
                                reader.Actions.Inventory.stop();
                                isRFIDPerforming = false;
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                // update UI
                                                editTextRFID.setText(epc);
                                                if (scannerDialog.isShowing()){
                                                    scannerDialog.dismiss();
                                                }
                                            }
                                        });
                                    }
                                }).start();
                            }

                        } catch (InvalidUsageException ex) {
                            ex.printStackTrace();
                        } catch (OperationFailureException ex) {
                            ex.printStackTrace();
                        }

                    }
                    else if (rssi > -45) {
                        i++;
                        previousEPC = epc;
                    }
                    else{
                        i = 1;
                        //SetTextView(txtStatus,"OKUNAMADI");
                    }



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
}