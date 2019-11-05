package com.example.mdp26;


import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class BluetoothFragment extends Fragment{
    private static final String TAG = "BluetoothConnect";

    // Declarations
    public ArrayList<BluetoothDevice> myBTDevArrayList = new ArrayList<>();
    public ArrayList<BluetoothDevice> myBTPairDevArrayList = new ArrayList<>();
    public DeviceListAdapter myDevListAdapter;
    public DeviceListAdapter myPairDevListAdapter;
    static BluetoothDevice myBTDevice;
    BluetoothDevice myBTConDevice;
    BluetoothAdapter myBTAdapter;
    ListView lvNewDev;
    ListView lvPairedDevices;
    Button btnSend;
    EditText sendMessage;
    Button btnSearch;
    StringBuilder incomingMsg;
    TextView incomingMsgTextView;
    Button bluetoothConnect;
    TextView deviceSearchStatus;
    ProgressDialog myProgressDialog;
    TextView pairedDeviceText;
    Intent connectIntent;


    // UUID
    private static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public static BluetoothDevice getBluetoothDevice(){
        return myBTDevice;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bluetooth,container, false);
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {

        bluetoothConnect = getActivity().findViewById(R.id.connectBtn);
        btnSearch = getActivity().findViewById(R.id.searchBtn);
        lvNewDev = getActivity().findViewById(R.id.listNewDevice);
        lvPairedDevices = getActivity().findViewById(R.id.pairedDeviceList);
        btnSend = getActivity().findViewById(R.id.btSend);
        sendMessage = getActivity().findViewById(R.id.messageText);
        incomingMsgTextView = getActivity().findViewById(R.id.incomingText);
        deviceSearchStatus = getActivity().findViewById(R.id.deviceSearchStatus);
        pairedDeviceText = getActivity().findViewById(R.id.pairedDeviceText);
        incomingMsg = new StringBuilder();
        myBTDevice = null;

        // Bluetooth Connection
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(btConnectionReceiver, new IntentFilter("btConnectionStatus"));

        // Incoming Messages
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(myReceiver, new IntentFilter("IncomingMsg"));

        // Bond State Changes
        IntentFilter bondFilter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        getActivity().registerReceiver(bondingBroadcastReceiver, bondFilter);

        // Discoverability
        IntentFilter intentFilter = new IntentFilter(myBTAdapter.ACTION_SCAN_MODE_CHANGED);
        getActivity().registerReceiver(discoverabilityBroadcastReceiver, intentFilter);

        // Discovered Device
        IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        getActivity().registerReceiver(discoveryBroadcastReceiver, discoverDevicesIntent);

        // Register End Discovering
        IntentFilter discoverEndedIntent = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        getActivity().registerReceiver(discoveryEndedBroadcastReceiver, discoverEndedIntent);

        // Enable/Disable Bluetooth
        IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        getActivity().registerReceiver(enableBTBroadcastReceiver, BTIntent);

        // Start Discovering
        IntentFilter discoverStartedIntent = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        getActivity().registerReceiver(discoveryStartedBroadcastReceiver, discoverStartedIntent);

        myBTDevArrayList = new ArrayList<>();
        myBTPairDevArrayList = new ArrayList<>();
        myBTAdapter = BluetoothAdapter.getDefaultAdapter();

        lvPairedDevices.setOnItemClickListener(
                new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                        myBTAdapter.cancelDiscovery();
                        myBTDevice = myBTPairDevArrayList.get(i);
                        lvNewDev.setAdapter(myDevListAdapter);
                    }
                }
        );

        lvNewDev.setOnItemClickListener(
                new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                        myBTAdapter.cancelDiscovery();

                        String deviceName = myBTDevArrayList.get(i).getName();
                        String deviceAddress = myBTDevArrayList.get(i).getAddress();

                        lvPairedDevices.setAdapter(myPairDevListAdapter);

                        // Create bond if > JELLYBEAN
                        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
                            myBTDevArrayList.get(i).createBond();
                            myBTDevice = myBTDevArrayList.get(i);



                        }

                    }
                }
        );

        btnSearch.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                enableBT();
                myBTDevArrayList.clear();


            }
        });

        bluetoothConnect.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {

                if (myBTDevice == null) {

                    Toast.makeText(getContext(), "No Paired Device! Please Select a Device.",
                            Toast.LENGTH_LONG).show();
                } else if(myBTAdapter.getProfileConnectionState(BluetoothHeadset.HEADSET) == BluetoothHeadset.STATE_CONNECTED){
                    Toast.makeText(getContext(), "Bluetooth Connected",
                            Toast.LENGTH_LONG).show();
                }

                else{
                    startBTConnection(myBTDevice, myUUID);
                }
                lvPairedDevices.setAdapter(myPairDevListAdapter);
            }
        });

        btnSend.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                byte[] bytes = sendMessage.getText().toString().getBytes(Charset.defaultCharset());
                BluetoothChat.writeMsg(bytes);
                sendMessage.setText("");
            }
        });



    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "ConnectActivity: onDestroyed: destroyed");
        super.onDestroy();
        getActivity().unregisterReceiver(discoverabilityBroadcastReceiver);
        getActivity().unregisterReceiver(discoveryBroadcastReceiver);
        getActivity().unregisterReceiver(bondingBroadcastReceiver);
        getActivity().unregisterReceiver(discoveryStartedBroadcastReceiver);
        getActivity().unregisterReceiver(discoveryEndedBroadcastReceiver);
        getActivity().unregisterReceiver(enableBTBroadcastReceiver);
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(myReceiver);
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(btConnectionReceiver);


    }


    BroadcastReceiver btConnectionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String connectionStatus = intent.getStringExtra("ConnectionStatus");
            myBTConDevice = intent.getParcelableExtra("Device");

            if(connectionStatus.equals("disconnect")){

                if(connectIntent != null) {
                    getActivity().stopService(connectIntent);
                }

                AlertDialog alertDialog = new AlertDialog.Builder(getContext()).create();
                alertDialog.setTitle("Bluetooth Disconnected");
                alertDialog.setMessage("Connection with device has ended. Do you want to reconnect?");
                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Yes",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                startBTConnection(myBTConDevice, myUUID);

                            }
                        });
                alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "No",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                alertDialog.show();
            }

            else if(connectionStatus.equals("connect")){

                Toast.makeText(getContext(), "Connection Established: "+ myBTConDevice.getName(),
                        Toast.LENGTH_SHORT).show();
            }
            else if(connectionStatus.equals("connectionFail")) {
                Toast.makeText(getContext(), "Connection Failed: "+ myBTConDevice.getName(),
                        Toast.LENGTH_SHORT).show();
            }

        }
    };

    private final BroadcastReceiver enableBTBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(myBTAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, myBTAdapter.ERROR);

                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG, "OnReceiver: STATE OFF");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG, "OnReceiver: STATE TURNING OFF");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "OnReceiver: STATE ON");

                        discoverabilityON();

                        break;

                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG, "OnReceiver: STATE TURNING ON");
                        break;
                }
            }
        }
    };


    private final BroadcastReceiver discoverabilityBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)) {
                int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR);

                switch (mode) {
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                        startSearch();

                        connectIntent = new Intent(getContext(), BluetoothConnectionService.class);
                        connectIntent.putExtra("serviceType", "listen");
                        getActivity().startService(connectIntent);

                        checkPairedDevice();

                        break;
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                        Log.d(TAG, "OnReceiver: DISCOVERABILITY DISABLED, ABLE TO RECEIVE CONNECTION");
                        break;
                    case BluetoothAdapter.SCAN_MODE_NONE:
                        Log.d(TAG, "OnReceiver: DISCOVERABILITY DISABLED, NOT ABLE TO RECEIVE CONNECTION");
                        break;
                    case BluetoothAdapter.STATE_CONNECTING:
                        Log.d(TAG, "OnReceiver: CONNECTING");
                        break;
                    case BluetoothAdapter.STATE_CONNECTED:
                        Log.d(TAG, "OnReceiver: CONNECTED");
                        break;
                }
            }
        }
    };


    private final BroadcastReceiver discoveryBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothDevice.ACTION_FOUND)) {

                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                myBTDevArrayList.add(device);
                myDevListAdapter = new DeviceListAdapter(context, R.layout.device_adapter_view, myBTDevArrayList);
                lvNewDev.setAdapter(myDevListAdapter);

            }
        }
    };


    private final BroadcastReceiver discoveryStartedBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_STARTED)) {

                deviceSearchStatus.setText(R.string.searchDevice);

            }
        }
    };


    private final BroadcastReceiver discoveryEndedBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {

                deviceSearchStatus.setText(R.string.searchDone);

            }
        }
    };


    private final BroadcastReceiver bondingBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {

                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if (device.getBondState() == BluetoothDevice.BOND_BONDED) {

                    myProgressDialog.dismiss();

                    Toast.makeText(getContext(), "Bound Successfully With: " + device.getName(),
                            Toast.LENGTH_LONG).show();
                    myBTDevice = device;
                    checkPairedDevice();
                    lvNewDev.setAdapter(myDevListAdapter);

                }
                if (device.getBondState() == BluetoothDevice.BOND_BONDING) {

                    myProgressDialog = ProgressDialog.show(getContext(), "Bonding With Device", "Please Wait...", true);
                }
                if (device.getBondState() == BluetoothDevice.BOND_NONE) {
                    myProgressDialog.dismiss();
                    AlertDialog alertDialog = new AlertDialog.Builder(getContext()).create();
                    alertDialog.setTitle("Bonding Status");
                    alertDialog.setMessage("Bond Disconnected!");
                    alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                    alertDialog.show();

                    myBTDevice = null;
                }

            }
        }
    };


    BroadcastReceiver myReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String msg = intent.getStringExtra("receivingMsg");
            incomingMsg.append(msg + "\n");
            incomingMsgTextView.setText(incomingMsg);

        }
    };

    private void discoverabilityON() {

        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 900);
        startActivity(discoverableIntent);


    }


    public void enableBT() {
        if (myBTAdapter == null) {
            Toast.makeText(getContext(), "Device Does Not Support Bluetooth.",
                    Toast.LENGTH_LONG).show();
            Log.d(TAG, "enableDisableBT: Does not have BT capabilities.");
        }
        if (!myBTAdapter.isEnabled()) {
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBTIntent);


        }
        if (myBTAdapter.isEnabled()) {
            discoverabilityON();
        }

    }


    private void checkBTPermission() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            int permissionCheck = ContextCompat.checkSelfPermission(getContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION);
            permissionCheck += ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION);
            if (permissionCheck != 0) {
                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001);
            }
        } else {
            Log.d(TAG, "checkBTPermissions: No need to check permissions. SDK version < LOLLIPOP.");

        }
    }

    private void startSearch() {

        if (myBTAdapter.isDiscovering()) {
            myBTAdapter.cancelDiscovery();
            checkBTPermission();
            myBTAdapter.startDiscovery();
        }
        if (!myBTAdapter.isDiscovering()) {
            checkBTPermission();
            myBTAdapter.startDiscovery();
        }
    }

    public void startBTConnection(BluetoothDevice device, UUID uuid) {

        connectIntent = new Intent(getContext(), BluetoothConnectionService.class);
        connectIntent.putExtra("serviceType", "connect");
        connectIntent.putExtra("device", device);
        connectIntent.putExtra("id", uuid);

        getActivity().startService(connectIntent);
    }

    public void checkPairedDevice() {

        Set<BluetoothDevice> pairedDevices = myBTAdapter.getBondedDevices();
        myBTPairDevArrayList.clear();

        if (pairedDevices.size() > 0) {

            for (BluetoothDevice device : pairedDevices) {
                myBTPairDevArrayList.add(device);

            }
            pairedDeviceText.setText("Paired Devices: ");
            myPairDevListAdapter = new DeviceListAdapter(getContext(), R.layout.device_adapter_view, myBTPairDevArrayList);
            lvPairedDevices.setAdapter(myPairDevListAdapter);

        } else {
            pairedDeviceText.setText("No Paired Devices: ");
        }
    }

}





