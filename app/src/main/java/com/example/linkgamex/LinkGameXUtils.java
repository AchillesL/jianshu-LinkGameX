package com.example.linkgamex;

import android.content.res.Resources;

/**
 * Created by Achilles on 2017/12/31.
 */

public class LinkGameXUtils {

    /*截图相关action*/
    public static final String ACTION_START_SCREEN_SHOT = "ACTION_START_SCREEN_SHOT";
    public static final String ACTION_SCREEN_SHOT_FINISH = "ACTION_SCREEN_SHOT_FINISH";

    public static final String ACTION_TO_FILLING = "ACTION_TO_FILLING";
    public static final String ACTION_FILLING_COMPLETE = "ACTION_FILLING_COMPLETE";

    public static final String INTENT_SCREEN_SHOT = "INTENT_SCREEN_SHOT";
    public static final String INTENT_LOC_POINT_LIST = "INTENT_LOC_POINT_LIST";

    public static final int ROW = 7;
    public static final int COL = 12;

    public static final int RECT_X = 135;
    public static final int RECT_Y = 155;
    public static final int RECT_WIDTH = 1648;
    public static final int RECT_HEIGH = 896;

    public static final String APP_CACHE_DIR = "/storage/emulated/0/linkGameX/";

    public static final int SMALL_SIZE_WIDTH = 280;
    public static final int SMALL_SIZE_HIGH = 150;

    public static int getScreenWidth() {
        return Resources.getSystem().getDisplayMetrics().widthPixels;
    }

    public static int getScreenHeight() {
        return Resources.getSystem().getDisplayMetrics().heightPixels;
    }

    public static int[] getRowColFromKey(String key) {
        int row = Integer.parseInt(key.length() == 3 ? key.substring(2, 3) : key.substring(1, 2));
        int col = Integer.parseInt(key.length() == 3 ? key.substring(0, 2) : key.substring(0, 1));
        return new int[]{row+1, col+1};
    }
}
