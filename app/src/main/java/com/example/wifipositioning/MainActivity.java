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

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.Manifest;

import org.json.JSONArray;
import org.json.JSONObject;

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

        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View arg0) {
                wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
                connection = wifiManager.getConnectionInfo();
                startWifiScan();

            }
        });

        //https://www.googleapis.com/geolocation/v1/geolocate?key=AIzaSyA_S2GR_W78DxLa0RQ87Ce643r3-IsHwWg

    }

    private void startWifiScan() {
        wifiManager.startScan();
        new SendWifiScanResultsTask().execute();
    }

    private class SendWifiScanResultsTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
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
                Request request = new Request.Builder()
                        .url("https://www.googleapis.com/geolocation/v1/geolocate?key=AIzaSyA_S2GR_W78DxLa0RQ87Ce643r3-IsHwWg")
                        .post(requestBody)
                        .build();
                Response response = client.newCall(request).execute();
                String responseBody = response.body().string();
                Log.d(TAG, "Response: " + responseBody);

                JSONObject jsonObject = new JSONObject(responseBody);
                JSONObject locationObject = jsonObject.getJSONObject("location");
                double latitude = locationObject.getDouble("lat");
                double longitude = locationObject.getDouble("lng");

                Log.d(TAG, "Latitude: " + latitude);
                Log.d(TAG, "Longitude: " + longitude);
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Error: " + e.getMessage(), e);
            }
            return null;
        }
    }
}