package com.example.chattingapp.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import androidx.annotation.NonNull;

public class NetworkHelper {
    
    private static NetworkHelper instance;
    private ConnectivityManager connectivityManager;
    private boolean isConnected = true;
    private NetworkCallback networkCallback;
    
    public interface NetworkCallback {
        void onConnectionChanged(boolean isConnected);
    }
    
    private NetworkHelper() {}
    
    public static synchronized NetworkHelper getInstance() {
        if (instance == null) {
            instance = new NetworkHelper();
        }
        return instance;
    }
    
    public void init(Context context) {
        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        registerNetworkCallback();
    }
    
    public void setNetworkCallback(NetworkCallback callback) {
        this.networkCallback = callback;
    }
    
    private void registerNetworkCallback() {
        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();
        
        connectivityManager.registerNetworkCallback(request, new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                isConnected = true;
                if (networkCallback != null) {
                    networkCallback.onConnectionChanged(true);
                }
            }
            
            @Override
            public void onLost(@NonNull Network network) {
                isConnected = false;
                if (networkCallback != null) {
                    networkCallback.onConnectionChanged(false);
                }
            }
        });
    }
    
    public boolean isConnected() {
        return isConnected;
    }
}
