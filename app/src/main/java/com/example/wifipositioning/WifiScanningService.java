package com.example.wifipositioning;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.util.Log;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.List;

public class WifiScanningService extends Service {

    private WifiManager wifiManager;
    private Handler handler = new Handler();
    private boolean isScanning = false;
    private NotificationManager notificationManager;
    private static final String TAG = "myapp:ScanService";

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

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

