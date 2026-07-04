package com.nullstorm.vpn;

import android.content.Intent;
import android.net.VpnService;
import android.util.Log;

import libXray.ConvertShareLinksToXrayJsonRequest;

public class CustomVpnService extends VpnService {

    private static final String TAG = "CustomVpnService";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        String config = intent.getStringExtra("config_content");

        if (config == null) {
            Log.e(TAG, "Config is null");
            return START_NOT_STICKY;
        }

        convertAndLog(config);

        return START_NOT_STICKY;
    }

    private void convertAndLog(String vlessLink) {

        try {
            // 1. create request
            ConvertShareLinksToXrayJsonRequest req =
                    new ConvertShareLinksToXrayJsonRequest();
            // 2. set vless link
            req.setText(vlessLink);

            // 3. get result (JSON)
            String json = req.getText();

            // 4. log output
            Log.i(TAG, "XRAY JSON OUTPUT:  " + json);

        } catch (Exception e) {
            Log.e(TAG, "Conversion failed", e);
        }
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "Service stopped");
        super.onDestroy();
    }
}