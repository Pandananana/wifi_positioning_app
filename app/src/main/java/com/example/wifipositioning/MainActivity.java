package com.example.wifipositioning;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.ScanResult;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.Manifest;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Button button;

    TextView textView;

    WifiManager wifiManager;
    WifiInfo connection;
    String display;
    private static final String TAG = "WifiScanActivity";

    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
        if (!isGranted) {
            System.exit(-1);
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        button = (Button) findViewById(R.id.button1);
        textView = (TextView) findViewById(R.id.text1);

        // Request fine location permission
        if (!(ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        // Request fine location permission
        if (!(ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION);
        }

        button.setOnClickListener(new View.OnClickListener(){
            public void onClick(View arg0) {
                wifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
                connection = wifiManager.getConnectionInfo();
                startWifiScan();

            }
        });

        //https://www.googleapis.com/geolocation/v1/geolocate?key=AIzaSyA_S2GR_W78DxLa0RQ87Ce643r3-IsHwWg

    }
    private void startWifiScan() {
        // Start the Wi-Fi scan
        wifiManager.startScan();

        // Get the scan results
        List<ScanResult> scanResults = wifiManager.getScanResults();

        // Display the scan results
        StringBuilder scanResultsBuilder = new StringBuilder();
        scanResultsBuilder.append("[");
        for (int i = 0; i < scanResults.size(); i++) {
            ScanResult scanResult = scanResults.get(i);
            scanResultsBuilder.append("{")
                    .append("\n\"macAddress\": ").append(scanResult.BSSID).append(",")
                    .append("\n\"signalStrength\": ").append(scanResult.level).append(",")
                    .append("\n}");
            if (i != scanResults.size() - 1) {
                scanResultsBuilder.append(",\n");
            }
        }
        scanResultsBuilder.append("]");
        textView.setText(scanResultsBuilder.toString());

        // Log the scan results
        for (ScanResult scanResult : scanResults) {
            Log.d(TAG, "SSID: " + scanResult.SSID
                    + ", BSSID: " + scanResult.BSSID
                    + ", RSSI: " + scanResult.level);
        }
    }
}