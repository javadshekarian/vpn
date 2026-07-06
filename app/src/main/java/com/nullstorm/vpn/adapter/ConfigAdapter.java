package com.nullstorm.vpn.adapter;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying VPN configuration cards with delete functionality
 */
public class ConfigAdapter extends RecyclerView.Adapter<ConfigAdapter.ViewHolder> {

    private final Context context;
    private final List<VpnConfig> configs;
    private final OnConfigClickListener listener;
    private int selectedPosition = RecyclerView.NO_POSITION;

    /**
     * Interface for handling configuration click events
     */
    public interface OnConfigClickListener {
        void onConfigSelected(VpnConfig config);
        void onConfigDeleted(int position, VpnConfig config);
    }

    public ConfigAdapter(Context context,
                         List<VpnConfig> configs,
                         OnConfigClickListener listener) {
        this.context = context;
        this.configs = configs;
        this.listener = listener;
    }

    /**
     * ViewHolder class for configuration cards
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final MaterialCardView card;
        public final TextView title;
        public final View statusDot;
        public final TextView configNumber;
        public final TextView protocolBadge;
        public final TextView serverAddress;
        public final TextView connectionDetails;
        public final TextView securityInfo;
        public final ImageView deleteButton;

        public ViewHolder(@NonNull MaterialCardView card, TextView title, View statusDot,
                          TextView configNumber, TextView protocolBadge, TextView serverAddress,
                          TextView connectionDetails, TextView securityInfo, ImageView deleteButton) {
            super(card);
            this.card = card;
            this.title = title;
            this.statusDot = statusDot;
            this.configNumber = configNumber;
            this.protocolBadge = protocolBadge;
            this.serverAddress = serverAddress;
            this.connectionDetails = connectionDetails;
            this.securityInfo = securityInfo;
            this.deleteButton = deleteButton;
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Create MaterialCardView as the main container
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

        // Main vertical layout for the card content
        LinearLayout mainLayout = new LinearLayout(context);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(
                UiUtils.dp(context, 20),
                UiUtils.dp(context, 18),
                UiUtils.dp(context, 20),
                UiUtils.dp(context, 18)
        );
        mainLayout.setBackgroundResource(R.drawable.config_item_background);

        // Contains: Number | Title + Status | Delete Button
        LinearLayout topRow = new LinearLayout(context);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        // --- Number Container (Circle badge with config number) ---
        LinearLayout numberContainer = new LinearLayout(context);
        numberContainer.setOrientation(LinearLayout.VERTICAL);
        numberContainer.setGravity(Gravity.CENTER);
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
        configNumber.setTypeface(null, Typeface.BOLD);
        configNumber.setGravity(Gravity.CENTER);
        numberContainer.addView(configNumber);

        // --- Title Container ---
        LinearLayout titleContainer = new LinearLayout(context);
        titleContainer.setOrientation(LinearLayout.VERTICAL);
        titleContainer.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1
        ));

        LinearLayout titleRow = new LinearLayout(context);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);

        // Title text
        TextView title = new TextView(context);
        title.setTextSize(UiUtils.sp(context, 7));
        title.setTextColor(context.getColor(android.R.color.white));
        title.setTypeface(null, Typeface.BOLD);
        title.setMaxLines(1);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        title.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1
        ));

        // Status dot (shows connected/disconnected state)
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

        // --- Delete Button (visible only for selected item) ---
        ImageView deleteButton = new ImageView(context);
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(
                UiUtils.dp(context, 32),
                UiUtils.dp(context, 32)
        );
        deleteParams.leftMargin = UiUtils.dp(context, 8);
        deleteButton.setLayoutParams(deleteParams);
        deleteButton.setImageResource(R.drawable.ic_delete);
        deleteButton.setColorFilter(context.getColor(R.color.gray_400));
        deleteButton.setVisibility(View.GONE); // Hidden by default

        topRow.addView(numberContainer);
        topRow.addView(titleContainer);
        topRow.addView(deleteButton);

        // Contains: Protocol Badge | Server Address
        LinearLayout middleRow = new LinearLayout(context);
        middleRow.setOrientation(LinearLayout.HORIZONTAL);
        middleRow.setPadding(0, UiUtils.dp(context, 10), 0, 0);
        middleRow.setGravity(Gravity.CENTER_VERTICAL);

        // Protocol badge (VLESS, VMESS, TROJAN, etc.)
        TextView protocolBadge = new TextView(context);
        protocolBadge.setTextSize(UiUtils.sp(context, 5));
        protocolBadge.setTextColor(context.getColor(android.R.color.white));
        protocolBadge.setTypeface(null, Typeface.BOLD);
        protocolBadge.setPadding(
                UiUtils.dp(context, 12),
                UiUtils.dp(context, 4),
                UiUtils.dp(context, 12),
                UiUtils.dp(context, 4)
        );
        protocolBadge.setBackgroundResource(R.drawable.protocol_badge);
        LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        badgeParams.rightMargin = UiUtils.dp(context, 12);
        protocolBadge.setLayoutParams(badgeParams);

        // Server address with icon
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

        // Contains: Security info (encryption, method, flow)
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

        // Contains: Network type, SNI, Host, Path, etc.
        TextView connectionDetails = new TextView(context);
        connectionDetails.setTextSize(UiUtils.sp(context, 4));
        connectionDetails.setTextColor(context.getColor(R.color.gray_500));
        connectionDetails.setPadding(0, UiUtils.dp(context, 8), 0, 0);
        connectionDetails.setMaxLines(3);
        connectionDetails.setEllipsize(android.text.TextUtils.TruncateAt.END);

        // Divider line
        View divider = new View(context);
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                UiUtils.dp(context, 1)
        );
        dividerParams.topMargin = UiUtils.dp(context, 10);
        dividerParams.bottomMargin = UiUtils.dp(context, 10);
        divider.setLayoutParams(dividerParams);
        divider.setBackgroundColor(context.getColor(R.color.gray_700));

        // Add all rows to main layout
        mainLayout.addView(topRow);
        mainLayout.addView(middleRow);
        mainLayout.addView(securityRow);
        mainLayout.addView(divider);
        mainLayout.addView(connectionDetails);
        card.addView(mainLayout);

        return new ViewHolder(card, title, statusDot, configNumber,
                protocolBadge, serverAddress, connectionDetails, securityInfo, deleteButton);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        int adapterPosition = holder.getAdapterPosition();
        if (adapterPosition == RecyclerView.NO_POSITION) {
            return;
        }

        VpnConfig config = configs.get(adapterPosition);
        ProfileItem profile = config.getProfile();

        String displayName = profile.getRemarks();
        if (displayName == null || displayName.isEmpty()) {
            displayName = config.getName();
        }
        holder.title.setText(displayName);
        holder.configNumber.setText(String.valueOf(adapterPosition + 1));

        EConfigType configType = profile.getConfigType();
        holder.protocolBadge.setText(configType.getProtocolScheme().toUpperCase());
        holder.protocolBadge.setBackgroundColor(getProtocolColor(configType));

        String server = profile.getServer();
        String port = profile.getServerPort();
        if (server != null && !server.isEmpty()) {
            String serverDisplay = (port != null && !port.isEmpty()) ?
                    server + ":" + port : server;
            holder.serverAddress.setText("🌐 " + serverDisplay);
        } else {
            holder.serverAddress.setText("❌ No server info");
        }

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

        StringBuilder details = new StringBuilder();

        // Network type
        String network = profile.getNetwork();
        if (network != null && !network.isEmpty()) {
            details.append(getNetworkIcon(network)).append(" Network: ").append(network.toUpperCase());
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

        // ALPN
        String alpn = profile.getAlpn();
        if (alpn != null && !alpn.isEmpty()) {
            if (details.length() > 0) details.append("  │  ");
            details.append("🔄 ALPN: ").append(alpn);
        }

        // Fingerprint
        String fingerPrint = profile.getFingerPrint();
        if (fingerPrint != null && !fingerPrint.isEmpty()) {
            if (details.length() > 0) details.append("  │  ");
            details.append("🖐️ FP: ").append(fingerPrint);
        }

        // If no details, show a summary
        if (details.length() == 0) {
            details.append("📡 ").append(configType.getProtocolScheme().toUpperCase());
            if (server != null) {
                details.append("  │  ").append(server);
            }
        }

        holder.connectionDetails.setText(details.toString());
        LinearLayout mainLayout = (LinearLayout) holder.card.getChildAt(0);

        if (selectedPosition == adapterPosition) {
            // Selected state - highlight the card
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

            // Show delete button for selected item
            holder.deleteButton.setVisibility(View.VISIBLE);
            holder.deleteButton.setColorFilter(context.getColor(R.color.purple_500));
        } else {
            // Unselected state - normal appearance
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

            // Hide delete button for unselected items
            holder.deleteButton.setVisibility(View.GONE);
        }

        // Click listener for selecting a config
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int currentPosition = holder.getAdapterPosition();
                if (currentPosition == RecyclerView.NO_POSITION) {
                    return;
                }

                int previousSelected = selectedPosition;
                selectedPosition = currentPosition;

                // Update previous selected item
                if (previousSelected != RecyclerView.NO_POSITION) {
                    notifyItemChanged(previousSelected);
                }
                // Update current selected item
                notifyItemChanged(currentPosition);

                if (listener != null) {
                    listener.onConfigSelected(config);
                }
            }
        });

        // Click listener for delete button
        holder.deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int currentPosition = holder.getAdapterPosition();
                if (currentPosition == RecyclerView.NO_POSITION) {
                    return;
                }
                deleteItem(currentPosition);
            }
        });
    }

    /**
     * Delete a configuration item from the list
     * @param position Position of the item to delete
     */
    private void deleteItem(int position) {
        if (position < 0 || position >= configs.size()) {
            return;
        }

        VpnConfig config = configs.get(position);

        // Remove from list
        configs.remove(position);

        // Update selected position
        if (selectedPosition == position) {
            selectedPosition = RecyclerView.NO_POSITION;
        } else if (selectedPosition > position) {
            selectedPosition--;
        }

        // Notify adapter about the removal
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, configs.size() - position);

        // Call listener
        if (listener != null) {
            listener.onConfigDeleted(position, config);
        }
    }

    /**
     * Get color for protocol badge based on protocol type
     */
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

    /**
     * Get appropriate icon for network type
     */
    private String getNetworkIcon(String network) {
        if (network == null) return "🔌";

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

    /**
     * Get the currently selected position
     */
    public int getSelectedPosition() {
        return selectedPosition;
    }

    /**
     * Get the currently selected configuration
     */
    public VpnConfig getSelectedConfig() {
        if (selectedPosition != RecyclerView.NO_POSITION && selectedPosition < configs.size()) {
            return configs.get(selectedPosition);
        }
        return null;
    }

    /**
     * Update the entire list of configurations
     */
    public void updateConfigs(List<VpnConfig> newConfigs) {
        this.configs.clear();
        this.configs.addAll(newConfigs);
        selectedPosition = RecyclerView.NO_POSITION;
        notifyDataSetChanged();
    }

    /**
     * Add a new configuration to the list
     */
    public void addConfig(VpnConfig config) {
        configs.add(config);
        notifyItemInserted(configs.size() - 1);
    }

    public void setSelectedPosition(int position) {
        selectedPosition = position;
        notifyDataSetChanged();
    }

    /**
     * Remove a configuration by position
     */
    public void removeConfig(int position) {
        deleteItem(position);
    }

    @Override
    public int getItemCount() {
        return configs.size();
    }
}