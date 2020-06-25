package com.example.rpsbt;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements BluetoothConnectionService.BluetoothListener {

    private BluetoothAdapter mBluetoothAdapter;

    private BluetoothConnectionService mBluetoothConnection;

    private static final UUID RPSBT_UUID = UUID.fromString("cd96e854-743e-49b7-b2bb-a9a9f2cf93e5");

    private BluetoothDevice mBTDevice;

    private String mBTDeviceName;

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

    //TODO check button scopes
    //TODO check on variable scopes
    //TODO put in disconnect resetting
    //TODO after result resetting
    //TODO dialog window close should not close app but "reset" it
    //TODO dialog finish doesnt destroy app

    @Nullable
    Handler handler = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        BTSmartPhones = new ArrayList<>();

        handler = new Handler(Looper.getMainLooper());

        deviceIndex = 0;

        radialButton    = findViewById(R.id.radial_button);
        rockButton      = findViewById(R.id.rock);
        paperButton     = findViewById(R.id.paper);
        scissorsButton  = findViewById(R.id.scissors);
        readyButton     = findViewById(R.id.ready);
        connectButton   = findViewById(R.id.connection);
        nextButton      = findViewById(R.id.next);
        previousButton  = findViewById(R.id.previous);
        partnerTextView = findViewById(R.id.deviceName);

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
                getDeviceAtIndex();
            }else {
                deviceIndex++;
                getDeviceAtIndex();
            }
        });

        previousButton.setOnClickListener(v -> {
            if (deviceIndex==0){
                deviceIndex = BTSmartPhones.size() -1;
                getDeviceAtIndex();
            }else {
                deviceIndex--;
                getDeviceAtIndex();
            }
        });
    }

    public void checkBT() {
        if (mBluetoothAdapter != null) {
            checkBTState();
        } else {
            Toast.makeText(this, "Device doesn't support Bluetooth", Toast.LENGTH_LONG).show();
        }
    }

    public void checkBTState() {
        if (mBluetoothAdapter.isEnabled()) {
            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
            BTSmartPhones.clear();
            for (BluetoothDevice device : pairedDevices) {
                if (device.getBluetoothClass().getDeviceClass() == BluetoothClass.Device.PHONE_SMART) {
                    BTSmartPhones.add(device);
                }
            }
            if (BTSmartPhones.size() > 0) {
                deviceIndex = 0;
                getDeviceAtIndex();
                startsBTService();
            }else { Toast.makeText(this, "No paired smart phones", Toast.LENGTH_LONG).show(); }
        }else { Toast.makeText(this, "Bluetooth is disabled", Toast.LENGTH_LONG).show(); }
    }

    public void getDeviceAtIndex() {
        mBTDevice = BTSmartPhones.get(deviceIndex);
        mBTDeviceName = BTSmartPhones.get(deviceIndex).getName();
        partnerTextView.setText(mBTDeviceName);
    }

    public void startsBTService() {
        mBluetoothAdapter.cancelDiscovery();
        if (mBluetoothConnection == null) {
            mBluetoothConnection = new BluetoothConnectionService(MainActivity.this);
            mBluetoothConnection.setBluetoothListener(this);
        }
    }

    public void game() {
        int result;
        if (!readyButton.isEnabled() && partnerHandInt != 0) {
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

    public void initBackgroundAnim() {
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

    @Override
    protected void onDestroy() {
        mBluetoothConnection.setBluetoothListener(null);
        handler = null;
        super.onDestroy();
    }

    @Override
    public void onConnected(BluetoothDevice device) {
        handler.post(() -> {
            mBTDeviceName = device.getName();
            partnerTextView.setText(mBTDeviceName);
            connectButton.setEnabled(false);
            connectButton.setText("connected");
            nextButton.setEnabled(false);
            previousButton.setEnabled(false);
            readyButton.setEnabled(true);
        });
    }

    @Override
    public void onReceive(int bytes) {
        partnerHandInt = bytes;
        game();
    }


    @Override
    public void onConnectionFailed() {
    }

    public void reset() {
        myHandInt = 0;
        partnerHandInt = 0;
        deviceIndex = 0;
        radialButton.setBackgroundResource(R.drawable.radial_default);
        rockButton.setEnabled(true);
        paperButton.setEnabled(true);
        scissorsButton.setEnabled(true);
        connectButton.setEnabled(true);
        readyButton.setEnabled(false);
        nextButton.setEnabled(true);
        previousButton.setEnabled(true);
        checkBTState();
    }
}
