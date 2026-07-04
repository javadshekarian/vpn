package com.nullstorm.vpn.utils;

import android.content.Context;
import android.util.TypedValue;

public class UiUtils {

    public static int dp(Context c, int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                c.getResources().getDisplayMetrics()
        );
    }

    public static int sp(Context c, int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                value,
                c.getResources().getDisplayMetrics()
        );
    }
}