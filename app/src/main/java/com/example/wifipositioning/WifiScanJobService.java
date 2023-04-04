package com.example.wifipositioning;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.util.List;


public class WifiScanJobService extends JobService {
    private static final String TAG = "myapp:JobService";
    @Override
    public boolean onStartJob(JobParameters params) {
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        Log.d(TAG, "Scan Done");
        wifiManager.startScan();
        Log.d(TAG, "Scan Done");
        List<ScanResult> scanResults = wifiManager.getScanResults();
        for (ScanResult scanResult : scanResults) {
            Log.d(TAG, "MAC: "+scanResult.BSSID);
            Log.d(TAG, "RSSI: "+ scanResult.level);
        }
        return false; // Return false to indicate that the job is finished
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }
}
