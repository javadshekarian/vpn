package com.nullstorm.vpn.ui;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toolbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.nullstorm.vpn.utils.UiUtils;

public class LoginActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle saveInstanceState) {
        super.onCreate(saveInstanceState);

        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setBackgroundColor(ContextCompat.getColor(this, android.R.color.black));
        mainLayout.setPadding(
                UiUtils.dp(this, 16),
                UiUtils.dp(this, 24),
                UiUtils.dp(this, 16),
                UiUtils.dp(this, 24)
        );
        mainLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        Toolbar toolbar = new Toolbar(this);
        toolbar.setBackgroundColor(ContextCompat.getColor(this, android.R.color.black));
        toolbar.setElevation(UiUtils.dp(this, 4));
        toolbar.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                UiUtils.dp(this, 30)
        ));
        mainLayout.addView(toolbar);

        Button backButton = new Button(this);
        backButton.setText("← Back to VPN");
        backButton.setTextSize(UiUtils.sp(this, 6));
        backButton.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent));
        backButton.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        backButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                finish();
            }
        });

        LinearLayout.LayoutParams backParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        backParams.setMargins(0, UiUtils.dp(this, 8), 0, UiUtils.dp(this, 16));
        backButton.setLayoutParams(backParams);
        mainLayout.addView(backButton);

        LinearLayout contentLayout = new LinearLayout(this);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setGravity(Gravity.CENTER);
        contentLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
        ));

        TextView placeholderText = new TextView(this);
        placeholderText.setText("Login Page\n\n(Design Comming Soon");
        placeholderText.setTextSize(UiUtils.sp(this, 8));
        placeholderText.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        placeholderText.setGravity(Gravity.CENTER);
        placeholderText.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        contentLayout.addView(placeholderText);
        mainLayout.addView(contentLayout);
        setContentView(mainLayout);
    }
}






























