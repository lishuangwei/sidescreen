package com.v.smartassistant.sidescreen.Activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.SwitchPreference;
import android.util.Log;

import com.v.smartassistant.sidescreen.GridUtils.Utils;
import com.v.smartassistant.sidescreen.R;
import com.v.smartassistant.sidescreen.Service.FloatballService;


/**
 * Created by lishuangwei on 17-12-8.
 */

public class SideScreen extends PreferenceActivity implements Preference.OnPreferenceChangeListener {
    public static final int REQUEST_POSITION = 3;
    private SwitchPreference mSwitch;
    private Preference mScreen;
    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.prefer);
        addPreferencesFromResource(R.xml.mypreference);
        initPreference();
    }

    private void initPreference() {
        mSwitch = (SwitchPreference) findPreference("crooked_switch");
        mSwitch.setOnPreferenceChangeListener(this);
        if (mSwitch.isChecked()) {
            Intent intent = new Intent(Utils.ACTION_SHOW_FLOAT);
            sendBroadcast(intent);
        }

        mScreen = findPreference("touch_position");
        sharedPreferences = getSharedPreferences("touch_position", MODE_PRIVATE);
        int state = sharedPreferences.getInt("position", 0);
        mScreen.setSummary(state == 0 ? R.string.touch_position_right : R.string.touch_position_left);
        mScreen.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(SideScreen.this, TouchPosition.class);
                startActivityForResult(intent, REQUEST_POSITION);
                return true;
            }
        });
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Log.d("shuang", "onPreferenceChange: 1111");
        if (preference.getKey().equals("crooked_switch")) {
            boolean show = (boolean) newValue;
            if (show) {
                Intent intent = new Intent(this, FloatballService.class);
                intent.putExtra("showfloat", true);
                startService(intent);
            } else {
                Intent intent = new Intent(Utils.ACTION_HIDE_FLOAT);
                sendBroadcast(intent);
            }
        }
        return true;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK)
            if (requestCode == REQUEST_POSITION) {
                int state = data.getIntExtra("position", 0);
                Log.d("shuang", "onActivityResult: state  " + state);
                mScreen.setSummary(state == 0 ? R.string.touch_position_right : R.string.touch_position_left);
            }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
