package com.example.wifipositioning;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.Manifest;
import android.widget.NumberPicker;
import android.widget.Toast;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private Button button;
    private Button download;
    private Button delete;
    private NumberPicker picker;
    private File csvFile;
    private WifiManager wifiManager;
    private static final String TAG = "myapp:WifiScanActivity";
    private static final int WAKE_LOCK_TIMEOUT = 5 * 1000; // 5 seconds
    private PowerManager.WakeLock mWakeLock;
    private boolean isAutomaticScanRunning = false;
    private String scanDelay = "5";

    private final ActivityResultLauncher<String[]> requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), permissions -> {
        if (permissions.containsValue(false)) {
            // Permissions not granted, exit app
            System.exit(-1);
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        mWakeLock.acquire(WAKE_LOCK_TIMEOUT);

        button = (Button) findViewById(R.id.button1);
        download = (Button) findViewById(R.id.button2);
        delete = (Button) findViewById(R.id.button3);
        picker = findViewById(R.id.numberPicker);
        picker.setMaxValue(60);
        picker.setMinValue(1);
        picker.setValue(5);

        // Request permissions
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.FOREGROUND_SERVICE) != PackageManager.PERMISSION_GRANTED) {
            String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.FOREGROUND_SERVICE};
            requestPermissionLauncher.launch(permissions);
        }


        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View arg0) {
                if (isAutomaticScanRunning) {
                    // Stop automatic scan
                    isAutomaticScanRunning = false;
                    stopWifiScanningService();
                    button.setText("Start");
                } else {
                    // Start automatic scan
                    isAutomaticScanRunning = true;
                    startWifiScanningService();
                    button.setText("Stop");

                }
            }
        });
        download.setOnClickListener(new View.OnClickListener() {
            public void onClick(View arg0) {
                downloadCsvFile();
            }
        });
        delete.setOnClickListener(new View.OnClickListener() {
            public void onClick(View arg0) {
                showDeleteConfirmationDialog();
            }
        });

    }

    private void downloadCsvFile() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        csvFile = new File(Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_DOWNLOADS + "/location_data.csv");
        Uri uri = FileProvider.getUriForFile(this, "com.example.myapp.fileprovider", csvFile);
        intent.setDataAndType(uri, "text/csv");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(Intent.createChooser(intent, "Download CSV file"));
    }

    private void showDeleteConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Are you sure you want to delete the CSV file?")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        deleteCsvFile();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                    }
                });
        // Create the AlertDialog object and show it
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void deleteCsvFile() {
        csvFile = new File(Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_DOWNLOADS + "/location_data.csv");
        if (csvFile.exists()) {
            boolean deleted = csvFile.delete();
            if (deleted) {
                Log.d(TAG, "File deleted successfully");
            } else {
                Log.d(TAG, "Failed to delete file");
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
            mWakeLock = null;
        }
    }

    private void startWifiScanningService() {
        Log.d(TAG, "startWifiScanningService: ");
        Intent intent = new Intent(this, WifiScanningService.class);
        Log.d(TAG, "Picker Value: " + picker.getValue());
        intent.putExtra("SCAN_DELAY", String.valueOf(picker.getValue()));
        ContextCompat.startForegroundService(this, intent);
    }

    public void stopWifiScanningService() {
        Log.d(TAG, "stopWifiScanningService: ");
        Intent intent = new Intent(this, WifiScanningService.class);
        stopService(intent);
    }
}