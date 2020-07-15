package com.example.rpsbt;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements BluetoothManager.ManagerListener, BluetoothConnectionService.ConnectionListener, ResultDialog.DialogListener {

    BluetoothManager mBluetoothManager;

    private ImageView radialButton;

    private TextView partnerTextView;

    private Button connectButton;
    private Button rockButton;
    private Button paperButton;
    private Button scissorsButton;
    private Button readyButton;
    private Button nextButton;
    private Button previousButton;

    private ResultDialog resultDialog;

    private int myHandInt;
    private int partnerHandInt;
    private int deviceIndex;
    private int numOfPairedPhones;

    private boolean readyState;

    @Nullable
    private Handler handler = null;

    BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case BluetoothAdapter.ACTION_STATE_CHANGED:
                    final int extraState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                    switch (extraState) {
                        case BluetoothAdapter.STATE_OFF:
                            Toast.makeText(getApplicationContext(), "Bluetooth is disabled", Toast.LENGTH_LONG).show();
                            connectButton.setEnabled(false);
                            resetBT();
                            break;
                        case BluetoothAdapter.STATE_ON:
                            resetBT();
                            break;
                    }
                    break;
                case BluetoothDevice.ACTION_BOND_STATE_CHANGED:
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (device.getBluetoothClass().getDeviceClass() == BluetoothClass.Device.PHONE_SMART) {
                        final int extraBondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothAdapter.ERROR);
                        switch (extraBondState)  {
                            case BluetoothDevice.BOND_BONDED:
                                mBluetoothManager.checkBondedPhones();
                                deviceIndex = 0;
                                numOfPairedPhones = mBluetoothManager.getBTSmartPhonesSize();
                                partnerTextView.setText(mBluetoothManager.getBluetoothDeviceName(deviceIndex));
                                break;
                        }
                    }
                    break;
            }
        }
    };

    //TODO clean up service class

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        handler = new Handler(Looper.getMainLooper());

        deviceIndex = 0;

        readyState = false;

        radialButton    = findViewById(R.id.radial_button);
        rockButton      = findViewById(R.id.rock);
        paperButton     = findViewById(R.id.paper);
        scissorsButton  = findViewById(R.id.scissors);
        readyButton     = findViewById(R.id.ready);
        connectButton   = findViewById(R.id.connection);
        nextButton      = findViewById(R.id.next);
        previousButton  = findViewById(R.id.previous);
        partnerTextView = findViewById(R.id.deviceName);

        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        this.registerReceiver(mBroadcastReceiver, filter);

        resultDialog = new ResultDialog();
        resultDialog.setDialogListener(this);

        mBluetoothManager = new BluetoothManager(this, this);
        mBluetoothManager.setManagerListener(this);

        mBluetoothManager.checkBT();
        numOfPairedPhones = mBluetoothManager.getBTSmartPhonesSize();
        partnerTextView.setText(mBluetoothManager.getBluetoothDeviceName(deviceIndex));

        initBackgroundAnim();

        rockButton.setOnClickListener(v -> {
            radialButton.setBackgroundResource(R.drawable.radial_rock);
            myHandInt = 1;
            if (!readyButton.isEnabled()){readyButton.setEnabled(true);}
        });

        paperButton.setOnClickListener(v -> {
            radialButton.setBackgroundResource(R.drawable.radial_paper);
            myHandInt = 2;
            if (!readyButton.isEnabled()){readyButton.setEnabled(true);}
        });

        scissorsButton.setOnClickListener(v -> {
            radialButton.setBackgroundResource(R.drawable.radial_scissors);
            myHandInt = 3;
            if (!readyButton.isEnabled()){readyButton.setEnabled(true);}
        });

        connectButton.setOnClickListener(v -> {
            if (!mBluetoothManager.getConnectedState()) {
                connectButton.setEnabled(false);
                nextButton.setEnabled(false);
                previousButton.setEnabled(false);
                mBluetoothManager.startConnecting(deviceIndex);
            }else {
                mBluetoothManager.cancelThreads();
                resetBT();
            }

        });

        readyButton.setOnClickListener(v -> {
            readyButton.setEnabled(false);
            radialDisable(myHandInt);
            rockButton.setEnabled(false);
            paperButton.setEnabled(false);
            scissorsButton.setEnabled(false);
            mBluetoothManager.write(myHandInt);
            readyState = true;
            game();
        });

        nextButton.setOnClickListener(v -> {
            if (numOfPairedPhones != 0) {
                if (deviceIndex==numOfPairedPhones -1) {
                    deviceIndex = 0;
                    partnerTextView.setText(mBluetoothManager.getBluetoothDeviceName(deviceIndex));
                }else {
                    deviceIndex++;
                    partnerTextView.setText(mBluetoothManager.getBluetoothDeviceName(deviceIndex));
                }
            }
        });

        previousButton.setOnClickListener(v -> {
            if (numOfPairedPhones != 0) {
                if (deviceIndex==0) {
                    deviceIndex = numOfPairedPhones -1;
                    partnerTextView.setText(mBluetoothManager.getBluetoothDeviceName(deviceIndex));
                }else {
                    deviceIndex--;
                    partnerTextView.setText(mBluetoothManager.getBluetoothDeviceName(deviceIndex));
                }
            }
        });
    }

    public void game() {
        int result;
        if (readyState && partnerHandInt != 0) {
            if (myHandInt == partnerHandInt) {
                result = 1;
                resultDialog.setVariables(result, myHandInt, partnerHandInt);
                resultDialog.show(getSupportFragmentManager(), "resultDialog");
                resetGame();
                //tie
            }
            else if (((3 + myHandInt - partnerHandInt) % 3 ) % 2 == 1) {
                result = 2;
                resultDialog.setVariables(result, myHandInt, partnerHandInt);
                resultDialog.show(getSupportFragmentManager(), "resultDialog");
                resetGame();
                //win
            }
            else {
                result = 3;
                resultDialog.setVariables(result, myHandInt, partnerHandInt);
                resultDialog.show(getSupportFragmentManager(), "resultDialog");
                resetGame();
                //lose
            }
        }
    }

    public void radialDisable(int hand) {
        switch (hand) {
            case 1:
                radialButton.setBackgroundResource(R.drawable.radial_rock_disabled);
                break;
            case 2:
                radialButton.setBackgroundResource(R.drawable.radial_paper_disabled);
                break;
            case 3:
                radialButton.setBackgroundResource(R.drawable.radial_scissors_disabled);
                break;
        }
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(mBroadcastReceiver);
        mBluetoothManager.cancelThreads();
        resultDialog.setDialogListener(null);
        mBluetoothManager.setManagerListener(null);
        mBluetoothManager.setListenerToNull();
        handler = null;
        super.onDestroy();
    }

    @Override
    public void onBluetoothEnabled() {
        deviceIndex = 0;
        connectButton.setEnabled(true);
        connectButton.setText("connect");
        nextButton.setEnabled(true);
        previousButton.setEnabled(true);
    }

    @Override
    public void onNoSelectedDevice() {
        connectButton.setEnabled(true);
        nextButton.setEnabled(true);
        previousButton.setEnabled(true);
        Toast.makeText(this, "no device selected", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onConnectionFailed() {
        handler.post(() -> {
            Toast.makeText(this, "failed to connect", Toast.LENGTH_LONG).show();
            connectButton.setText("connect");
            resetBT();
        });
    }

    @Override
    public void onConnected(BluetoothDevice device) {
        handler.post(() -> {
            partnerTextView.setText(device.getName());
            connectButton.setEnabled(true);
            connectButton.setText("disconnect");
            nextButton.setEnabled(false);
            previousButton.setEnabled(false);
            rockButton.setEnabled(true);
            paperButton.setEnabled(true);
            scissorsButton.setEnabled(true);
            radialButton.setBackgroundResource(R.drawable.radial_default);
        });
    }

    @Override
    public void onDisconnect() {
        handler.post(() -> {
            Toast.makeText(this, "lost connection to device", Toast.LENGTH_LONG).show();
            resetGame();
            disableGame();
            resetBT();
        });
    }

    @Override
    public void onBTRead(int bytes) {
        handler.post(() -> {
            partnerHandInt = bytes;
            game();
        });

    }
    @Override
    public void onPositiveButtonPress() {

    }

    public void resetGame() {
        readyState = false;
        myHandInt = 0;
        partnerHandInt = 0;
        radialButton.setBackgroundResource(R.drawable.radial_default);
        rockButton.setEnabled(true);
        paperButton.setEnabled(true);
        scissorsButton.setEnabled(true);
    }

    public void disableGame() {
        radialButton.setBackgroundResource(R.drawable.radial_default_disabled);
        rockButton.setEnabled(false);
        paperButton.setEnabled(false);
        scissorsButton.setEnabled(false);
        readyButton.setEnabled(false);
    }

    public void resetBT() {
        mBluetoothManager.cancelThreads();
        mBluetoothManager.checkBT();
        deviceIndex = 0;
        numOfPairedPhones = mBluetoothManager.getBTSmartPhonesSize();
        partnerTextView.setText(mBluetoothManager.getBluetoothDeviceName(deviceIndex));
        nextButton.setEnabled(true);
        previousButton.setEnabled(true);
    };

    public void initBackgroundAnim() {
        ConstraintLayout constraintLayout = findViewById(R.id.main_layout);
        AnimationDrawable animationDrawable = (AnimationDrawable) constraintLayout.getBackground();
        animationDrawable.setEnterFadeDuration(2000);
        animationDrawable.setExitFadeDuration(4000);
        animationDrawable.start();
    }



}
