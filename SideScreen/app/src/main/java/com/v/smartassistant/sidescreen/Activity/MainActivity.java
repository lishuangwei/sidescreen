package com.v.smartassistant.sidescreen.Activity;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.PendingIntent;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.speech.RecognizerIntent;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.transition.Fade;
import android.transition.Slide;
import android.transition.Visibility;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Toast;

import com.v.smartassistant.sidescreen.GridUtils.CheeseDynamicAdapter;
import com.v.smartassistant.sidescreen.GridUtils.FlashlightController;
import com.v.smartassistant.sidescreen.GridUtils.LabelInfo;

import com.v.smartassistant.sidescreen.GridUtils.Utils;
import com.v.smartassistant.sidescreen.GridUtils.WLANListener;
import com.v.smartassistant.sidescreen.R;

import org.askerov.dynamicgrid.DynamicGridView;

import java.util.ArrayList;


public class MainActivity extends AppCompatActivity implements FlashlightController.FlashlightListener {
    private static final String TAG = "shuang";
    private LabelInfo mWiFi, mCalculator, mFlashlight, mCamera, mVoice;
    private DrawerLayout mDrawer;
    private int mOrientation;
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

    private HomeReceiver mReceiver;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case Utils.FLASH_CLOSE:
                    mFlashlight.setDrawable(R.drawable.flashlight_normal);
                    mFlashlight.setClick(false);
                    mAdapter.notifyDataSetChanged();
                    break;
                case Utils.FLASH_OPEN:
                    mFlashlight.setDrawable(R.drawable.flashlight_press);
                    mFlashlight.setClick(true);
                    mAdapter.notifyDataSetChanged();
                    break;
                case Utils.SET_BLUR_BLACKGROUND:
                    Drawable wallPaper = WallpaperManager.getInstance(MainActivity.this).getDrawable();
                    BitmapDrawable bt = (BitmapDrawable) wallPaper;
                    Bitmap blurBitmap = Utils.blurBitmap(MainActivity.this, bt.getBitmap(), 20f);
                    BitmapDrawable bd = new BitmapDrawable((blurBitmap));
                    Log.d(TAG, "handleMessage: aaaaaa" + bd.getAlpha());
                    bd.setAlpha(150);
                    Log.d(TAG, "handleMessage: bbbbbb" + bd.getAlpha());
                    getWindow().setBackgroundDrawable(bd);
                    break;
            }
        }
    };

    private DynamicGridView mGridView;
    private ArrayList<LabelInfo> mDates;
    CheeseDynamicAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: ");
        super.onCreate(savedInstanceState);
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.flags = WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        params.width = WindowManager.LayoutParams.MATCH_PARENT;
        params.height = WindowManager.LayoutParams.MATCH_PARENT;
        getWindow().setAttributes(params);

        if (Utils.isNeedPermission(this)) return;
        mOrientation = getIntent().getIntExtra("orientation", Utils.MOVE_FROM_RIGHT);
//        int style = mOrientation == (Utils.MOVE_FROM_RIGHT) ? R.style.RighttoLeft : R.style.LefttoRight;
        getWindow().setWindowAnimations(R.style.AlpahStyle);

        setContentView(mOrientation == (Utils.MOVE_FROM_RIGHT) ? R.layout.activity_main : R.layout.layout_left);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND, WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        mDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        init();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause: ");
        super.onPause();
    }

    @Override
    public void finish() {
        Log.d(TAG, "finish: ");
        super.finish();
        overridePendingTransition(R.anim.alpha_out, 0);
//        if (mOrientation == Utils.MOVE_FROM_RIGHT) {
//            Log.d(TAG, "onDestroy: right");
//            overridePendingTransition(R.anim.alpha_out, 0);
//        } else {
//            Log.d(TAG, "onDestroy: left");
//            overridePendingTransition(R.anim.alpha_out, 0);
//        }
    }

    private void init() {
        mWifi = (WifiManager) getSystemService(MainActivity.WIFI_SERVICE);
        mListener = new WLANListener(this);
        mFlash = new FlashlightController(this);
        mFlash.addListener(this);

        mSeekbar = (SeekBar) findViewById(R.id.seekbar_brightness);
        mProgress = Utils.getSystemBrightness(this);
        mSeekbar.setProgress(mProgress);
        mSeekbar.setOnSeekBarChangeListener(mSeekBarListener);
        mBrightnessObserver = new BrightnessObserver(new Handler(), this);
        mAutomatic = Utils.isAutoBrightness(this);
        mDates = getDates();
        mAdapter = new CheeseDynamicAdapter(this, mDates, 1);
        mGridView = (DynamicGridView) findViewById(R.id.dynamic_grid);
        mGridView.setAdapter(mAdapter);
        mGridView.setOnDropListener(new DynamicGridView.OnDropListener() {
            @Override
            public void onActionDrop() {
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
                Log.d(TAG, String.format("drag item position changed from %d to %d", oldPosition, newPosition));
                mGridView.stopEditMode();
            }
        });
        mGridView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                mGridView.startEditMode(position);
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

        mDrawer.setDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                Log.d(TAG, "onDrawerSlide: ");
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                Log.d(TAG, "onDrawerOpened: ");
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                MainActivity.this.finish();

            }

            @Override
            public void onDrawerStateChanged(int newState) {
                ;

            }
        });

        mReceiver = new HomeReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        registerReceiver(mReceiver, filter);
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "onStart: ");
        super.onStart();
        mBrightnessObserver.startObserving();
        Intent intent = new Intent(Utils.ACTION_HIDE_FLOAT);
        sendBroadcast(intent);

//        mHandler.sendEmptyMessage(Utils.SET_BLUR_BLACKGROUND);
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume: ");
        super.onResume();
        mDrawer.openDrawer(mOrientation == (Utils.MOVE_FROM_RIGHT) ? Gravity.RIGHT : Gravity.LEFT);
        Log.d(TAG, "onResume: " + (mOrientation == (Utils.MOVE_FROM_RIGHT)));
    }

    SeekBar.OnSeekBarChangeListener mSeekBarListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            Log.d(TAG, "onPreogressChanged " + progress);
            mProgress = progress;
            Utils.setBrightness(MainActivity.this, mProgress, mAutomatic);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            if (mAutomatic) {
                Utils.setScreenMode(Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL, MainActivity.this);
                mAutomatic = false;
            }

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        }
    };

    private void registerWifiLister(WLANListener listener, final LabelInfo info) {
        if (!info.getTitle().equals(getResources().getString(R.string.wifi_label))) return;
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

    private ArrayList<LabelInfo> getDates() {
        boolean wifiState = mWifi.isWifiEnabled();
        boolean flashState = mFlash.isEnabled();
        ArrayList<LabelInfo> labels = new ArrayList<>();
        mWiFi = new LabelInfo(R.drawable.wifi_normal, getResources().getString(R.string.wifi_label), wifiState);
        mCalculator = new LabelInfo(R.drawable.calculator_bg, getResources().getString(R.string.calculator_label), false);
        mFlashlight = new LabelInfo(R.drawable.flashlight_normal, getResources().getString(R.string.flashlight_label), flashState);
        mCamera = new LabelInfo(R.drawable.camera_bg, getResources().getString(R.string.camera_label), false);
        mVoice = new LabelInfo(R.drawable.voice_bg, getResources().getString(R.string.voice_label), false);
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
        startActivity(intent);
    }

    private void onCameraClick(LabelInfo info) {
        Intent intent = new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
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
        startActivity(intent);
    }

    private void setItemLongClcik(LabelInfo info) {
        if (info.getTitle().equals(getResources().getString(R.string.wifi_label))) {
            onWifiLongClick(info);
        }

    }

    private void onWifiLongClick(LabelInfo info) {
        Intent intent = new Intent("android.settings.WIFI_SETTINGS");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
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

    @Override
    public void onBackPressed() {
        if (mGridView.isEditMode()) {
            mGridView.stopEditMode();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onFlashlightChanged(boolean enabled) {
        Log.d(TAG, "onFlashChanged");
        int state = enabled ? Utils.FLASH_OPEN : Utils.FLASH_CLOSE;
        mHandler.sendEmptyMessage(state);
    }

    @Override
    public void onFlashlightError() {
        Log.d(TAG, "onFlashError");

    }

    @Override
    public void onFlashlightAvailabilityChanged(boolean available) {
        Log.d(TAG, "onFlashAviailability");

    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: ");
        if (mListener != null) {
            mListener.unregister();
        }
        mBrightnessObserver.stopObserving();
        unregisterReceiver(mReceiver);

        Intent intent = new Intent(Utils.ACTION_SHOW_FLOAT);
        sendBroadcast(intent);
        super.onDestroy();
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
                    mAutomatic = Utils.isAutoBrightness(MainActivity.this);
                    Utils.updateSeekBar(MainActivity.this, mSeekbar, mAutomatic);
                } else if (BRIGHTNESS_URI.equals(uri) && !mAutomatic) {
                    Log.d(TAG, "onChange: 2");
                    mAutomatic = Utils.isAutoBrightness(MainActivity.this);
                    Utils.updateSeekBar(MainActivity.this, mSeekbar, mAutomatic);
                } else if (BRIGHTNESS_ADJ_URI.equals(uri) && mAutomatic) {
                    Log.d(TAG, "onChange: 3");
                    mAutomatic = Utils.isAutoBrightness(MainActivity.this);
                    Utils.updateSeekBar(MainActivity.this, mSeekbar, mAutomatic);
                } else {
                    mAutomatic = Utils.isAutoBrightness(MainActivity.this);
                    Utils.updateSeekBar(MainActivity.this, mSeekbar, mAutomatic);
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

    class HomeReceiver extends BroadcastReceiver {
        String REASON = "reason";
        String HOMEKEY = "homekey";
        String RECENTAPPS = "recentapps";

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive: home" + intent.getAction());
            if (intent.getAction().equals(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)) {
                String reason = intent.getStringExtra(REASON);
                if (TextUtils.equals(reason, HOMEKEY)) {
                    Log.d(TAG, "onReceive: home");
                    finish();
                } else if (TextUtils.equals(reason, RECENTAPPS)) {
                    Log.d(TAG, "onReceive: recent");
                    finish();
                }
            }
        }
    }
}
