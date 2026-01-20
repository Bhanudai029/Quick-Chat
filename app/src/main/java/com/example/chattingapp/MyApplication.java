package com.example.chattingapp;

import android.app.Application;
import com.example.chattingapp.utils.NetworkHelper;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize Supabase
        SupabaseClientHelper.init();
        
        // Initialize Network Helper for auto-reconnect
        NetworkHelper.getInstance().init(this);
    }
}
