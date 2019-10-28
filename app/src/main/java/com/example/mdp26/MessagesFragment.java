package com.example.mdp26;

import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.nio.charset.Charset;
import java.util.UUID;

public class MessagesFragment extends Fragment {

    private static final String TAG = "MessagesFragment";

    // Declarations
    Button btn_save, btn_reset, btn_retrieve, btn_f1, btn_f2;
    EditText et_f1, et_f2;
    SharedPreferences myPrefs;
    public static final String mypreference="mypref";
    public static final String F1="f1";
    public static final String F2="f2";

    // For bluetooth connection status
    private static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    BluetoothDevice myBTConnectionDevice;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_messages,container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //Register Broadcast Receiver for incoming bluetooth message
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(btConnectionReceiver, new IntentFilter("btConnectionStatus"));

        // GUI Buttons
        btn_save = (Button) getActivity().findViewById(R.id.btn_save);
        btn_reset = (Button) getActivity().findViewById(R.id.btn_reset);
        btn_retrieve = (Button) getActivity().findViewById(R.id.btn_retrieve);
        btn_f1 = (Button) getActivity().findViewById(R.id.btn_f1);
        btn_f2 = (Button) getActivity().findViewById(R.id.btn_f2);

        // Use Shared Preferences to save string commands
        myPrefs=getActivity().getSharedPreferences(mypreference, Context.MODE_PRIVATE);

        // Save string commands using Shared Preferences Editor
        btn_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences.Editor editor = myPrefs.edit();
                editor.putString(F1, et_f1.getText().toString());
                editor.putString(F2, et_f2.getText().toString());
                editor.apply();

                Toast.makeText(getContext(), "Commands saved!", Toast.LENGTH_SHORT).show();
            }
        });

        // Reset saved string commands using Shared Preferences Editor
        btn_reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences.Editor editor = myPrefs.edit();
                et_f1.setText("");
                et_f2.setText("");
                editor.clear();
                editor.apply();

                Toast.makeText(getContext(), "Commands reset!", Toast.LENGTH_SHORT).show();
            }
        });

        // Retrieve and display previously saved string commands using Shared Preferences
        btn_retrieve.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String str_f1 = myPrefs.getString(F1, "");
                et_f1.setText(str_f1);
                String str_f2 = myPrefs.getString(F2, "");
                et_f2.setText(str_f2);
                if (myPrefs.contains(F1)){
                    et_f1.setText(myPrefs.getString(F1,"String Not Found"));
                }
                if (myPrefs.contains(F2)){
                    et_f2.setText(myPrefs.getString(F2,"String Not Found"));
                }
            }
        });

        init();
        onClickF1();
        onClickF2();
    }


    // On initialisation
    private void init() {
        et_f1 = (EditText) getActivity().findViewById(R.id.et_f1);
        et_f2 = (EditText) getActivity().findViewById(R.id.et_f2);
    }


    // Outgoing message for string command F1
    public void onClickF1(){

        btn_f1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String tempF1 = myPrefs.getString(F1, "");
                byte[] bytes = tempF1.getBytes(Charset.defaultCharset());
                BluetoothChat.writeMsg(bytes);

                Log.d(TAG, "Outgoing F1 string command: " + tempF1);

                Toast.makeText(getContext(), "F1 string command sent.", Toast.LENGTH_SHORT).show();
            }
        });

    }

    // Outgoing message for string command F2
    public void onClickF2() {

        btn_f2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String tempF2 = myPrefs.getString(F2, "");
                byte[] bytes = tempF2.getBytes(Charset.defaultCharset());
                BluetoothChat.writeMsg(bytes);

                Log.d(TAG, "Outgoing F2 string command: " + tempF2);

                Toast.makeText(getContext(), "F2 string command sent.", Toast.LENGTH_SHORT).show();
            }
        });

    }


    //Broadcast Receiver for Bluetooth Connection Status (Robust BT Connection)
    BroadcastReceiver btConnectionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Log.d(TAG, "Receiving btConnectionStatus Msg!!!");

            String connectionStatus = intent.getStringExtra("ConnectionStatus");
            myBTConnectionDevice = intent.getParcelableExtra("Device");

            // IF disconnected from Bluetooth
            if(connectionStatus.equals("disconnect")){

                Log.d("ConnectAcitvity:","Device Disconnected");

                //Stop Bluetooth Connection Service

                // Reconnect Bluetooth
                AlertDialog alertDialog = new AlertDialog.Builder(getContext()).create();
                alertDialog.setTitle("BLUETOOTH DISCONNECTED");
                alertDialog.setMessage("Connection with device has ended. Do you want to reconnect?");
                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Yes",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {

                                // Start Bluetooth Connection Service
                                Intent connectIntent = new Intent(getContext(), BluetoothConnectionService.class);
                                connectIntent.putExtra("serviceType", "connect");
                                connectIntent.putExtra("device", myBTConnectionDevice);
                                connectIntent.putExtra("id", myUUID);
                                getActivity().startService(connectIntent);

                            }
                        });
                alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "No",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });

                if(!getActivity().isFinishing()){
                    alertDialog.show();
                }
            }

            // Connected to Bluetooth
            else if(connectionStatus.equals("connect")){


                Log.d("ConnectAcitvity:","Device Connected");
                Toast.makeText(getContext(), "Connection Established: "+ myBTConnectionDevice.getName(),
                        Toast.LENGTH_LONG).show();
            }

            // Connection to Bluetooth failed
            else if(connectionStatus.equals("connectionFail")) {
                Toast.makeText(getContext(), "Connection Failed: "+ myBTConnectionDevice.getName(),
                        Toast.LENGTH_LONG).show();
            }

        }
    };

}
