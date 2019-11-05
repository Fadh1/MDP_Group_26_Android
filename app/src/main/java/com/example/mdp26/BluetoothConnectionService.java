package com.example.mdp26;


import android.app.IntentService;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.IOException;
import java.util.UUID;

public class BluetoothConnectionService extends IntentService {
    private static final String TAG = "BTConnectionService";
    private static final String appName = "MDP26";

    // Declarations
    private static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothAdapter mBAdapter;
    private AcceptThread mAThread;
    private ConnectThread mConThread;
    private BluetoothDevice mmDevice;
    private UUID deviceUUID;
    Context mContext;

    public BluetoothConnectionService() {
        super("BluetoothConnectionService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

        mContext = getApplicationContext();
        mBAdapter = BluetoothAdapter.getDefaultAdapter();

        if (intent.getStringExtra("serviceType").equals("listen")) {
            mmDevice = (BluetoothDevice) intent.getExtras().getParcelable("device");
            startAcceptThread();
        } else {
            mmDevice = (BluetoothDevice) intent.getExtras().getParcelable("device");
            deviceUUID = (UUID) intent.getSerializableExtra("id");
            startClient(mmDevice, deviceUUID);
        }

    }

    /**
     * Thread will run continuously.
     * It runs until a connection is accepted.
     * (or until cancelled).
     */

    private class AcceptThread extends Thread {

        // Declare Server Socket
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread(){
            BluetoothServerSocket tmp = null;

            // New Listening Server Socket
            try{
                tmp = mBAdapter.listenUsingInsecureRfcommWithServiceRecord(appName, myUUID);

            }catch (IOException e){
                Log.e(TAG, "IOException: " + e.getMessage() );
            }

            mmServerSocket = tmp;
        }

        public void run() {

            BluetoothSocket socket;
            Intent connectionStatusIntent;

            try {

                socket = mmServerSocket.accept();
                connectionStatusIntent = new Intent("btConnectionStatus");
                connectionStatusIntent.putExtra("ConnectionStatus", "connect");
                connectionStatusIntent.putExtra("Device", BluetoothFragment.getBluetoothDevice());
                LocalBroadcastManager.getInstance(mContext).sendBroadcast(connectionStatusIntent);
                BluetoothChat.connected(socket, mmDevice, mContext);


            } catch (IOException e) {

                connectionStatusIntent = new Intent("btConnectionStatus");
                connectionStatusIntent.putExtra("ConnectionStatus", "connectionFail");
                connectionStatusIntent.putExtra("Device",  BluetoothFragment.getBluetoothDevice());
            }

        }

        public void cancel() {

            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.d(TAG, "Closing AcceptThread Failed. " + e.getMessage());
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
            BluetoothSocket temp = null;
            Intent connectionStatusIntent;

            try {
                temp = mmDevice.createRfcommSocketToServiceRecord(deviceUUID);
            } catch (IOException e) {

                Log.d(TAG, "ConnectThread Error " + e.getMessage());
            }

            mmSocket = temp;
            mBAdapter.cancelDiscovery();

            try {
                mmSocket.connect();
                connectionStatusIntent = new Intent("btConnectionStatus");
                connectionStatusIntent.putExtra("ConnectionStatus", "connect");
                connectionStatusIntent.putExtra("Device", mmDevice);
                LocalBroadcastManager.getInstance(mContext).sendBroadcast(connectionStatusIntent);
                BluetoothChat.connected(mmSocket, mmDevice, mContext);

                if (mAThread != null) {
                    mAThread.cancel();
                    mAThread = null;
                }

            } catch (IOException e) {
                try {
                    mmSocket.close();

                    connectionStatusIntent = new Intent("btConnectionStatus");
                    connectionStatusIntent.putExtra("ConnectionStatus", "connectionFail");
                    connectionStatusIntent.putExtra("Device", mmDevice);
                    LocalBroadcastManager.getInstance(mContext).sendBroadcast(connectionStatusIntent);
                    Log.d(TAG, "Failed to Connect" + e.getMessage());

                } catch (IOException e1) {
                    Log.d(TAG, "Unable to close socket" + e1.getMessage());
                }

            }

            try {

            } catch (NullPointerException e) {
                e.printStackTrace();
            }

        }

        public void cancel() {

            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.d(TAG, "Closing mySocket failed" + e.getMessage());
            }
        }
    }


    /**
     * Begins chat service.
     */
    public synchronized void startAcceptThread() {
        if (mConThread != null) {
            mConThread.cancel();
            mConThread = null;
        }
        if (mAThread == null) {
            mAThread = new AcceptThread();
            mAThread.start();
        }
    }

    public void startClient(BluetoothDevice device,UUID uuid){
        mConThread = new ConnectThread(device, uuid);
        mConThread.start();
    }

}