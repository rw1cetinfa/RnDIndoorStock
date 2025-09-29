package com.example.rndlaboratorystock;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.TextView;

import com.airbnb.lottie.LottieAnimationView;
import com.example.rndlaboratorystock.Classes.APICallAynscTask;
import com.example.rndlaboratorystock.Classes.APIClient;
import com.example.rndlaboratorystock.Helpers.LocaleHelper;
import com.example.rndlaboratorystock.Helpers.StringHelper;
import com.example.rndlaboratorystock.Interfaces.APIInterface;
import com.example.rndlaboratorystock.Models.BlankModel;
import com.example.rndlaboratorystock.Models.IncreaseShelfPostModel;
import com.example.rndlaboratorystock.Models.ResponseModel;
import com.google.android.material.datepicker.MaterialDatePicker;
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

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

import retrofit2.Call;

public class RfidWriteActivity extends AppCompatActivity {

    APIInterface apiInterface;

    private static Readers readers;
    private static ArrayList<ReaderDevice> availableRFIDReaderList;
    private static ReaderDevice readerDevice;
    private static RFIDReader reader;
    private EventHandler eventHandler;
    private Boolean isRFIDPerforming = false;

    Button btnBack;
    Button btnSave;

    Dialog scannerDialog;

    TextView txtRFIDRead;
    TextView txtRFIDToBeWritten;
    TextView txtFail;
    TextView txtSuccess;

    EditText editTextRFID;

    FrameLayout failOverlay;
    FrameLayout successOverlay;

    private LottieAnimationView confettiView;

    TextView txtScannerAnimationNumber;

    Dialog loadingDialog;
    Dialog loadingDialogRFID;

    private String wmCode;
    private String wmName;
    private String wmSurname;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        wmCode = intent.getStringExtra("wmCode");
        wmName = intent.getStringExtra("wmName");
        wmSurname = intent.getStringExtra("wmSurname");

        apiInterface = APIClient.getClient().create(APIInterface.class);

        setContentView(R.layout.activity_rfid_write);

        btnBack = findViewById(R.id.btnBack);
        btnSave = findViewById(R.id.btnSave);

        txtRFIDRead = findViewById(R.id.txtRFIDRead);
        txtRFIDToBeWritten = findViewById(R.id.txtRFIDToBeWritten);
        txtFail = findViewById(R.id.txtFail);
        txtSuccess = findViewById(R.id.txtSuccess);

        editTextRFID = findViewById(R.id.editTextRFID);

        failOverlay = findViewById(R.id.failOverlay);
        successOverlay = findViewById(R.id.successOverlay);

        confettiView = findViewById(R.id.confettiView);

        loadingDialog = new Dialog(RfidWriteActivity.this);
        loadingDialog.setContentView(R.layout.loading_popup_layout);  // Popup layout'unu belirtiyoruz
        loadingDialog.setCancelable(false);  // Kullanıcının popup'ı kapatmasını engelliyoruz
        loadingDialog.getWindow().setBackgroundDrawableResource(R.drawable.loading_popup_background);


        // Classic loading dialog
        scannerDialog = new Dialog(RfidWriteActivity.this);
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
        loadingDialogRFID = new Dialog(RfidWriteActivity.this);
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
                // Metin değiştiği anda tetiklenir
                txtRFIDRead.setText(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Metin değiştikten sonra tetiklenir
            }
        });

        Spinner spProductNumber = findViewById(R.id.productNumber);

        List<String> items1 = new ArrayList<>();
        items1.add("Ürün numarası seçiniz"); // ilk eleman sabit
        for (int i = 1; i <= 150; i++) {
            items1.add(String.valueOf(i));
        }
        ArrayAdapter<String> adapter1 = new ArrayAdapter<>(
                this,
                R.layout.spinner_item,
                items1
        );
        adapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spProductNumber.setAdapter(adapter1);


        Button btnSelectDate = findViewById(R.id.btnSelectDate);
        TextView txtSelectedDate = findViewById(R.id.txtSelectedDate);

        btnSelectDate.setOnClickListener(v -> {
            final Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            Context context = LocaleHelper.setLocale(RfidWriteActivity.this, "tr"); // helper sınıf kullanabilirsin
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    this,
                    android.R.style.Theme_Holo_Dialog, // koyu tema
                    (view, selectedYear, selectedMonth, selectedDay) -> {
                        // Gün ve ayı 2 haneli yap
                        String date = String.format("%02d.%02d.%04d", selectedDay, selectedMonth + 1, selectedYear);
                        String formatteddate = String.format("%02d%02d%02d", selectedDay, selectedMonth + 1, selectedYear % 100);

                        txtSelectedDate.setText(date);
                        txtRFIDToBeWritten.setText(
                                StringHelper.replaceSubstring(
                                        txtRFIDToBeWritten.getText().toString(),
                                        13,
                                        6,
                                        formatteddate
                                )
                        );
                    },
                    year, month, day
            );

            datePickerDialog.show();
        });

        Spinner spCupboard = findViewById(R.id.cupboardIdentifier);
        ArrayAdapter<String> adapter2 = new ArrayAdapter<>(
                this,
                R.layout.spinner_item,
                new String[]{"Dolap numarası seçiniz", "AA", "AB", "AC", "AD", "AE", "AF",
                        "BA", "BB", "BC", "BD", "BE", "BF"
                });
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCupboard.setAdapter(adapter2);

        Spinner spShelfNumber = findViewById(R.id.shelfNumber);
        ArrayAdapter<String> adapter3 = new ArrayAdapter<>(
                this,
                R.layout.spinner_item,
                new String[]{"Raf numarası seçiniz", "1", "2", "3", "4", "5", "6",
                        "7", "8", "9"
                });
        adapter3.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spShelfNumber.setAdapter(adapter3);

        Spinner spMaterialNumber = findViewById(R.id.materialNumber);
        ArrayAdapter<String> adapter4 = new ArrayAdapter<>(
                this,
                R.layout.spinner_item,
                new String[]{"Adet seçiniz", "1", "2", "3", "4", "5", "6",
                        "7", "8", "9"
                });
        adapter4.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spMaterialNumber.setAdapter(adapter4);

        spProductNumber.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Seçilen elemanı al
                if (position != 0) {
                    String selectedProductNumber = parent.getItemAtPosition(position).toString();

                    int number = Integer.parseInt(selectedProductNumber);
                    String formatted = String.format("%03d", number);

                    txtRFIDToBeWritten.setText(
                            StringHelper.replaceSubstring(
                                    txtRFIDToBeWritten.getText().toString(),
                                    0,
                                    3,
                                    formatted
                            )
                    );
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Hiçbir şey seçilmediğinde çalışır (opsiyonel)
            }
        });

        spCupboard.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Seçilen elemanı al
                if (position != 0) {
                    String selectedCupboard = parent.getItemAtPosition(position).toString();

                    txtRFIDToBeWritten.setText(
                            StringHelper.replaceSubstring(
                                    txtRFIDToBeWritten.getText().toString(),
                                    19,
                                    2,
                                    selectedCupboard
                            )
                    );
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Hiçbir şey seçilmediğinde çalışır (opsiyonel)
            }
        });

        spShelfNumber.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Seçilen elemanı al
                if (position != 0) {
                    String selectedShelfNumber = parent.getItemAtPosition(position).toString();

                    txtRFIDToBeWritten.setText(
                            StringHelper.replaceSubstring(
                                    txtRFIDToBeWritten.getText().toString(),
                                    21,
                                    1,
                                    selectedShelfNumber
                            )
                    );
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Hiçbir şey seçilmediğinde çalışır (opsiyonel)
            }
        });

        spMaterialNumber.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Seçilen elemanı al
                if (position != 0) {
                    String selectedMaterialNumber = parent.getItemAtPosition(position).toString();

                    int number = Integer.parseInt(selectedMaterialNumber);
                    String formatted = String.format("%02d", number);

                    txtRFIDToBeWritten.setText(
                            StringHelper.replaceSubstring(
                                    txtRFIDToBeWritten.getText().toString(),
                                    23,
                                    2,
                                    selectedMaterialNumber
                            )
                    );
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Hiçbir şey seçilmediğinde çalışır (opsiyonel)
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (txtRFIDToBeWritten.getText().toString().equals("000000000000000000000000")) {
                    txtFail.setText("Lütfen tüm alanları doldurunuz.");
                    failOverlay.setVisibility(View.VISIBLE);
                    new Handler().postDelayed(() -> {
                        failOverlay.setVisibility(View.GONE);
                    }, 2000);
                } else if (editTextRFID.getText().toString().equals("")) {
                    txtFail.setText("Lütfen RFID okutunuz.");
                    failOverlay.setVisibility(View.VISIBLE);
                    new Handler().postDelayed(() -> {
                        failOverlay.setVisibility(View.GONE);
                    }, 2000);
                } else if (spProductNumber.getSelectedItemPosition() == 0) {
                    txtFail.setText("Lütfen ürün numarası seçiniz.");
                    failOverlay.setVisibility(View.VISIBLE);
                    new Handler().postDelayed(() -> {
                        failOverlay.setVisibility(View.GONE);
                    }, 2000);
                } else if (txtSelectedDate.equals("Seçilen Tarih")) {
                    txtFail.setText("Lütfen SKT seçiniz.");
                    failOverlay.setVisibility(View.VISIBLE);
                    new Handler().postDelayed(() -> {
                        failOverlay.setVisibility(View.GONE);
                    }, 2000);
                } else if (spCupboard.getSelectedItemPosition() == 0) {
                    txtFail.setText("Lütfen dolap seçiniz.");
                    failOverlay.setVisibility(View.VISIBLE);
                    new Handler().postDelayed(() -> {
                        failOverlay.setVisibility(View.GONE);
                    }, 2000);
                } else if (spShelfNumber.getSelectedItemPosition() == 0) {
                    txtFail.setText("Lütfen raf numarası seçiniz.");
                    failOverlay.setVisibility(View.VISIBLE);
                    new Handler().postDelayed(() -> {
                        failOverlay.setVisibility(View.GONE);
                    }, 2000);
                } else if (spMaterialNumber.getSelectedItemPosition() == 0) {
                    txtFail.setText("Lütfen adet seçiniz.");
                    failOverlay.setVisibility(View.VISIBLE);
                    new Handler().postDelayed(() -> {
                        failOverlay.setVisibility(View.GONE);
                    }, 2000);
                } else {
                    loadingDialog.show();

                    new Thread(() -> {
                        try {
                            Call<BlankModel> sessionCall = apiInterface.CheckRFIDAvailability(txtRFIDToBeWritten.getText().toString());
                            ResponseModel<BlankModel> sessionCallResponse = null;
                            sessionCallResponse = new APICallAynscTask<BlankModel>().execute(sessionCall).get();
                            ResponseModel<BlankModel> finalSessionCallResponse = sessionCallResponse;
                            runOnUiThread(() -> {
                                if (finalSessionCallResponse.Error != null && finalSessionCallResponse.Error.ErrorCode == 404) {

                                    //RFID YAZ
                                    int password = 0;
                                    // perform write, offset is two for EPC ID
                                    String response = writeTag(editTextRFID.getText().toString(), password, MEMORY_BANK.MEMORY_BANK_EPC, txtRFIDToBeWritten.getText().toString(), 2);

                                    System.out.println("Response:" + response);

                                    runOnUiThread(() -> loadingDialog.dismiss());
                                } else if (finalSessionCallResponse.Error == null && finalSessionCallResponse.Content.ResponseCode == 200) {
                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    // update UI
                                                    System.out.println(finalSessionCallResponse.ResponseCode);
                                                    txtFail.setText("RFID kod farklı bir malzeme için kullanılmakta.");
                                                    failOverlay.setVisibility(View.VISIBLE);
                                                    new Handler().postDelayed(() -> {
                                                        failOverlay.setVisibility(View.GONE);
                                                    }, 3000);
                                                }
                                            });
                                        }
                                    }).start();
                                    runOnUiThread(() -> loadingDialog.dismiss());
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
        } else {

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
            if (operationFailureExceptions[0] != null) {
                message = operationFailureExceptions[0].getVendorMessage();
            } else if (invalidUsageExceptions[0] != null) {
                message = invalidUsageExceptions[0].getVendorMessage();
            } else {
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

    int i = 1;

    public class EventHandler implements RfidEventsListener {
        // Read Event Notification
        public void eventReadNotify(RfidReadEvents e) {
            TagData[] myTags = reader.Actions.getReadTags(100);
            if (myTags != null) {
                for (TagData tag : myTags) {
                    String epc = tag.getTagID();
                    // Tag EPC bilgisini alırsın
                    int rssi = tag.getPeakRSSI(); // <<< Buradan RSSI değerini alıyorsun
                    System.out.println(tag.getTagID() + " / " + rssi);

                    System.out.println("i:" + i);
                    if (rssi > -45 && i > 1) {
                        i++;

                        try {


                            if (isRFIDPerforming) {
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
                                                if (scannerDialog.isShowing()) {
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

                    } else if (rssi > -45) {
                        i++;
                    } else {
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

                        if (!isRFIDPerforming) {
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


                        if (isRFIDPerforming) {
                            reader.Actions.Inventory.stop();
                            isRFIDPerforming = false;
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            // update UI
                                            if (scannerDialog.isShowing()) {
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