package com.v.smartassistant.sidescreen.CustomView;

import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.SeekBar;

import com.v.smartassistant.sidescreen.GridUtils.CheeseDynamicAdapter;
import com.v.smartassistant.sidescreen.GridUtils.FlashlightController;
import com.v.smartassistant.sidescreen.GridUtils.LabelInfo;
import com.v.smartassistant.sidescreen.GridUtils.Utils;
import com.v.smartassistant.sidescreen.GridUtils.WLANListener;
import com.v.smartassistant.sidescreen.R;

import org.askerov.dynamicgrid.DynamicGridView;

import java.util.ArrayList;

import static android.content.Context.MODE_PRIVATE;

public class VDHLayout extends ViewGroup {
    private static final String TAG = "lishuang";

    private static final int MIN_DRAWER_MARGIN = 64; // dp
    /**
     * Minimum velocity that will be detected as a fling
     */
    private static final int MIN_FLING_VELOCITY = 200; // dips per second

    /**
     * drawer离父容器右边的最小外边距
     */
    private int mMinDrawerMargin, mLeft;

    private View mLeftMenuView;
    private ImageView mButton;

    private ViewDragHelper mDragHelper;
    /**
     * drawer显示出来的占自身的百分比
     */
    private float mLeftMenuOnScreen;

    private boolean mIsLeft;
    private SharedPreferences mShare;
    private SharedPreferences mSharePos;
    private DynamicGridView mGridView;
    private ArrayList<LabelInfo> mDates;
    private CheeseDynamicAdapter mAdapter;
    private LabelInfo mWiFi, mCalculator, mFlashlight, mCamera, mVoice;
    private Context mContext;
    private float mStartX, mStartY;
    private int mLeftPosition, mRightPosition;
    //wifi
    private WifiManager mWifi;
    private WLANListener mListener;
    //flashlight
    private FlashlightController mFlash;
    //brightness isauto
    private SeekBar mSeekbar;
    private int mProgress;
    private boolean mAutomatic;
    private BrightnessObserver mBrightnessObserver;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case Utils.FLASH_CLOSE:
                    mFlashlight.setDrawable(R.drawable.flashlight_normal);
                    mAdapter.notifyDataSetChanged();
                    break;
                case Utils.FLASH_OPEN:
                    mFlashlight.setDrawable(R.drawable.flashlight_press);
                    mAdapter.notifyDataSetChanged();
                    break;
                case Utils.MOVE_FROM_LEFT:
                    mIsLeft = true;
                    mButton.setImageResource(R.drawable.slide_left);
                    requestLayout();
                    break;
                case Utils.MOVE_FROM_RIGHT:
                    mIsLeft = false;
                    mButton.setImageResource(R.drawable.slide_right);
                    requestLayout();
                    break;
                case Utils.MOVE_LEFT_TOP:
                    mLeftPosition = 0;
                    requestLayout();
                    break;
                case Utils.MOVE_LEFT_CENTER:
                    mLeftPosition = 1;
                    requestLayout();
                    break;
                case Utils.MOVE_LEFT_BOTTOM:
                    mLeftPosition = 2;
                    requestLayout();
                    break;
                case Utils.MOVE_RIGHT_TOP:
                    mRightPosition = 0;
                    requestLayout();
                    break;
                case Utils.MOVE_RIGHT_CENTER:
                    mRightPosition = 1;
                    requestLayout();
                    break;
                case Utils.MOVE_RIGHT_BOTTOM:
                    mRightPosition = 2;
                    requestLayout();
                    break;
                case Utils.SET_BLUR_BLACKGROUND:
                    break;
            }
        }
    };

    private void setBlur(float blur) {
        Drawable wallPaper = WallpaperManager.getInstance(getContext()).getDrawable();
        BitmapDrawable bt = (BitmapDrawable) wallPaper;
        Bitmap blurBitmap = Utils.blurBitmap(getContext(), bt.getBitmap(), blur);
        setBackgroundDrawable(new BitmapDrawable(blurBitmap));
    }

    public VDHLayout(Context context) {
        this(context, null);
    }

    public VDHLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VDHLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        float density = getResources().getDisplayMetrics().density;
        float minVel = MIN_FLING_VELOCITY * density;  //1200
        mMinDrawerMargin = (int) (MIN_DRAWER_MARGIN * density + 0.5f);
        mDragHelper = ViewDragHelper.create(this, 1.0f, new ViewDragHelper.Callback() {
            @Override
            public boolean tryCaptureView(View child, int pointerId) {
                return child == mLeftMenuView;
            }

            @Override
            public int clampViewPositionHorizontal(View child, int left, int dx) {
                int newLeft = mIsLeft ? Math.max(-child.getWidth(), Math.min(left, 0)) : Math.max(getWidth() - child.getWidth(), Math.min(left, getWidth()));
                Log.d(TAG, "clampViewPositionHorizontal: newLeft = " + newLeft + "----left= " + left);
                return newLeft;
            }

//            @Override
//            public int clampViewPositionVertical(View child, int top, int dy) {
//                int topBound = getPaddingTop();
//                int bottomBound = getHeight() - child.getHeight() - topBound;
//                int newTop = Math.min(Math.max(top, topBound), bottomBound);
//                return newTop;
//            }

            @Override
            public void onViewReleased(View releasedChild, float xvel, float yvel) {
                int childWidth = releasedChild.getWidth();
                //0~1f
                float offset = mIsLeft ? (childWidth + releasedChild.getLeft()) * 1.0f / childWidth : (getWidth() - releasedChild.getLeft()) * 1.0f / childWidth;
                Log.d(TAG, "onViewReleased: xvel=" + xvel + "-----yvel= " + yvel + "---offset= " + offset);
                int left;
                if (mIsLeft) {
                    left = xvel > 0 || xvel == 0 && offset > 0.5f ? 0 : -childWidth;
                } else {
                    final int width = getWidth();
                    left = xvel < 0 || xvel == 0 && offset > 0.5f ? width - childWidth : width;
                }
                mDragHelper.settleCapturedViewAt(left, releasedChild.getTop());
                invalidate();
            }

            @Override
            public void onEdgeDragStarted(int edgeFlags, int pointerId) {
                mDragHelper.captureChildView(mLeftMenuView, pointerId);

            }

            @Override
            public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
                mLeft = left;
                int childWidth = changedView.getWidth();
                float offset = mIsLeft ? (float) (childWidth + left) / childWidth : (float) (getWidth() - left) / childWidth;
                mLeftMenuOnScreen = offset;
                changedView.setVisibility(offset == 0 ? View.INVISIBLE : View.VISIBLE);
                mButton.setVisibility(offset > 0 ? INVISIBLE : VISIBLE);
                //invalidate();
                requestLayout();
            }

            @Override
            public int getViewHorizontalDragRange(View child) {
                return mLeftMenuView == child ? child.getWidth() : 0;
            }

        });
        mDragHelper.setEdgeTrackingEnabled(ViewDragHelper.EDGE_LEFT | ViewDragHelper.EDGE_RIGHT);
        mDragHelper.setMinVelocity(minVel);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        init();
        IntentFilter myIntentFilter = new IntentFilter();
        myIntentFilter.addAction(Utils.ACTION_MOVR_LEFT);
        myIntentFilter.addAction(Utils.ACTION_MOVR_RIGHT);
        myIntentFilter.addAction(Utils.ACTION_LEFT_TOP);
        myIntentFilter.addAction(Utils.ACTION_LEFT_CENTER);
        myIntentFilter.addAction(Utils.ACTION_LEFT_BOTTOM);
        myIntentFilter.addAction(Utils.ACTION_RIGHT_TOP);
        myIntentFilter.addAction(Utils.ACTION_RIGHT_CENTER);
        myIntentFilter.addAction(Utils.ACTION_RIGHT_BOTTOM);
        mContext.registerReceiver(mReceiver, myIntentFilter);
        mBrightnessObserver.startObserving();
        mShare = mContext.getSharedPreferences("position", MODE_PRIVATE);
        mIsLeft = !mShare.getBoolean("isright", true);
        if (mIsLeft) mButton.setImageResource(R.drawable.slide_left);

        mSharePos = mContext.getSharedPreferences("touch_position", MODE_PRIVATE);
        mLeftPosition = mSharePos.getInt("left_position", 1);
        mRightPosition = mSharePos.getInt("right_position", 1);
        Log.d("xiao", "attachwindow vhd " + this.getWidth() + "," + this.getHeight());
    }

    private int getpositon(int pos) {
        int positon = 0;
        if (pos == 0) {
            positon = Utils.INIT_TOUCH_POSITION - Utils.OFFSET_TOUCH_POSITION;
        } else if (pos == 1) {
            positon = Utils.INIT_TOUCH_POSITION;
        } else {
            positon = Utils.INIT_TOUCH_POSITION + Utils.OFFSET_TOUCH_POSITION;
        }
        return positon;
    }

    private int getOffset(boolean isleft) {
        return isleft ? getpositon(mLeftPosition) : getpositon(mRightPosition);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mListener != null) {
            mListener.unregister();
        }
        mContext.unregisterReceiver(mReceiver);
        mBrightnessObserver.stopObserving();

    }

    private void init() {
        mWifi = (WifiManager) mContext.getSystemService(mContext.WIFI_SERVICE);
        mListener = new WLANListener(mContext);
        mFlash = new FlashlightController(mContext);
        mFlash.addListener(mFlashListen);

        mSeekbar = (SeekBar) findViewById(R.id.seekbar_brightness);
        mProgress = Utils.getSystemBrightness(mContext);
        mSeekbar.setProgress(mProgress);
        mSeekbar.setOnSeekBarChangeListener(mSeekBarListener);
        mBrightnessObserver = new BrightnessObserver(new Handler(), mContext);
        mAutomatic = Utils.isAutoBrightness(mContext);
        mDates = getDates();
        mAdapter = new CheeseDynamicAdapter(mContext, mDates, 1);
        mGridView = findViewById(R.id.dynamic_grid);
        Log.d(TAG, "init: " + mGridView);
        mGridView.setAdapter(mAdapter);
        mGridView.setOnDropListener(new DynamicGridView.OnDropListener() {
            @Override
            public void onActionDrop() {
                mGridView.stopEditMode();
                Log.d(TAG, "onActionDrop");
            }
        });
        mGridView.setOnDragListener(new DynamicGridView.OnDragListener() {
            @Override
            public void onDragStarted(int position) {
                Log.d(TAG, "drag started at position " + position);
            }

            @Override
            public void onDragPositionsChanged(int oldPosition, int newPosition) {
                mGridView.stopEditMode();
                Log.d(TAG, String.format("drag item position changed from %d to %d", oldPosition, newPosition));
            }
        });
        mGridView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "itemlongclick " + mGridView.isEditMode());
                if (mGridView.isEditMode()) {
                    mGridView.stopEditMode();
                } else {
                    mGridView.startEditMode();
                }
                return true;
            }
        });

        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "onItemClick " + parent.getAdapter().getItem(position).toString());
                LabelInfo info = (LabelInfo) parent.getAdapter().getItem(position);
                setItemClcik(info);
                mAdapter.notifyDataSetChanged();
            }
        });

    }

    SeekBar.OnSeekBarChangeListener mSeekBarListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            Log.d(TAG, "onPreogressChanged " + progress);
            mProgress = progress;
            Utils.setBrightness(mContext, mProgress, mAutomatic);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            if (mAutomatic) {
                Utils.setScreenMode(Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL, mContext);
                mAutomatic = false;
            }

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        }
    };

    private void setItemClcik(LabelInfo info) {
        if (info.getTitle().equals(getResources().getString(R.string.wifi_label))) {
            onWifiClick(info);
        } else if (info.getTitle().equals(getResources().getString(R.string.calculator_label))) {
            onCalculatorClick(info);
        } else if (info.getTitle().equals(getResources().getString(R.string.flashlight_label))) {
            onFlashlightClcik(info);
        } else if (info.getTitle().equals(getResources().getString(R.string.camera_label))) {
            onCameraClick(info);
        } else if (info.getTitle().equals((getResources().getString(R.string.voice_label)))) {
            onVoiceClick(info);
        }

    }

    private void onVoiceClick(LabelInfo info) {
        Intent intent = new Intent();
        intent.setClassName("com.google.android.googlequicksearchbox", "com.google.android.apps.gsa.queryentry.QueryEntryActivity");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
    }

    private void onCameraClick(LabelInfo info) {
        Intent intent = new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
    }

    private void onFlashlightClcik(LabelInfo info) {
        if (info.isClick()) {
            mFlash.setFlashlight(false);
            info.setClick(false);
            info.setDrawable(R.drawable.flashlight_normal);
        } else {
            mFlash.setFlashlight(true);
            info.setClick(true);
            info.setDrawable(R.drawable.flashlight_press);
        }
    }

    private void onCalculatorClick(LabelInfo info) {
        Intent intent = new Intent();
        intent.setClassName("com.android.calculator2", "com.android.calculator2.Calculator");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
    }

    private void onWifiClick(LabelInfo info) {
        if (mWifi.isWifiEnabled()) {
            mWifi.setWifiEnabled(false);
            info.setDrawable(R.drawable.wifi_normal);
        } else {
            mWifi.setWifiEnabled(true);
            info.setDrawable(R.drawable.wifi_press);
        }
    }

    private ArrayList<LabelInfo> getDates() {
        boolean wifiState = mWifi.isWifiEnabled();
        boolean flashState = mFlash.isEnabled();
        ArrayList<LabelInfo> labels = new ArrayList<>();
        mWiFi = new LabelInfo(R.drawable.wifi_normal, getResources().getString(R.string.wifi_label), wifiState);
        mCalculator = new LabelInfo(R.drawable.calculator_normal, getResources().getString(R.string.calculator_label), false);
        mFlashlight = new LabelInfo(R.drawable.flashlight_normal, getResources().getString(R.string.flashlight_label), flashState);
        mCamera = new LabelInfo(R.drawable.camera_normal, getResources().getString(R.string.camera_label), false);
        mVoice = new LabelInfo(R.drawable.voice_normal, getResources().getString(R.string.voice_label), false);
        labels.add(mWiFi);
        labels.add(mCalculator);
        labels.add(mFlashlight);
        labels.add(mCamera);
        labels.add(mVoice);

        mWiFi.setDrawable(mWiFi.isClick() ? R.drawable.wifi_press : R.drawable.wifi_normal);
        mFlashlight.setDrawable(mFlashlight.isClick() ? R.drawable.flashlight_press : R.drawable.flashlight_normal);
        registerWifiLister(mListener, mWiFi);
        return labels;
    }

    @Override
    public void computeScroll() {
        if (mDragHelper.continueSettling(true)) {
            invalidate();
        }
    }

    public int getWid() {
        return (int) (20 + 165 * mLeftMenuOnScreen);
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        if (widthMode != MeasureSpec.EXACTLY || heightMode != MeasureSpec.EXACTLY) {
            if (widthMode == MeasureSpec.AT_MOST) {
                widthMode = MeasureSpec.EXACTLY;
            } else if (widthMode == MeasureSpec.UNSPECIFIED) {
                widthMode = MeasureSpec.EXACTLY;
                widthSize = 300;
            }
            if (heightMode == MeasureSpec.AT_MOST) {
                heightMode = MeasureSpec.EXACTLY;
            } else if (heightMode == MeasureSpec.UNSPECIFIED) {
                heightMode = MeasureSpec.EXACTLY;
                heightSize = 300;
            }
        }
        setMeasuredDimension(widthSize, heightSize);
        Log.d("xiao", "onMeasure: 宽" + widthSize + "高" + heightSize + "---top---" + getTop());

        View leftMenuView = getChildAt(0);
        MarginLayoutParams lp = (MarginLayoutParams) leftMenuView.getLayoutParams();

        final int drawerWidthSpec = getChildMeasureSpec(widthMeasureSpec,
                mMinDrawerMargin + lp.leftMargin + lp.rightMargin,
                lp.width);
        final int drawerHeightSpec = getChildMeasureSpec(heightMeasureSpec,
                lp.topMargin + lp.bottomMargin,
                lp.height);
        leftMenuView.measure(drawerWidthSpec, drawerHeightSpec);
        Log.d("xiao", "onMeasure: 测量leftmenu" + (lp.leftMargin + lp.rightMargin) + "," + (lp.topMargin + lp.bottomMargin) + "," + lp.width + "," + lp.height);
        mLeftMenuView = leftMenuView;

        View leftbt = getChildAt(1);
        MarginLayoutParams lp1 = (MarginLayoutParams) leftbt.getLayoutParams();
        final int drawerWidthSpec1 = getChildMeasureSpec(widthMeasureSpec,
                mMinDrawerMargin + lp1.leftMargin + lp1.rightMargin,
                lp1.width);
        final int drawerHeightSpec1 = getChildMeasureSpec(heightMeasureSpec,
                lp1.topMargin + lp1.bottomMargin,
                lp1.height);
        leftbt.measure(drawerWidthSpec1, drawerHeightSpec1);
        Log.d("xiao", "onMeasure: 测量button" + (lp.leftMargin + lp.rightMargin) + "," + (lp.topMargin + lp.bottomMargin + "," + lp.width + "," + lp.height));
        mButton = (ImageView) leftbt;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        Log.d(TAG, "onLayout: onlayout 位置" + l + "---" + t + "---" + r + "---" + b);
        View menuView = mLeftMenuView;

        MarginLayoutParams lp = (MarginLayoutParams) menuView.getLayoutParams();

        final int menuWidth = menuView.getMeasuredWidth();
        int childLeft = 0;
        if (mIsLeft) {
            childLeft = -menuWidth + (int) (menuWidth * mLeftMenuOnScreen);
        } else {
            childLeft = (getWidth() - (int) (menuWidth * mLeftMenuOnScreen));
        }
        menuView.layout(childLeft, lp.topMargin, childLeft + menuWidth,
                lp.topMargin + menuView.getMeasuredHeight());
        Log.d(TAG, "onLayout: 布局leftmenu" + childLeft + "," + lp.topMargin + "," + (childLeft + menuWidth) + "," + (lp.topMargin + menuView.getMeasuredHeight()));
        View btview = mButton;
        MarginLayoutParams lp1 = (MarginLayoutParams) btview.getLayoutParams();
        final int menuWidth1 = btview.getMeasuredWidth();
        int childLeft1 = 0;
        if (mIsLeft) {
            childLeft1 = childLeft + menuWidth;
        } else {
            childLeft1 = childLeft - menuWidth1;
        }
        int pos = getOffset(mIsLeft);
        btview.layout(childLeft1, pos, childLeft1 + menuWidth1,
                pos + btview.getMeasuredHeight());
        Log.d(TAG, "onLayout: 布局button" + childLeft1 + "," + pos + "," + (childLeft1 + menuWidth1) + "," + (pos + menuView.getMeasuredHeight()));
    }

    public void closeDrawer() {
        View menuView = mLeftMenuView;
        mLeftMenuOnScreen = 0.f;
        mDragHelper.smoothSlideViewTo(menuView, mIsLeft ? -menuView.getWidth() : getWidth(), menuView.getTop());
        ViewCompat.postInvalidateOnAnimation(this);
    }

    public void openDrawer() {
        View menuView = mLeftMenuView;
        mLeftMenuOnScreen = 1.0f;
        mDragHelper.smoothSlideViewTo(menuView, mIsLeft ? 0 : getWidth() - menuView.getWidth(), menuView.getTop());
        ViewCompat.postInvalidateOnAnimation(this);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        final int action = MotionEventCompat.getActionMasked(ev);

        if (action != MotionEvent.ACTION_DOWN) {
            mDragHelper.cancel();
            return super.onInterceptTouchEvent(ev);
        }

        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            mDragHelper.cancel();
            return false;
        }

        final float x = ev.getX();
        final float y = ev.getY();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mStartX = x;
                mStartY = y;
                break;
            case MotionEvent.ACTION_MOVE:
                final float adx = Math.abs(x - mStartX);
                final float ady = Math.abs(y - mStartY);
                final int slop = mDragHelper.getTouchSlop();
                if (ady > slop && adx > ady) {
                    mDragHelper.cancel();
                    return false;
                }
                break;
        }
        return mDragHelper.shouldInterceptTouchEvent(ev);
    }

    @Override
    protected void onFinishInflate() {
        Log.d(TAG, "onFinishInflate: ");
        super.onFinishInflate();
        mLeftMenuView = getChildAt(0);
        mButton = (ImageView) getChildAt(1);
        mButton.setFocusable(true);
//        mButton.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                if (mLeftMenuOnScreen == 0.f) {
//                    openDrawer();
//                    Log.d("lishuang", "onLayout: onClick");
//                } else {
//                    closeDrawer();
//                }
//            }
//        });
        Log.d(TAG, "onFinishInflate: " + mGridView);
        for (int i = 0; i < getChildCount(); i++) {
            Log.d(TAG, "onFinishInflate: " + getChildAt(i).toString());
        }
    }

    @Override
    protected LayoutParams generateLayoutParams(LayoutParams p) {
        return new MarginLayoutParams(p);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new MarginLayoutParams(getContext(), attrs);
    }

    private class BrightnessObserver extends ContentObserver {
        private Context mContext;

        private final Uri BRIGHTNESS_MODE_URI =
                Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS_MODE);
        private final Uri BRIGHTNESS_URI =
                Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS);
        private final Uri BRIGHTNESS_ADJ_URI =
                Settings.System.getUriFor("screen_auto_brightness_adj");

        public BrightnessObserver(Handler handler, Context context) {
            super(handler);
            mContext = context;
        }

        @Override
        public void onChange(boolean selfChange) {
            onChange(selfChange, null);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (selfChange) return;
            try {
                if (BRIGHTNESS_MODE_URI.equals(uri)) {
                    Log.d(TAG, "onChange: 1");
                    mAutomatic = Utils.isAutoBrightness(mContext);
                    Utils.updateSeekBar(mContext, mSeekbar, mAutomatic);
                } else if (BRIGHTNESS_URI.equals(uri) && !mAutomatic) {
                    Log.d(TAG, "onChange: 2");
                    mAutomatic = Utils.isAutoBrightness(mContext);
                    Utils.updateSeekBar(mContext, mSeekbar, mAutomatic);
                } else if (BRIGHTNESS_ADJ_URI.equals(uri) && mAutomatic) {
                    Log.d(TAG, "onChange: 3");
                    mAutomatic = Utils.isAutoBrightness(mContext);
                    Utils.updateSeekBar(mContext, mSeekbar, mAutomatic);
                } else {
                    mAutomatic = Utils.isAutoBrightness(mContext);
                    Utils.updateSeekBar(mContext, mSeekbar, mAutomatic);
                }
            } finally {
            }
        }

        public void startObserving() {
            final ContentResolver cr = mContext.getContentResolver();
            cr.unregisterContentObserver(this);
            cr.registerContentObserver(BRIGHTNESS_MODE_URI, false, this);
            cr.registerContentObserver(BRIGHTNESS_URI, false, this);
            cr.registerContentObserver(BRIGHTNESS_ADJ_URI, false, this);
        }

        public void stopObserving() {
            final ContentResolver cr = mContext.getContentResolver();
            cr.unregisterContentObserver(this);
        }
    }

    FlashlightController.FlashlightListener mFlashListen = new FlashlightController.FlashlightListener() {
        @Override
        public void onFlashlightChanged(boolean enabled) {
            int state = enabled ? Utils.FLASH_OPEN : Utils.FLASH_CLOSE;
            mHandler.sendEmptyMessage(state);
            mFlashlight.setClick(enabled);
        }

        @Override
        public void onFlashlightError() {

        }

        @Override
        public void onFlashlightAvailabilityChanged(boolean available) {

        }
    };

    private void registerWifiLister(WLANListener listener, final LabelInfo info) {
        if (!info.getTitle().equals(mContext.getResources().getString(R.string.wifi_label))) return;
        listener.register(new WLANListener.WLANStateListener() {
            @Override
            public void onStateChanged() {
                Log.d(TAG, "onStateChanged");

            }

            @Override
            public void onStateDisabled() {
                Log.d(TAG, "onStateDisabled");
                info.setDrawable(R.drawable.wifi_normal);
                mAdapter.notifyDataSetChanged();
            }

            @Override
            public void onStateDisabling() {
                Log.d(TAG, "onStateDisabling");
            }

            @Override
            public void onStateEnabled() {
                Log.d(TAG, "onStateEnabled");
                info.setDrawable(R.drawable.wifi_press);
                mAdapter.notifyDataSetChanged();
            }

            @Override
            public void onStateEnabling() {
                Log.d(TAG, "onSateEnabled");

            }

            @Override
            public void onStateUnknow() {
                Log.d(TAG, "onSateUbknow");

            }
        });
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
            }
        }

    };

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mDragHelper.processTouchEvent(event);

        final int action = event.getAction();
        final float x = event.getX();
        final float y = event.getY();
        boolean isInButton = mDragHelper.isViewUnder(mButton, (int) x, (int) y);
        switch (action & MotionEventCompat.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                mStartX = x;
                mStartY = y;
                Log.d(TAG, "onTouchEvent: actiondown startx=" + mStartX + " starty=" + mStartY);
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                final float dx = x - mStartX;
                final float dy = y - mStartY;
                final float slop = mDragHelper.getTouchSlop();
                if (isInButton && dx * dx + dy * dy > slop * slop) {
                    Log.d(TAG, "onTouchEvent: actionup dx＝" + dx + " dy= " + dy + " slop= " + slop + "isinbutton=" + isInButton);
                    if (mLeftMenuOnScreen == 1) {
                        //closeDrawer();
                    } else {
                        //openDrawer();
                    }
                }
                break;
            case MotionEvent.ACTION_MOVE:
                //Log.d(TAG, "onTouchEvent: actionmove x= " + x + " y = " + y);
                break;
        }

        return true;
    }
}
