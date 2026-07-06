package com.nullstorm.vpn;

import android.app.Application;
import android.util.Log;

import com.tencent.mmkv.MMKV;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class VpnApplication extends Application {

    private static final String TAG = "VpnApplication";
    private static final String GEOSITE_DAT = "geosite.dat";
    private static final String GEOIP_DAT = "geoip.dat";

    @Override
    public void onCreate() {
        super.onCreate();

        MMKV.initialize(this);

//        copyAssetsToInternalStorage();
    }

//    private void copyAssetsToInternalStorage() {
//        try {
//            File extFolder = new File(getExternalFilesDir(null), "assets");
//            if (!extFolder.exists()) {
//                extFolder.mkdirs();
//            }
//
//            String[] geoFiles = {GEOSITE_DAT, GEOIP_DAT};
//
//            for (String fileName : geoFiles) {
//                try {
//                    InputStream inputStream = getAssets().open(fileName);
//                    File targetFile = new File(extFolder, fileName);
//
//                    if (!targetFile.exists()) {
//                        FileOutputStream outputStream = new FileOutputStream(targetFile);
//                        byte[] buffer = new byte[1024];
//                        int read;
//                        while ((read = inputStream.read(buffer)) != -1) {
//                            outputStream.write(buffer, 0, read);
//                        }
//                        inputStream.close();
//                        outputStream.close();
//                        Log.i(TAG, "Copied " + fileName + " to " + targetFile.getAbsolutePath());
//                    }
//                } catch (Exception e) {
//                    Log.e(TAG, "Failed to copy " + fileName, e);
//                }
//            }
//        } catch (Exception e) {
//            Log.e(TAG, "Failed to copy assets to internal storage", e);
//        }
//    }
}