package com.example.rndlaboratorystock;

import androidx.appcompat.app.AppCompatActivity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Dialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;
import com.example.rndlaboratorystock.Classes.APICallAynscTask;
import com.example.rndlaboratorystock.Classes.APIClient;
import com.example.rndlaboratorystock.Interfaces.APIInterface;
import com.example.rndlaboratorystock.Models.AddEventModel;
import com.example.rndlaboratorystock.Models.ResponseModel;
import com.example.rndlaboratorystock.Models.BlankModel;
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

public class MainActivity extends AppCompatActivity {
    private static Readers readers;
    private static ArrayList<ReaderDevice> availableRFIDReaderList;
    private static ReaderDevice readerDevice;
    private static RFIDReader reader;
    private EventHandler eventHandler;
    APIInterface apiInterface;

    private TextView txtStatus, txtRFIDData, txtRFIDToBeWritten;
    private EditText editTextLotNumber;
    //private EditText editRFID;
    private Button btnWrite;
    private Button btnBack;

    private LottieAnimationView confettiView;
    //private LottieAnimationView loadingAnimation;

    private ImageView imgTire;
    private RotateAnimation rotate;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        apiInterface = APIClient.getClient().create(APIInterface.class);

        txtStatus = findViewById(R.id.txtStatus);
        txtRFIDData = findViewById(R.id.txtRFIDData);
        txtRFIDToBeWritten = findViewById(R.id.txtRFIDToBeWritten);
        btnWrite = findViewById(R.id.btnWrite);
        btnBack = findViewById(R.id.btnBack);
        confettiView = findViewById(R.id.confettiView);
        LinearLayout mainLayout = findViewById(R.id.layoutMain); // Ana layout'un ID'si



        editTextLotNumber = findViewById(R.id.editTextLotNumber);
        editTextLotNumber.requestFocus();
        //loadingAnimation = findViewById(R.id.loadingAnimation);

        FrameLayout successOverlay = findViewById(R.id.successOverlay);
        FrameLayout failOverlay = findViewById(R.id.failOverlay);

        imgTire = findViewById(R.id.imgTire);
        rotate = new RotateAnimation(0f, 360f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        rotate.setDuration(2500);
        rotate.setInterpolator(new LinearInterpolator());
        rotate.setRepeatCount(Animation.INFINITE);

        Intent intent = getIntent();
        String supplier = intent.getStringExtra("supplier");

        btnWrite.setOnClickListener(v -> {

            if(editTextLotNumber.getText().toString().isEmpty() || editTextLotNumber.getText().toString().length() < 5)
            {
                Toast.makeText(this,"ILK LOT NUMARASINI OKUTUNUZ",Toast.LENGTH_LONG).show();
                editTextLotNumber.setText("");
                txtStatus.setText("LOT OKUTUNUZ");
            }

            else if(!txtRFIDData.getText().toString().equals("-")) {


                Dialog loadingDialog = new Dialog(this);
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
                btnWrite.setEnabled(false);

                Runnable afterPopupShown = () -> {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {


                            // Bu kısım, popup görünür hale GELDİKTEN SONRA çalışacak.
                            // Buraya ağır işi başlatabilirsin veya başka işlere devam edebilirsin.
                            String epcRead = txtRFIDData.getText().toString();
                            String epcWrite = txtRFIDToBeWritten.getText().toString();
                            String lotNumber = editTextLotNumber.getText().toString();

                            AddEventModel model = new AddEventModel();
                            model.readPoint = "MOBILK01";
                            model.supplier = supplier;
                            model.lotNumber = lotNumber;
                            model.epcread = epcRead;
                            model.epcwrite = epcWrite;
                            System.out.println("txtRFIDData: " + epcRead);

                            int password = 0;
                            // perform write, offset is two for EPC ID
                            //String response = writeTag(epcRead, password, MEMORY_BANK.MEMORY_BANK_EPC, epcWrite, 2);
                            String response = "OK";
                            System.out.println("Response: " + response);

                            if (response.equals("OK")) {
                                model.eventState = "RFIDWRITESUCCESSFUL";
                                model.errorLog = response;
                            } else {
                                model.eventState = "RFIDWRITEFAILED";
                                model.errorLog = response;
                            }

                            Call<BlankModel> eventCall = apiInterface.AddEvent(model);
                            ResponseModel<BlankModel> eventCallResponse = null;
                            try {
                                eventCallResponse = new APICallAynscTask<BlankModel>().execute(eventCall).get();
                                if (eventCallResponse.Error == null && response.equals("OK")) {

                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    // update UI
                                                    loadingDialog.dismiss();

                                                }
                                            });
                                        }
                                    }).start();
                                    btnWrite.setEnabled(true);

                                    confettiView.setVisibility(View.VISIBLE);
                                    confettiView.playAnimation();
                                    successOverlay.setVisibility(View.VISIBLE);
                                    new Handler().postDelayed(() -> {
                                        successOverlay.setVisibility(View.GONE);
                                    }, 2000);
                                    confettiView.addAnimatorListener(new AnimatorListenerAdapter() {
                                        @Override
                                        public void onAnimationEnd(Animator animation) {
                                            confettiView.setVisibility(View.GONE);
                                        }
                                    });

                                } else {
                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    // update UI
                                                    loadingDialog.dismiss();

                                                }
                                            });
                                        }
                                    }).start();
                                    btnWrite.setEnabled(true);
                                    failOverlay.setVisibility(View.VISIBLE);
                                    new Handler().postDelayed(() -> {
                                        failOverlay.setVisibility(View.GONE);
                                    }, 2000);
                                }
                            } catch (ExecutionException e) {

                            } catch (InterruptedException e) {

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
            else{
                Toast.makeText(this,"ILK OKUMA YAPINIZ",Toast.LENGTH_LONG).show();
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OpenLoginPage();
            }
        });

        editTextLotNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Yazı değişmeden hemen önce
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Her karakter değiştiğinde tetiklenir
                int length = s.length();
                if (length > 5) {
                    Toast.makeText(getApplicationContext(),"RFID Konfigüre edilebilir",Toast.LENGTH_LONG).show();
                    txtStatus.setText("RFID OKUTUNUZ");
                    mainLayout.requestFocus();

                    Dialog loadingDialog = new Dialog(MainActivity.this);
                    loadingDialog.setContentView(R.layout.rfid_loading_popup_layout);  // Popup layout'unu belirtiyoruz
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


                    new Handler().postDelayed(() -> {
                        loadingDialog.dismiss();
                    }, 2000);
                    /*
                    if (readers == null) {
                        readers = new Readers(this, ENUM_TRANSPORT.SERVICE_SERIAL);

                    }
                    else{
                        readers.Dispose();
                    }
                    ConnectRFIDReader();
                    */
                }
                else{
                    /*
                    if (reader.isConnected()){
                        try {
                            reader.Actions.Inventory.stop();
                            reader.Config.setTriggerMode(ENUM_TRIGGER_MODE.RFID_MODE, false);
                            reader.Config.setTriggerMode(ENUM_TRIGGER_MODE.BARCODE_MODE, true);
                            rfidMode = false;
                            reader.disconnect();
                        } catch (InvalidUsageException e) {
                            throw new RuntimeException(e);
                        } catch (OperationFailureException e) {
                            throw new RuntimeException(e);
                        }
                    }*/
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Değişiklik tamamlandıktan sonra
            }
        });


    }

    private void OpenLoginPage(){
        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
        startActivity(intent);
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
                SetTextView(txtStatus, "YAZILDI");
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
                                    ConfigureReader();
                                    // tag event with tag data
                                    System.out.println("Reader configured");

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
                    //txtInfo.setText(tag.getTagID() + " / " + tag.getPeakRSSI());
                    if (rssi > -45 && i > 3){
                        i++;
                        SetTextView(txtRFIDData,tag.getTagID());
                        //SetEditText(edtInfo,tag.getTagID());
                        SetTextView(txtStatus,"OKUNDU");
                        StopRotateTyre();

                        try {
                            reader.Actions.Inventory.stop();
                        } catch (InvalidUsageException ex) {
                            ex.printStackTrace();
                        } catch (OperationFailureException ex) {
                            ex.printStackTrace();
                        }

                    }
                    else if (rssi > -45) {
                        i++;
                    }
                    else{
                        i = 0;
                        //SetTextView(txtStatus,"OKUNAMADI");
                    }
                    //System.out.println("i:" + i);
                }

            }

        }

        // Status Event Notification
        public void eventStatusNotify(RfidStatusEvents rfidStatusEvents) {

            if (rfidStatusEvents.StatusEventData.getStatusEventType() == STATUS_EVENT_TYPE.HANDHELD_TRIGGER_EVENT) {
                if (rfidStatusEvents.StatusEventData.HandheldTriggerEventData.getHandheldEvent() == HANDHELD_TRIGGER_EVENT_TYPE.HANDHELD_TRIGGER_PRESSED) {
                    //SetTextView("Butona basıldı");
                    System.out.println("BUTONA BASILDI!!!");
                    RotateTyre();
                    SetTextView(txtStatus,"RFID OKUNUYOR...");
                    SetTextView(txtRFIDData, "-");
                    try {
                        reader.Actions.Inventory.perform();
                    } catch (InvalidUsageException e) {
                        e.printStackTrace();
                    } catch (OperationFailureException e) {
                        e.printStackTrace();
                    }

                }
                if (rfidStatusEvents.StatusEventData.HandheldTriggerEventData.getHandheldEvent() == HANDHELD_TRIGGER_EVENT_TYPE.HANDHELD_TRIGGER_RELEASED) {
                    StopRotateTyre();

                    try {
                        if (!txtRFIDData.getText().toString().equals("-")){
                            SetTextView(txtStatus, "OKUNDU");
                        }
                        else if(txtRFIDData.getText().toString().equals("-")){
                            SetTextView(txtStatus, "RFID OKUTUNUZ");
                        }
                        reader.Actions.Inventory.stop();
                    } catch (InvalidUsageException e) {
                        e.printStackTrace();
                    } catch (OperationFailureException e) {
                        e.printStackTrace();
                    }

                }

            }
        }
    }

    public void RotateTyre(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // update UI
                        imgTire.startAnimation(rotate);

                    }
                });
            }
        }).start();
    }

    public void StopRotateTyre(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // update UI
                        imgTire.clearAnimation();
                    }
                });
            }
        }).start();
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

}
