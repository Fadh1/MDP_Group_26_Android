package com.example.mdp26;

import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.UUID;

import static java.lang.Math.abs;

public class HomeFragment extends Fragment implements SensorEventListener{
    private static final String TAG = "HomeFragment";


    // Declarations for main screen with map
    PixelGridView mPGV;
    ImageButton forwardButton, leftRotateButton, rightRotateButton, reverseButton;
    Button btn_update, btn_sendToAlgo, btn_calibrate;
    TextView tv_status, tv_map_exploration, tv_mystatus, tv_mystringcmd;
    ToggleButton tb_setWaypointCoord, tb_setStartCoord, tb_autoManual, tb_fastestpath, tb_exploration,tiltbtn;
    ArrayList<String> commandBuffer = new ArrayList<String>();

    // Declarations for Tilt Control
    private SensorManager sensorManager;
    private Sensor sensor;
    boolean tiltNavi;

    // Declarations for Bluetooth Connection
    private static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    BluetoothDevice myBTConnectionDevice;
    static String connectedDevice;
    boolean connectedState;
    boolean currentActivity;



    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home,container, false);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)

    {
        super.onViewCreated(view, savedInstanceState);

        connectedDevice = null;
        connectedState = false;
        currentActivity = true;

        // Register Broadcast Receiver for incoming bluetooth connection
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(btConnectionReceiver, new IntentFilter("btConnectionStatus"));

        // Register Broadcast Receiver for incoming bluetooth message
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(incomingMessageReceiver, new IntentFilter("IncomingMsg"));

        tv_status = (TextView) getActivity().findViewById(R.id.tv_status);
        tv_map_exploration = (TextView) getActivity().findViewById(R.id.tv_map_exploration);

        tv_mystatus = (TextView) getActivity().findViewById(R.id.tv_mystatus);
        tv_mystatus.setText("Stop\n");
        tv_mystatus.setMovementMethod(new ScrollingMovementMethod());

        tv_mystringcmd = (TextView) getActivity().findViewById(R.id.tv_mystringcmd);
        tv_mystringcmd.setMovementMethod(new ScrollingMovementMethod());

        mPGV = getActivity().findViewById(R.id.map);
        mPGV.initializeMap();


        // Manual Mode; Update button
        btn_update = (Button) getActivity().findViewById(R.id.btn_update);
        btn_update.setEnabled(false);
        btn_update.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Updating....");
                mPGV.refreshMap(true);
                Log.d(TAG, "Update completed!");
                Toast.makeText(getContext(), "Update completed", Toast.LENGTH_SHORT).show();
            }
        });

        // Forward button
        forwardButton = (ImageButton) getActivity().findViewById(R.id.fwd_btn);
        forwardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // Check BT connection If not connected to any bluetooth device
                if (connectedDevice == null) {
                    Toast.makeText(getContext(), "Please connect to bluetooth device first!", Toast.LENGTH_SHORT).show();
                } else {

                    // If already connected to a bluetooth device
                    // Outgoing message to Arduino to move forward
                    String navigate = "And|Ard|w|";
                    byte[] bytes = navigate.getBytes(Charset.defaultCharset());
                    BluetoothChat.writeMsg(bytes);
                    Log.d(TAG, "Android Controller: Move Forward sent");
                    tv_mystatus.append("Moving\n");
                    tv_mystringcmd.append("Android Controller: Move Forward\n");
                    mPGV.moveForward();
                }
            }
        });

        // Turn Left button
        leftRotateButton = (ImageButton) getActivity().findViewById(R.id.left_btn);
        leftRotateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // Check BT connectionIf not connected to any bluetooth device
                if (connectedDevice == null) {
                    Toast.makeText(getContext(), "Please connect to bluetooth device first!", Toast.LENGTH_SHORT).show();
                } else {

                    // If already connected to a bluetooth device
                    // Outgoing message to Arduino to turn left
                    String navigate = "And|Ard|a|";
                    byte[] bytes = navigate.getBytes(Charset.defaultCharset());
                    BluetoothChat.writeMsg(bytes);
                    Log.d(TAG, "Android Controller: Turn Left sent");
                    tv_mystatus.append("Moving\n");
                    tv_mystringcmd.append("Android Controller: Turn Left\n");
                    mPGV.rotateLeft();
                }
            }
        });

        // Turn right button
        rightRotateButton = (ImageButton) getActivity().findViewById(R.id.right_btn);
        rightRotateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // Check BT connectionIf not connected to any bluetooth device
                if (connectedDevice == null) {
                    Toast.makeText(getContext(), "Please connect to bluetooth device first!", Toast.LENGTH_SHORT).show();
                } else {

                    // If already connected to a bluetooth device
                    // Outgoing message to Arduino to turn right
                    String navigate = "And|Ard|d|";
                    byte[] bytes = navigate.getBytes(Charset.defaultCharset());
                    BluetoothChat.writeMsg(bytes);
                    Log.d(TAG, "Android Controller: Turn Right sent");
                    tv_mystatus.append("Moving\n");
                    tv_mystringcmd.append("Android Controller: Turn Right\n");
                    mPGV.rotateRight();
                }
            }
        });


        // Reverse button
        reverseButton = (ImageButton) getActivity().findViewById(R.id.rev_btn);
        reverseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Check BT connectionIf not connected to any bluetooth device
                if (connectedDevice == null) {
                    Toast.makeText(getContext(), "Please connect to bluetooth device first!", Toast.LENGTH_SHORT).show();
                } else {

                    // If already connected to a bluetooth device
                    // Outgoing message to Arduino to move backwards
                    String navigate = "And|Ard|s|";
                    byte[] bytes = navigate.getBytes(Charset.defaultCharset());
                    BluetoothChat.writeMsg(bytes);
                    Log.d(TAG, "Android Controller: Move Backwards sent");
                    tv_mystatus.append("Moving\n");
                    tv_mystringcmd.append("Android Controller: Rotate 180 \n");
                    mPGV.moveBackwards();
                }
            }
        });

        // Select Waypoint button
        tb_setWaypointCoord = (ToggleButton) getActivity().findViewById(R.id.tb_setWaypointCoord);
        tb_setWaypointCoord.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                // Check BT connectionIf not connected to any bluetooth device
                if (connectedDevice == null) {
                    Toast.makeText(getContext(), "Please connect to bluetooth device first!", Toast.LENGTH_SHORT).show();
                } else {

                    if (isChecked) {
                        // The toggle is enabled : To select waypoint on map
                        mPGV.selectWayPoint();
                        tb_setWaypointCoord.toggle();
                    }
                }
            }
        });

        // Select Start Point button
        tb_setStartCoord = (ToggleButton) getActivity().findViewById(R.id.tb_setStartCoord);
        tb_setStartCoord.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                // Check BT connectionIf not connected to any bluetooth device
                if (connectedDevice == null) {
                    Toast.makeText(getContext(), "Please connect to bluetooth device first!", Toast.LENGTH_SHORT).show();
                } else {
                    if (isChecked) {
                        // The toggle is enabled: To select start point on map
                        mPGV.selectStartPoint();
                        setStartDirection();
                        tb_setStartCoord.toggle();
                    }
                }
            }
        });


        // To send start coordinates and waypoint coordinates to Algorithm
        btn_sendToAlgo = (Button) getActivity().findViewById(R.id.btn_sendToAlgo);
        btn_sendToAlgo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Check BT connectionIf not connected to any bluetooth device
                if (connectedDevice == null) {
                    Toast.makeText(getContext(), "Please connect to bluetooth device first!", Toast.LENGTH_SHORT).show();
                } else {

                    //If already connected to a bluetooth device
                    // Send both coordinates to Algorithm as one string
                    int convertDirection = mPGV.getRobotDirection();
                    String sendAlgoCoord = "And|Alg|10|".concat(Integer.toString(mPGV.getStartCoord()[0])).concat(",").concat(Integer.toString(mPGV.getStartCoord()[1])).concat(",").concat(Integer.toString(convertDirection)).concat(",").concat(Integer.toString(mPGV.getWayPoint()[0])).concat(",").concat(Integer.toString(mPGV.getWayPoint()[1]));
                    byte[] bytes = sendAlgoCoord.getBytes(Charset.defaultCharset());
                    BluetoothChat.writeMsg(bytes);
                    Log.d(TAG, "Sent Start and Waypoint Coordinates to Algo");
                    Toast.makeText(getContext(), "Start & Waypoint coordinates sent", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // To send permission to Calibrate Robot to Algorithm
        btn_calibrate = (Button) getActivity().findViewById(R.id.btn_calibrateRobot);
        btn_calibrate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Check BT connection If not connected to any bluetooth device
                if (connectedDevice == null) {
                    Toast.makeText(getContext(), "Please connect to bluetooth device first!", Toast.LENGTH_SHORT).show();
                } else {

                    // If already connected to a bluetooth device
                    // Outgoing message to Algorithm to calibrate robot
                    String navigate = "And|Alg|C|";
                    byte[] bytes = navigate.getBytes(Charset.defaultCharset());
                    BluetoothChat.writeMsg(bytes);
                    Log.d(TAG, "Android Controller: Calibrate sent");

                    tv_mystatus.append("Calibrating robot...\n");
                    tv_mystringcmd.append("Calibrating robot...\n");
                }
            }
        });

        // Auto / Manual mode button
        tb_autoManual = (ToggleButton) getActivity().findViewById(R.id.tb_autoManual);
        tb_autoManual.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (connectedDevice == null) {
                    Toast.makeText(getContext(), "Please connect to bluetooth device first!", Toast.LENGTH_SHORT).show();
                } else {
                    if (isChecked) {
                        // The toggle is enabled; Manual Mode

                        // Direction buttons are disabled
                        // Update button is enabled
                        btn_update.setEnabled(true);
                        forwardButton.setEnabled(false);
                        leftRotateButton.setEnabled(false);
                        rightRotateButton.setEnabled(false);
                        reverseButton.setEnabled(false);
                        Toast.makeText(getContext(), "Manual Mode enabled", Toast.LENGTH_SHORT).show();
//                        updateMap = false;
                        mPGV.setAutoUpdate(false);
                        Log.d(TAG, "Auto updates disabled.");

                    } else {
                        // The toggle is disabled; Auto Mode

                        // Update button is disabled
                        // Direction buttons are enabled
                        mPGV.refreshMap(true);
                        btn_update.setEnabled(false);
                        forwardButton.setEnabled(true);
                        leftRotateButton.setEnabled(true);
                        rightRotateButton.setEnabled(true);
                        reverseButton.setEnabled(true);
                        Toast.makeText(getContext(), "Auto Mode enabled", Toast.LENGTH_SHORT).show();
                        mPGV.setAutoUpdate(true);
                        Log.d(TAG, "Auto updates enabled.");
                    }
                }
            }
        });

        // Start Exploration button
        tb_exploration = (ToggleButton) getActivity().findViewById(R.id.tb_exploration);
        tb_exploration.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (connectedDevice == null) {
                    Toast.makeText(getContext(), "Please connect to bluetooth device first!", Toast.LENGTH_SHORT).show();
                } else {
                    if (isChecked) {
                        // The toggle is enabled; Start Exploration Mode
                        startExploration();
                    }
                }
            }
        });

        // TILT CONTROL
        tiltNavi = false;
        // Declaring Sensor Manager and sensor type
        sensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        tiltbtn = (ToggleButton) getActivity().findViewById(R.id.tiltbtn);
        tiltbtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    tiltNavi = true;
                    Log.d(TAG, "Tilt Switch On!");

                } else {
                    tiltNavi = false;
                    Log.d(TAG, "Tilt Switch Off!");
                }
            }
        });

        // Start Fastest Path button
        tb_fastestpath = (ToggleButton) getActivity().findViewById(R.id.tb_fastestpath);
        tb_fastestpath.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                // Check BT connectionIf not connected to any bluetooth device
                if (connectedDevice == null) {
                    Toast.makeText(getContext(), "Please connect to bluetooth device first!", Toast.LENGTH_SHORT).show();
                } else {
                    if (isChecked) {
                        // The toggle is enabled; Start Fastest Path Mode
                        startFastestPath();
                    }
                }
            }
        });




    }

    // Broadcast Receiver for incoming messages
    BroadcastReceiver incomingMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String allMsg = intent.getStringExtra("receivingMsg");

            Log.d(TAG, "Receiving incoming message: " + allMsg);

            tv_mystringcmd.append(allMsg + "\n");

            // Add incoming commands into a buffer to process
            commandBuffer.add(allMsg);

           // String incomingMsg = "Alg|And|rp|0|0|north";
            //String incomingMsg2= "Alg|And|rp|0|0|north";
            while(!commandBuffer.isEmpty()){

                 String incomingMsg = commandBuffer.remove(0);
                 /*if(!commandBuffer.isEmpty()) {
                     incomingMsg2 = commandBuffer.get(0);
                 }*/
                // Filter empty and concatenated string from receiving channel
                if (incomingMsg.length() > 8 && incomingMsg.length() < 345) {
                    // Check if string is for android
                    try {
                        if (incomingMsg.substring(4, 7).equals("And")) {
                            String[] filteredMsg = msgDelimiter(incomingMsg.replaceAll("\\,", "\\|").trim(), "\\|");
                            //String[] filteredMsg2 = msgDelimiter(incomingMsg2.replaceAll("\\,", "\\|").trim(), "\\|");
                            Log.d(TAG, "Incoming Message filtered: " + filteredMsg[2]);

                            // String commands for Android
                            switch (filteredMsg[2]) {

                                // Command: FORWARD
                                case "fwd":
                                    for (int counter = Integer.parseInt(filteredMsg[3]); counter >= 1; counter--) {
                                        mPGV.moveForward();
                                        tv_mystringcmd.append("Move Forward\n");
                                        tv_mystatus.append("Moving\n");
                                    }
                                    break;
                                // Command: TURN LEFT
                                case "left":

                                    for (int counter = Integer.parseInt(filteredMsg[3]); counter >= 1; counter--) {
                                        mPGV.rotateLeft();
                                        tv_mystringcmd.append("Turn Left\n");
                                        tv_mystatus.append("Moving\n");
                                    }
                                    break;


                                // Command: TURN RIGHT
                                case "right":
                                    for (int counter = Integer.parseInt(filteredMsg[3]); counter >= 1; counter--) {
                                        mPGV.rotateRight();
                                        tv_mystringcmd.append("Turn Right\n");
                                        tv_mystatus.append("Moving\n");
                                    }
                                    break;


                                // Command: MOVE BACKWARDS
                                case "back":
                                    for (int counter = Integer.parseInt(filteredMsg[3]); counter >= 1; counter--) {
                                        mPGV.moveBackwards();
                                        tv_mystringcmd.append("Move Backwards\n");
                                        tv_mystatus.append("Moving\n");
                                    }
                                    break;


                                // Command: CALIBRATE : ALIGN_FRONT
                                case "4":
                                case "ALIGN_FRONT":
                                    tv_mystatus.append("Calibrating robot...\n");
                                    tv_mystringcmd.append("Calibrating robot...\n");
                                    break;

                                // Command: CALIBRATE : ALIGN_RIGHT
                                case "5":
                                    tv_mystatus.append("Calibrating robot...\n");
                                    tv_mystringcmd.append("Calibrating robot...\n");
                                    break;

                                // Command: END EXPLORATION
                                case "8":
                                case "ENDEXP":
                                    endExploration();
                                    break;


                                // Command: END FASTEST PATH
                                case "9":
                                case "ENDFAST":
                                    endFastestPath();
                                    break;

                                // Command: ARROW DETECTED
                                case "img":
                                    mPGV.setArrowImageCoord(filteredMsg[3], filteredMsg[4], filteredMsg[5]);
                                    break;

                                // Command: Arrow String from Algo
                                case "done":
                                    Log.d(TAG, "Arrow String Command for Algo");
                                    break;

                                // Command: Part 1 of MAP Descriptor
                                case "md1":
                                    String mapDes1 = filteredMsg[3];
                                    mPGV.mapDescriptorExplored(mapDes1);
                                    break;


                                // Command: Part 2 of Map Descriptor
                                case "md2":
                                    String mapDes2 = filteredMsg[3];
                                    mPGV.mapDescriptorObstacle(mapDes2);
                                    break;


                                // Command: Robot has stopped moving
                                case "stop":
                                    tv_mystatus.append("Stop\n");
                                    tv_mystringcmd.append(" \n");
                                    break;

                                    // For robot position in exploration stage
                                case "rp":
                                    int robotPosCol = Integer.parseInt(filteredMsg[4]);
                                    int robotPosRow = Integer.parseInt(filteredMsg[3]);
                                    //int robotPosDeg = Integer.parseInt(filteredMsg[3]);

                                    int robotPosDir = 0;
                                    Log.d(TAG, "filteredMsg[5]: " + filteredMsg[5]);
                                         // Up
                                    if (filteredMsg[5].equals("north"))
                                        robotPosDir = 0;
                                        //Right
                                    else if (filteredMsg[5].equals("east"))
                                        robotPosDir = 3;
                                        //Down
                                    else if (filteredMsg[5].equals("south"))
                                        robotPosDir = 2;
                                        // Left
                                    else if (filteredMsg[5].equals("west"))
                                        robotPosDir = 1;
                                    // For setting robot start position from AMD
                                    Log.d(TAG, "RobotPosDir " + robotPosDir);
                                    mPGV.setCurPos(robotPosRow, robotPosCol);
                                    mPGV.setRobotDirection(robotPosDir);
                                    break;

                                    //For robot position in Fastest Path Stage
                                case "fp":
                                    int robotPosDir2 = 0;
                                    Log.d(TAG, "filteredMsg[4]: " + filteredMsg[4]);
                                    // Up
                                    if (filteredMsg[4].equals("north"))
                                        robotPosDir2 = 0;
                                        //Right
                                    else if (filteredMsg[4].equals("east"))
                                        robotPosDir2 = 3;
                                        //Down
                                    else if (filteredMsg[4].equals("south"))
                                        robotPosDir2 = 2;
                                        // Left
                                    else if (filteredMsg[4].equals("west"))
                                        robotPosDir2 = 1;
                                    // For setting robot start position from AMD
                                    mPGV.setRobotDirection(robotPosDir2);
                                    int increment = Integer.parseInt(filteredMsg[3]);
                                    for(int i =0; i<increment; i++){
                                        mPGV.moveForward();
                                    }
                                    break;

                                    // Default case; string not recognised
                                default:
                                    Log.d(TAG, "String command not recognised.");
                                    break;
                            }


                            // To handle concatenation with the previous string command
                            if (filteredMsg.length >= 5) {

                                // If the concatenated string command is for Android
                                if (filteredMsg[5].equals("and")) {

                                    Log.d(TAG, "Incoming Message 2 filtered: " + filteredMsg[6]);

                                    // Command for Android
                                    switch (filteredMsg[6]) {

                                        // Command: FORWARD
                                        case "fwd":
                                            for (int counter = Integer.parseInt(filteredMsg[7]); counter >= 1; counter--) {
                                                mPGV.moveForward();
                                                tv_mystringcmd.append("Move Forward\n");
                                                tv_mystatus.append("Moving\n");
                                            }
                                            break;


                                        // Command: TURN LEFT
                                        case "left":
                                            for (int counter = Integer.parseInt(filteredMsg[7]); counter >= 1; counter--) {
                                                mPGV.rotateLeft();
                                                tv_mystringcmd.append("Turn Left\n");
                                                tv_mystatus.append("Moving\n");
                                            }
                                            break;


                                        // Command: TURN RIGHT
                                        case "right":
                                            for (int counter = Integer.parseInt(filteredMsg[7]); counter >= 1; counter--) {
                                                mPGV.rotateRight();
                                                tv_mystringcmd.append("Turn Right\n");
                                                tv_mystatus.append("Moving\n");
                                            }
                                            break;


                                        // Command: BACKWARDS
                                        case "back":
                                            for (int counter = Integer.parseInt(filteredMsg[7]); counter >= 1; counter--) {
                                                mPGV.moveBackwards();
                                                tv_mystringcmd.append("Move Backwards\n");
                                                tv_mystatus.append("Moving\n");
                                            }
                                            break;


                                        // Command: CALIBRATE : ALIGN_FRONT
                                        case "4":
                                        case "ALIGN_FRONT":
                                            tv_mystatus.append("Calibrating robot...\n");
                                            tv_mystringcmd.append("Calibrating robot...\n");
                                            break;

                                        // Command: CALIBRATE : ALIGN_RIGHT
                                        case "5":
                                            tv_mystatus.append("Calibrating robot...\n");
                                            tv_mystringcmd.append("Calibrating robot...\n");
                                            break;

                                        // Command: END EXPLORATION
                                        case "8":
                                        case "ENDEXP":
                                            endExploration();
                                            break;


                                        // Command: END FASTEST PATH
                                        case "9":
                                        case "ENDFAST":
                                            endFastestPath();
                                            break;

                                        // Command: ARROW DETECTED
                                        case "image":
                                            mPGV.setArrowImageCoord(filteredMsg[7], filteredMsg[8], filteredMsg[9]);
                                            break;

                                        // Command: Arrow String from Algo
                                        case "done":
                                            Log.d(TAG, "Arrow String Command for Algo");
                                            break;

                                        // Command: Part 1 of Map Descriptor
                                        case "md1":
                                            String mapDes1 = filteredMsg[7];
                                            mPGV.mapDescriptorExplored(mapDes1);
                                            break;


                                        // Command: Part 2 of Map Descriptor
                                        case "md2":
                                            String mapDes2 = filteredMsg[7];
                                            mPGV.mapDescriptorObstacle(mapDes2);
                                            break;

                                        // Command: Robot has stopped moving
                                        case "stop":
                                            tv_mystatus.append("Stop\n");
                                            tv_mystringcmd.append(" \n");
                                            break;
                                        // For robot position in Exploration Stage
                                        case "rp":
                                            int robotPosCol = Integer.parseInt(filteredMsg[4]);
                                            int robotPosRow = Integer.parseInt(filteredMsg[3]);
                                            //int robotPosDeg = Integer.parseInt(filteredMsg[3]);

                                            int robotPosDir = 0;
                                            Log.d(TAG, "filteredMsg[5]: " + filteredMsg[5]);
                                            // Up
                                            if (filteredMsg[5].equals("north"))
                                                robotPosDir = 0;
                                                //Right
                                            else if (filteredMsg[5].equals("east"))
                                                robotPosDir = 3;
                                                //Down
                                            else if (filteredMsg[5].equals("south"))
                                                robotPosDir = 2;
                                                // Left
                                            else if (filteredMsg[5].equals("west"))
                                                robotPosDir = 1;
                                            // For setting robot start position from AMD
                                            mPGV.setCurPos(robotPosRow, robotPosCol);
                                            mPGV.setRobotDirection(robotPosDir);
                                            break;
                                            //For robot position in fastest path stage
                                        case "fp":
                                            int robotPosDir2 = 0;
                                            Log.d(TAG, "filteredMsg[4]: " + filteredMsg[4]);
                                            // Up
                                            if (filteredMsg[4].equals("north"))
                                                robotPosDir2 = 0;
                                                //Right
                                            else if (filteredMsg[4].equals("east"))
                                                robotPosDir2 = 3;
                                                //Down
                                            else if (filteredMsg[4].equals("south"))
                                                robotPosDir2 = 2;
                                                // Left
                                            else if (filteredMsg[4].equals("west"))
                                                robotPosDir2 = 1;
                                            // For setting robot start position from AMD
                                            //mPGV.setCurPos(robotPosRow, robotPosCol);
                                            mPGV.setRobotDirection(robotPosDir2);
                                            int increment = Integer.parseInt(filteredMsg[3]);
                                            for(int i =0; i<increment; i++){
                                                mPGV.moveForward();
                                            }
                                            break;
                                            // Default case: string not recognised
                                        default:
                                            Log.d(TAG, "String command not recognised.");
                                            break;
                                    }
                                }
                            }


                        }
                    }
                    catch(Exception e){
                        Log.d(TAG, "App Crash.");;
                    }
                }

                // The following is for clearing checklist commands only.

                // For receiving AMD robotPosition and grid
                /*if (incomingMsg.substring(0, 1).equals("{")) {
                    Log.d(TAG, "Incoming Message from AMD: " + incomingMsg);
                    String[] filteredMsg = msgDelimiter(incomingMsg.replaceAll(" ", "").replaceAll(",", "\\|").replaceAll("\\{", "").replaceAll("\\}", "").replaceAll("\\:", "\\|").replaceAll("\"", "").replaceAll("\\[", "").replaceAll("\\]", "").trim(), "\\|");
                    Log.d(TAG, "filteredMsg: " + filteredMsg);

                    // AMD Robot Position
                    if (filteredMsg[0].equals("robotposition")) {
                        int robotPosCol = Integer.parseInt(filteredMsg[2]) ;
                        int robotPosRow = Integer.parseInt(filteredMsg[1]) ;
                        //int robotPosDeg = Integer.parseInt(filteredMsg[3]);
                        int robotPosDir = 0;
                        Log.d(TAG, "filteredMsg[3]: " + filteredMsg[3]);
                        // Up
                        if (filteredMsg[3].equals("north"))
                            robotPosDir = 0;
                            //Right
                        else if (filteredMsg[3].equals("east"))
                            robotPosDir = 3;
                            //Down
                        else if (filteredMsg[3].equals("south"))
                            robotPosDir = 2;
                            // Left
                        else if (filteredMsg[3].equals("west"))
                            robotPosDir = 1;
                        // For setting robot start position from AMD
                        mPGV.setCurPos(robotPosRow, robotPosCol);
                        mPGV.setRobotDirection(robotPosDir);
                    }

                    // AMD Map Descriptor
                    else if (filteredMsg[0].equals("grid")) {
                        String mdAMD = filteredMsg[1];
                        mPGV.mapDescriptorChecklist(mdAMD);
                        mPGV.refreshMap(mPGV.getAutoUpdate());
                        Log.d(TAG, "mdAMD: " + mdAMD);

                        // For setting up map from received AMD MDF String, use mdAMD
                        Log.d(TAG, "Processing mdAMD...");
                    }
                }*/
            }

        }
    };


    // Broadcast Receiver for Bluetooth Connection Status
    BroadcastReceiver btConnectionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Log.d(TAG, "Receiving btConnectionStatus Msg!!!");

            String connectionStatus = intent.getStringExtra("ConnectionStatus");
            myBTConnectionDevice = intent.getParcelableExtra("Device");

            // Disconnected from Bluetooth
            if (connectionStatus.equals("disconnect")) {

                Log.d("MainActivity:", "Device Disconnected");
                connectedDevice = null;
                connectedState = false;

                if (currentActivity) {

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
                    //  alertDialog.show();

                }
            }
            // Connected to Bluetooth Device
            else if (connectionStatus.equals("connect")) {

                connectedDevice = myBTConnectionDevice.getName();
                connectedState = true;
                Log.d("MainActivity:", "Device Connected " + connectedState);
                Toast.makeText(getContext(), "Connection Established: " + myBTConnectionDevice.getName(),
                        Toast.LENGTH_SHORT).show();
            }

            // Bluetooth Connection failed
            else if (connectionStatus.equals("connectionFail")) {
                Toast.makeText(getContext(), "Connection Failed: " + myBTConnectionDevice.getName(),
                        Toast.LENGTH_SHORT).show();
            }

        }
    };



    // Setting Start Point Direction
    public void setStartDirection() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Select Robot Direction")
                .setItems(R.array.directions_array, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int i) {
                        mPGV.setRobotDirection(i);
                        dialog.dismiss();
                        Log.d(TAG, "Start Point Direction set");
                    }
                });
        builder.create();
        builder.create().show();
    }

    // Start Exploration
    public void startExploration() {
        Toast.makeText(getContext(), "Exploration started", Toast.LENGTH_SHORT).show();
        String startExp = "And|Alg|EX_START|";
        byte[] bytes = startExp.getBytes(Charset.defaultCharset());
        BluetoothChat.writeMsg(bytes);
        Log.d(TAG, "Android Controller: Start Exploration");
        tv_mystringcmd.append("Start Exploration\n");
        tv_mystatus.append("Moving\n");
    }

    // End Exploration
    public void endExploration() {
        Log.d(TAG, "Algorithm: End Exploration");
        tv_mystringcmd.append("End Exploration\n");
        tv_mystatus.append("Stop\n");
        Toast.makeText(getContext(), "Exploration ended", Toast.LENGTH_SHORT).show();
    }

    // Start Fastest Path
    public void startFastestPath() {
        Toast.makeText(getContext(), "Fastest Path started", Toast.LENGTH_SHORT).show();
        String startFP = "And|Alg|FP_START|";
        byte[] bytes = startFP.getBytes(Charset.defaultCharset());
        BluetoothChat.writeMsg(bytes);
        Log.d(TAG, "Android Controller: Start Fastest Path");
        tv_mystringcmd.append("Start Fastest Path\n");
        tv_mystatus.append("Moving\n");
    }

    // End Fastest Path
    public void endFastestPath() {
        Log.d(TAG, "Algorithm: Fastest Path Ended.");
        tv_mystringcmd.append("End Fastest Path\n");
        tv_mystatus.append("Stop\n");
        Toast.makeText(getContext(), "Fastest Path ended", Toast.LENGTH_SHORT).show();
    }


    // Delimiter for messages
    private String[] msgDelimiter(String message, String delimiter) {
        return (message.toLowerCase()).split(delimiter);
    }

    // For checklist only
    // EXTENSION BEYOND THE BASICS
    // Tilt Control

    // onClickListener for Tilt Button


    @Override
    public void onSensorChanged(SensorEvent event) {
        float x = event.values[0];
        float y = event.values[1];

        // Check if Tilt Control activated
        if (tiltNavi == true) {
            if (abs(x) > abs(y)) {

                // Right Tilt
                if (x < 0) {
                    Log.d("MainActivity:", "RIGHT TILT!!");
                    tv_mystatus.append("Moving\n");
                    tv_mystringcmd.append("Android Controller: Turn Right\n");
                    mPGV.rotateRight();
                }

                // Left Tilt
                if (x > 0) {
                    Log.d("MainActivity:", "LEFT TILT!!");
                    tv_mystatus.append("Moving\n");
                    tv_mystringcmd.append("Android Controller: Turn Left\n");
                    mPGV.rotateLeft();
                }
            }

            else {
                // Forward Tilt
                if (y < 0) {
                    Log.d("MainActivity:", "UP TILT!!");
                    tv_mystatus.append("Moving\n");
                    tv_mystringcmd.append("Android Controller: Move Forward\n");
                    mPGV.moveForward();
                }

                // Backward Tilt
                if (y > 0) {
                    Log.d("MainActivity:", "DOWN TILT!!");
                    tv_mystatus.append("Moving\n");
                    tv_mystringcmd.append("Android Controller: Move Backwards\n");
                    mPGV.moveBackwards();
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor arg0, int arg1) {
    }

    @Override
    public void onResume() {
        super.onResume();
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onPause() {
        super.onPause();
        //unregister Sensor listener
        sensorManager.unregisterListener(this);
    }


}
