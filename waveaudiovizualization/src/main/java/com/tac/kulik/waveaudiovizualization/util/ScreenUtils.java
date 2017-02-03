package com.tac.kulik.waveaudiovizualization.util;

import android.content.Context;
import android.util.TypedValue;

/**
 * Created by kulik on 31.01.17.
 */
public class ScreenUtils {

    public static int dp2px(Context context, int dp) {
        int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
        return px;
    }
}
