package com.example.wifipositioning;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.ScanResult;

import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.Manifest;

import android.os.Handler;
import java.util.List;


import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Button button;
    private TextView textView;

    private WifiManager wifiManager;
    private WifiInfo connection;
    private String display;
    private static final String TAG = "WifiScanActivity";

    private final ActivityResultLauncher<String[]> requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), permissions -> {
        if (!permissions.containsValue(false)) {
            // Permissions granted, start scan
            startWifiScan();
        } else {
            // Permissions not granted, exit app
            System.exit(-1);
        }
    });

    private final Handler handler = new Handler();
    private boolean isAutomaticScanRunning = true;
    private final Runnable automaticScanRunnable  = new Runnable() {
        @Override
        public void run() {
            startWifiScan();
            handler.postDelayed(this, 5000); // 5 seconds delay
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        button = findViewById(R.id.button1);
        textView = findViewById(R.id.text1);

        // Request permissions
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
            requestPermissionLauncher.launch(permissions);
        } else {
            // Permissions already granted, start scan
            startWifiScan();
        }

        button.setOnClickListener(new View.OnClickListener(){
            public void onClick(View arg0) {
                if (isAutomaticScanRunning) {
                    // Stop automatic scan
                    handler.removeCallbacks(automaticScanRunnable);
                    isAutomaticScanRunning = false;
                    button.setText("Start");
                } else {
                    // Start automatic scan
                    wifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
                    connection = wifiManager.getConnectionInfo();
                    startWifiScan();
                    handler.postDelayed(automaticScanRunnable, 5000); // Run every 5 seconds
                    isAutomaticScanRunning = true;
                    button.setText("Stop");
                }
            }
        });
    }

    private void startWifiScan() {
        // Start the Wi-Fi scan
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiManager.startScan();

        // Get the scan results
        List<ScanResult> scanResults = wifiManager.getScanResults();

        // Display the scan results
        StringBuilder scanResultsBuilder = new StringBuilder();
        for (ScanResult scanResult : scanResults) {
            scanResultsBuilder.append("SSID: ")
                    .append(scanResult.SSID)
                    .append("\nMAC Address: ")
                    .append(scanResult.BSSID)
                    .append("\nRSSI: ")
                    .append(scanResult.level)
                    .append("\n\n");
        }
        textView.setText(scanResultsBuilder.toString());

        // Log the scan results
        for (ScanResult scanResult : scanResults) {
            Log.d(TAG, "SSID: " + scanResult.SSID
                    + ", BSSID: " + scanResult.BSSID
                    + ", RSSI: " + scanResult.level);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Start the handler when the activity is resumed
        handler.postDelayed(automaticScanRunnable , 0);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop the handler when the activity is paused
        handler.removeCallbacks(automaticScanRunnable );
    }
}
