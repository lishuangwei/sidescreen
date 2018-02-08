package com.v.smartassistant.sidescreen.Activity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.v.smartassistant.sidescreen.GridUtils.Utils;
import com.v.smartassistant.sidescreen.R;


/**
 * Created by lishuangwei on 17-12-11.
 */

public class TouchPosition extends Activity implements View.OnClickListener {
    private ImageView image_left, image_right, image_choose_left, image_choose_right;
    private LinearLayout mLeftbutton, mRightbutton;
    //记录当前所选项 取值0,1 分别为右左 默认0
    private int mState = 0;
    private SharedPreferences mShare;
    private SharedPreferences.Editor mEditor;
    //记录右侧上下所选块位置 取值0,1,2 分别为上中下默认1
    private int mRightPosition = 1;
    //记录左侧上下所选块位置 取值0,1,2 分别为上中下默认1
    private int mLeftPosition = 1;
    private ImageView mLeftTop, mLeftCenter, mLeftBottom, mRightTop, mRightCenter, mRightBottom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.touchposition);
        init();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void init() {
        image_left = (ImageView) findViewById(R.id.image_left);
        image_right = (ImageView) findViewById(R.id.image_right);
        image_choose_left = (ImageView) findViewById(R.id.choose_left);
        image_choose_right = (ImageView) findViewById(R.id.choose_right);
        image_left.setOnClickListener(this);
        image_right.setOnClickListener(this);
        image_choose_left.setOnClickListener(this);
        image_choose_right.setOnClickListener(this);

        mLeftbutton = (LinearLayout) findViewById(R.id.left_position);
        mRightbutton = (LinearLayout) findViewById(R.id.right_position);
        mLeftTop = (ImageView) findViewById(R.id.left_top);
        mLeftCenter = (ImageView) findViewById(R.id.left_center);
        mLeftBottom = (ImageView) findViewById(R.id.left_bottom);
        mRightTop = (ImageView) findViewById(R.id.right_top);
        mRightCenter = (ImageView) findViewById(R.id.right_center);
        mRightBottom = (ImageView) findViewById(R.id.right_bottom);
        mLeftTop.setOnClickListener(this);
        mLeftCenter.setOnClickListener(this);
        mLeftBottom.setOnClickListener(this);
        mRightTop.setOnClickListener(this);
        mRightCenter.setOnClickListener(this);
        mRightBottom.setOnClickListener(this);

        mShare = getSharedPreferences("touch_position", MODE_PRIVATE);
        mEditor = mShare.edit();
        mState = mShare.getInt("position", 0);
        initPosition(mState);

        mLeftPosition = mShare.getInt("left_position", 1);
        mRightPosition = mShare.getInt("right_position", 1);
        initSlidePosition(mLeftPosition, mRightPosition);
        Log.d("shuang", "init: left" + mLeftPosition + "---" + mRightPosition);
    }

    private void sendMyBroadcast(String action) {
        Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.image_left:
                if (mState == 0) updatePosttion(0);
                sendMyBroadcast(Utils.ACTION_MOVR_LEFT);
                break;
            case R.id.image_right:
                if (mState == 1) updatePosttion(1);
                sendMyBroadcast(Utils.ACTION_MOVR_RIGHT);
                break;
            case R.id.choose_left:
                if (mState == 0) updatePosttion(0);
                sendMyBroadcast(Utils.ACTION_MOVR_LEFT);
                break;
            case R.id.choose_right:
                if (mState == 1) updatePosttion(1);
                sendMyBroadcast(Utils.ACTION_MOVR_RIGHT);
                break;
            case R.id.left_top:
                updateSlidePosition(R.id.left_top);
                sendMyBroadcast(Utils.ACTION_LEFT_TOP);
                break;
            case R.id.left_center:
                updateSlidePosition(R.id.left_center);
                sendMyBroadcast(Utils.ACTION_LEFT_CENTER);
                break;
            case R.id.left_bottom:
                updateSlidePosition(R.id.left_bottom);
                sendMyBroadcast(Utils.ACTION_LEFT_BOTTOM);
                break;
            case R.id.right_top:
                updateSlidePosition(R.id.right_top);
                sendMyBroadcast(Utils.ACTION_RIGHT_TOP);
                break;
            case R.id.right_center:
                updateSlidePosition(R.id.right_center);
                sendMyBroadcast(Utils.ACTION_RIGHT_CENTER);
                break;
            case R.id.right_bottom:
                updateSlidePosition(R.id.right_bottom);
                sendMyBroadcast(Utils.ACTION_RIGHT_BOTTOM);

                break;
            default:
                break;
        }

    }

    private void updateSlidePosition(int id) {
        switch (id) {
            case R.id.left_top:
                resetSlide();
                mLeftTop.setImageResource(R.drawable.left_side_press);
                mLeftPosition = 0;
                mEditor.putInt("left_position", mLeftPosition);
                mEditor.commit();
                break;
            case R.id.left_center:
                resetSlide();
                mLeftCenter.setImageResource(R.drawable.left_side_press);
                mLeftPosition = 1;
                mEditor.putInt("left_position", mLeftPosition);
                mEditor.commit();
                break;
            case R.id.left_bottom:
                resetSlide();
                mLeftBottom.setImageResource(R.drawable.left_side_press);
                mLeftPosition = 2;
                mEditor.putInt("left_position", mLeftPosition);
                mEditor.commit();
                break;
            case R.id.right_top:
                resetSlide();
                mRightTop.setImageResource(R.drawable.right_side_press);
                mRightPosition = 0;
                mEditor.putInt("right_position", mRightPosition);
                mEditor.commit();
                break;
            case R.id.right_center:
                resetSlide();
                mRightCenter.setImageResource(R.drawable.right_side_press);
                mRightPosition = 1;
                mEditor.putInt("right_position", mRightPosition);
                mEditor.commit();
                break;
            case R.id.right_bottom:
                resetSlide();
                mRightBottom.setImageResource(R.drawable.right_side_press);
                mRightPosition = 2;
                mEditor.putInt("right_position", mRightPosition);
                mEditor.commit();
                break;

        }

    }


    private void initPosition(int state) {
        if (state == 0) {
            image_left.setImageResource(R.drawable.left_normal);
            image_right.setImageResource(R.drawable.right_press);
            image_choose_left.setImageResource(R.drawable.choose_normal);
            image_choose_right.setImageResource(R.drawable.choose_press);
            mRightbutton.setVisibility(View.VISIBLE);
            mLeftbutton.setVisibility(View.GONE);
        } else {
            image_left.setImageResource(R.drawable.left_press);
            image_right.setImageResource(R.drawable.right_normal);
            image_choose_left.setImageResource(R.drawable.choose_press);
            image_choose_right.setImageResource(R.drawable.choose_normal);
            mRightbutton.setVisibility(View.GONE);
            mLeftbutton.setVisibility(View.VISIBLE);
        }
    }

    private void initSlidePosition(int left, int right) {
        resetSlide();
        if (left == 0) {
            mLeftTop.setImageResource(R.drawable.left_side_press);
        } else if (left == 1) {
            mLeftCenter.setImageResource(R.drawable.left_side_press);
        } else {
            mLeftBottom.setImageResource(R.drawable.left_side_press);
        }
        if (right == 0) {
            mRightTop.setImageResource(R.drawable.right_side_press);
        } else if (right == 1) {
            mRightCenter.setImageResource(R.drawable.right_side_press);
        } else {
            mRightBottom.setImageResource(R.drawable.right_side_press);
        }
    }

    private void updatePosttion(int state) {
        if (state == 0) {
            image_left.setImageResource(R.drawable.left_press);
            image_right.setImageResource(R.drawable.right_normal);
            image_choose_left.setImageResource(R.drawable.choose_press);
            image_choose_right.setImageResource(R.drawable.choose_normal);
            mRightbutton.setVisibility(View.GONE);
            mLeftbutton.setVisibility(View.VISIBLE);
            mState = 1;
        } else {
            image_left.setImageResource(R.drawable.left_normal);
            image_right.setImageResource(R.drawable.right_press);
            image_choose_left.setImageResource(R.drawable.choose_normal);
            image_choose_right.setImageResource(R.drawable.choose_press);
            mRightbutton.setVisibility(View.VISIBLE);
            mLeftbutton.setVisibility(View.GONE);
            mState = 0;
        }
        mEditor.putInt("position", mState);
        mEditor.commit();
    }

    private void resetSlide() {
        if (mState == 1) {
            mLeftTop.setImageResource(R.drawable.left_side_normal);
            mLeftCenter.setImageResource(R.drawable.left_side_normal);
            mLeftBottom.setImageResource(R.drawable.left_side_normal);
        } else {
            mRightTop.setImageResource(R.drawable.right_side_normal);
            mRightCenter.setImageResource(R.drawable.right_side_normal);
            mRightBottom.setImageResource(R.drawable.right_side_normal);
        }
    }

    @Override
    public void onBackPressed() {
        Log.d("shuang", "onBackPressed");
        Intent intent = new Intent();
        intent.putExtra("position", mState);
        setResult(RESULT_OK, intent);
        super.onBackPressed();
    }
}
