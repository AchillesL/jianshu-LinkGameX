package com.example.linkgamex;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class LinkGameXService extends Service {

    private static final int MESSAGE_COMPLETE = 0;
    private static final int MESSAGE_NO_SOLVE = 1;

    private WindowManager.LayoutParams mParams;
    private WindowManager mWindowManager;

    private Button mBtn;
    private LinearLayout mLinearLayout;

    private LocalReceiver mLocalReceiver;
    private LocalBroadcastManager mLocalBroadcastManager;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MESSAGE_COMPLETE: {
                    Toast.makeText(LinkGameXService.this, "模拟点击中..", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(LinkGameXUtils.ACTION_TO_FILLING);
                    ArrayList<LocInfo> pairs = (ArrayList<LocInfo>) msg.obj;
                    intent.putParcelableArrayListExtra(LinkGameXUtils.INTENT_LOC_POINT_LIST, pairs);
                    mLocalBroadcastManager.sendBroadcast(intent);
                    break;
                }
                case MESSAGE_NO_SOLVE: {
                    Toast.makeText(LinkGameXService.this, "无法一次性完成，请换另一题!", Toast.LENGTH_LONG).show();
                    mLinearLayout.setVisibility(View.VISIBLE);
                    break;
                }
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mParams = new WindowManager.LayoutParams();
        mWindowManager = (WindowManager) getApplication().getSystemService(Context.WINDOW_SERVICE);

        initView();
        initBroadcast();
    }

    private void initView() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            mParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        }
        mParams.format = PixelFormat.RGBA_8888;
        mParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;

        mParams.gravity = Gravity.END | Gravity.TOP;
        mParams.x = LinkGameXUtils.getScreenWidth();
        mParams.y = LinkGameXUtils.getScreenHeight();

        mParams.width = LinkGameXUtils.SMALL_SIZE_WIDTH;
        mParams.height = LinkGameXUtils.SMALL_SIZE_HIGH;

        mLinearLayout = (LinearLayout) LayoutInflater.from(getApplication()).inflate(R.layout.layout, null);
        mBtn = mLinearLayout.findViewById(R.id.btn);
        mWindowManager.addView(mLinearLayout, mParams);

        mBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLinearLayout.setVisibility(View.GONE);
                getScreenshotBitmap();
            }
        });
    }

    private void getScreenshotBitmap() {
        Intent intent = new Intent(LinkGameXUtils.ACTION_START_SCREEN_SHOT);
        mLocalBroadcastManager.sendBroadcast(intent);
    }

    private void initBroadcast() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(LinkGameXUtils.ACTION_SCREEN_SHOT_FINISH);
        intentFilter.addAction(LinkGameXUtils.ACTION_FILLING_COMPLETE);

        mLocalReceiver = new LocalReceiver();
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        mLocalBroadcastManager.registerReceiver(mLocalReceiver, intentFilter);
    }

    private class LocalReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() == LinkGameXUtils.ACTION_SCREEN_SHOT_FINISH) {
                final Bitmap bitmap = intent.getParcelableExtra(LinkGameXUtils.INTENT_SCREEN_SHOT);
                Toast.makeText(LinkGameXService.this, "图像识别中..", Toast.LENGTH_SHORT).show();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        LinkGameXAnalyse linkGameXAnalyse = new LinkGameXAnalyse();
                        ArrayList<LocInfo> locInfos =  linkGameXAnalyse.getTouchList(bitmap);

                        if (locInfos == null) {
                            Message message = new Message();
                            message.what = MESSAGE_NO_SOLVE;
                            mHandler.sendMessage(message);
                            return;
                        }

                        Message message = new Message();
                        message.what = MESSAGE_COMPLETE;
                        message.obj = locInfos;
                        mHandler.sendMessage(message);
                    }
                }).start();
            } else if (intent.getAction() == LinkGameXUtils.ACTION_FILLING_COMPLETE) {
                mLinearLayout.setVisibility(View.VISIBLE);
            }
        }
    }

    private void saveBitmap(Bitmap bitmap, String fileName) {
        File cacheDir = new File(LinkGameXUtils.APP_CACHE_DIR);
        if (!cacheDir.exists()) {
            cacheDir.mkdir();
        }
        try {
            File file = new File(LinkGameXUtils.APP_CACHE_DIR + fileName + ".jpg");
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private Bitmap rotateBitmapByDegree(Bitmap bm, int degree) {
        Bitmap returnBm = null;

        // 根据旋转角度，生成旋转矩阵
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        try {
            // 将原始图片按照旋转矩阵进行旋转，并得到新的图片
            returnBm = Bitmap.createBitmap(bm, 0, 0, bm.getHeight(), bm.getWidth(), matrix, true);
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (returnBm == null) {
            returnBm = bm;
        }
        if (bm != returnBm) {
            bm.recycle();
        }
        return returnBm;
    }

    /**
     * 从Assets中读取图片
     */
    private Bitmap getImageFromAssetsFile(String fileName) {
        Bitmap image = null;
        AssetManager am = getResources().getAssets();
        try {
            InputStream is = am.open(fileName);
            image = BitmapFactory.decodeStream(is);
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return image;
    }
}
