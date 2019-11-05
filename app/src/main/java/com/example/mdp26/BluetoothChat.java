package com.example.mdp26;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

public class BluetoothChat extends Thread {

    private static final String TAG = "BluetoothChat";


    // Declarations
    private static Context myContext;
    private static BluetoothSocket mySocket;
    private static InputStream myInputStream;
    private static OutputStream myOutPutStream;
    private static BluetoothDevice myBluetoothConnectionDevice;

    public static BluetoothDevice getBluetoothDevice(){
        return myBluetoothConnectionDevice;
    }

    public static void startChat(BluetoothSocket socket) {

        mySocket = socket;
        InputStream tempIn = null;
        OutputStream tempOut = null;


        try {
            tempIn = mySocket.getInputStream();
            tempOut = mySocket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
        myInputStream = tempIn;
        myOutPutStream = tempOut;

        byte[] buffer = new byte[1024];

        int bytes;

        while (true) {
            try {
                bytes = myInputStream.read(buffer);
                String incomingMessage = new String(buffer, 0, bytes);
                Log.d(TAG, "InputStream: " + incomingMessage);

                Intent incomingMsgIntent = new Intent("IncomingMsg");
                incomingMsgIntent.putExtra("receivingMsg", incomingMessage);
                LocalBroadcastManager.getInstance(myContext).sendBroadcast(incomingMsgIntent);


            } catch (IOException e) {

                Intent connectionStatusIntent = new Intent("btConnectionStatus");
                connectionStatusIntent.putExtra("ConnectionStatus", "disconnect");
                connectionStatusIntent.putExtra("Device",myBluetoothConnectionDevice);
                LocalBroadcastManager.getInstance(myContext).sendBroadcast(connectionStatusIntent);

                e.printStackTrace();
                break;

            } catch (Exception e){
                e.printStackTrace();

            }

        }
    }


    public static void write(byte[] bytes) {

        String text = new String(bytes, Charset.defaultCharset());
        Log.d(TAG, "Write: Writing to outputstream: " + text);

        try {
            myOutPutStream.write(bytes);
        } catch (Exception e) {
            Log.d(TAG, "Write: Error writing to output stream: " + e.getMessage());
        }
    }

    public void cancel() {
        try {
            mySocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    static void connected(BluetoothSocket mySocket, BluetoothDevice myDevice, Context context) {

        //showToast("Connection Established With: "+myDevice.getName());
        myBluetoothConnectionDevice = myDevice;
        myContext = context;
        startChat(mySocket);


    }

    public static void writeMsg(byte[] out) {
        write(out);

    }
}