package com.nullstorm.vpn.ui.stateless;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.navigation.NavigationView;
import com.nullstorm.vpn.R;
import com.nullstorm.vpn.utils.UiUtils;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.Build;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.GravityCompat;

public class MainUI {

    public static View createHeaderSection(Context context) {
        LinearLayout headerLayout = new LinearLayout(context);
        headerLayout.setOrientation(LinearLayout.VERTICAL);
        headerLayout.setGravity(Gravity.CENTER);

        TextView title = new TextView(context);
        title.setText(R.string.vpn_name);
        title.setTextSize(UiUtils.sp(context, 10));
        title.setTextColor(ContextCompat.getColor(context, android.R.color.white));
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setGravity(Gravity.CENTER);

        TextView subtitle = new TextView(context);
        subtitle.setText(R.string.vpn_subtitle);
        subtitle.setTextSize(UiUtils.sp(context, 4));
        subtitle.setTextColor(ContextCompat.getColor(context, R.color.gray_400));
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, UiUtils.dp(context, 4), 0, 0);

        headerLayout.addView(title);
        headerLayout.addView(subtitle);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.bottomMargin = UiUtils.dp(context, 0);
        headerLayout.setLayoutParams(params);

        return headerLayout;
    }

    // متد جدید برای ساخت دکمه‌های import
    public static View createImportButton(Context context, View.OnClickListener importListener, View.OnClickListener freeListener) {
        LinearLayout buttonContainer = new LinearLayout(context);
        buttonContainer.setOrientation(LinearLayout.HORIZONTAL);
        buttonContainer.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        buttonContainer.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams containerParams = (LinearLayout.LayoutParams) buttonContainer.getLayoutParams();
        containerParams.bottomMargin = UiUtils.dp(context, 20);
        buttonContainer.setLayoutParams(containerParams);

        com.google.android.material.button.MaterialButton importButton = new com.google.android.material.button.MaterialButton(context);
        LinearLayout.LayoutParams importParams = new LinearLayout.LayoutParams(
                0,
                UiUtils.dp(context, 50),
                1
        );
        importParams.rightMargin = UiUtils.dp(context, 8);
        importButton.setLayoutParams(importParams);
        importButton.setText(R.string.vpn_import_config);
        importButton.setTextSize(UiUtils.sp(context, 5));
        importButton.setTextColor(ContextCompat.getColor(context, R.color.gray_400));
        importButton.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
        importButton.setStrokeColorResource(R.color.gray_600);
        importButton.setStrokeWidth(UiUtils.dp(context, 1));
        importButton.setCornerRadius(UiUtils.dp(context, 25));
        importButton.setOnClickListener(importListener);

        com.google.android.material.button.MaterialButton freeConfigButton = new com.google.android.material.button.MaterialButton(context);
        LinearLayout.LayoutParams freeParams = new LinearLayout.LayoutParams(
                0,
                UiUtils.dp(context, 50),
                1
        );
        freeParams.leftMargin = UiUtils.dp(context, 8);
        freeConfigButton.setLayoutParams(freeParams);
        freeConfigButton.setText("Free Config");
        freeConfigButton.setTextSize(UiUtils.sp(context, 5));
        freeConfigButton.setTextColor(ContextCompat.getColor(context, R.color.purple_500));
        freeConfigButton.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
        freeConfigButton.setStrokeColorResource(R.color.purple_500);
        freeConfigButton.setStrokeWidth(UiUtils.dp(context, 1));
        freeConfigButton.setCornerRadius(UiUtils.dp(context, 25));
        freeConfigButton.setOnClickListener(freeListener);

        buttonContainer.addView(importButton);
        buttonContainer.addView(freeConfigButton);

        return buttonContainer;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static DrawerLayout createDrawer(
            AppCompatActivity activity,
            NavigationView.OnNavigationItemSelectedListener navListener,
            View connectionStatus,
            View connectSection,
            View importButton,
            View configList
    ) {
        DrawerLayout drawerLayout = new DrawerLayout(activity);
        drawerLayout.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        LinearLayout mainContent = new LinearLayout(activity);
        mainContent.setOrientation(LinearLayout.VERTICAL);
        mainContent.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        // Toolbar
        Toolbar toolbar = new Toolbar(activity);
        toolbar.setBackgroundColor(Color.BLACK);
        toolbar.setElevation(UiUtils.dp(activity, 4));

        LinearLayout.LayoutParams toolbarParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        UiUtils.dp(activity, 30)
                );
        toolbar.setLayoutParams(toolbarParams);

        mainContent.addView(toolbar);

        // ✅ ROOT LAYOUT - اینجا تغییر اصلی
        LinearLayout rootLayout = new LinearLayout(activity);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setPadding(
                UiUtils.dp(activity, 10),
                UiUtils.dp(activity, 16),
                UiUtils.dp(activity, 10),
                UiUtils.dp(activity, 24)
        );
        rootLayout.setBackgroundColor(
                ContextCompat.getColor(activity, android.R.color.black)
        );

        // ✅ مهم: به rootLayout وزن 1 بدید تا تمام فضای باقیمونده رو بگیره
        LinearLayout.LayoutParams rootParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,  // ارتفاع صفر
                1f  // وزن 1
        );
        rootLayout.setLayoutParams(rootParams);

        // افزودن ویوها به rootLayout
        rootLayout.addView(createHeaderSection(activity));
        rootLayout.addView(connectionStatus);
        rootLayout.addView(connectSection);
        rootLayout.addView(importButton);
        rootLayout.addView(configList);  // RecyclerView اینجا اضافه میشه

        mainContent.addView(rootLayout);
        drawerLayout.addView(mainContent);

        // Navigation View
        NavigationView navigationView = new NavigationView(activity);
        navigationView.setNavigationItemSelectedListener(navListener);

        DrawerLayout.LayoutParams navParams =
                new DrawerLayout.LayoutParams(
                        UiUtils.dp(activity, 280),
                        ViewGroup.LayoutParams.MATCH_PARENT
                );

        navParams.gravity = GravityCompat.START;
        navigationView.setLayoutParams(navParams);
        navigationView.setBackgroundColor(Color.BLACK);
        navigationView.inflateMenu(R.menu.navigation_menu);
        navigationView.setItemTextColor(
                ColorStateList.valueOf(Color.WHITE)
        );

        // Header Navigation
        LinearLayout headerView = new LinearLayout(activity);
        headerView.setOrientation(LinearLayout.VERTICAL);
        headerView.setGravity(Gravity.CENTER);
        headerView.setMinimumHeight(UiUtils.dp(activity, 100));
        headerView.setBackgroundColor(Color.parseColor("#1A1A1A"));

        TextView headerTitle = new TextView(activity);
        headerTitle.setText("NullStorm VPN");
        headerTitle.setTextSize(UiUtils.sp(activity, 8));
        headerTitle.setTextColor(Color.WHITE);

        Typeface typeface =
                ResourcesCompat.getFont(
                        activity,
                        R.font.barlowcondensed_medium
                );

        headerTitle.setTypeface(typeface);
        headerTitle.setGravity(Gravity.CENTER);

        headerView.addView(headerTitle);
        navigationView.addHeaderView(headerView);

        drawerLayout.addView(navigationView);

        // Toggle
        ActionBarDrawerToggle toggle =
                new ActionBarDrawerToggle(
                        activity,
                        drawerLayout,
                        toolbar,
                        R.string.vpn_menu_open,
                        R.string.vpn_menu_close
                );

        toggle.getDrawerArrowDrawable().setColor(Color.WHITE);

        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        return drawerLayout;
    }
}