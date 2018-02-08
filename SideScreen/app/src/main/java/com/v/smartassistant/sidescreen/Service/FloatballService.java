package com.v.smartassistant.sidescreen.Service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import com.v.smartassistant.sidescreen.FloatManager.FloatBallManager;
import com.v.smartassistant.sidescreen.GridUtils.Utils;

/**
 * Created by lishuangwei on 17-12-7.
 */

public class FloatballService extends Service {
    private FloatBallManager mFloatballManager;
    private SharedPreferences mShare;
    private SharedPreferences.Editor mEditor;
    private boolean mIsShow;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case Utils.HIDE_FLOAT:
                    Log.d("shuang", "handleMessage: hide");
                    showFloat(false);
                    mIsShow = false;
                    mEditor.putBoolean("show", mIsShow);
                    mEditor.commit();
                    break;
                case Utils.SHOW_FLOAT:
                    if (mFloatballManager.getShowing()) return;
                    Log.d("shuang", "handleMessage: show");
                    showFloat(true);
                    mIsShow = true;
                    mEditor.putBoolean("show", mIsShow);
                    mEditor.commit();
                    break;
                case Utils.MOVE_FROM_LEFT:
                    Log.d("shuang", "handleMessage: MOVE_FROM_LEFT");
                    mFloatballManager.updatePostion(false);
                    showFloat(false);
                    if (mIsShow) showFloat(true);
                    break;
                case Utils.MOVE_FROM_RIGHT:
                    Log.d("shuang", "handleMessage: MOVE_FROM_RIGHT");
                    mFloatballManager.updatePostion(true);
                    showFloat(false);
                    if (mIsShow) showFloat(true);
                    break;
                case Utils.MOVE_LEFT_TOP:
                    mFloatballManager.updatePostion(false, Utils.INIT_TOUCH_POSITION - Utils.OFFSET_TOUCH_POSITION);
                    break;
                case Utils.MOVE_LEFT_CENTER:
                    mFloatballManager.updatePostion(false, Utils.INIT_TOUCH_POSITION);
                    break;
                case Utils.MOVE_LEFT_BOTTOM:
                    mFloatballManager.updatePostion(false, Utils.INIT_TOUCH_POSITION + Utils.OFFSET_TOUCH_POSITION);
                    break;
                case Utils.MOVE_RIGHT_TOP:
                    mFloatballManager.updatePostion(true, Utils.INIT_TOUCH_POSITION - Utils.OFFSET_TOUCH_POSITION);
                    break;
                case Utils.MOVE_RIGHT_CENTER:
                    mFloatballManager.updatePostion(true, Utils.INIT_TOUCH_POSITION);
                    break;
                case Utils.MOVE_RIGHT_BOTTOM:
                    mFloatballManager.updatePostion(true, Utils.INIT_TOUCH_POSITION + Utils.OFFSET_TOUCH_POSITION);
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        init();
    }

    private void init() {
        mFloatballManager = new FloatBallManager(this);

        IntentFilter myIntentFilter = new IntentFilter();
        myIntentFilter.addAction(Utils.ACTION_SHOW_FLOAT);
        myIntentFilter.addAction(Utils.ACTION_HIDE_FLOAT);
        myIntentFilter.addAction(Utils.ACTION_MOVR_LEFT);
        myIntentFilter.addAction(Utils.ACTION_MOVR_RIGHT);
        myIntentFilter.addAction(Utils.ACTION_LEFT_TOP);
        myIntentFilter.addAction(Utils.ACTION_LEFT_CENTER);
        myIntentFilter.addAction(Utils.ACTION_LEFT_BOTTOM);
        myIntentFilter.addAction(Utils.ACTION_RIGHT_TOP);
        myIntentFilter.addAction(Utils.ACTION_RIGHT_CENTER);
        myIntentFilter.addAction(Utils.ACTION_RIGHT_BOTTOM);
        registerReceiver(mReceiver, myIntentFilter);

        mShare = getSharedPreferences("touch_position", MODE_PRIVATE);
        mEditor = mShare.edit();
    }


    private void showFloat(boolean show) {
        if (show) {
            mFloatballManager.show();
        } else {
            mFloatballManager.hide();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mIsShow = mShare.getBoolean("show", false);
        if (null != intent) {
            boolean ishow = intent.getBooleanExtra("showfloat", false);
            if (ishow) mHandler.sendEmptyMessage(Utils.SHOW_FLOAT);
        } else {
            if (mIsShow) mHandler.sendEmptyMessage(Utils.SHOW_FLOAT);
        }
        Log.d("shuang", "onStartCommand");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.d("shuang", "onDestroy");
        super.onDestroy();
        unregisterReceiver(mReceiver);
        mHandler.sendEmptyMessage(Utils.HIDE_FLOAT);
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Utils.ACTION_MOVR_LEFT)) {
                mHandler.sendEmptyMessage(Utils.MOVE_FROM_LEFT);
            } else if (action.equals(Utils.ACTION_MOVR_RIGHT)) {
                mHandler.sendEmptyMessage(Utils.MOVE_FROM_RIGHT);
            } else if (action.equals(Utils.ACTION_LEFT_TOP)) {
                mHandler.sendEmptyMessage(Utils.MOVE_LEFT_TOP);
            } else if (action.equals(Utils.ACTION_LEFT_CENTER)) {
                mHandler.sendEmptyMessage(Utils.MOVE_LEFT_CENTER);
            } else if (action.equals(Utils.ACTION_LEFT_BOTTOM)) {
                mHandler.sendEmptyMessage(Utils.MOVE_LEFT_BOTTOM);
            } else if (action.equals(Utils.ACTION_RIGHT_TOP)) {
                mHandler.sendEmptyMessage(Utils.MOVE_RIGHT_TOP);
            } else if (action.equals(Utils.ACTION_RIGHT_CENTER)) {
                mHandler.sendEmptyMessage(Utils.MOVE_RIGHT_CENTER);
            } else if (action.equals(Utils.ACTION_RIGHT_BOTTOM)) {
                mHandler.sendEmptyMessage(Utils.MOVE_RIGHT_BOTTOM);
            } else if (action.equals(Utils.ACTION_SHOW_FLOAT)) {
                mHandler.sendEmptyMessage(Utils.SHOW_FLOAT);
            } else if (action.equals(Utils.ACTION_HIDE_FLOAT)) {
                mHandler.sendEmptyMessage(Utils.HIDE_FLOAT);
            }
        }

    };

}
