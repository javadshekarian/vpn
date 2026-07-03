package com.nullstorm.vpn.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.nullstorm.vpn.R;
import com.nullstorm.vpn.model.VpnConfig;
import com.nullstorm.vpn.ui.UiUtils;

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
        TextView subtitle;
        View statusDot;
        MaterialCardView card;

        public ViewHolder(@NonNull MaterialCardView cardView, TextView title, TextView subtitle, View statusDot) {
            super(cardView);
            this.card = cardView;
            this.title = title;
            this.subtitle = subtitle;
            this.statusDot = statusDot;
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
        card.setRadius(UiUtils.dp(context, 20));
        card.setCardElevation(UiUtils.dp(context, 2));
        card.setBackgroundColor(context.getColor(R.color.gray_700));

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setPadding(
                UiUtils.dp(context, 20),
                UiUtils.dp(context, 16),
                UiUtils.dp(context, 20),
                UiUtils.dp(context, 16)
        );
        layout.setGravity(android.view.Gravity.CENTER_VERTICAL);

        View statusDot = new View(context);
        LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(
                UiUtils.dp(context, 8),
                UiUtils.dp(context, 8)
        );
        dotParams.rightMargin = UiUtils.dp(context, 16);
        statusDot.setLayoutParams(dotParams);
        statusDot.setBackgroundResource(R.drawable.status_indicator_disconnected);

        LinearLayout textContainer = new LinearLayout(context);
        textContainer.setOrientation(LinearLayout.VERTICAL);
        textContainer.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1
        ));

        TextView title = new TextView(context);
        title.setTextSize(UiUtils.sp(context, 16));
        title.setTextColor(context.getColor(android.R.color.white));
        title.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView subtitle = new TextView(context);
        subtitle.setTextSize(UiUtils.sp(context, 12));
        subtitle.setTextColor(context.getColor(R.color.gray_400));
        subtitle.setPadding(0, UiUtils.dp(context, 4), 0, 0);

        textContainer.addView(title);
        textContainer.addView(subtitle);

        TextView arrow = new TextView(context);
        arrow.setText("›");
        arrow.setTextSize(UiUtils.sp(context, 24));
        arrow.setTextColor(context.getColor(R.color.gray_400));
        arrow.setPadding(UiUtils.dp(context, 16), 0, 0, 0);

        layout.addView(statusDot);
        layout.addView(textContainer);
        layout.addView(arrow);
        card.addView(layout);

        return new ViewHolder(card, title, subtitle, statusDot);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        int adapterPosition = holder.getAdapterPosition();
        if (adapterPosition == RecyclerView.NO_POSITION) {
            return;
        }

        VpnConfig config = configs.get(adapterPosition);
        holder.title.setText(config.getName());

        String content = config.getContent();
        String preview = content.length() > 50 ? content.substring(0, 50) + "..." : content;
        holder.subtitle.setText(preview);

        if (selectedPosition == adapterPosition) {
            holder.card.setStrokeColor(context.getColor(R.color.purple_500));
            holder.card.setStrokeWidth(UiUtils.dp(context, 2));
            holder.statusDot.setBackgroundResource(R.drawable.status_indicator_connected);
        } else {
            holder.card.setStrokeWidth(0);
            holder.statusDot.setBackgroundResource(R.drawable.status_indicator_disconnected);
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

    @Override
    public int getItemCount() {
        return configs.size();
    }
}