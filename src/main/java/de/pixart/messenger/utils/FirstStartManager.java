package de.pixart.messenger.utils;

import android.content.Context;
import android.content.SharedPreferences;
import de.pixart.messenger.BuildConfig;

import static android.content.Context.MODE_PRIVATE;

public class FirstStartManager {
    Context context;
    private SharedPreferences pref;
    private SharedPreferences.Editor editor;
    private static final String PREF_NAME = BuildConfig.APPLICATION_ID;
    private static final String IS_FIRST_TIME_LAUNCH = "IsFirstTimeLaunch";

    public FirstStartManager(Context context) {
        this.context = context;
        pref = this.context.getSharedPreferences(PREF_NAME, MODE_PRIVATE);
    }

    public void setFirstTimeLaunch(boolean isFirstTime) {
        editor = pref.edit();
        editor.putBoolean(IS_FIRST_TIME_LAUNCH, isFirstTime);
        editor.commit();
    }

    public boolean isFirstTimeLaunch() {
        return pref.getBoolean(IS_FIRST_TIME_LAUNCH, true);
    }
}
