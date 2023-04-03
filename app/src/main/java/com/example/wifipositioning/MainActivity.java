package com.example.wifipositioning;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.app.AlertDialog;
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
import okhttp3.ResponseBody;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Button button;
    private Button download;
    private Button delete;
    private File csvFile;

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
        download = (Button) findViewById(R.id.button2);
        delete = (Button) findViewById(R.id.button3);
        textView = (TextView) findViewById(R.id.text1);

        // Request fine location permission
        if (!(ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        // Request fine location permission
        if (!(ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION);
        }

        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View arg0) {
                wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
                connection = wifiManager.getConnectionInfo();
                startWifiScan();
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
        wifiManager.startScan();
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

                Log.d(TAG, "Latitude: " + latitude);
                Log.d(TAG, "Longitude: " + longitude);
                Log.d(TAG, "Accuracy: " + accuracy);

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


}