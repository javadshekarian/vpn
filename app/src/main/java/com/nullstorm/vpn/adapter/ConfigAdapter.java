package com.nullstorm.vpn.adapter;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.nullstorm.vpn.R;
import com.nullstorm.vpn.model.VpnConfig;
import com.nullstorm.vpn.parser.dto.entities.ProfileItem;
import com.nullstorm.vpn.parser.enums.EConfigType;
import com.nullstorm.vpn.utils.UiUtils;

import java.util.List;

public class ConfigAdapter extends RecyclerView.Adapter<ConfigAdapter.ViewHolder> {
    private final Context context;
    private final List<VpnConfig> configs;
    private final OnConfigClickListener listener;
    private int selectedPosition = RecyclerView.NO_POSITION;

    public interface OnConfigClickListener {
        void onConfigSelected(VpnConfig config);
    }

    public ConfigAdapter(Context context, List<VpnConfig> configs, OnConfigClickListener listener) {
        this.context = context;
        this.configs = configs;
        this.listener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        View statusDot;
        MaterialCardView card;
        TextView configNumber;
        TextView protocolBadge;
        TextView serverAddress;
        TextView connectionDetails;
        TextView securityInfo;

        public ViewHolder(@NonNull MaterialCardView cardView, TextView title,
                          View statusDot, TextView configNumber, TextView protocolBadge,
                          TextView serverAddress, TextView connectionDetails, TextView securityInfo) {
            super(cardView);
            this.card = cardView;
            this.title = title;
            this.statusDot = statusDot;
            this.configNumber = configNumber;
            this.protocolBadge = protocolBadge;
            this.serverAddress = serverAddress;
            this.connectionDetails = connectionDetails;
            this.securityInfo = securityInfo;
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        MaterialCardView card = new MaterialCardView(context);
        RecyclerView.LayoutParams cardParams = new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.bottomMargin = UiUtils.dp(context, 12);
        card.setLayoutParams(cardParams);
        card.setRadius(UiUtils.dp(context, 16));
        card.setCardElevation(UiUtils.dp(context, 2));
        card.setBackgroundColor(context.getColor(android.R.color.transparent));
        card.setStrokeWidth(0);

        LinearLayout mainLayout = new LinearLayout(context);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(
                UiUtils.dp(context, 20),
                UiUtils.dp(context, 18),
                UiUtils.dp(context, 20),
                UiUtils.dp(context, 18)
        );
        mainLayout.setBackgroundResource(R.drawable.config_item_background);

        // Top row: Number + Title + Status
        LinearLayout topRow = new LinearLayout(context);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(android.view.Gravity.CENTER_VERTICAL);

        // Number container - bigger
        LinearLayout numberContainer = new LinearLayout(context);
        numberContainer.setOrientation(LinearLayout.VERTICAL);
        numberContainer.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams numberParams = new LinearLayout.LayoutParams(
                UiUtils.dp(context, 40),
                UiUtils.dp(context, 40)
        );
        numberParams.rightMargin = UiUtils.dp(context, 16);
        numberContainer.setLayoutParams(numberParams);
        numberContainer.setBackgroundResource(R.drawable.config_number_badge);

        TextView configNumber = new TextView(context);
        configNumber.setTextSize(UiUtils.sp(context, 5));
        configNumber.setTextColor(context.getColor(android.R.color.white));
        configNumber.setTypeface(null, android.graphics.Typeface.BOLD);
        configNumber.setGravity(android.view.Gravity.CENTER);
        numberContainer.addView(configNumber);

        // Title and status container
        LinearLayout titleContainer = new LinearLayout(context);
        titleContainer.setOrientation(LinearLayout.VERTICAL);
        titleContainer.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1
        ));

        LinearLayout titleRow = new LinearLayout(context);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(android.view.Gravity.CENTER_VERTICAL);

        TextView title = new TextView(context);
        title.setTextSize(UiUtils.sp(context, 7));
        title.setTextColor(context.getColor(android.R.color.white));
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setMaxLines(1);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1
        );
        title.setLayoutParams(titleParams);

        // Status dot - bigger
        View statusDot = new View(context);
        LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(
                UiUtils.dp(context, 10),
                UiUtils.dp(context, 10)
        );
        dotParams.leftMargin = UiUtils.dp(context, 10);
        statusDot.setLayoutParams(dotParams);
        statusDot.setBackgroundResource(R.drawable.status_indicator_disconnected);

        titleRow.addView(title);
        titleRow.addView(statusDot);
        titleContainer.addView(titleRow);

        topRow.addView(numberContainer);
        topRow.addView(titleContainer);

        // Middle row: Protocol badge + Server
        LinearLayout middleRow = new LinearLayout(context);
        middleRow.setOrientation(LinearLayout.HORIZONTAL);
        middleRow.setPadding(0, UiUtils.dp(context, 10), 0, 0);
        middleRow.setGravity(android.view.Gravity.CENTER_VERTICAL);

        // Protocol badge - bigger
        TextView protocolBadge = new TextView(context);
        protocolBadge.setTextSize(UiUtils.sp(context, 5));
        protocolBadge.setTextColor(context.getColor(android.R.color.white));
        protocolBadge.setTypeface(null, android.graphics.Typeface.BOLD);
        protocolBadge.setPadding(UiUtils.dp(context, 12), UiUtils.dp(context, 4),
                UiUtils.dp(context, 12), UiUtils.dp(context, 4));
        protocolBadge.setBackgroundResource(R.drawable.protocol_badge);
        LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        badgeParams.rightMargin = UiUtils.dp(context, 12);
        protocolBadge.setLayoutParams(badgeParams);

        // Server address
        TextView serverAddress = new TextView(context);
        serverAddress.setTextSize(UiUtils.sp(context, 5));
        serverAddress.setTextColor(context.getColor(R.color.gray_300));
        serverAddress.setTypeface(Typeface.create("monospace", Typeface.NORMAL));
        serverAddress.setMaxLines(1);
        serverAddress.setEllipsize(android.text.TextUtils.TruncateAt.END);
        serverAddress.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1
        ));

        middleRow.addView(protocolBadge);
        middleRow.addView(serverAddress);

        // Security info row
        LinearLayout securityRow = new LinearLayout(context);
        securityRow.setOrientation(LinearLayout.HORIZONTAL);
        securityRow.setPadding(0, UiUtils.dp(context, 6), 0, 0);

        TextView securityInfo = new TextView(context);
        securityInfo.setTextSize(UiUtils.sp(context, 4));
        securityInfo.setTextColor(context.getColor(R.color.gray_400));
        securityInfo.setMaxLines(1);
        securityInfo.setEllipsize(android.text.TextUtils.TruncateAt.END);
        securityInfo.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1
        ));

        securityRow.addView(securityInfo);

        // Bottom row: Connection details
        TextView connectionDetails = new TextView(context);
        connectionDetails.setTextSize(UiUtils.sp(context, 4));
        connectionDetails.setTextColor(context.getColor(R.color.gray_500));
        connectionDetails.setPadding(0, UiUtils.dp(context, 8), 0, 0);
        connectionDetails.setMaxLines(3);
        connectionDetails.setEllipsize(android.text.TextUtils.TruncateAt.END);

        // Add divider line
        View divider = new View(context);
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                UiUtils.dp(context, 1)
        );
        dividerParams.topMargin = UiUtils.dp(context, 10);
        dividerParams.bottomMargin = UiUtils.dp(context, 10);
        divider.setLayoutParams(dividerParams);
        divider.setBackgroundColor(context.getColor(R.color.gray_700));

        mainLayout.addView(topRow);
        mainLayout.addView(middleRow);
        mainLayout.addView(securityRow);
        mainLayout.addView(divider);
        mainLayout.addView(connectionDetails);
        card.addView(mainLayout);

        return new ViewHolder(card, title, statusDot, configNumber,
                protocolBadge, serverAddress, connectionDetails, securityInfo);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        int adapterPosition = holder.getAdapterPosition();
        if (adapterPosition == RecyclerView.NO_POSITION) {
            return;
        }

        VpnConfig config = configs.get(adapterPosition);
        ProfileItem profile = config.getProfile();

        // Set title
        String displayName = profile.getRemarks();
        if (displayName == null || displayName.isEmpty()) {
            displayName = config.getName();
        }
        holder.title.setText(displayName);
        holder.configNumber.setText(String.valueOf(adapterPosition + 1));

        // Set protocol badge with proper color
        EConfigType configType = profile.getConfigType();
        String protocolName = configType.getProtocolScheme().toUpperCase();
        holder.protocolBadge.setText(protocolName);

        // Set badge color based on protocol
        int badgeColor = getProtocolColor(configType);
        holder.protocolBadge.setBackgroundColor(badgeColor);

        // Set server address
        String server = profile.getServer();
        String port = profile.getServerPort();
        if (server != null && !server.isEmpty()) {
            String serverDisplay = port != null && !port.isEmpty() ?
                    server + ":" + port : server;
            holder.serverAddress.setText("🌐 " + serverDisplay);
        } else {
            holder.serverAddress.setText("❌ No server info");
        }

        // Build security info
        StringBuilder securityInfo = new StringBuilder();

        String security = profile.getSecurity();
        if (security != null && !security.isEmpty()) {
            securityInfo.append("🔒 ").append(security.toUpperCase());
        }

        String method = profile.getMethod();
        if (method != null && !method.isEmpty()) {
            if (securityInfo.length() > 0) securityInfo.append("  •  ");
            securityInfo.append("🔑 ").append(method);
        }

        String flow = profile.getFlow();
        if (flow != null && !flow.isEmpty()) {
            if (securityInfo.length() > 0) securityInfo.append("  •  ");
            securityInfo.append("📊 ").append(flow);
        }

        holder.securityInfo.setText(securityInfo.toString());

        // Build connection details
        StringBuilder details = new StringBuilder();

        // Network type
        String network = profile.getNetwork();
        if (network != null && !network.isEmpty()) {
            String networkIcon = getNetworkIcon(network);
            details.append(networkIcon).append(" Network: ").append(network.toUpperCase());
        }

        // SNI
        String sni = profile.getSni();
        if (sni != null && !sni.isEmpty()) {
            if (details.length() > 0) details.append("  │  ");
            details.append("🎯 SNI: ").append(sni);
        }

        // Host
        String host = profile.getHost();
        if (host != null && !host.isEmpty() && !host.equals(server)) {
            if (details.length() > 0) details.append("  │  ");
            details.append("🏷️ Host: ").append(host);
        }

        // Path
        String path = profile.getPath();
        if (path != null && !path.isEmpty()) {
            if (details.length() > 0) details.append("  │  ");
            String shortPath = path.length() > 25 ? path.substring(0, 25) + "..." : path;
            details.append("📁 Path: ").append(shortPath);
        }

        // Additional info
        String alpn = profile.getAlpn();
        if (alpn != null && !alpn.isEmpty()) {
            if (details.length() > 0) details.append("  │  ");
            details.append("🔄 ALPN: ").append(alpn);
        }

        String fingerPrint = profile.getFingerPrint();
        if (fingerPrint != null && !fingerPrint.isEmpty()) {
            if (details.length() > 0) details.append("  │  ");
            details.append("🖐️ FP: ").append(fingerPrint);
        }

        if (details.length() == 0) {
            // If no details, show a summary
            details.append("📡 ").append(configType.getProtocolScheme().toUpperCase());
            if (server != null) {
                details.append("  │  ").append(server);
            }
        }

        holder.connectionDetails.setText(details.toString());

        // Update UI based on selection state
        LinearLayout mainLayout = (LinearLayout) holder.card.getChildAt(0);

        if (selectedPosition == adapterPosition) {
            holder.card.setStrokeColor(context.getColor(R.color.purple_500));
            holder.card.setStrokeWidth(UiUtils.dp(context, 2));
            holder.card.setCardElevation(UiUtils.dp(context, 8));
            mainLayout.setBackgroundResource(R.drawable.config_item_selected);
            holder.title.setTextColor(context.getColor(R.color.purple_500));
            holder.serverAddress.setTextColor(context.getColor(R.color.purple_400));
            holder.securityInfo.setTextColor(context.getColor(R.color.purple_400));
            holder.connectionDetails.setTextColor(context.getColor(R.color.purple_300));
            holder.statusDot.setBackgroundResource(R.drawable.status_indicator_connected);
            holder.configNumber.setBackgroundResource(R.drawable.config_number_badge_selected);
            holder.configNumber.setTextColor(context.getColor(android.R.color.white));
        } else {
            holder.card.setStrokeWidth(0);
            holder.card.setCardElevation(UiUtils.dp(context, 2));
            mainLayout.setBackgroundResource(R.drawable.config_item_background);
            holder.title.setTextColor(context.getColor(android.R.color.white));
            holder.serverAddress.setTextColor(context.getColor(R.color.gray_300));
            holder.securityInfo.setTextColor(context.getColor(R.color.gray_400));
            holder.connectionDetails.setTextColor(context.getColor(R.color.gray_500));
            holder.statusDot.setBackgroundResource(R.drawable.status_indicator_disconnected);
            holder.configNumber.setBackgroundResource(R.drawable.config_number_badge);
            holder.configNumber.setTextColor(context.getColor(android.R.color.white));
        }

        holder.itemView.setOnClickListener(v -> {
            int previousSelected = selectedPosition;
            int currentPosition = holder.getAdapterPosition();

            if (currentPosition == RecyclerView.NO_POSITION) {
                return;
            }

            selectedPosition = currentPosition;

            if (previousSelected != RecyclerView.NO_POSITION) {
                notifyItemChanged(previousSelected);
            }
            notifyItemChanged(currentPosition);

            if (listener != null) {
                listener.onConfigSelected(config);
            }
        });
    }

    private int getProtocolColor(EConfigType configType) {
        switch (configType) {
            case VLESS:
                return context.getColor(R.color.vless_color);
            case VMESS:
                return context.getColor(R.color.vmess_color);
            case TROJAN:
                return context.getColor(R.color.trojan_color);
            case SHADOWSOCKS:
                return context.getColor(R.color.shadowsocks_color);
            case WIREGUARD:
                return context.getColor(R.color.wireguard_color);
            case HYSTERIA:
            case HYSTERIA2:
                return context.getColor(R.color.hysteria_color);
            default:
                return context.getColor(R.color.default_protocol_color);
        }
    }

    private String getNetworkIcon(String network) {
        switch (network.toLowerCase()) {
            case "tcp":
                return "🔗";
            case "ws":
            case "websocket":
                return "🌐";
            case "grpc":
                return "⚡";
            case "http":
                return "🌍";
            case "kcp":
                return "📶";
            case "quic":
                return "🚀";
            default:
                return "🔌";
        }
    }

    public int getSelectedPosition() {
        return selectedPosition;
    }

    @Override
    public int getItemCount() {
        return configs.size();
    }
}