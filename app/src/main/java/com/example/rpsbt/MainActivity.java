package com.example.rpsbt;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter mBluetoothAdapter;

    private BluetoothConnectionService mBluetoothConnection;

    private static final UUID RPSBT_UUID = UUID.fromString("cd96e854-743e-49b7-b2bb-a9a9f2cf93e5");

    private BluetoothDevice mBTDevice;

    private String mBTDeviceName;
    private String mBTDeviceAddress;

    private ImageView radialButton;

    private TextView partnerTextView;

    private Button connectButton;
    private Button rockButton;
    private Button paperButton;
    private Button scissorsButton;
    private Button readyButton;
    private Button nextButton;
    private Button previousButton;

    private ArrayList<BluetoothDevice> BTSmartPhones;

    private int myHandInt;
    private int partnerHandInt;
    private int deviceIndex;

    //TODO no landscape mode
    //TODO check button scopes
    //TODO check on variable scopes
    //TODO button disables and enables
    //TODO put in result dialog background color code
    //TODO put in disconnect resetting
    //TODO after result resetting
    //TODO partnerdevice name updates when receiving a connection

    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            partnerHandInt = intent.getIntExtra("theMessage", 0);
            game();
        }
    };

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)) {

                int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, BluetoothAdapter.ERROR);

                switch (mode) {
                    case BluetoothAdapter.STATE_DISCONNECTING:
                        break;
                    case BluetoothAdapter.STATE_DISCONNECTED:
                        connectButton.setEnabled(true);
                        nextButton.setEnabled(true);
                        previousButton.setEnabled(true);
                        break;
                    case BluetoothAdapter.STATE_CONNECTING:
                        connectButton.setText("connecting");
                        connectButton.setEnabled(false);
                        nextButton.setEnabled(false);
                        previousButton.setEnabled(false);
                        break;
                    case BluetoothAdapter.STATE_CONNECTED:
                        connectButton.setText("connected");
                        readyButton.setEnabled(true);
                        break;
                }
            }
        }
    };

    @Override
    protected void onDestroy() {
        unregisterReceiver(mBroadcastReceiver);
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        BTSmartPhones =     new ArrayList<>();

        deviceIndex = 0;

        radialButton =      findViewById(R.id.radial_button);
        rockButton =        findViewById(R.id.rock);
        paperButton =       findViewById(R.id.paper);
        scissorsButton =    findViewById(R.id.scissors);
        readyButton =       findViewById(R.id.ready);
        connectButton =     findViewById(R.id.connection);
        nextButton =        findViewById(R.id.next);
        previousButton =    findViewById(R.id.previous);

        partnerTextView =   findViewById(R.id.deviceName);


        IntentFilter intentFilter = new IntentFilter(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
        registerReceiver(mBroadcastReceiver,intentFilter);

        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, new IntentFilter("incomingMessage"));

        initBackgroundAnim();

        checkBT();

        connectButton.setEnabled(true);
        nextButton.setEnabled(true);
        previousButton.setEnabled(true);

        rockButton.setOnClickListener(v -> {
            radialButton.setBackgroundResource(R.drawable.radial_rock);
            myHandInt = 1;
        });

        paperButton.setOnClickListener(v -> {
            radialButton.setBackgroundResource(R.drawable.radial_paper);
            myHandInt = 2;
        });

        scissorsButton.setOnClickListener(v -> {
            radialButton.setBackgroundResource(R.drawable.radial_scissors);
            myHandInt = 3;
        });

        connectButton.setOnClickListener(v -> {
            connectButton.setEnabled(false);
            nextButton.setEnabled(false);
            previousButton.setEnabled(false);
            mBluetoothConnection.startConnecting(mBTDevice, RPSBT_UUID);
        });

        readyButton.setOnClickListener(v -> {
            readyButton.setEnabled(false);
            rockButton.setEnabled(false);
            paperButton.setEnabled(false);
            scissorsButton.setEnabled(false);
            mBluetoothConnection.write(myHandInt);
            game();
        });

        nextButton.setOnClickListener(v -> {
            if (deviceIndex==BTSmartPhones.size() -1){
                deviceIndex = 0;
                getDevice();
            }else {
                deviceIndex++;
                getDevice();
            }
        });

        previousButton.setOnClickListener(v -> {
            if (deviceIndex==0){
                deviceIndex = BTSmartPhones.size() -1;
                getDevice();
            }else {
                deviceIndex--;
                getDevice();
            }
        });
    }

    public void checkBT() {
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Device doesn't support Bluetooth", Toast.LENGTH_LONG).show();
        } else {
            checkBTState();
        }
    }

    public void checkBTState(){
        if (mBluetoothAdapter.isEnabled()) {
            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
            BTSmartPhones.clear();
            for (BluetoothDevice device : pairedDevices) {
                if (device.getBluetoothClass().getDeviceClass() == BluetoothClass.Device.PHONE_SMART){
                    BTSmartPhones.add(device);
                }
            }
        }else{Toast.makeText(this, "Bluetooth is disabled", Toast.LENGTH_LONG).show(); }

        if (BTSmartPhones.size() == 0){Toast.makeText(this, "No paired smart phones", Toast.LENGTH_LONG).show(); }
        else{
            getDevice();
            startsBTservice();
        }
    }

    public void getDevice(){
        mBTDevice = BTSmartPhones.get(deviceIndex);
        mBTDeviceName = BTSmartPhones.get(deviceIndex).getName();
        mBTDeviceAddress = BTSmartPhones.get(deviceIndex).getAddress();
        partnerTextView.setText(mBTDeviceName);
    }

    public void startsBTservice(){
        mBluetoothAdapter.cancelDiscovery();
        if (mBluetoothConnection == null){mBluetoothConnection = new BluetoothConnectionService(MainActivity.this);}
    }

    public void game() {
        int result;
        if (myHandInt != 0 && partnerHandInt != 0) {
            if (myHandInt == partnerHandInt) {
                result = 1;
                ResultDialog resultDialog = new ResultDialog(result, myHandInt, partnerHandInt);
                resultDialog.show(getSupportFragmentManager(), "resultDialog");
                //tie
            }
            else if (((3 + myHandInt - partnerHandInt) % 3 ) % 2 == 1) {
                result = 2;
                ResultDialog resultDialog = new ResultDialog(result, myHandInt, partnerHandInt);
                resultDialog.show(getSupportFragmentManager(), "resultDialog");
                //win
            }
            else {
                result = 3;
                ResultDialog resultDialog = new ResultDialog(result, myHandInt, partnerHandInt);
                resultDialog.show(getSupportFragmentManager(), "resultDialog");
                //lose
            }
        }
    }

    public void initBackgroundAnim(){
        ConstraintLayout constraintLayout = findViewById(R.id.main_layout);
        AnimationDrawable animationDrawable = (AnimationDrawable) constraintLayout.getBackground();
        animationDrawable.setEnterFadeDuration(2000);
        animationDrawable.setExitFadeDuration(4000);
        animationDrawable.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //disconnect from device
    }
}
