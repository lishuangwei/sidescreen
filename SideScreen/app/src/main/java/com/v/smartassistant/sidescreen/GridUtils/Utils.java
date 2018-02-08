package com.v.smartassistant.sidescreen.GridUtils;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.Settings;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.Toast;

import com.v.smartassistant.sidescreen.Activity.MainActivity;
import com.v.smartassistant.sidescreen.FloatManager.FloatBall;
import com.v.smartassistant.sidescreen.FloatManager.FloatBallManager;
import com.v.smartassistant.sidescreen.R;

/**
 * Created by lishuangwei on 17-12-25.
 */

public class Utils {
    public static final int FLASH_CLOSE = 0;
    public static final int FLASH_OPEN = 1;
    public static final float BRIGHTNESS_ADJ_RESOLUTION = 2048;
    public static final int BRIGHTNESS_MANUAL_RESOLUTION = 255;
    private static final float BITMAP_SCALE = 0.4f;

    public static final String ACTION_SHOW_FLOAT = "action.show.float";
    public static final String ACTION_HIDE_FLOAT = "action.hide.float";
    public static final String ACTION_MOVR_LEFT = "action.right.to.left";
    public static final String ACTION_MOVR_RIGHT = "action.left.to.right";
    public static final String ACTION_LEFT_TOP = "action.left.top";
    public static final String ACTION_LEFT_CENTER = "action.left.center";
    public static final String ACTION_LEFT_BOTTOM = "action.left.bottom";
    public static final String ACTION_RIGHT_TOP = "action.right.top";
    public static final String ACTION_RIGHT_CENTER = "action.right.center";
    public static final String ACTION_RIGHT_BOTTOM = "action.right.bottom";
    public static final int INIT_TOUCH_POSITION = 80;
    public static final int OFFSET_TOUCH_POSITION = 519;
    public static final int OFFSET_TOUCH_POSITION_LAND = -36;
    public static final int HIDE_FLOAT = 0;
    public static final int SHOW_FLOAT = 1;
    public static final int MOVE_FROM_LEFT = 2;
    public static final int MOVE_FROM_RIGHT = 3;
    public static final int MOVE_LEFT_TOP = 4;
    public static final int MOVE_LEFT_CENTER = 5;
    public static final int MOVE_LEFT_BOTTOM = 6;
    public static final int MOVE_RIGHT_TOP = 7;
    public static final int MOVE_RIGHT_CENTER = 8;
    public static final int MOVE_RIGHT_BOTTOM = 9;
    public static final int SET_BLUR_BLACKGROUND = 10;

    public static final int TOP_POSITION = 200;
    public static final int CENTER_POSITION = 600;
    public static final int BOTTOM_POSITION = 1000;

    public static int getSystemBrightness(Context context) {
        int systemBrightness = 0;
        try {
            systemBrightness = Settings.System.getInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        return systemBrightness;
    }

    public static boolean isAutoBrightness(Context context) {
        boolean autoBrightness = false;
        ContentResolver contentResolver = context.getContentResolver();
        try {
            autoBrightness = Settings.System.getInt(contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS_MODE) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        return autoBrightness;
    }

    public static void setScreenMode(int mode, Context context) {
        try {
            Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, mode);
        } catch (Exception localException) {
            localException.printStackTrace();
        }
    }

    public static void setBrightness(final Context context, final int value, Boolean auto) {
        if (!auto) {
            AsyncTask.execute(new Runnable() {
                public void run() {
                    Settings.System.putInt(context.getContentResolver(),
                            Settings.System.SCREEN_BRIGHTNESS, value);
                }
            });
        }
    }

    public static void updateSeekBar(Context context, SeekBar seek, boolean auto) {
        if (auto) {
            float value = Settings.System.getFloat(context.getContentResolver(),
                    "screen_auto_brightness_adj", 0);
            seek.setMax((int) BRIGHTNESS_ADJ_RESOLUTION);
            seek.setProgress((int) ((value + 1) * BRIGHTNESS_ADJ_RESOLUTION / 2f));
        } else {
            int value = Settings.System.getInt(context.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS, BRIGHTNESS_MANUAL_RESOLUTION);
            seek.setMax(BRIGHTNESS_MANUAL_RESOLUTION);
            seek.setProgress(value);
        }
    }

    public static WindowManager.LayoutParams getLayoutParams() {
        WindowManager.LayoutParams mLayoutParams = new WindowManager.LayoutParams();
        mLayoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        mLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        mLayoutParams.format = PixelFormat.RGBA_8888;
//        mLayoutParams.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        mLayoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        mLayoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        return mLayoutParams;
    }

    public static int dip2px(Context context, float dpValue) {
        final float scale = getScale(context);
        return (int) (dpValue * scale + 0.5f);
    }

    private static float getScale(Context context) {
        final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        return findScale(fontScale);
    }

    private static float findScale(float scale) {
        if (scale <= 1) {
            scale = 1;
        } else if (scale <= 1.5) {
            scale = 1.5f;
        } else if (scale <= 2) {
            scale = 2f;
        } else if (scale <= 3) {
            scale = 3f;
        }
        return scale;
    }

    public static Bitmap blurBitmap(Context context, Bitmap image, float blurRadius) {
        int width = Math.round(image.getWidth() * BITMAP_SCALE);
        int height = Math.round(image.getHeight() * BITMAP_SCALE);
        Bitmap inputBitmap = Bitmap.createScaledBitmap(image, width, height, false);
        Bitmap outputBitmap = Bitmap.createBitmap(inputBitmap);

        RenderScript rs = RenderScript.create(context);
        ScriptIntrinsicBlur blurScript = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));

        Allocation tmpIn = Allocation.createFromBitmap(rs, inputBitmap);
        Allocation tmpOut = Allocation.createFromBitmap(rs, outputBitmap);

        blurScript.setRadius(blurRadius);
        blurScript.setInput(tmpIn);
        blurScript.forEach(tmpOut);
        tmpOut.copyTo(outputBitmap);

        return outputBitmap;
    }

    public static boolean isNeedPermission(Activity activity) {
        if (!Settings.canDrawOverlays(activity) || (!Settings.System.canWrite(activity))) {
            Toast.makeText(activity, activity.getString(R.string.request_permission), Toast.LENGTH_SHORT).show();
            return true;
        }
        return false;
    }

    public static int getOffset(FloatBallManager floatBallManager) {
        if (floatBallManager.isLand()) {
            return 0;
        } else {
            return Utils.OFFSET_TOUCH_POSITION;
        }
    }

    public static int getInit(FloatBallManager floatBallManager) {
        if (floatBallManager.isLand()) {
            return -64;
        } else {
            return Utils.INIT_TOUCH_POSITION;
        }
    }
}
