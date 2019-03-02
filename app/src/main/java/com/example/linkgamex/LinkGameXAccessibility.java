package com.example.linkgamex;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Path;
import android.graphics.Point;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import java.util.ArrayList;

public class LinkGameXAccessibility extends AccessibilityService {
    private static final String TAG = "LinkGameXAccessibility";

    private LocalReceiver mLocalReceiver;
    private LocalBroadcastManager mLocalBroadcastManager;

    private ArrayList<ArrayList<Point>> mPoints = new ArrayList<>();
    private ArrayList<LocInfo> mLocInfos;

    @Override
    public void onCreate() {
        super.onCreate();

        initPanelPointList();
        initBroadcast();
    }

    private void initPanelPointList() {
        double width = LinkGameXUtils.RECT_WIDTH * 1.0 / LinkGameXUtils.COL;
        double high = LinkGameXUtils.RECT_HEIGH * 1.0 / LinkGameXUtils.ROW;

        for (int i = 0; i < LinkGameXUtils.ROW; i++) {
            ArrayList<Point> points = new ArrayList<>();
            for (int j = 0; j < LinkGameXUtils.COL; j++) {
                int x = (int) (LinkGameXUtils.RECT_Y + width * j + width / 2);
                int y = (int) (LinkGameXUtils.RECT_X + high * i + high / 2);

                points.add(new Point(x, y));
            }
            mPoints.add(points);
        }
    }

    private void initBroadcast() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(LinkGameXUtils.ACTION_TO_FILLING);

        mLocalReceiver = new LocalReceiver();
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        mLocalBroadcastManager.registerReceiver(mLocalReceiver, intentFilter);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        Log.d(TAG, "onAccessibilityEvent: " + event.toString());
    }

    @Override
    public void onInterrupt() {

    }

    public void dispatchGestureView(int startTime, int x, int y) {
        Point position = new Point(x, y);
        GestureDescription.Builder builder = new GestureDescription.Builder();
        Path p = new Path();
        p.moveTo(position.x, position.y);
        /**
         * StrokeDescription参数：
         * path：笔画路径
         * startTime：时间 (以毫秒为单位)，从手势开始到开始笔划的时间，非负数
         * duration：笔划经过路径的持续时间(以毫秒为单位)，非负数*/
        builder.addStroke(new GestureDescription.StrokeDescription(p, startTime, 1));
        dispatchGesture(builder.build(), null, null);
    }

    private class LocalReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() == LinkGameXUtils.ACTION_TO_FILLING) {
                mLocInfos = intent.getParcelableArrayListExtra(LinkGameXUtils.INTENT_LOC_POINT_LIST);
                mHandler.sendEmptyMessage(0);
            }
        }
    }

    private Handler mHandler = new Handler(new Handler.Callback() {
        int i = 0;
        @Override
        public boolean handleMessage(Message msg) {
            if (i < mLocInfos.size()) {
                LocInfo locInfo = mLocInfos.get(i);
                Point point = mPoints.get(locInfo.x - 1).get(locInfo.y - 1);
                dispatchGestureView(0, point.x, point.y);

                i++;
                mHandler.sendEmptyMessageDelayed(0, 50);
            } else {
                i = 0;
                mHandler.removeCallbacksAndMessages(null);
                mLocalBroadcastManager.sendBroadcast(new Intent(LinkGameXUtils.ACTION_FILLING_COMPLETE));
            }
            return false;
        }
    });
}
