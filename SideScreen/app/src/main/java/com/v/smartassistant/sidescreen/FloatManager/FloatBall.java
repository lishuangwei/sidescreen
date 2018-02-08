package com.v.smartassistant.sidescreen.FloatManager;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Vibrator;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.v.smartassistant.sidescreen.Activity.MainActivity;
import com.v.smartassistant.sidescreen.CustomView.VDHLayout;
import com.v.smartassistant.sidescreen.GridUtils.Utils;
import com.v.smartassistant.sidescreen.R;

import static android.content.Context.MODE_PRIVATE;


public class FloatBall extends FrameLayout implements ICarrier {

    private FloatBallManager floatBallManager;
    private ImageView imageView;
    private WindowManager.LayoutParams mLayoutParams;
    private WindowManager windowManager;
    private boolean isAdded = false;
    private int mTouchSlop;
    //判断手势方向
    private int mCurrentMode;
    private final static int MODE_NONE = 0x01;
    private final static int MODE_RIGHT = 0x02;
    private final static int MODE_LEFT = 0x03;
    private final static int MODE_MOVE = 0x04;

    private boolean mIsLongTouch;
    private boolean mIsTouching;
    private final static long LONG_CLICK_LIMIT = 300;
    private long mLastDownTime;
    private Vibrator mVibrator;
    private long[] mPattern = {0, 100};
    private Context mContext;

    private boolean mIsLand;
    //保存滑块位置
    private SharedPreferences mShare;
    private SharedPreferences.Editor mEdit;
    private boolean mIsright = true;
    private int mRihgtPosition, mLeftPosition;

    private boolean isClick;
    private int mDownX, mDownY, mLastX, mLastY;
    private int mSize;
    private ScrollRunner mRunner;
    private int mVelocityX, mVelocityY;
    private MotionVelocityUtil mVelocity;
    private boolean sleep = false;
    private VDHLayout mLayout;
    private OnceRunnable mSleepRunnable = new OnceRunnable() {
        @Override
        public void onRun() {
            if (isAdded) {
                sleep = true;
                moveToEdge(false, sleep);
            }
        }
    };

    public FloatBall(Context context, FloatBallManager floatBallManager) {
        super(context);
        this.floatBallManager = floatBallManager;

        mContext = context;
        init(context);
    }

    private void init(Context context) {
        imageView = new ImageView(context);
        imageView.setFocusable(false);
        imageView.setImageResource(R.drawable.slide_right);
        addView(imageView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        View v = LayoutInflater.from(mContext).inflate(R.layout.custom_layout, null);
        //addView(v);
        mLayout = v.findViewById(R.id.customLay);
        mLayoutParams = Utils.getLayoutParams();
        Log.d("xiao", "init: " + mLayoutParams.x + "----" + mLayoutParams.y);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mRunner = new ScrollRunner(this);
        mVelocity = new MotionVelocityUtil(context);

        mCurrentMode = MODE_NONE;
        mVibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        mShare = mContext.getSharedPreferences("position", MODE_PRIVATE);
        mEdit = mShare.edit();
        initPosition();
    }

    public void updatePosition(boolean right, int position) {
        imageView.setImageResource(mIsright ? R.drawable.slide_right : R.drawable.slide_left);
        if (right) {
            mLayoutParams.gravity = Gravity.RIGHT;
            mLayoutParams.y = !mIsLand ? position : Utils.OFFSET_TOUCH_POSITION_LAND;
            mRihgtPosition = position;
            imageView.setImageResource(R.drawable.slide_right);
        } else {
            mLayoutParams.gravity = Gravity.LEFT;
            mLayoutParams.y = !mIsLand ? position : Utils.OFFSET_TOUCH_POSITION_LAND;
            mLeftPosition = position;
            imageView.setImageResource(R.drawable.slide_left);
        }
        Log.d("shuang", "updatePosition: right" + right + "---" + position + "----" + mLayoutParams.gravity);
        mEdit.putBoolean("isright", right);
        mEdit.putInt("left_position", mLeftPosition);
        mEdit.putInt("right_position", mRihgtPosition);
        mEdit.commit();
        if (null != windowManager) windowManager.updateViewLayout(this, mLayoutParams);
    }

    public void updatePosition(boolean right) {
        imageView.setImageResource(mIsright ? R.drawable.slide_right : R.drawable.slide_left);
        if (right) {
            mLayoutParams.gravity = Gravity.RIGHT;
            mLayoutParams.y = !mIsLand ? mRihgtPosition : Utils.OFFSET_TOUCH_POSITION_LAND;
            imageView.setImageResource(R.drawable.slide_right);
        } else {
            mLayoutParams.gravity = Gravity.LEFT;
            mLayoutParams.y = !mIsLand ? mLeftPosition : Utils.OFFSET_TOUCH_POSITION_LAND;
            imageView.setImageResource(R.drawable.slide_left);
        }
        mEdit.putBoolean("isright", right);
        mEdit.commit();
        if (null != windowManager) windowManager.updateViewLayout(this, mLayoutParams);
    }

    private void initPosition() {
        mIsright = mShare.getBoolean("isright", true);
        mLeftPosition = mShare.getInt("left_position", Utils.INIT_TOUCH_POSITION);
        mRihgtPosition = mShare.getInt("right_position", Utils.INIT_TOUCH_POSITION);
        if (mIsright) {
            mLayoutParams.gravity = Gravity.RIGHT;
            mLayoutParams.y = !mIsLand ? mRihgtPosition : Utils.OFFSET_TOUCH_POSITION_LAND;
            imageView.setImageResource(R.drawable.slide_right);
        } else {
            mLayoutParams.gravity = Gravity.LEFT;
            mLayoutParams.y = !mIsLand ? mLeftPosition : Utils.OFFSET_TOUCH_POSITION_LAND;
            imageView.setImageResource(R.drawable.slide_left);
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
            Log.d("xiao", "attachwindow floatball: " + mLayout.getMeasuredWidth() + "," + mLayout.getMeasuredHeight());
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
        mIsLand = mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        initPosition();
        moveToEdge(false, false);
        postSleepRunnable();
    }

    public boolean isLand() {
        return mIsLand;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.d("lishuang", "float onTouchEvent: " + event.getAction());
        int action = event.getAction();
        int x = (int) event.getRawX();
        int y = (int) event.getRawY();
        mVelocity.acquireVelocityTracker(event);
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                Log.d("lishuang", "float onTouchEvent:actiondown ");
                touchDown(x, y);
                break;
            case MotionEvent.ACTION_MOVE:
                Log.d("lishuang", "float onTouchEvent:actionmove ");
                touchMove(x, y);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                Log.d("lishuang", "float onTouchEvent:actionup ");
                //touchUp();
                mIsTouching = false;
                if (mIsLongTouch) {
                    mIsLongTouch = false;
                } else if (isClick) {
                    onClick();
                } else {
                    doUp();
                }
                mCurrentMode = MODE_NONE;
                break;
            case MotionEvent.ACTION_OUTSIDE:
                mLayout.closeDrawer();
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
        //removeSleepRunnable();

        mIsTouching = true;
        mLastDownTime = System.currentTimeMillis();
        postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isLongTouch()) {
                    mIsLongTouch = true;
                    mVibrator.vibrate(mPattern, -1);
                }
            }
        }, LONG_CLICK_LIMIT);
    }

    private void touchMove(int x, int y) {
        if (!mIsLongTouch && isTouchSlop(x, y)) {
            return;
        }
        int deltaX = x - mLastX;
        int deltaY = y - mLastY;
        if (!isTouchSlop(x, y)) {
            isClick = false;
        }
        mLastX = x;
        mLastY = y;
        if (!isClick) {
            //onMove(deltaX, deltaY);
        }
        if (mIsLongTouch && (mCurrentMode == MODE_NONE || mCurrentMode == MODE_MOVE)) {
            mCurrentMode = MODE_MOVE;
        } else {
            doGesture(x, y);
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
        } else {
            onMove(destX - mLayoutParams.x, destY);
            postSleepRunnable();
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
        moveToX(true, destX);
    }

    private void moveToEdge(boolean smooth, boolean forceSleep) {
        final int screenWidth = floatBallManager.mScreenWidth;
        int width = getWidth();
        int halfWidth = width / 2;
        int centerX = (screenWidth / 2 - halfWidth);
        int destX;
        final int minVelocity = mVelocity.getMinVelocity();
        if (mLayoutParams.x < centerX) {
            sleep = forceSleep ? true : Math.abs(mVelocityX) > minVelocity && mVelocityX < 0 || mLayoutParams.x < 0;
            destX = sleep ? -halfWidth : 0;
        } else {
            sleep = forceSleep ? true : Math.abs(mVelocityX) > minVelocity && mVelocityX > 0 || mLayoutParams.x > screenWidth - width;
            destX = sleep ? screenWidth - halfWidth : screenWidth - width;
        }
        //moveToX(smooth, destX);
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

    public int getSize() {
        return mSize;
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
        if (!sleep && isAdded) {
            mSleepRunnable.postDelaySelf(this, 3000);
        }
    }

    private void doUp() {
        Log.d("lishuang", "doUp: " + mCurrentMode);
        switch (mCurrentMode) {
            case MODE_LEFT:
                Intent intent = new Intent(mContext, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                intent.putExtra("orientation", Utils.MOVE_FROM_RIGHT);
                PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, intent, 0);
                try {
                    pendingIntent.send();
                } catch (PendingIntent.CanceledException e) {
                    e.printStackTrace();
                }
                break;
            case MODE_RIGHT:
                Intent intent1 = new Intent(mContext, MainActivity.class);
                intent1.setFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                intent1.putExtra("orientation", Utils.MOVE_FROM_LEFT);
                PendingIntent pendingIntent1 = PendingIntent.getActivity(mContext, 0, intent1, 0);
                try {
                    pendingIntent1.send();
                } catch (PendingIntent.CanceledException e) {
                    e.printStackTrace();
                }
                break;
            case MODE_MOVE:
                break;
            default:
                break;

        }

    }

    private void doGesture(int x, int y) {
        Log.d("shuang", "doGesture: ");
        float offsetX = x - mDownX;
        float offsetY = y - mDownY;

        if (Math.abs(offsetX) < mTouchSlop && Math.abs(offsetY) < mTouchSlop) {
            Log.d("shuang", "doGesture: 1");
            return;
        }
        if (Math.abs(offsetX) > Math.abs(offsetY)) {
            if (offsetX > 0) {
                Log.d("shuang", "doGesture: 2");
                if (mCurrentMode == MODE_RIGHT) {
                    return;
                }
                mCurrentMode = MODE_RIGHT;
            } else {
                Log.d("shuang", "doGesture: 3");
                if (mCurrentMode == MODE_LEFT) {
                    return;
                }
                mCurrentMode = MODE_LEFT;
            }
        }
    }

    private boolean isLongTouch() {
        long time = System.currentTimeMillis();
        if (mIsTouching && mCurrentMode == MODE_NONE && (time - mLastDownTime >= LONG_CLICK_LIMIT)) {
            return true;
        }
        return false;
    }

    private boolean isTouchSlop(int x, int y) {
        if (Math.abs(x - mDownX) < mTouchSlop && Math.abs(y - mDownY) < mTouchSlop) {
            return true;
        }
        return false;
    }
}
