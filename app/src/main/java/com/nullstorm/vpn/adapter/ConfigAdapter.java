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
        TextView configNumber;

        public ViewHolder(@NonNull MaterialCardView cardView, TextView title, TextView subtitle, View statusDot, TextView configNumber) {
            super(cardView);
            this.card = cardView;
            this.title = title;
            this.subtitle = subtitle;
            this.statusDot = statusDot;
            this.configNumber = configNumber;
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
        cardParams.bottomMargin = UiUtils.dp(context, 8);
        card.setLayoutParams(cardParams);
        card.setRadius(UiUtils.dp(context, 12));
        card.setCardElevation(0);
        card.setBackgroundColor(context.getColor(android.R.color.transparent));
        card.setStrokeWidth(0);

        LinearLayout mainLayout = new LinearLayout(context);
        mainLayout.setOrientation(LinearLayout.HORIZONTAL);
        mainLayout.setPadding(
                UiUtils.dp(context, 16),
                UiUtils.dp(context, 14),
                UiUtils.dp(context, 16),
                UiUtils.dp(context, 14)
        );
        mainLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);
        mainLayout.setBackgroundResource(R.drawable.config_item_background);

        LinearLayout numberContainer = new LinearLayout(context);
        numberContainer.setOrientation(LinearLayout.VERTICAL);
        numberContainer.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams numberParams = new LinearLayout.LayoutParams(
                UiUtils.dp(context, 32),
                UiUtils.dp(context, 32)
        );
        numberParams.rightMargin = UiUtils.dp(context, 14);
        numberContainer.setLayoutParams(numberParams);
        numberContainer.setBackgroundResource(R.drawable.config_number_badge);

        TextView configNumber = new TextView(context);
        configNumber.setTextSize(UiUtils.sp(context, 4));
        configNumber.setTextColor(context.getColor(android.R.color.white));
        configNumber.setTypeface(null, android.graphics.Typeface.BOLD);
        configNumber.setGravity(android.view.Gravity.CENTER);
        numberContainer.addView(configNumber);

        LinearLayout textContainer = new LinearLayout(context);
        textContainer.setOrientation(LinearLayout.VERTICAL);
        textContainer.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1
        ));

        LinearLayout titleRow = new LinearLayout(context);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(android.view.Gravity.CENTER_VERTICAL);

        TextView title = new TextView(context);
        title.setTextSize(UiUtils.sp(context, 6));
        title.setTextColor(context.getColor(android.R.color.white));
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1
        );
        title.setLayoutParams(titleParams);

        View statusDot = new View(context);
        LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(
                UiUtils.dp(context, 6),
                UiUtils.dp(context, 6)
        );
        dotParams.leftMargin = UiUtils.dp(context, 8);
        statusDot.setLayoutParams(dotParams);
        statusDot.setBackgroundResource(R.drawable.status_indicator_disconnected);

        titleRow.addView(title);
        titleRow.addView(statusDot);

        TextView subtitle = new TextView(context);
        subtitle.setTextSize(UiUtils.sp(context, 5));
        subtitle.setTextColor(context.getColor(R.color.gray_500));
        subtitle.setPadding(0, UiUtils.dp(context, 2), 0, 0);
        subtitle.setMaxLines(1);

        textContainer.addView(titleRow);
        textContainer.addView(subtitle);

        mainLayout.addView(numberContainer);
        mainLayout.addView(textContainer);
        card.addView(mainLayout);

        return new ViewHolder(card, title, subtitle, statusDot, configNumber);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        int adapterPosition = holder.getAdapterPosition();
        if (adapterPosition == RecyclerView.NO_POSITION) {
            return;
        }

        VpnConfig config = configs.get(adapterPosition);
        holder.title.setText(config.getName());
        holder.configNumber.setText(String.valueOf(adapterPosition + 1));

        String content = config.getContent();
        String preview = content.length() > 60 ? content.substring(0, 60) + "..." : content;
        holder.subtitle.setText(preview);

        LinearLayout mainLayout = (LinearLayout) holder.card.getChildAt(0);

        if (selectedPosition == adapterPosition) {
            holder.card.setStrokeColor(context.getColor(R.color.purple_500));
            holder.card.setStrokeWidth(UiUtils.dp(context, 1));
            holder.card.setCardElevation(UiUtils.dp(context, 4));
            mainLayout.setBackgroundResource(R.drawable.config_item_selected);
            holder.title.setTextColor(context.getColor(R.color.purple_500));
            holder.statusDot.setBackgroundResource(R.drawable.status_indicator_connected);
            holder.configNumber.setBackgroundResource(R.drawable.config_number_badge_selected);
            holder.configNumber.setTextColor(context.getColor(android.R.color.white));
        } else {
            holder.card.setStrokeWidth(0);
            holder.card.setCardElevation(0);
            mainLayout.setBackgroundResource(R.drawable.config_item_background);
            holder.title.setTextColor(context.getColor(android.R.color.white));
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

    public int getSelectedPosition(){
        return selectedPosition;
    }

    @Override
    public int getItemCount() {
        return configs.size();
    }
}