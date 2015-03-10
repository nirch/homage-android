package com.homage.app.Utils;

import android.content.Context;

/**
 * Created by dangal on 3/10/15.
 */
public class conversions {

    public static int pixelsToDp(Context context, int pixels){
        float scale = context.getResources().getDisplayMetrics().density;
        int dpAsPixels = (int) (pixels*scale + 0.5f);
        return dpAsPixels;
    }
}
