package com.robindeits.luxit;

import android.support.v7.app.AppCompatActivity;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebSettings;
import android.os.Handler;
import android.view.Window;

import java.net.InetAddress;

// http://www.dodgycoder.net/2015/02/setting-up-bonjourzeroconfmdnsnsd.html

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getName();
    private static final String SERVICE_TYPE = "_http._tcp.";
    private static final String LOCAL_NAME = "lights";
    private static final int PORT = 80;
    final Handler mHandler = new Handler();

    final Runnable mUpdateWebView = new Runnable() {
        @Override
        public void run() {
            loadWebView();
        }
    };

    NsdManager mNsdManager;
    NsdManager.DiscoveryListener mDiscoveryListener;
    NsdManager.ResolveListener mResolveListener;
    NsdServiceInfo mServiceInfo;
    String mRPiAddress;
    WebView mWebView;

    protected void loadWebView() {
        mWebView.loadUrl("http://" + mRPiAddress + ":" + PORT);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mWebView = findViewById(R.id.webview);
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        mRPiAddress = "";
    }

    protected void onResume() {
        super.onResume();
        mNsdManager = (NsdManager) this.getSystemService(this.NSD_SERVICE);
        if (mRPiAddress.equals("")) {
            initializeResolveListener();
            initializeDiscoveryListener();
            mNsdManager.discoverServices(SERVICE_TYPE,
                    NsdManager.PROTOCOL_DNS_SD,
                    mDiscoveryListener);
        } else {
            loadWebView();
        }
    }

    public void initializeDiscoveryListener() {

        // Instantiate a new DiscoveryListener
        mDiscoveryListener = new NsdManager.DiscoveryListener() {

            // Called as soon as service discovery begins.
            @Override
            public void onDiscoveryStarted(String regType) {
                Log.d(TAG, "Service discovery started");
            }

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                // A service was found! Do something with it.
                String name = service.getServiceName();
                String type = service.getServiceType();
                Log.d(TAG, "Service discovery success" + service);
                Log.d(TAG, "Service type: " + type);
                Log.d(TAG, "Service name: " + name);
                if (type.equals(SERVICE_TYPE) && name.contains(LOCAL_NAME)) {
                    Log.d(TAG, "Service found @ '" + name + "'");
                    mNsdManager.resolveService(service, mResolveListener);
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo service) {
                // When the network service is no longer available.
                // Internal bookkeeping code goes here.
                Log.e(TAG, "service lost" + service);
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.i(TAG, "Discovery stopped: " + serviceType);
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                mNsdManager.stopServiceDiscovery(this);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                mNsdManager.stopServiceDiscovery(this);
            }
        };
    }

    private void initializeResolveListener() {
        mResolveListener = new NsdManager.ResolveListener() {
            @Override
            public void onResolveFailed(NsdServiceInfo nsdServiceInfo, int errorCode) {
                Log.e(TAG, "Resolve failed" + errorCode);
            }

            @Override
            public void onServiceResolved(NsdServiceInfo nsdServiceInfo) {
                mServiceInfo = nsdServiceInfo;
                int port = mServiceInfo.getPort();
                InetAddress host = mServiceInfo.getHost();
                Log.d(TAG, "host: " + host);
                String address = host.getHostAddress();
                Log.d(TAG, "Resolved address = " + address);
                mRPiAddress = host.toString();
                mHandler.post(mUpdateWebView);
            }
        };
    }
}
