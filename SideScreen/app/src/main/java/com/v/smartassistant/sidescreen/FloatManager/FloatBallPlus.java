package com.v.smartassistant.sidescreen.FloatManager;

import android.app.WallpaperManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
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
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
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


public class FloatBallPlus extends LinearLayout implements ICarrier {
    private static final String TAG = "lishuang_FloatBallPlus";

    private FloatBallManager floatBallManager;
    private WindowManager.LayoutParams mLayoutParams;
    private WindowManager windowManager;
    private boolean isFirst = true;
    private boolean isAdded = false;
    private int mTouchSlop;

    private boolean isClick;
    private int mDownX, mDownY, mLastX, mLastY;
    private ScrollRunner mRunner;
    private int mVelocityX, mVelocityY;
    private MotionVelocityUtil mVelocity;
    private boolean sleep = false;
    //shuang
    private SharedPreferences mShare;
    private int mIsright;
    private SharedPreferences.Editor mEdit;
    private int mRightPosition, mLeftPosition;

    private Context mContext;
    private LinearLayout mLayout;
    private ImageView mImage;
    private DynamicGridView mGridView;
    private ArrayList<LabelInfo> mDates;
    private CheeseDynamicAdapter mAdapter;
    private LabelInfo mWiFi, mCalculator, mFlashlight, mCamera, mVoice;

    private WifiManager mWifi;
    private WLANListener mListener;
    private FlashlightController mFlash;
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
                case Utils.SET_BLUR_BLACKGROUND:
                    Drawable wallPaper = WallpaperManager.getInstance(getContext()).getDrawable();
                    BitmapDrawable bt = (BitmapDrawable) wallPaper;
                    Bitmap blurBitmap = Utils.blurBitmap(getContext(), bt.getBitmap(), 20f);
                    setBackgroundDrawable(new BitmapDrawable(blurBitmap));
                    break;

            }
        }
    };

    private OnceRunnable mSleepRunnable = new OnceRunnable() {
        @Override
        public void onRun() {
            if (isAdded) {
                sleep = true;
                Log.d("shuang", "onRun: ");
                moveToEdge(false, sleep);
            }
        }
    };

    public FloatBallPlus(Context context, FloatBallManager floatBallManager) {
        super(context);
        mContext = context;
        this.floatBallManager = floatBallManager;
        init(context);
    }

    private void init(Context context) {
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mRunner = new ScrollRunner(this);
        mVelocity = new MotionVelocityUtil(context);
        initPosition();
        initWidget();
    }

    private void initWidget() {
        Log.d(TAG, "initWidget:");
        setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        if (mIsright == 0) {
            addView(mImage);
            addView(mLayout);
            mImage.setImageResource(R.drawable.slide_right);
        } else {
            addView(mLayout);
            addView(mImage);
            mImage.setImageResource(R.drawable.slide_left);
        }

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

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        Log.d(TAG, "onLayout: ");
        super.onLayout(changed, left, top, right, bottom);
    }

    public void updatePosition(boolean right, int position) {
        if (right) {
            mLayoutParams.gravity = Gravity.RIGHT;
            mRightPosition = position;
            mImage.setY(position);
        } else {
            mLayoutParams.gravity = Gravity.LEFT;
            mLeftPosition = position;
            mImage.setY(position);
        }
        Log.d("shuang", "updatePosition: right" + right + "---" + position + "----" + mLayoutParams.gravity);
        mEdit.putBoolean("isright", right);
        mEdit.putInt("left_position", mLeftPosition);
        mEdit.putInt("right_position", mRightPosition);
        mEdit.commit();
        if (null != windowManager) windowManager.updateViewLayout(this, mLayoutParams);
    }

    public void updatePosition(boolean right) {
        Log.d(TAG, "updatePositionAAA: ");
        if (right) {
            mLayoutParams.gravity = Gravity.RIGHT;
            removeView(mLayout);
            addView(mLayout);
            mImage.setImageResource(R.drawable.slide_right);
            mImage.setY(mRightPosition);
        } else {
            mLayoutParams.gravity = Gravity.LEFT;
            removeView(mImage);
            addView(mImage);
            mImage.setImageResource(R.drawable.slide_left);
            mImage.setY(mLeftPosition);
        }
        mEdit.putBoolean("isright", right);
        mEdit.commit();
        if (null != windowManager) windowManager.updateViewLayout(this, mLayoutParams);
        invalidate();
    }

    private void initPosition() {
        mLayout = (LinearLayout) LayoutInflater.from(mContext).inflate(R.layout.main_layout, null);
        mImage = (ImageView) LayoutInflater.from(mContext).inflate(R.layout.image_layout, null);
        mLayoutParams = Utils.getLayoutParams();
        mShare = mContext.getSharedPreferences("position", MODE_PRIVATE);
        mEdit = mShare.edit();
        mIsright = mContext.getSharedPreferences("touch_position", MODE_PRIVATE).getInt("position", 0);
        mLeftPosition = mShare.getInt("left_position", Utils.CENTER_POSITION);
        mRightPosition = mShare.getInt("right_position", Utils.CENTER_POSITION);
        if (mIsright == 0) {
            mLayoutParams.gravity = Gravity.RIGHT;
            mImage.setY(mRightPosition);
        } else {
            mLayoutParams.gravity = Gravity.LEFT;
            mImage.setY(mLeftPosition);
        }
        if (null != windowManager) windowManager.updateViewLayout(this, mLayoutParams);
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility == VISIBLE) {
            onConfigurationChanged(null);
        }
    }

    public void attachToWindow(WindowManager windowManager) {
        this.windowManager = windowManager;
        if (!isAdded) {
            windowManager.addView(this, mLayoutParams);
            isAdded = true;
        }
    }

    public void detachFromWindow(WindowManager windowManager) {
        this.windowManager = null;
        if (isAdded) {
            removeSleepRunnable();
            windowManager.removeView(this);
            isAdded = false;
            sleep = false;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        floatBallManager.onConfigurationChanged(newConfig);
        moveToEdge(false, false);
        postSleepRunnable();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.d(TAG, "onTouchEvent: " + event.getAction());
        int action = event.getAction();
        int x = (int) event.getRawX();
        int y = (int) event.getRawY();
        mVelocity.acquireVelocityTracker(event);
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                touchDown(x, y);
                break;
            case MotionEvent.ACTION_MOVE:
                //touchMove(x, y);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                touchUp();
                break;
            case MotionEvent.ACTION_OUTSIDE:
                sleep = true;
                mImage.setVisibility(VISIBLE);
                Log.d(TAG, "onTouchEvent: ACTION_OUTSIDE"+mImage.getY());
                moveToX(true, -mLayout.getMeasuredWidth());
                break;
        }
        return super.onTouchEvent(event);
    }

    private void touchDown(int x, int y) {
        mDownX = x;
        mDownY = y;
        mLastX = mDownX;
        mLastY = mDownY;
        isClick = true;
        removeSleepRunnable();
    }

    private void touchMove(int x, int y) {
        int totalDeltaX = x - mDownX;
        int totalDeltaY = y - mDownY;
        int deltaX = x - mLastX;
        int deltaY = y - mLastY;
        if (Math.abs(totalDeltaX) > mTouchSlop || Math.abs(totalDeltaY) > mTouchSlop) {
            isClick = false;
        }
        mLastX = x;
        mLastY = y;
        if (!isClick) {
            onMove(deltaX, deltaY);
        }
    }

    private void touchUp() {
        mVelocity.computeCurrentVelocity();
        mVelocityX = (int) mVelocity.getXVelocity();
        mVelocityY = (int) mVelocity.getYVelocity();
        mVelocity.releaseVelocityTracker();
        if (sleep) {
            wakeUp();
        } else {
            if (isClick) {
                onClick();
            } else {
                moveToEdge(true, false);
                Log.d("shuang", "touchUp: move");
            }
        }
        mVelocityX = 0;
        mVelocityY = 0;
    }

    private void moveToX(boolean smooth, int destX) {
        final int screenHeight = floatBallManager.mScreenHeight;
        int height = getHeight();
        int destY = 0;
        if (mLayoutParams.y < 0) {
            destY = 0 - mLayoutParams.y;
        } else if (mLayoutParams.y > screenHeight - height) {
            destY = screenHeight - height - mLayoutParams.y;
        }
        if (smooth) {
            int dx = destX - mLayoutParams.x;
            int duration = getScrollDuration(Math.abs(dx));
            mRunner.start(dx, destY, duration);
            Log.d("shuang", "moveToX: start" + dx + "," + destY);
        } else {
            onMove(destX - mLayoutParams.x, destY);
            postSleepRunnable();
            Log.d("shuang", "moveToX: post" + (destX - mLayoutParams.x) + "," + destY);
        }
    }

    private void wakeUp() {
        final int screenWidth = floatBallManager.mScreenWidth;
        int width = getWidth();
        int halfWidth = width / 2;
        int centerX = (screenWidth / 2 - halfWidth);
        int destX;
        destX = mLayoutParams.x < centerX ? 0 : screenWidth - width;
        sleep = false;
        mImage.setVisibility(INVISIBLE);
        moveToX(true, destX);
    }

    private void moveToEdge(boolean smooth, boolean forceSleep) {
        final int screenWidth = floatBallManager.mScreenWidth;
        int width = getWidth();
        int halfWidth = width / 2;
        int laywidth = mLayout.getMeasuredWidth();
        int btwidth = mImage.getMeasuredWidth();
        int centerX = (screenWidth / 2 - halfWidth);
        int destX;
        final int minVelocity = mVelocity.getMinVelocity();
        if (mLayoutParams.x < centerX) {
            sleep = forceSleep ? true : Math.abs(mVelocityX) > minVelocity && mVelocityX < 0 || mLayoutParams.x < 0;
            destX = sleep ? -laywidth : 0;
            Log.d("shuang", "moveToEdge: sleep=" + sleep + "," + centerX + "," + mLayoutParams.x + "," + destX);
        } else {
            sleep = forceSleep ? true : Math.abs(mVelocityX) > minVelocity && mVelocityX > 0 || mLayoutParams.x > screenWidth - width;
            destX = sleep ? screenWidth - btwidth : screenWidth - width;
        }
        moveToX(smooth, destX);
    }

    private int getScrollDuration(int distance) {
        return (int) (250 * (1.0f * distance / 800));
    }

    private void onMove(int deltaX, int deltaY) {
        mLayoutParams.x += deltaX;
        mLayoutParams.y += deltaY;
        if (windowManager != null) {
            windowManager.updateViewLayout(this, mLayoutParams);
        }
    }

    public void onMove(int lastX, int lastY, int curX, int curY) {
        onMove(curX - lastX, curY - lastY);
    }

    @Override
    public void onDone() {
        postSleepRunnable();
    }

    private void moveTo(int x, int y) {
        mLayoutParams.x += x - mLayoutParams.x;
        mLayoutParams.y += y - mLayoutParams.y;
        if (windowManager != null) {
            windowManager.updateViewLayout(this, mLayoutParams);
        }
    }

    private void onClick() {
        floatBallManager.floatballX = mLayoutParams.x;
        floatBallManager.floatballY = mLayoutParams.y;
        floatBallManager.onFloatBallClick();
    }

    private void removeSleepRunnable() {
        mSleepRunnable.removeSelf(this);
    }

    public void postSleepRunnable() {
        Log.d(TAG, "postSleepRunnable:vis " + sleep);
        mImage.setVisibility(sleep ? VISIBLE : INVISIBLE);
        if (!sleep && isAdded) {
            mSleepRunnable.postDelaySelf(this, 3000);
        }
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
        return labels;
    }

    @Override
    protected void onAttachedToWindow() {
        Log.d(TAG, "onAttachedToWindow: ");
        super.onAttachedToWindow();
        if (mListener != null) {
            registerWifiLister(mListener, mWiFi);
        }
        mBrightnessObserver.startObserving();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mListener != null) {
            mListener.unregister();
        }
        mBrightnessObserver.stopObserving();
    }
}
