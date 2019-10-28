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
    public ArrayList<BluetoothDevice> myBTDevicesArrayList = new ArrayList<>();
    public ArrayList<BluetoothDevice> myBTPairedDevicesArrayList = new ArrayList<>();
    public DeviceListAdapter myDeviceListAdapter;
    public DeviceListAdapter myPairedDeviceListAdapter;
    static BluetoothDevice myBTDevice;
    BluetoothDevice myBTConnectionDevice;
    BluetoothAdapter myBluetoothAdapter;
    ListView lvNewDevices;
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
        lvNewDevices = getActivity().findViewById(R.id.listNewDevice);
        lvPairedDevices = getActivity().findViewById(R.id.pairedDeviceList);
        btnSend = getActivity().findViewById(R.id.btSend);
        sendMessage = getActivity().findViewById(R.id.messageText);
        incomingMsgTextView = getActivity().findViewById(R.id.incomingText);
        deviceSearchStatus = getActivity().findViewById(R.id.deviceSearchStatus);
        pairedDeviceText = getActivity().findViewById(R.id.pairedDeviceText);
        incomingMsg = new StringBuilder();
        myBTDevice = null;

        // Register Broadcast Receiver for Bluetooth Connection
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(btConnectionReceiver, new IntentFilter("btConnectionStatus"));

        // Register Broadcast Receiver for incoming message
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(myReceiver, new IntentFilter("IncomingMsg"));

        // Register broadcast when bond state changed (E.g. PAIRING)
        IntentFilter bondFilter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        getActivity().registerReceiver(bondingBroadcastReceiver, bondFilter);

        // Register Discoverability Broadcast Receiver
        IntentFilter intentFilter = new IntentFilter(myBluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        getActivity().registerReceiver(discoverabilityBroadcastReceiver, intentFilter);

        // Register Discovered Device Broadcast Receiver
        IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        getActivity().registerReceiver(discoveryBroadcastReceiver, discoverDevicesIntent);

        // Register End Discovering Broadcast Receiver
        IntentFilter discoverEndedIntent = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        getActivity().registerReceiver(discoveryEndedBroadcastReceiver, discoverEndedIntent);

        // Register Enable/Disable Bluetooth Broadcast Receiver
        IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        getActivity().registerReceiver(enableBTBroadcastReceiver, BTIntent);


        // Register Start Discovering Broadcast Receiver
        IntentFilter discoverStartedIntent = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        getActivity().registerReceiver(discoveryStartedBroadcastReceiver, discoverStartedIntent);


        myBTDevicesArrayList = new ArrayList<>();
        myBTPairedDevicesArrayList = new ArrayList<>();
        myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();


        // onClick Listener for Paired Device List
        lvPairedDevices.setOnItemClickListener(
                new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                        // Cancel device search discovery
                        myBluetoothAdapter.cancelDiscovery();

                        myBTDevice = myBTPairedDevicesArrayList.get(i);

                        //Deselect Search Device List
                        lvNewDevices.setAdapter(myDeviceListAdapter);

                        Log.d(TAG, "onItemClick: Paired Device = " + myBTPairedDevicesArrayList.get(i).getName());
                        Log.d(TAG, "onItemClick: DeviceAddress = " + myBTPairedDevicesArrayList.get(i).getAddress());

                    }
                }
        );

        // onClick Listener for Search Device List
        lvNewDevices.setOnItemClickListener(
                new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                        // Cancel Device Search Discovery
                        myBluetoothAdapter.cancelDiscovery();

                        Log.d(TAG, "onItemClick: Item Selected");

                        String deviceName = myBTDevicesArrayList.get(i).getName();
                        String deviceAddress = myBTDevicesArrayList.get(i).getAddress();

                        // Deselect Paired Device List
                        lvPairedDevices.setAdapter(myPairedDeviceListAdapter);


                        Log.d(TAG, "onItemClick: DeviceName = " + deviceName);
                        Log.d(TAG, "onItemClick: DeviceAddress = " + deviceAddress);

                        // Create bond if > JELLYBEAN
                        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
                            Log.d(TAG, "Trying to pair with: " + deviceName);

                            // Create bond with selected device
                            myBTDevicesArrayList.get(i).createBond();

                            // Assign selected device info to myBTDevice
                            myBTDevice = myBTDevicesArrayList.get(i);



                        }

                    }
                }
        );

        // OnClick Listener for Search button
        btnSearch.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Log.d(TAG, "onClick: search button");
                enableBT();
                myBTDevicesArrayList.clear();


            }
        });

        // OnClick Listener for Connect button
        bluetoothConnect.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {

                if (myBTDevice == null) {

                    Toast.makeText(getContext(), "No Paired Device! Please Search/Select a Device.",
                            Toast.LENGTH_LONG).show();
                } else if(myBluetoothAdapter.getProfileConnectionState(BluetoothHeadset.HEADSET) == BluetoothHeadset.STATE_CONNECTED){
                    Toast.makeText(getContext(), "Bluetooth Already Connected",
                            Toast.LENGTH_LONG).show();
                }

                else{
                    Log.d(TAG, "onClick: connect button");

                    // Start connection with the bonded device
                    startBTConnection(myBTDevice, myUUID);
                }
                lvPairedDevices.setAdapter(myPairedDeviceListAdapter);
            }
        });

        // OnClick Listener for Send button
        btnSend.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                byte[] bytes = sendMessage.getText().toString().getBytes(Charset.defaultCharset());
                BluetoothChat.writeMsg(bytes);
                sendMessage.setText("");
            }
        });



    }

    // Unregister receivers
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


    // Broadcast Receiver for Bluetooth Connection Status
    BroadcastReceiver btConnectionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Log.d(TAG, "Receiving btConnectionStatus Msg!!!");

            String connectionStatus = intent.getStringExtra("ConnectionStatus");
            myBTConnectionDevice = intent.getParcelableExtra("Device");

            // Disconnected from Bluetooth
            if(connectionStatus.equals("disconnect")){

                Log.d("ConnectAcitvity:","Device Disconnected");

                // Check for not null
                if(connectIntent != null) {
                    //Stop Bluetooth Connection Service
                    getActivity().stopService(connectIntent);
                }

                // Reconnect Bluetooth
                AlertDialog alertDialog = new AlertDialog.Builder(getContext()).create();
                alertDialog.setTitle("BLUETOOTH DISCONNECTED");
                alertDialog.setMessage("Connection with device has ended. Do you want to reconnect?");
                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Yes",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                startBTConnection(myBTConnectionDevice, myUUID);

                            }
                        });
                alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "No",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
              //  alertDialog.show();
            }

            // Connected to Bluetooth Device
            else if(connectionStatus.equals("connect")){


                Log.d("ConnectAcitvity:","Device Connected");
                Toast.makeText(getContext(), "Connection Established: "+ myBTConnectionDevice.getName(),
                        Toast.LENGTH_SHORT).show();
            }

            // Bluetooth Connection failed
            else if(connectionStatus.equals("connectionFail")) {
                Toast.makeText(getContext(), "Connection Failed: "+ myBTConnectionDevice.getName(),
                        Toast.LENGTH_SHORT).show();
            }

        }
    };

    // Create a BroadcastReceiver for ACTION_FOUND (EnableBT)
    private final BroadcastReceiver enableBTBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(myBluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, myBluetoothAdapter.ERROR);

                switch (state) {
                    // Bluetooth STATE OFF
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG, "OnReceiver: STATE OFF");
                        break;
                    // Bluetooth STATE TURNING OFF
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG, "OnReceiver: STATE TURNING OFF");
                        break;
                    // Bluetooth STATE ON
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "OnReceiver: STATE ON");

                        // Turn on discoverability
                        discoverabilityON();

                        break;

                    // Bluetooth STATE TURNING ON
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG, "OnReceiver: STATE TURNING ON");
                        break;
                }
            }
        }
    };



    // Create a BroadcastReceiver for ACTION_FOUND (Enable Discoverability)
    private final BroadcastReceiver discoverabilityBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)) {
                int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR);

                switch (mode) {
                    // Device is on discoverable mode
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                        Log.d(TAG, "OnReceiver: DISCOVERABILITY ENABLED");

                        // Discover devices
                        startSearch();

                        // Start BluetoothConnectionService to listen for connection
                        connectIntent = new Intent(getContext(), BluetoothConnectionService.class);
                        connectIntent.putExtra("serviceType", "listen");
                        getActivity().startService(connectIntent);

                        // Check Paired Devices list
                        checkPairedDevice();



                        break;
                    // Device not on discoverable mode
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                        Log.d(TAG, "OnReceiver: DISCOVERABILITY DISABLED, ABLE TO RECEIVE CONNECTION");
                        break;
                    // Device not in valid scan mode
                    case BluetoothAdapter.SCAN_MODE_NONE:
                        Log.d(TAG, "OnReceiver: DISCOVERABILITY DISABLED, NOT ABLE TO RECEIVE CONNECTION");
                        break;
                    // Bluetooth STATE CONNECTING
                    case BluetoothAdapter.STATE_CONNECTING:
                        Log.d(TAG, "OnReceiver: CONNECTING");
                        break;
                    // Bluetooth State CONNECTED
                    case BluetoothAdapter.STATE_CONNECTED:
                        Log.d(TAG, "OnReceiver: CONNECTED");
                        break;
                }
            }
        }
    };



    // Create a BroadcastReceiver for ACTION_FOUND (Get Discovered Devices Info)
    private final BroadcastReceiver discoveryBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "SEARCH ME!");

            if (action.equals(BluetoothDevice.ACTION_FOUND)) {

                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                myBTDevicesArrayList.add(device);
                Log.d(TAG, "OnReceive: " + device.getName() + ": " + device.getAddress());
                myDeviceListAdapter = new DeviceListAdapter(context, R.layout.device_adapter_view, myBTDevicesArrayList);
                lvNewDevices.setAdapter(myDeviceListAdapter);

            }
        }
    };


    // Create a BroadcastReceiver for ACTION_DISCOVERY_STARTED  (Start Discovering Devices)
    private final BroadcastReceiver discoveryStartedBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_STARTED)) {

                Log.d(TAG, "STARTED DISCOVERY!!!");

                deviceSearchStatus.setText(R.string.searchDevice);

            }
        }
    };


    // Create a BroadcastReceiver for ACTION_DISCOVERY_FINISHED  (End Discovering Devices)
    private final BroadcastReceiver discoveryEndedBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {

                Log.d(TAG, "ENDED DISCOVERY!!!");

                deviceSearchStatus.setText(R.string.searchDone);

            }
        }
    };


    // Create a BroadcastReceiver for ACTION_FOUND (Pairing Devices)
    private final BroadcastReceiver bondingBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {

                // Bonding device
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                // Device already bonded
                if (device.getBondState() == BluetoothDevice.BOND_BONDED) {

                    Log.d(TAG, "BoundReceiver: Bond Bonded with: " + device.getName());

                    myProgressDialog.dismiss();

                    Toast.makeText(getContext(), "Bound Successfully With: " + device.getName(),
                            Toast.LENGTH_LONG).show();
                    myBTDevice = device;
                    checkPairedDevice();
                    lvNewDevices.setAdapter(myDeviceListAdapter);

                }
                // Device is bonded with another device
                if (device.getBondState() == BluetoothDevice.BOND_BONDING) {
                    Log.d(TAG, "BoundReceiver: Bonding With Another Device");

                    myProgressDialog = ProgressDialog.show(getContext(), "Bonding With Device", "Please Wait...", true);


                }
                // Break bond
                if (device.getBondState() == BluetoothDevice.BOND_NONE) {
                    Log.d(TAG, "BoundReceiver: Breaking Bond");

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

                    // Reset variable
                    myBTDevice = null;
                }

            }
        }
    };


    // Broadcast Receiver for incoming message
    BroadcastReceiver myReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Log.d(TAG, "Receiving Message!");

            String msg = intent.getStringExtra("receivingMsg");
            incomingMsg.append(msg + "\n");
            incomingMsgTextView.setText(incomingMsg);

        }
    };

    // Turn On Discoverability
    private void discoverabilityON() {

        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 900);
        startActivity(discoverableIntent);


    }


    // Enable Bluetooth
    public void enableBT() {
        // Device does not have Bluetooth
        if (myBluetoothAdapter == null) {
            Toast.makeText(getContext(), "Device Does Not Support Bluetooth.",
                    Toast.LENGTH_LONG).show();
            Log.d(TAG, "enableDisableBT: Does not have BT capabilities.");
        }
        // Device's Bluetooth is disabled
        if (!myBluetoothAdapter.isEnabled()) {
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBTIntent);


        }
        // Device's Bluetooth is enabled
        if (myBluetoothAdapter.isEnabled()) {
            discoverabilityON();
        }

    }


    //Check BT permission in manifest (For Start Discovery)
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



    // Start Discovering Other Devices
    private void startSearch() {
        Log.d(TAG, "btnDiscover: Looking for unpaired devices.");

        // Discover devices
        if (myBluetoothAdapter.isDiscovering()) {
            myBluetoothAdapter.cancelDiscovery();
            Log.d(TAG, "BTDiscovery: canceling discovery");

            // Check Bluetooth Permission in Manifest
            checkBTPermission();

            myBluetoothAdapter.startDiscovery();
            Log.d(TAG, "BTDiscovery: enable discovery");

        }
        if (!myBluetoothAdapter.isDiscovering()) {

            // Check Bluetooth Permission in Manifest
            checkBTPermission();

            myBluetoothAdapter.startDiscovery();
            Log.d(TAG, "BTDiscovery: enable discovery");


        }
    }


    // Start BluetoothChat Service Method
    public void startBTConnection(BluetoothDevice device, UUID uuid) {

        Log.d(TAG, "StartBTConnection: Initializing RFCOM Bluetooth Connection");

        connectIntent = new Intent(getContext(), BluetoothConnectionService.class);
        connectIntent.putExtra("serviceType", "connect");
        connectIntent.putExtra("device", device);
        connectIntent.putExtra("id", uuid);

        Log.d(TAG, "StartBTConnection: Starting Bluetooth Connection Service!");

        getActivity().startService(connectIntent);
    }


    // Check for paired devices
    public void checkPairedDevice() {

        Set<BluetoothDevice> pairedDevices = myBluetoothAdapter.getBondedDevices();
        myBTPairedDevicesArrayList.clear();

        if (pairedDevices.size() > 0) {

            for (BluetoothDevice device : pairedDevices) {
                Log.d(TAG, "PAIRED DEVICES: " + device.getName() + "," + device.getAddress());
                myBTPairedDevicesArrayList.add(device);

            }
            pairedDeviceText.setText("Paired Devices: ");
            myPairedDeviceListAdapter = new DeviceListAdapter(getContext(), R.layout.device_adapter_view, myBTPairedDevicesArrayList);
            lvPairedDevices.setAdapter(myPairedDeviceListAdapter);

        } else {
            pairedDeviceText.setText("No Paired Devices: ");

            Log.d(TAG, "NO PAIRED DEVICE!!");
        }
    }

}





