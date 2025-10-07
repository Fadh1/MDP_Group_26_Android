# MDP Robot Maze Navigator - Android Controller

An Android application for controlling and visualizing a robot's navigation through a maze environment. Developed as part of the Multidisciplinary Project (MDP) course at Nanyang Technological University (2019).

## Overview

This application serves as the control interface for an autonomous maze-navigating robot. It provides real-time visualization of the robot's position, obstacle detection, and path planning through an intuitive touch-based interface. The app communicates with the robot via Bluetooth, enabling both manual control and autonomous navigation modes.

## Key Features

### Dual Control Modes
- **Manual Mode**: Direct control using on-screen directional buttons (forward, left, right, reverse)
- **Auto Mode**: Autonomous navigation with real-time map updates based on sensor feedback
- **Tilt Control**: Experimental motion-based control using device accelerometer

### Interactive Map Visualization
- 15x20 grid-based map representation
- Real-time obstacle detection and visualization
- Explored area tracking with color-coded cells
- Start point, waypoint, and goal position markers
- Arrow/image recognition display on detected obstacles
- Custom `PixelGridView` for efficient grid rendering

### Robust Bluetooth Communication
- Device discovery and pairing
- Persistent connection management with auto-reconnect
- Bidirectional message protocol for Android ↔ Algorithm ↔ Arduino communication
- Message buffering and parsing for concurrent command handling
- Connection status monitoring

### Path Planning Features
- Interactive waypoint selection via touch
- Exploration mode for initial maze mapping
- Fastest path computation mode
- Map descriptor encoding/decoding (hex to binary conversion)
- Robot position tracking with directional orientation

### Command Persistence
- Save custom command strings using SharedPreferences
- Quick-access function buttons (F1, F2) for frequently used commands
- Command management interface (save, reset, retrieve)

## Technology Stack

- **Language**: Java
- **Framework**: Android SDK (API 24-29)
- **UI Components**:
  - AndroidX AppCompat
  - Material Design Components
  - Fragment-based navigation with BottomNavigationView
  - Custom View for map rendering
- **Communication**:
  - Bluetooth Classic (RFCOMM)
  - LocalBroadcastManager for inter-component messaging
- **Sensors**: Accelerometer for tilt control
- **Build System**: Gradle
- **IDE**: Android Studio

## Project Structure

```
app/
├── src/main/java/com/example/mdp26/
│   ├── MainActivity.java              # Main activity with fragment management
│   ├── HomeFragment.java              # Main control interface and map view
│   ├── BluetoothFragment.java         # Bluetooth device management
│   ├── MessagesFragment.java          # Custom command storage/sending
│   ├── PixelGridView.java             # Custom view for map grid rendering
│   ├── BluetoothConnectionService.java # BT connection handling (AcceptThread, ConnectThread)
│   ├── BluetoothChat.java             # Message sending/receiving over BT
│   └── DeviceListAdapter.java         # BT device list adapter
├── src/main/res/
│   ├── layout/                        # XML layouts for activities and fragments
│   ├── drawable/                      # Arrow/obstacle marker images
│   ├── values/                        # Colors, strings, dimensions, styles
│   └── menu/                          # Bottom navigation menu
└── src/main/AndroidManifest.xml       # App permissions and components
```

## Installation and Setup

### Prerequisites
- Android Studio Arctic Fox (2020.3.1) or newer
- Android device or emulator running API 24 (Android 7.0) or higher
- Physical Android device with Bluetooth for actual robot communication

### Build Instructions

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd MDP_Group_26_Android
   ```

2. **Open in Android Studio**
   - Launch Android Studio
   - Select "Open an Existing Project"
   - Navigate to the cloned directory

3. **Sync Gradle**
   - Android Studio should automatically detect the Gradle configuration
   - If not, go to `File > Sync Project with Gradle Files`

4. **Build the project**
   ```bash
   ./gradlew build
   ```

5. **Run on device/emulator**
   - Connect an Android device via USB (with USB debugging enabled)
   - Or start an Android emulator
   - Click the "Run" button in Android Studio (or `Shift + F10`)

## Usage Guide

### Initial Setup

1. **Launch the app** and navigate to the "Bluetooth" tab
2. **Enable Bluetooth** by tapping "Search Devices"
3. **Grant location permissions** when prompted (required for BT discovery on Android)
4. **Select a device** from the paired or discovered devices list
5. **Connect** by tapping the "Connect" button

### Manual Robot Control

1. Navigate to the **Home** tab
2. Ensure you're in **Auto Mode** (toggle off if in Manual Mode)
3. Use the **directional buttons** to control robot movement:
   - Forward: Move one grid cell forward
   - Left/Right: Rotate 90° in place
   - Reverse: Rotate 180°
4. The map updates automatically with robot position and detected obstacles

### Autonomous Navigation

1. **Set Start Position**:
   - Tap "Set Start Point" toggle
   - Touch the map to place the robot's starting position
   - Select the initial direction from the dialog

2. **Set Waypoint**:
   - Tap "Set Waypoint" toggle
   - Touch the map to place the waypoint

3. **Send Coordinates**:
   - Tap "Send to Algorithm" to transmit start/waypoint to the planning algorithm

4. **Start Exploration**:
   - Toggle "Exploration" to begin autonomous maze exploration
   - The robot will navigate and map unexplored areas
   - Map updates in real-time based on sensor feedback

5. **Fastest Path**:
   - After exploration, toggle "Fastest Path"
   - Robot executes the optimized path to the goal

### Custom Commands

1. Navigate to the **Messages** tab
2. Enter custom command strings in F1 and F2 fields
3. Tap "Save" to persist commands
4. Use F1/F2 buttons to quickly send saved commands
5. Tap "Retrieve" to reload previously saved commands

## Communication Protocol

### Message Format
Messages follow a pipe-delimited format:
```
Source|Destination|Command|Parameters
```

**Examples:**
- `And|Ard|w|` - Android → Arduino: Move forward
- `Alg|And|rp|5|10|north` - Algorithm → Android: Robot position update
- `And|Alg|EX_START|` - Android → Algorithm: Start exploration
- `Alg|And|md1|<hex>` - Algorithm → Android: Map descriptor part 1

### Key Commands

| Command | Source → Dest | Description |
|---------|--------------|-------------|
| `w`, `a`, `d`, `s` | And → Ard | Move forward, left, right, backward |
| `fwd`, `left`, `right`, `back` | Alg → And | Movement with repetition count |
| `rp` | Alg → And | Robot position update (row, col, direction) |
| `md1`, `md2` | Alg → And | Map descriptor (explored cells, obstacles) |
| `img` | Alg → And | Image/arrow detected at coordinates |
| `EX_START`, `FP_START` | And → Alg | Start exploration/fastest path |
| `ENDEXP`, `ENDFAST` | Alg → And | End exploration/fastest path |
| `C`, `ALIGN_FRONT` | Various | Calibration commands |

## Technical Highlights

### Custom Map Rendering
The `PixelGridView` class extends Android's `View` to efficiently render a 15x20 grid with:
- Dynamic cell sizing based on screen dimensions
- Canvas-based drawing for performance
- Touch event handling for interactive position selection
- Real-time updates via `invalidate()` calls

### Map Descriptor Encoding
- **Part 1 (Explored Cells)**: 300-bit binary string (15x20 grid) encoded as hexadecimal
- **Part 2 (Obstacles)**: Variable-length binary string for obstacles in explored cells
- BigInteger conversion handles large binary strings without overflow
- Efficient bit manipulation for grid state representation

### Bluetooth Architecture
- **AcceptThread**: Server socket waiting for incoming connections
- **ConnectThread**: Client socket initiating connections
- **BluetoothChat**: Continuous read loop on a background thread
- LocalBroadcastManager decouples BT events from UI components
- Graceful handling of connection loss with auto-reconnect prompts

### Multi-Fragment Architecture
- Bottom navigation with three fragments (Home, Bluetooth, Messages)
- Fragment transaction management for smooth UI transitions
- Shared Bluetooth state across fragments via broadcast receivers
- Activity-scoped services maintain connections across fragment lifecycle

## Algorithms and Data Structures

### Path Finding
The robot uses sensor data to build a map representation:
- **Grid Representation**: 2D boolean arrays (`cellExplored[][]`, `obstacles[][]`)
- **Direction Mapping**: Integer encoding (0=North, 1=West, 2=South, 3=East)
- **Coordinate System**: Inverted Y-axis (row 0 at bottom, row 19 at top) for intuitive visualization

### Message Buffering
- ArrayList-based command queue in `HomeFragment`
- Sequential processing to prevent race conditions
- Delimiter-based parsing with fallback for malformed messages

## Permissions

The app requires the following permissions (declared in `AndroidManifest.xml`):
- `BLUETOOTH` - Send/receive data via Bluetooth
- `BLUETOOTH_ADMIN` - Discover devices and manage connections
- `ACCESS_FINE_LOCATION` - Required for Bluetooth discovery on Android 6.0+
- `ACCESS_COARSE_LOCATION` - Alternative location permission

## Known Limitations

- Bluetooth Classic only (not BLE) - requires hardware support
- Fixed grid size (15x20) - hardcoded in `PixelGridView`
- Single robot support - no multi-robot coordination
- No map persistence - state lost on app restart
- Tilt control is experimental and may be sensitive to movement

## Future Enhancements

Potential improvements for the project:
- [ ] Persistent map storage using Room database or file I/O
- [ ] Bluetooth Low Energy (BLE) support for broader device compatibility
- [ ] Configurable grid dimensions via settings
- [ ] Export map as image or JSON
- [ ] Command history and replay functionality
- [ ] Multi-language support (i18n)
- [ ] Dark mode theme
- [ ] Unit tests for core logic (map updates, message parsing)
- [ ] Integration with computer vision for image recognition

## Academic Context

**Course**: Multidisciplinary Project (MDP)
**Institution**: Nanyang Technological University
**Year**: 2019
**Team**: Group 26

This project integrated concepts from:
- Embedded systems (Arduino programming)
- Mobile application development (Android)
- Algorithms (path planning, search)
- Communication protocols (Bluetooth, serial)
- Real-time systems (sensor processing)

The project demonstrated end-to-end system integration across hardware, firmware, and software layers, emphasizing teamwork and multidisciplinary problem-solving.

## Credits

**Developer**: Fad Rahim
**Year**: 2019
**University**: Nanyang Technological University

Special thanks to the MDP teaching team and Group 26 members for their collaboration on the robot hardware and algorithm components.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

**Note**: This repository represents academic coursework completed in 2019. The project is archived for portfolio purposes and is no longer under active development.
