package com.nullstorm.vpn;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.nullstorm.vpn.adapter.ConfigAdapter;
import com.nullstorm.vpn.adapter.ConfigAdapter;
import com.nullstorm.vpn.model.VpnConfig;
import com.nullstorm.vpn.ui.UiUtils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private final List<VpnConfig> configs = new ArrayList<>();
    private ConfigAdapter adapter;
    private MaterialButton connectButton;
    private TextView connectionStatus;
    private View statusIndicator;
    private boolean isConnected = false;

    private final ActivityResultLauncher<String> filePicker
            = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            this::onFileSelected
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(createRootView());
    }

    private View createRootView() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(
                UiUtils.dp(this, 24),
                UiUtils.dp(this, 40),
                UiUtils.dp(this, 24),
                UiUtils.dp(this, 24)
        );
        root.setBackgroundColor(getColor(android.R.color.black));

        root.addView(createHeaderSection());
        root.addView(createConnectionStatusSection());
        root.addView(createConnectSection());
        root.addView(createImportButton());
        root.addView(createConfigsList());

        return root;
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
        params.bottomMargin = UiUtils.dp(this, 32);
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
        dotParams.rightMargin = UiUtils.dp(this, 12);
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
        params.bottomMargin = UiUtils.dp(this, 20);
        statusLayout.setLayoutParams(params);

        return statusLayout;
    }

    private View createConnectSection() {

        int cardHeight = UiUtils.dp(this, 230);
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

        int size = UiUtils.dp(this, 220);

        LinearLayout.LayoutParams btnParams =
                new LinearLayout.LayoutParams(size, size);

        connectButton.setLayoutParams(btnParams);

        connectButton.setText(R.string.vpn_connect);
        connectButton.setTextSize(UiUtils.sp(this, 7));
        connectButton.setTextColor(Color.WHITE);

        connectButton.setCornerRadius(size / 2);

        connectButton.setAllCaps(true);
        connectButton.setTypeface(null, Typeface.BOLD);
        connectButton.setBackgroundTintList(
                ColorStateList.valueOf(Color.parseColor("#1F1F1F"))
        );

        connectButton.setOnClickListener(v -> toggleConnection());
        connectLayout.addView(connectButton);
        connectCard.addView(connectLayout);

        return connectCard;
    }

    private View createImportButton() {
        MaterialButton importButton = new MaterialButton(this);
        importButton.setText(R.string.vpn_import_config);
        importButton.setTextSize(UiUtils.sp(this, 6));
        importButton.setTextColor(getColor(R.color.gray_400));
        importButton.setBackgroundColor(getColor(android.R.color.transparent));
        importButton.setStrokeColorResource(R.color.gray_600);
        importButton.setStrokeWidth(UiUtils.dp(this, 1));
        importButton.setCornerRadius(UiUtils.dp(this, 25));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                UiUtils.dp(this, 50)
        );
        params.bottomMargin = UiUtils.dp(this, 20);
        importButton.setLayoutParams(params);

        importButton.setOnClickListener(v -> filePicker.launch("*/*"));

        return importButton;
    }

    private View createConfigsList() {
        RecyclerView recyclerView = new RecyclerView(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ConfigAdapter(this, configs, this::onConfigSelected);
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
        isConnected = !isConnected;

        if (isConnected) {
            connectToVpn();
        } else {
            disconnectFromVpn();
        }

        animateConnectButton();
        animateStatusIndicator();
    }

    private void connectToVpn() {
        Intent intent = new Intent(this, CustomVpnService.class);
        intent.setAction("START_VPN");
        startService(intent);

        connectionStatus.setText(R.string.vpn_connecting);
        connectionStatus.setTextColor(getColor(R.color.purple_500));

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            connectionStatus.setText(R.string.vpn_connected);
            connectionStatus.setTextColor(getColor(R.color.green_400));
        }, 1500);
    }

    private void disconnectFromVpn() {
        Intent intent = new Intent(this, CustomVpnService.class);
        stopService(intent);

        connectionStatus.setText(R.string.vpn_disconnecting);
        connectionStatus.setTextColor(getColor(R.color.gray_500));

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            connectionStatus.setText(R.string.vpn_disconnected);
            connectionStatus.setTextColor(getColor(R.color.gray_400));
        }, 500);
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

        connectButton.setText(isConnected ? R.string.vpn_disconnect : R.string.vpn_connect);
        connectButton.setTextColor(isConnected ? getColor(R.color.red_400) : getColor(android.R.color.white));
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

    private void onConfigSelected(VpnConfig config) {
        Toast.makeText(this, String.format(getString(R.string.vpn_config_selected), config.getName()), Toast.LENGTH_SHORT).show();
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

            String fileName = uri.getLastPathSegment();
            if (fileName != null && fileName.contains("/")) {
                fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
            }

            configs.add(new VpnConfig(fileName != null ? fileName : getString(R.string.vpn_imported_config), content));
            adapter.notifyItemInserted(configs.size() - 1);

            Toast.makeText(this, R.string.vpn_config_imported, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Import File Failed", e);
            Toast.makeText(this, R.string.vpn_config_import_failed, Toast.LENGTH_SHORT).show();
        }
    }
}