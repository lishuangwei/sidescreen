package com.v.smartassistant.sidescreen.FloatManager;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Build;
import android.view.View;
import android.view.WindowManager;

public class FloatBallManager {
    public int mScreenWidth, mScreenHeight;
    private int mStatusBarHeight;

    private OnFloatBallClickListener mFloatballClickListener;
    private WindowManager mWindowManager;
    private Context mContext;
    private FloatBall floatBall;
    public int floatballX, floatballY;
    private boolean isShowing = false;

    public FloatBallManager(Context application) {
        mContext = application;
        int statusbarId = application.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (statusbarId > 0) {
            mStatusBarHeight = application.getResources().getDimensionPixelSize(statusbarId);
        }
        mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        computeScreenSize();
        floatBall = new FloatBall(mContext, this);
    }

    public void computeScreenSize() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            Point point = new Point();
            mWindowManager.getDefaultDisplay().getSize(point);
            mScreenWidth = point.x;
            mScreenHeight = point.y;
        } else {
            mScreenWidth = mWindowManager.getDefaultDisplay().getWidth();
            mScreenHeight = mWindowManager.getDefaultDisplay().getHeight();
        }
        mScreenHeight -= mStatusBarHeight;
    }

    public void show() {
        if (isShowing) return;
        isShowing = true;
        floatBall.setVisibility(View.VISIBLE);
        floatBall.attachToWindow(mWindowManager);
    }

    public boolean isLand() {
        return floatBall.isLand();
    }

    public void reset() {
        floatBall.setVisibility(View.VISIBLE);
        floatBall.postSleepRunnable();
    }

    public void onFloatBallClick() {
        if (mFloatballClickListener != null) {
            mFloatballClickListener.onFloatBallClick();
        }
    }

    public void hide() {
        if (!isShowing) return;
        isShowing = false;
        floatBall.detachFromWindow(mWindowManager);
    }

    public void updatePostion(boolean right, int position) {
        floatBall.updatePosition(right, position);
    }

    public void updatePostion(boolean right) {
        floatBall.updatePosition(right);
    }

    public void onConfigurationChanged(Configuration newConfig) {
        computeScreenSize();
        reset();
    }

    public boolean getShowing() {
        return isShowing;
    }


    public void setOnFloatBallClickListener(OnFloatBallClickListener listener) {
        mFloatballClickListener = listener;
    }

    public interface OnFloatBallClickListener {
        void onFloatBallClick();
    }
}
