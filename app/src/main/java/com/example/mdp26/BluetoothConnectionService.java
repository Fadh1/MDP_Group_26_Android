package com.example.mdp26;


import android.app.IntentService;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.content.BroadcastReceiver;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.UUID;

public class BluetoothConnectionService extends IntentService {
    private static final String TAG = "BTConnectionService";
    private static final String appName = "MDP26";

    // UUID
    private static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");


    //Declaration
    private BluetoothAdapter mBluetoothAdapter;
    private AcceptThread myAcceptThread;
    private ConnectThread mConnectThread;
    private BluetoothDevice mmDevice;
    private UUID deviceUUID;
    Context mContext;


    // Constructor
    public BluetoothConnectionService() {
        super("BluetoothConnectionService");
    }


    // Handle Intent for Service
    // Starts When Service is Created
    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

        mContext = getApplicationContext();
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (intent.getStringExtra("serviceType").equals("listen")) {
            mmDevice = (BluetoothDevice) intent.getExtras().getParcelable("device");
            Log.d(TAG, "Service Handle: startAcceptThread");
            startAcceptThread();
        } else {
            mmDevice = (BluetoothDevice) intent.getExtras().getParcelable("device");
            deviceUUID = (UUID) intent.getSerializableExtra("id");
            Log.d(TAG, "Service Handle: startClientThread");
            startClient(mmDevice, deviceUUID);
        }

    }

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private class AcceptThread extends Thread {

        // The local server socket
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread(){
            BluetoothServerSocket tmp = null;

            // Create a new listening server socket
            try{
                tmp = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(appName, myUUID);

                Log.d(TAG, "AcceptThread: Setting up Server using: " + myUUID);
            }catch (IOException e){
                Log.e(TAG, "AcceptThread: IOException: " + e.getMessage() );
            }

            mmServerSocket = tmp;
        }

        public void run() {

            Log.d(TAG, "AcceptThread: Running");

            BluetoothSocket socket;
            Intent connectionStatusIntent;

            try {

                Log.d(TAG, "Run: RFCOM server socket start....");

                // Blocking call which will only return on a successful connection / exception
                socket = mmServerSocket.accept();

                // Broadcast connection message
                connectionStatusIntent = new Intent("btConnectionStatus");
                connectionStatusIntent.putExtra("ConnectionStatus", "connect");
                connectionStatusIntent.putExtra("Device", BluetoothFragment.getBluetoothDevice());
                LocalBroadcastManager.getInstance(mContext).sendBroadcast(connectionStatusIntent);

                // Successfully connected
                Log.d(TAG, "Run: RFCOM server socket accepted connection");

                // Start BluetoothChat
                BluetoothChat.connected(socket, mmDevice, mContext);


            } catch (IOException e) {

                connectionStatusIntent = new Intent("btConnectionStatus");
                connectionStatusIntent.putExtra("ConnectionStatus", "connectionFail");
                connectionStatusIntent.putExtra("Device",  BluetoothFragment.getBluetoothDevice());

                Log.d(TAG, "AcceptThread: Connection Failed ,IOException: " + e.getMessage());
            }

            Log.d(TAG, "Ended AcceptThread");

        }

        public void cancel() {

            Log.d(TAG, "Cancel: Canceling AcceptThread");

            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.d(TAG, "Cancel: Closing AcceptThread Failed. " + e.getMessage());
            }
        }


    }

    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private BluetoothSocket mmSocket;

        public ConnectThread(BluetoothDevice device, UUID uuid) {
            Log.d(TAG, "ConnectThread: started.");
            mmDevice = device;
            deviceUUID = uuid;
        }

        public void run() {
            BluetoothSocket temp = null;
            Intent connectionStatusIntent;

            Log.d(TAG, "Run: myConnectThread");

            // BluetoothSocket for connection with given BluetoothDevice
            try {
                Log.d(TAG, "ConnectThread: Trying to create InsecureRFcommSocket using UUID: " +
                        myUUID);
                temp = mmDevice.createRfcommSocketToServiceRecord(deviceUUID);
            } catch (IOException e) {

                Log.d(TAG, "ConnectThread: Could not create InsecureRFcommSocket " + e.getMessage());
            }

            mmSocket = temp;

            // Cancel discovery to prevent slow connection
            mBluetoothAdapter.cancelDiscovery();

            try {

                Log.d(TAG, "Connecting to Device: " + mmDevice);
                // Blocking call and will only return on a successful connection / exception
                mmSocket.connect();


                // Broadcast connection message
                connectionStatusIntent = new Intent("btConnectionStatus");
                connectionStatusIntent.putExtra("ConnectionStatus", "connect");
                connectionStatusIntent.putExtra("Device", mmDevice);
                LocalBroadcastManager.getInstance(mContext).sendBroadcast(connectionStatusIntent);

                Log.d(TAG, "run: ConnectThread connected");

                // Start BluetoothChat
                BluetoothChat.connected(mmSocket, mmDevice, mContext);

                // Cancel myAcceptThread for listening
                if (myAcceptThread != null) {
                    myAcceptThread.cancel();
                    myAcceptThread = null;
                }

            } catch (IOException e) {

                // Close socket on error
                try {
                    mmSocket.close();

                    connectionStatusIntent = new Intent("btConnectionStatus");
                    connectionStatusIntent.putExtra("ConnectionStatus", "connectionFail");
                    connectionStatusIntent.putExtra("Device", mmDevice);
                    LocalBroadcastManager.getInstance(mContext).sendBroadcast(connectionStatusIntent);
                    Log.d(TAG, "run: Socket Closed: Connection Failed!! " + e.getMessage());

                } catch (IOException e1) {
                    Log.d(TAG, "myConnectThread, run: Unable to close socket connection: " + e1.getMessage());
                }

            }

            try {

            } catch (NullPointerException e) {
                e.printStackTrace();
            }

        }

        public void cancel() {

            try {
                Log.d(TAG, "Cancel: Closing Client Socket");
                mmSocket.close();
            } catch (IOException e) {
                Log.d(TAG, "Cancel: Closing mySocket in ConnectThread Failed " + e.getMessage());
            }
        }
    }


    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume()
     */
    public synchronized void startAcceptThread() {
        Log.d(TAG, "start");

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if (myAcceptThread == null) {
            myAcceptThread = new AcceptThread();
            myAcceptThread.start();
        }
    }

    /**
     AcceptThread starts and sits waiting for a connection.
     Then ConnectThread starts and attempts to make a connection with the other devices AcceptThread.
     **/

    public void startClient(BluetoothDevice device,UUID uuid){
        Log.d(TAG, "startClient: Started.");
        mConnectThread = new ConnectThread(device, uuid);
        mConnectThread.start();
    }

}