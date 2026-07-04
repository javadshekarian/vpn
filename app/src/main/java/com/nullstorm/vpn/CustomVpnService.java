package com.nullstorm.vpn;

import android.content.Intent;
import android.net.VpnService;
import android.util.Log;
import com.nullstorm.vpn.parser.fmt.VlessFmt;
import com.nullstorm.vpn.parser.dto.entities.ProfileItem;
import com.nullstorm.vpn.utils.Utils;

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
            String fixedLink = Utils.sanitizeVlessLink(vlessLink);
            ProfileItem config = VlessFmt.INSTANCE.parse(fixedLink);
            if(config == null) {
                Log.e(TAG, "Invalid Vless Link");
                return;
            }

            Log.i(TAG, "===== VLESS CONFIG START =====");
            Log.i(TAG, "Server: " + config.getServer());
            Log.i(TAG, "Port: " + config.getServerPort());
            Log.i(TAG, "Method: " + config.getMethod());
            Log.i(TAG, "Password/UserInfo: " + config.getPassword());
            Log.i(TAG, "Remarks: " + config.getRemarks());
            Log.i(TAG, "Extra: " + config.toString());
            Log.i(TAG, "===== VLESS CONFIG END =====");
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