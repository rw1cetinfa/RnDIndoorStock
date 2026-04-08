package com.example.rndlaboratorystock;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
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
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;
import com.example.rndlaboratorystock.Classes.APICallAynscTask;
import com.example.rndlaboratorystock.Classes.APIClient;
import com.example.rndlaboratorystock.Interfaces.APIInterface;
import com.example.rndlaboratorystock.Models.BlankModel;
import com.example.rndlaboratorystock.Models.ResponseModel;
import com.example.rndlaboratorystock.Models.RndIndoorLaboratoryMaterial;
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
import java.util.concurrent.ExecutionException;

import retrofit2.Call;

public class StockUpdateActivity extends AppCompatActivity {

    APIInterface apiInterface;

    private static Readers readers;
    private static ArrayList<ReaderDevice> availableRFIDReaderList;
    private static ReaderDevice readerDevice;
    private static RFIDReader reader;
    private EventHandler eventHandler;
    private Boolean isRFIDPerforming = false;

    Button btnUpdate;

    Dialog scannerDialog;

    TextView txtMaterialName;
    TextView txtWarehouseCode;
    TextView txtStock;
    TextView txtCriticalStock;
    TextView txtFail;
    TextView txtSuccess;

    EditText editTextRFID;
    EditText editTextNewQuantity;

    FrameLayout failOverlay;
    FrameLayout successOverlay;

    private LottieAnimationView confettiView;

    TextView txtScannerAnimationNumber;

    Dialog loadingDialog;
    Dialog loadingDialogRFID;

    private String currentEPC = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_update);

        apiInterface = APIClient.getClient().create(APIInterface.class);

        btnUpdate = findViewById(R.id.btnUpdate);

        txtMaterialName = findViewById(R.id.txtMaterialName);
        txtWarehouseCode = findViewById(R.id.txtWarehouseCode);
        txtStock = findViewById(R.id.txtStock);
        txtCriticalStock = findViewById(R.id.txtCriticalStock);
        txtFail = findViewById(R.id.txtFail);
        txtSuccess = findViewById(R.id.txtSuccess);

        editTextRFID = findViewById(R.id.editTextRFID);
        editTextNewQuantity = findViewById(R.id.editTextNewQuantity);

        failOverlay = findViewById(R.id.failOverlay);
        successOverlay = findViewById(R.id.successOverlay);

        confettiView = findViewById(R.id.confettiView);

        loadingDialog = new Dialog(StockUpdateActivity.this);
        loadingDialog.setContentView(R.layout.loading_popup_layout);
        loadingDialog.setCancelable(false);
        loadingDialog.getWindow().setBackgroundDrawableResource(R.drawable.loading_popup_background);

        scannerDialog = new Dialog(StockUpdateActivity.this);
        scannerDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        scannerDialog.setContentView(R.layout.popup_lottie);

        LottieAnimationView lottieAnimation = scannerDialog.findViewById(R.id.lottieAnimation);
        lottieAnimation.playAnimation();

        txtScannerAnimationNumber = scannerDialog.findViewById(R.id.txtScannerAnimationNumber);
        if (scannerDialog.getWindow() != null) {
            scannerDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            scannerDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        loadingDialogRFID = new Dialog(StockUpdateActivity.this);
        loadingDialogRFID.setContentView(R.layout.rfid_loading_popup_layout);
        loadingDialogRFID.setCancelable(false);
        loadingDialogRFID.getWindow().setBackgroundDrawableResource(R.drawable.loading_popup_background);

        LottieAnimationView popupAnimation = loadingDialogRFID.findViewById(R.id.loadingAnimation);
        popupAnimation.playAnimation();

        new Thread(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        loadingDialogRFID.show();
                    }
                });
            }
        }).start();

        editTextRFID.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!s.toString().equals("")) {
                    loadingDialog.show();
                    currentEPC = s.toString().toUpperCase();

                    new Thread(() -> {
                        try {
                            Call<RndIndoorLaboratoryMaterial> sessionCall = apiInterface.GetMaterialDetailsByRFID(currentEPC);
                            ResponseModel<RndIndoorLaboratoryMaterial> sessionCallResponse = null;
                            sessionCallResponse = new APICallAynscTask<RndIndoorLaboratoryMaterial>().execute(sessionCall).get();
                            ResponseModel<RndIndoorLaboratoryMaterial> finalSessionCallResponse = sessionCallResponse;
                            runOnUiThread(() -> {
                                if (finalSessionCallResponse.Error == null && finalSessionCallResponse.Content.ResponseCode == 200) {
                                    runOnUiThread(() -> loadingDialog.dismiss());
                                    RndIndoorLaboratoryMaterial.Data material = finalSessionCallResponse.Content.Data.get(0);

                                    txtMaterialName.setText(material.ChemicalName != null ? material.ChemicalName : "-");
                                    txtWarehouseCode.setText(material.BrandCode != null ? material.BrandCode : "-");
                                    txtStock.setText("" + material.Amount);
                                    txtCriticalStock.setText("" + material.Limit);

                                    editTextRFID.setText("");
                                    editTextNewQuantity.requestFocus();
                                } else {
                                    ResponseModel<RndIndoorLaboratoryMaterial> finalsessionCallResponse = finalSessionCallResponse;
                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
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
                                    txtMaterialName.setText("-");
                                    txtWarehouseCode.setText("-");
                                    txtStock.setText("-");
                                    txtCriticalStock.setText("-");
                                    currentEPC = "";

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
            }
        });

        btnUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newQuantity = editTextNewQuantity.getText().toString().trim();

                if (currentEPC.isEmpty()) {
                    txtFail.setText("Lütfen RFID okutunuz.");
                    failOverlay.setVisibility(View.VISIBLE);
                    new Handler().postDelayed(() -> {
                        failOverlay.setVisibility(View.GONE);
                    }, 2000);
                } else if (newQuantity.isEmpty()) {
                    txtFail.setText("Lütfen yeni miktar giriniz.");
                    failOverlay.setVisibility(View.VISIBLE);
                    new Handler().postDelayed(() -> {
                        failOverlay.setVisibility(View.GONE);
                    }, 2000);
                } else {
                    loadingDialog.show();

                    new Thread(() -> {
                        try {
                            Call<BlankModel> sessionCall = apiInterface.UpdateStockByEPC(currentEPC, newQuantity);
                            ResponseModel<BlankModel> sessionCallResponse = null;
                            sessionCallResponse = new APICallAynscTask<BlankModel>().execute(sessionCall).get();
                            ResponseModel<BlankModel> finalSessionCallResponse = sessionCallResponse;

                            runOnUiThread(() -> {
                                if (finalSessionCallResponse.Error == null && finalSessionCallResponse.Content.ResponseCode == 200) {
                                    txtSuccess.setText("Stok başarıyla güncellendi.");
                                    successOverlay.setVisibility(View.VISIBLE);
                                    new Handler().postDelayed(() -> {
                                        successOverlay.setVisibility(View.GONE);
                                        clearFields();
                                    }, 3000);
                                } else {
                                    ResponseModel<BlankModel> finalsessionCallResponse = finalSessionCallResponse;
                                    txtFail.setText(finalsessionCallResponse.Error.ErrorDesc);
                                    failOverlay.setVisibility(View.VISIBLE);
                                    new Handler().postDelayed(() -> {
                                        failOverlay.setVisibility(View.GONE);
                                    }, 3000);
                                }
                                runOnUiThread(() -> loadingDialog.dismiss());
                            });
                        } catch (ExecutionException e) {
                            runOnUiThread(() -> {
                                txtFail.setText(e.getLocalizedMessage());
                                failOverlay.setVisibility(View.VISIBLE);
                                new Handler().postDelayed(() -> {
                                    failOverlay.setVisibility(View.GONE);
                                }, 3000);
                                loadingDialog.dismiss();
                            });
                        } catch (InterruptedException e) {
                            runOnUiThread(() -> {
                                txtFail.setText(e.getLocalizedMessage());
                                failOverlay.setVisibility(View.VISIBLE);
                                new Handler().postDelayed(() -> {
                                    failOverlay.setVisibility(View.GONE);
                                }, 3000);
                                loadingDialog.dismiss();
                            });
                        }
                    }).start();
                }
            }
        });

        if (readers == null) {
            readers = new Readers(this, ENUM_TRANSPORT.SERVICE_SERIAL);
            ConnectRFIDReader();
        } else {
            ConnectRFIDReader();
        }
    }

    private void clearFields() {
        currentEPC = "";
        txtMaterialName.setText("-");
        txtWarehouseCode.setText("-");
        txtStock.setText("-");
        txtCriticalStock.setText("-");
        editTextNewQuantity.setText("");
        editTextRFID.requestFocus();
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        if (reader != null && reader.isConnected()) {
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
        super.onBackPressed();
    }

    public void ConnectRFIDReader() {
        new AsyncTask() {
            @Override
            protected Boolean doInBackground(Object[] objects) {
                try {
                    if (readers != null) {
                        if (readers.GetAvailableRFIDReaderList() != null) {
                            availableRFIDReaderList = readers.GetAvailableRFIDReaderList();
                            System.out.println("Available readers:" + availableRFIDReaderList.size());
                            if (availableRFIDReaderList.size() != 0) {
                                for (int i = 0; i < availableRFIDReaderList.size(); i++) {
                                    System.out.println("Reader " + i + ": " + availableRFIDReaderList.get(0).getName());
                                }
                                readerDevice = availableRFIDReaderList.get(0);
                                reader = readerDevice.getRFIDReader();

                                System.out.println("Reader is connected:" + reader.isConnected());
                                if (!reader.isConnected()) {
                                    System.out.println("Reader connecting");
                                    reader.connect();
                                    reader.Config.setTriggerMode(ENUM_TRIGGER_MODE.RFID_MODE, true);
                                    System.out.println("Reader connected");
                                    ConfigureReader();
                                    System.out.println("Reader configured");
                                    runOnUiThread(() -> loadingDialogRFID.dismiss());
                                    return true;
                                } else {
                                    reader.Config.setTriggerMode(ENUM_TRIGGER_MODE.RFID_MODE, true);
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
                if (eventHandler == null)
                    eventHandler = new EventHandler();
                reader.Events.addEventsListener(eventHandler);
                reader.Events.setHandheldEvent(true);
                reader.Events.setTagReadEvent(true);
                reader.Events.setAttachTagDataWithReadEvent(false);
                reader.Config.setTriggerMode(ENUM_TRIGGER_MODE.RFID_MODE, true);
                reader.Config.setStartTrigger(triggerInfo.StartTrigger);
                reader.Config.setStopTrigger(triggerInfo.StopTrigger);

                POWER_EVENT power = new POWER_EVENT();
                power.setPower(500);

                float pwr = power.getPower();
                System.out.println("Power:" + pwr);

                Antennas.AntennaRfConfig antennaRfConfig = reader.Config.Antennas.getAntennaRfConfig(1);
                antennaRfConfig.setrfModeTableIndex(0);
                antennaRfConfig.setTari(0);
                reader.Config.Antennas.setAntennaRfConfig(1, antennaRfConfig);

                Antennas.SingulationControl s1_singulationControl = reader.Config.Antennas.getSingulationControl(1);
                s1_singulationControl.setSession(SESSION.SESSION_S1);
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
        public void eventReadNotify(RfidReadEvents e) {
            TagData[] myTags = reader.Actions.getReadTags(100);
            if (myTags != null) {
                for (TagData tag : myTags) {
                    String epc = tag.getTagID();
                    int rssi = tag.getPeakRSSI();
                    System.out.println(tag.getTagID() + " / " + rssi);

                    System.out.println("i:" + i);

                    if (!previousEPC.equals(epc)) {
                        i = 1;
                    }

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
                        previousEPC = epc;
                    } else {
                        i = 1;
                    }
                }
            }
        }

        public void eventStatusNotify(RfidStatusEvents rfidStatusEvents) {
            if (rfidStatusEvents.StatusEventData.getStatusEventType() == STATUS_EVENT_TYPE.HANDHELD_TRIGGER_EVENT) {
                if (rfidStatusEvents.StatusEventData.HandheldTriggerEventData.getHandheldEvent() == HANDHELD_TRIGGER_EVENT_TYPE.HANDHELD_TRIGGER_PRESSED) {
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
