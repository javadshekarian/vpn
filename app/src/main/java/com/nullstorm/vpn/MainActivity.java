package com.nullstorm.vpn;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.navigation.NavigationView;
import com.nullstorm.vpn.adapter.ConfigAdapter;
import com.nullstorm.vpn.model.VpnConfig;
import com.nullstorm.vpn.parser.core.CoreServiceManager;
import com.nullstorm.vpn.parser.dto.entities.ProfileItem;
import com.nullstorm.vpn.parser.fmt.VlessFmt;
import com.nullstorm.vpn.parser.fmt.VmessFmt;
import com.nullstorm.vpn.parser.handler.MmkvManager;
import com.nullstorm.vpn.parser.service.CoreVpnService;
import com.nullstorm.vpn.utils.UiUtils;
import com.nullstorm.vpn.utils.Utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements ConfigAdapter.OnConfigClickListener {

    private static final String TAG = "MainActivity";
    private static final String PREFS_NAME = "vpn_configs";
    private static final String KEY_CONFIGS = "configs_list";
    private static final int REQUEST_VPN_PERMISSION = 100;

    private final List<VpnConfig> configs = new ArrayList<>();
    private ConfigAdapter adapter;
    private MaterialButton connectButton;
    private TextView connectionStatus;
    private View statusIndicator;
    private boolean isConnected = false;
    private boolean isProcessing = false;
    private DrawerLayout drawerLayout;
    private SharedPreferences sharedPreferences;
    private RecyclerView recyclerView;
    private VpnConfig pendingConfig = null;
    private String currentConfigGuid = null;

    private final ActivityResultLauncher<String> filePicker
            = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            this::onFileSelected
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        loadConfigsFromStorage();
        setupDrawer();
    }

    private void loadConfigsFromStorage() {
        Set<String> configSet = sharedPreferences.getStringSet(KEY_CONFIGS, new HashSet<>());
        configs.clear();
        for (String configData : configSet) {
            String[] parts = configData.split("\\|\\|\\|");
            if (parts.length == 2) {
                String fixedLink = Utils.sanitizeVlessLink(parts[1]);
                ProfileItem profile = null;

                if (fixedLink != null && fixedLink.startsWith("vless://")) {
                    profile = VlessFmt.INSTANCE.parse(fixedLink);
                } else if (fixedLink != null && fixedLink.startsWith("vmess://")) {
                    profile = VmessFmt.INSTANCE.parse(fixedLink);
                }

                if (profile != null) {
                    configs.add(new VpnConfig(parts[0], parts[1], profile));
                }
            }
        }
    }

    private void saveConfigsToStorage() {
        Set<String> configSet = new HashSet<>();
        for (VpnConfig config : configs) {
            configSet.add(config.getName() + "|||" + config.getContent());
        }
        sharedPreferences.edit().putStringSet(KEY_CONFIGS, configSet).apply();
    }

    private void setupDrawer() {
        drawerLayout = new DrawerLayout(this);
        drawerLayout.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        LinearLayout mainContent = new LinearLayout(this);
        mainContent.setOrientation(LinearLayout.VERTICAL);
        mainContent.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        Toolbar toolbar = new Toolbar(this);
        toolbar.setBackgroundColor(Color.BLACK);
        toolbar.setElevation(UiUtils.dp(this, 4));

        LinearLayout.LayoutParams toolbarParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                UiUtils.dp(this, 30)
        );
        toolbar.setLayoutParams(toolbarParams);

        mainContent.addView(toolbar);

        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setPadding(
                UiUtils.dp(this, 10),
                UiUtils.dp(this, 16),
                UiUtils.dp(this, 10),
                UiUtils.dp(this, 24)
        );
        rootLayout.setBackgroundColor(getColor(android.R.color.black));
        rootLayout.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        rootLayout.addView(createHeaderSection());
        rootLayout.addView(createConnectionStatusSection());
        rootLayout.addView(createConnectSection());
        rootLayout.addView(createImportButton());
        rootLayout.addView(createConfigsList());

        mainContent.addView(rootLayout);

        drawerLayout.addView(mainContent);

        NavigationView navigationView = new NavigationView(this);
        navigationView.setNavigationItemSelectedListener(this::onNavigationItemSelected);
        DrawerLayout.LayoutParams navParams = new DrawerLayout.LayoutParams(
                UiUtils.dp(this, 280),
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        navParams.gravity = GravityCompat.START;
        navigationView.setLayoutParams(navParams);
        navigationView.setBackgroundColor(Color.BLACK);
        navigationView.inflateMenu(R.menu.navigation_menu);
        navigationView.setItemTextColor(ColorStateList.valueOf(Color.WHITE));
        navigationView.setElevation(UiUtils.dp(this, 16));

        LinearLayout headerView = new LinearLayout(this);
        headerView.setOrientation(LinearLayout.VERTICAL);
        headerView.setGravity(Gravity.CENTER);
        headerView.setMinimumHeight(UiUtils.dp(this, 100));
        headerView.setBackgroundColor(Color.parseColor("#1A1A1A"));

        TextView headerTitle = new TextView(this);
        headerTitle.setText("NullStorm VPN");
        headerTitle.setTextSize(UiUtils.sp(this, 8));
        headerTitle.setTextColor(Color.WHITE);

        Typeface typeface = ResourcesCompat.getFont(this, R.font.barlowcondensed_medium);
        headerTitle.setTypeface(typeface);

        headerTitle.setGravity(Gravity.CENTER);
        headerView.addView(headerTitle);

        navigationView.addHeaderView(headerView);

        drawerLayout.addView(navigationView);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.vpn_menu_open, R.string.vpn_menu_close
        );

        toggle.getDrawerArrowDrawable().setColor(Color.WHITE);

        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        setContentView(drawerLayout);
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

    private View createHeaderSection() {
        LinearLayout headerLayout = new LinearLayout(this);
        headerLayout.setOrientation(LinearLayout.VERTICAL);
        headerLayout.setGravity(Gravity.CENTER);

        TextView title = new TextView(this);
        title.setText(R.string.vpn_name);
        title.setTextSize(UiUtils.sp(this, 10));
        title.setTextColor(getColor(android.R.color.white));
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setGravity(Gravity.CENTER);

        TextView subtitle = new TextView(this);
        subtitle.setText(R.string.vpn_subtitle);
        subtitle.setTextSize(UiUtils.sp(this, 4));
        subtitle.setTextColor(getColor(R.color.gray_400));
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, UiUtils.dp(this, 4), 0, 0);

        headerLayout.addView(title);
        headerLayout.addView(subtitle);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.bottomMargin = UiUtils.dp(this, 0);
        headerLayout.setLayoutParams(params);

        return headerLayout;
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

    private View createImportButton() {
        LinearLayout buttonContainer = new LinearLayout(this);
        buttonContainer.setOrientation(LinearLayout.HORIZONTAL);
        buttonContainer.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        buttonContainer.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams containerParams = (LinearLayout.LayoutParams) buttonContainer.getLayoutParams();
        containerParams.bottomMargin = UiUtils.dp(this, 20);
        buttonContainer.setLayoutParams(containerParams);

        MaterialButton importButton = new MaterialButton(this);
        LinearLayout.LayoutParams importParams = new LinearLayout.LayoutParams(
                0,
                UiUtils.dp(this, 50),
                1
        );
        importParams.rightMargin = UiUtils.dp(this, 8);
        importButton.setLayoutParams(importParams);
        importButton.setText(R.string.vpn_import_config);
        importButton.setTextSize(UiUtils.sp(this, 5));
        importButton.setTextColor(getColor(R.color.gray_400));
        importButton.setBackgroundColor(getColor(android.R.color.transparent));
        importButton.setStrokeColorResource(R.color.gray_600);
        importButton.setStrokeWidth(UiUtils.dp(this, 1));
        importButton.setCornerRadius(UiUtils.dp(this, 25));
        importButton.setOnClickListener(v -> filePicker.launch("*/*"));

        MaterialButton freeConfigButton = new MaterialButton(this);
        LinearLayout.LayoutParams freeParams = new LinearLayout.LayoutParams(
                0,
                UiUtils.dp(this, 50),
                1
        );
        freeParams.leftMargin = UiUtils.dp(this, 8);
        freeConfigButton.setLayoutParams(freeParams);
        freeConfigButton.setText("Free Config");
        freeConfigButton.setTextSize(UiUtils.sp(this, 5));
        freeConfigButton.setTextColor(getColor(R.color.purple_500));
        freeConfigButton.setBackgroundColor(getColor(android.R.color.transparent));
        freeConfigButton.setStrokeColorResource(R.color.purple_500);
        freeConfigButton.setStrokeWidth(UiUtils.dp(this, 1));
        freeConfigButton.setCornerRadius(UiUtils.dp(this, 25));
        freeConfigButton.setOnClickListener(v -> Toast.makeText(this, "Free config loaded", Toast.LENGTH_SHORT).show());

        buttonContainer.addView(importButton);
        buttonContainer.addView(freeConfigButton);

        return buttonContainer;
    }

    private View createConfigsList() {
        recyclerView = new RecyclerView(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ConfigAdapter(this, configs, this);
        recyclerView.setAdapter(adapter);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0
        );
        params.weight = 1;
        recyclerView.setLayoutParams(params);

        return recyclerView;
    }

    private void toggleConnection() {
        if (isProcessing) {
            return;
        }

        isProcessing = true;
        connectButton.setEnabled(false);
        isConnected = !isConnected;

        if (isConnected) {
            int selectedPosition = adapter.getSelectedPosition();
            if (selectedPosition != RecyclerView.NO_POSITION && configs.size() > selectedPosition) {
                VpnConfig selectedConfig = configs.get(selectedPosition);
                connectToVpn(selectedConfig);
            } else {
                Toast.makeText(this, "Please select a config first", Toast.LENGTH_SHORT).show();
                isConnected = false;
                isProcessing = false;
                connectButton.setEnabled(true);
                return;
            }
        } else {
            disconnectFromVpn();
        }

        animateConnectButton();
        animateStatusIndicator();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            connectButton.setEnabled(true);
            isProcessing = false;
        }, 500);
    }

    /*
     * Connect to VPN with the selected config
     * First check if VPN permission is granted, if not request it
     */
    private void connectToVpn(VpnConfig config) {
        Intent prepareIntent = VpnService.prepare(this);
        if (prepareIntent != null) {
            pendingConfig = config;
            startActivityForResult(prepareIntent, REQUEST_VPN_PERMISSION);
            return;
        }
        startVpnService(config);
    }

    /*
     * Start the VPN service using CoreVpnService (like v2rayNG)
     */
    private void startVpnService(VpnConfig config) {
        try {
            // Save config to MMKV for CoreServiceManager
            String guid = "vpn_config_" + System.currentTimeMillis();
            MmkvManager.INSTANCE.encodeServerConfig(guid, config.getProfile());
            MmkvManager.INSTANCE.setSelectServer(guid);
            currentConfigGuid = guid;

            // Setup required settings for CoreVpnService
            setupVpnSettings();

            // Start CoreVpnService (like v2rayNG)
            Intent intent = new Intent(this, CoreVpnService.class);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }

            connectionStatus.setText(R.string.vpn_connecting);
            connectionStatus.setTextColor(getColor(R.color.purple_500));

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (CoreServiceManager.INSTANCE.isRunning()) {
                    connectionStatus.setText(R.string.vpn_connected);
                    connectionStatus.setTextColor(getColor(R.color.green_400));
                    statusIndicator.setBackgroundResource(R.drawable.status_indicator_connected);
                }
            }, 2000);

        } catch (Exception e) {
            Log.e(TAG, "Failed to start VPN service", e);
            Toast.makeText(this, "Failed to start VPN: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            isConnected = false;
            connectButton.setEnabled(true);
            isProcessing = false;
        }
    }

    /*
     * Setup required settings for CoreVpnService
     */
    private void setupVpnSettings() {
        try {
            // Enable VPN mode (required for CoreVpnService)
            MmkvManager.INSTANCE.encodeSettings("pref_vpn_mode", true);

            // Disable root mode
            MmkvManager.INSTANCE.encodeSettings("pref_root_mode", false);

            // Disable proxy sharing
            MmkvManager.INSTANCE.encodeSettings("pref_proxy_sharing", false);

            // ===== تنظیمات DNS =====
            Set<String> dnsSet = new HashSet<>();
            dnsSet.add("1.1.1.1");
            dnsSet.add("8.8.8.8");
            dnsSet.add("9.9.9.9");
            MmkvManager.INSTANCE.encodeSettings("pref_vpn_dns", dnsSet);

            // فعال کردن DNS داخلی
            MmkvManager.INSTANCE.encodeSettings("pref_local_dns_enabled", true);

            // فعال کردن FakeDNS
            MmkvManager.INSTANCE.encodeSettings("pref_fake_dns_enabled", true);

            // ===== تنظیمات مسیریابی =====
            MmkvManager.INSTANCE.encodeSettings("pref_routing_domain_strategy", "IPIfNonMatch");

            // ===== تنظیمات شبکه =====
            MmkvManager.INSTANCE.encodeSettings("pref_vpn_mtu", 1500);
            MmkvManager.INSTANCE.encodeSettings("pref_ipv6_enabled", false);

            // ===== تنظیمات Sniffing =====
            MmkvManager.INSTANCE.encodeSettings("pref_sniffing_enabled", true);

            // ===== تنظیمات دیگر =====
            MmkvManager.INSTANCE.encodeSettings("pref_per_app_proxy", false);

            // غیرفعال کردن HevTun (اگر مشکل دارد)
            MmkvManager.INSTANCE.encodeSettings("pref_hev_tun", false);

            Log.i(TAG, "VPN settings configured");
        } catch (Exception e) {
            Log.e(TAG, "Failed to setup VPN settings", e);
        }
    }

    private void disconnectFromVpn() {
        try {
            // Stop CoreVpnService
            Intent intent = new Intent(this, CoreVpnService.class);
            stopService(intent);

            // Clear selection
            if (currentConfigGuid != null) {
                String selected = MmkvManager.INSTANCE.getSelectServer();
                if (selected != null && selected.equals(currentConfigGuid)) {
                    MmkvManager.INSTANCE.setSelectServer("");
                }
                currentConfigGuid = null;
            }

            connectionStatus.setText(R.string.vpn_disconnecting);
            connectionStatus.setTextColor(getColor(R.color.gray_500));

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                connectionStatus.setText(R.string.vpn_disconnected);
                connectionStatus.setTextColor(getColor(R.color.gray_400));
                statusIndicator.setBackgroundResource(R.drawable.status_indicator_disconnected);
            }, 500);

        } catch (Exception e) {
            Log.e(TAG, "Failed to disconnect VPN", e);
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_VPN_PERMISSION) {
            if (resultCode == RESULT_OK && pendingConfig != null) {
                startVpnService(pendingConfig);
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
            stopService(intent);

            // Clear selection
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
        configs.remove(position);
        adapter.notifyItemRemoved(position);
        adapter.notifyItemRangeChanged(position, configs.size() - position);
        saveConfigsToStorage();

        if (isConnected) {
            int selectedPosition = adapter.getSelectedPosition();
            if (selectedPosition == RecyclerView.NO_POSITION) {
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
                stopService(intent);

                // Clear selection
                if (currentConfigGuid != null) {
                    String selected = MmkvManager.INSTANCE.getSelectServer();
                    if (selected != null && selected.equals(currentConfigGuid)) {
                        MmkvManager.INSTANCE.setSelectServer("");
                    }
                    currentConfigGuid = null;
                }
            }
        }

        Toast.makeText(this, "Config deleted: " + config.getName(), Toast.LENGTH_SHORT).show();
    }

    private void onFileSelected(Uri uri) {
        if (uri == null) return;

        try {
            InputStream is = getContentResolver().openInputStream(uri);
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(is)
            );
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

            String fileName = uri.getLastPathSegment();
            if (fileName != null && fileName.contains("/"))
                fileName = fileName.substring(fileName.lastIndexOf("/") + 1);

            VpnConfig newConfig = new VpnConfig(
                    fileName != null ? fileName : getString(R.string.vpn_imported_config),
                    content,
                    profile
            );

            configs.add(newConfig);
            adapter.notifyItemInserted(configs.size() - 1);
            saveConfigsToStorage();

            Toast.makeText(this, R.string.vpn_config_imported, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Import File Failed", e);
            Toast.makeText(this, R.string.vpn_config_import_failed, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Update connection status if VPN is running
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