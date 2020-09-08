package com.example.rpsbt;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;

import androidx.annotation.Nullable;

public class BluetoothManager {

    private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    private BluetoothConnectionService mBluetoothConnection;

    private Context mainContext;

    private BluetoothConnectionService.ConnectionListener mainConnectionListener;

    private ArrayList<BluetoothDevice> BTSmartPhones;

    @Nullable
    ManagerListener mManagerListener;

    interface ManagerListener {
        void onBluetoothEnabled();
        void onNoSelectedDevice();
    }

    public void setManagerListener(@Nullable ManagerListener managerListener) {
        mManagerListener = managerListener;
    }

    public BluetoothManager(Context context, BluetoothConnectionService.ConnectionListener connectionListener) {
        mainContext = context;
        mainConnectionListener = connectionListener;
        BTSmartPhones  = new ArrayList<>();
    }

    public void checkBT() {
        if (mBluetoothAdapter != null) {
            checkBTState();
        } else {
            Toast.makeText(mainContext, "Device doesn't support Bluetooth", Toast.LENGTH_LONG).show();
        }
    }

    public void checkBTState() {
        if (mBluetoothAdapter.isEnabled()) {
            mManagerListener.onBluetoothEnabled();
            checkBondedPhones();
            if (BTSmartPhones.size() > 0) {
                startBTService();
            }else { Toast.makeText(mainContext, "No paired smart phones", Toast.LENGTH_LONG).show(); }
        }else {
            BTSmartPhones.clear();
            Toast.makeText(mainContext, "Bluetooth is disabled", Toast.LENGTH_LONG).show();
        }
    }

    public void startBTService() {
        mBluetoothAdapter.cancelDiscovery();
        if (mBluetoothConnection == null) {
            mBluetoothConnection = new BluetoothConnectionService(mainContext);
            mBluetoothConnection.setBluetoothListener(mainConnectionListener);
        }
        mBluetoothConnection.startAccepting();
    }

    public void setListenerToNull() {
        mBluetoothConnection.setBluetoothListener(null);
    }

    public void checkBondedPhones() {
        BTSmartPhones.clear();
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : pairedDevices) {
            if (device.getBluetoothClass().getDeviceClass() == BluetoothClass.Device.PHONE_SMART) {
                BTSmartPhones.add(device);
            }
        }
    }

    public void startConnecting(int index) {
        if (BTSmartPhones.get(index) != null) {
            mBluetoothConnection.startConnecting(BTSmartPhones.get(index));
        }else { mManagerListener.onNoSelectedDevice(); }
    }

    public void write(int hand) {
        mBluetoothConnection.write(hand);
    }

    public String getBluetoothDeviceName(int index) {
        if (BTSmartPhones.isEmpty()){
            return "no device";
        }else{
            return BTSmartPhones.get(index).getName();
        }
    }

    public int getBTSmartPhonesSize() {
        return BTSmartPhones.size();
    }

    public void cancelThreads() {
        if (mBluetoothConnection != null) {
            mBluetoothConnection.cancelThreads();
        }
    }

    public boolean getConnectedState() {
        return mBluetoothConnection.isConnected();
    }
}