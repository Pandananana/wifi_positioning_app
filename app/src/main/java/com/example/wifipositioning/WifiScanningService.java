package com.example.wifipositioning;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.os.IBinder;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class WifiScanningService extends Service {

    private WifiManager wifiManager;
    private Handler handler = new Handler();
    private boolean isScanning = false;
    private NotificationManager notificationManager;
    private static final String TAG = "myapp:ScanService";
    private File csvFile;

    @Override
    public void onCreate() {
        super.onCreate();
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        startForeground(1, createNotification());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startScanning();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopScanning();
    }

    private void startScanning() {
        if (!isScanning) {
            isScanning = true;
            handler.post(scanRunnable);
        }
    }

    private void stopScanning() {
        if (isScanning) {
            isScanning = false;
            handler.removeCallbacks(scanRunnable);
        }
    }

    private final Runnable scanRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isScanning) {
                return;
            }
            wifiManager.startScan();
            List<ScanResult> scanResults = wifiManager.getScanResults();
            for (ScanResult scanResult : scanResults) {
                Log.d(TAG, "MAC: " + scanResult.BSSID);
                Log.d(TAG, "RSSI: " + scanResult.level);
            }
            new SendWifiScanResultsTask().execute();
            handler.postDelayed(this, 5000); // 5 seconds delay
        }
    };

    private Notification createNotification() {
        String CHANNEL_ID = "WifiScanningServiceChannel";
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Wifi Scanning Service", NotificationManager.IMPORTANCE_DEFAULT);
        notificationManager.createNotificationChannel(channel);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Wifi Scanning Service")
                .setContentText("Scanning wifi every 5 seconds")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        return builder.build();
    }

    public class SendWifiScanResultsTask extends AsyncTask<Void, Void, LocationData> {
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

                saveDataToCsv(latitude, longitude, accuracy);
                return new LocationData(latitude, longitude, accuracy);
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Error: " + e.getMessage(), e);
            }
            return null;
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

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

