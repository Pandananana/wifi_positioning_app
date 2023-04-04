package com.example.wifipositioning;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.Service;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.ScanResult;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.PersistableBundle;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.Manifest;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileWriter;
import java.util.List;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONException;
import java.io.IOException;
import android.os.Handler;
import android.os.IBinder;



public class MainActivity extends AppCompatActivity {

    private Button button;
    private Button download;
    private Button delete;
    private File csvFile;
    private TextView textView;
    private WifiManager wifiManager;
    private WifiInfo connection;
    private String display;
    private static final String TAG = "myapp:WifiScanActivity";
    private static final int WAKE_LOCK_TIMEOUT = 5 * 1000; // 5 seconds
    private PowerManager.WakeLock mWakeLock;

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
            Log.d(TAG, "Starting Wifi Scan");
            startWifiScan();
            handler.postDelayed(this, 5000); // 5 seconds delay
        }
    };

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
        textView = (TextView) findViewById(R.id.text1);

        // Request permissions
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
            requestPermissionLauncher.launch(permissions);
        } else {
            // Permissions already granted, start scan
            startWifiScan();
        }

        button.setOnClickListener(new View.OnClickListener() {
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
            
    private void startWifiScan() {
        // Start the Wi-Fi scan
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiManager.startScan();
        Log.d(TAG, "Scan Done");
        List<ScanResult> scanResults = wifiManager.getScanResults();
        for (ScanResult scanResult : scanResults) {
            Log.d(TAG, "MAC: "+scanResult.BSSID);
            Log.d(TAG, "RSSI: "+ scanResult.level);
        }
        new SendWifiScanResultsTask().execute();
    }

    private class SendWifiScanResultsTask extends AsyncTask<Void, Void, LocationData> {
        @Override
        protected LocationData doInBackground(Void... voids) {
            try {
                // Create a JSON object with the Wi-Fi scan results
                JSONObject json = new JSONObject();
                JSONArray array = new JSONArray();
                List<ScanResult> scanResults = wifiManager.getScanResults();
                for (ScanResult scanResult : scanResults) {
                    JSONObject wifiObject = new JSONObject();
                    wifiObject.put("macAddress", scanResult.BSSID);
                    wifiObject.put("signalStrength", scanResult.level);
                    array.put(wifiObject);
                    Log.d(TAG, "MAC: "+scanResult.BSSID);
                    Log.d(TAG, "RSSI: "+ scanResult.level);
                }
                json.put("wifiAccessPoints", array);

                // Send the JSON object to the Google Maps Geolocation API
                OkHttpClient client = new OkHttpClient();
                MediaType mediaType = MediaType.parse("application/json");
                RequestBody requestBody = RequestBody.create(json.toString(), mediaType);
                String apiKey = getString(R.string.google_maps_api_key);
                Request request = new Request.Builder()
                        .url("https://www.googleapis.com/geolocation/v1/geolocate?key="+apiKey)
                        .post(requestBody)
                        .build();
                Response response = client.newCall(request).execute();
                String responseBody = response.body().string();
                Log.d(TAG, "Response: " + responseBody);

                JSONObject jsonObject = new JSONObject(responseBody);
                JSONObject locationObject = jsonObject.getJSONObject("location");
                double latitude = locationObject.getDouble("lat");
                double longitude = locationObject.getDouble("lng");
                double accuracy = jsonObject.getDouble("accuracy");

                saveDataToCsv(latitude, longitude, accuracy);
                return new LocationData(latitude, longitude, accuracy);
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Error: " + e.getMessage(), e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(LocationData locationData) {
            if (locationData != null) {
                double latitude = locationData.getLatitude();
                double longitude = locationData.getLongitude();
                double accuracy = locationData.getAccuracy();
                // Use the latitude, longitude, and accuracy values here
                textView = (TextView) findViewById(R.id.text1);
                String locationText = "Latitude: " + latitude + "\nLongitude: " + longitude + "\nAccuracy: " + accuracy;
                textView.setText(locationText);
            }
        }
    }

    public class LocationData {
        private double latitude;
        private double longitude;
        private double accuracy;

        public LocationData(double latitude, double longitude, double accuracy) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.accuracy = accuracy;
        }

        public double getLatitude() {
            return latitude;
        }

        public double getLongitude() {
            return longitude;
        }

        public double getAccuracy() {
            return accuracy;
        }
    }

    public void saveDataToCsv(double latitude, double longitude, double accuracy) {
        csvFile = new File(Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_DOWNLOADS + "/location_data.csv");
        String[] data = {String.valueOf(latitude), String.valueOf(longitude), String.valueOf(accuracy)};
        String csvRow = TextUtils.join(",", data);

        try {
            FileWriter csvWriter = new FileWriter(csvFile, true);
            csvWriter.write(csvRow);
            csvWriter.write("\n");
            csvWriter.flush();
            csvWriter.close();
        } catch (IOException e) {
            Log.e(TAG, "Error saving data to CSV file: " + e.getMessage(), e);
        }
    }

    private void downloadCsvFile() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
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
    protected void onResume() {
        super.onResume();
        // Start the handler when the activity is resumed
        handler.postDelayed(automaticScanRunnable , 0);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop the handler when the activity is paused
        // handler.removeCallbacks(automaticScanRunnable );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
            mWakeLock = null;
        }
    }
}