package com.nullstorm.vpn;

import android.content.Intent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.IOException;

public class CustomVpnService extends VpnService {
    private static final String TAG = "CustomVpnService";
    private ParcelFileDescriptor vpnInterface;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "VPN Service Created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Starting VPN");

        Builder builder = new Builder();
        builder.setSession("Nebula VPN");
        builder.addAddress("10.0.0.2", 32);
        builder.addRoute("0.0.0.0", 0);
        builder.addDnsServer("1.1.1.1");
        builder.addDnsServer("8.8.8.8");
        builder.setBlocking(false);
        builder.setMtu(1500);

        try {
            vpnInterface = builder.establish();
            if (vpnInterface == null) {
                Log.e(TAG, "Failed To Establish VPN");
                stopSelf();
            } else Log.i(TAG, "VPN Established Successfully");
        } catch (Exception e) {
            Log.e(TAG, "VPN establishment error", e);
            stopSelf();
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "Stopping VPN");
        try {
            if (vpnInterface != null) {
                vpnInterface.close();
                vpnInterface = null;
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed To Close VPN", e);
        }
        super.onDestroy();
    }
}