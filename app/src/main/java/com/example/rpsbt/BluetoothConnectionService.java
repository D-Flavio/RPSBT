package com.example.rpsbt;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import androidx.annotation.Nullable;

public class BluetoothConnectionService {
    private static final UUID RPSBT_UUID = UUID.fromString("cd96e854-743e-49b7-b2bb-a9a9f2cf93e5");

    private static final String TAG = "BluetoothConnectionServ";

    private final BluetoothAdapter mBluetoothAdapter;

    private static final String appName = "RPSBT";

    private AcceptThread mInsecureAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;

    private ProgressDialog mProgressDialog;

    private BluetoothDevice mmDevice;

    private UUID deviceUUID;

    private Context mContext;

    public BluetoothConnectionService(Context context) {
        mContext = context;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        startAccepting();
    }

    @Nullable
    BluetoothListener bluetoothListener = null;

    interface BluetoothListener {
        void onConnected(BluetoothDevice device);
        void onReceive(int bytes);
        void onConnectionFailed();
    }

    public void setBluetoothListener(@Nullable BluetoothListener bluetoothListener) {
        if (bluetoothListener != null)
        this.bluetoothListener = bluetoothListener;
    }

    public synchronized void startAccepting() {
        Log.d(TAG, "startAccepting");

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if (mInsecureAcceptThread == null) {
            mInsecureAcceptThread = new AcceptThread();
            mInsecureAcceptThread.start();
        }
    }

    public synchronized void cancelThreads() {
        Log.d(TAG, "cancelThreads");

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread = null;
        }
        if (mConnectedThread != null){
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
    }

    public void startConnecting(BluetoothDevice device) {
        mProgressDialog = ProgressDialog.show(mContext,"Connecting Bluetooth","Please Wait...",true);
        mConnectThread = new ConnectThread(device, RPSBT_UUID);
        mConnectThread.start();
    }

    private void connected(BluetoothSocket mmSocket) {

        mConnectedThread = new ConnectedThread(mmSocket);
        mConnectedThread.start();
    }

    public void write(int out) {
            mConnectedThread.write(out);
    }

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread(){
            BluetoothServerSocket tmp = null;
            try{
                tmp = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(appName, RPSBT_UUID);
            }catch (IOException e){
            }
            mmServerSocket = tmp;
        }

        public void run() {

            BluetoothSocket socket = null;

            try{
                socket = mmServerSocket.accept();
            }catch (IOException e){
            }
            if(socket != null){
                connected(socket);
            }
        }

        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
            }
        }
    }

    private class ConnectThread extends Thread {
        private BluetoothSocket mmSocket;

        public ConnectThread(BluetoothDevice device, UUID uuid) {
            mmDevice = device;
            deviceUUID = uuid;
        }

        public void run() {
            BluetoothSocket tmp = null;

            try {
                tmp = mmDevice.createRfcommSocketToServiceRecord(deviceUUID);
            } catch (IOException e) {
            }

            mmSocket = tmp;
            mBluetoothAdapter.cancelDiscovery();

            try {
                mmSocket.connect();
            } catch (IOException e) {
                try {
                    mmSocket.close();
                    bluetoothListener.onConnectionFailed();
                } catch (IOException e1) {
                }
            }
            connected(mmSocket);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try{
                mProgressDialog.dismiss();
            }catch (NullPointerException e){
                e.printStackTrace();
            }

            try {
                tmpIn = mmSocket.getInputStream();
                tmpOut = mmSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            int bytes;

            if (bluetoothListener != null) {
                mmDevice = mmSocket.getRemoteDevice();
                bluetoothListener.onConnected(mmDevice);
            }

            while (true) {
                try {
                    bytes = mmInStream.read();

                    if (bytes > 0) {
                        bluetoothListener.onReceive(bytes);
                    }
                } catch (IOException e) {
                    break;
                }
            }
        }

        public void write(int out) {
            try {
                mmOutStream.write(out);
            } catch (IOException e) {
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }
}
