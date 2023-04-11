# WiFi Positioning App

This is an Android app that allows you to perform WiFi scanning to collect location data in the form of a CSV file. The app consists of two main Java files,      `MainActivity.java` and `WifiScanningService.java`.

<br/>

## Getting Started

To get started with the WiFi Positioning App, follow the steps below:

### Prerequisites
- Android Studio installed on your computer.
- An Android device or emulator running Android 6.0 (Marshmallow) or higher.

### Installation

1. Clone or download the repository to your local machine.
2. Open the project in Android Studio.
3. Connect your Android device to your computer or start an Android emulator.
4. Build and run the app on your device/emulator.

### Permissions

The app requires the following permissions to function properly:

- `ACCESS_FINE_LOCATION`: To scan for WiFi networks and collect location data.
- `ACCESS_COARSE_LOCATION`: To scan for WiFi networks and collect location data.
- `FOREGROUND_SERVICE`: To run the WiFi scanning service in the foreground and prevent it from being killed by the system.
- Disable any battery optimization for the app, or else it won't run properly in the background.

Make sure to grant these permissions when prompted by the app.


## MainActivity.java

The `MainActivity.java` file contains the main activity of the app, which displays the user interface and handles user interactions. It has the following features:

- Start/Stop Automatic Scan: Clicking the "Start" button starts the WiFi scanning service to automatically scan for WiFi networks and collect location data. Clicking the "Stop" button stops the scanning service.
- Download CSV File: Clicking the "Download" button opens a file chooser dialog to download the collected location data in CSV format.
- Delete CSV File: Clicking the "Delete" button shows a confirmation dialog to delete the downloaded CSV file.

## WifiScanningService.java

The WifiScanningService.java file is a service that runs in the background and performs the WiFi scanning. It collects location data in CSV format and saves it to the device's external storage directory. The service runs at a specified interval, which can be set using the number picker in the `MainActivity`.

## Contributing

If you would like to contribute to the WiFi Positioning App, please follow the standard GitHub workflow:

1. Fork the repository.
2. Create a new branch for your feature or bugfix.
3. Make changes and commit them to your branch.
4. Push your branch to your forked repository.
5. Create a pull request to merge your changes into the main repository.
