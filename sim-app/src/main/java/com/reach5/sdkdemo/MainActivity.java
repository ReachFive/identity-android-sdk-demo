package com.reach5.sdkdemo;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.sim.sdk.R5Client;
import com.sim.sdk.R5Exception;
import com.sim.sdk.model.ProviderEnum;
import com.sim.sdk.model.R5InitResponse;
import com.sim.sdk.model.R5LoginResponse;

import java.util.Arrays;

public class MainActivity extends Activity implements R5Client.R5LoginListener {

    private ProgressDialog simSDKInitProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        try {
            //initialize SDK
            R5Client.initWithCallback(this, "9rnkdhnwC1nzKpQ3yFTE", "reach5-stg.og4.me", null, new R5Client.R5InitCallback() {
                @Override
                public void success(R5InitResponse response) {
                    String providers = Arrays.toString(response.getProviders().toArray());
                    Log.i("Activity", "Initialized providers = " + providers);
                    ((TextView) findViewById(R.id.textView)).setText("Initialized providers = " + providers);
                }

                @Override
                public void failure(R5Exception e) {
                    showError(e);
                }
            });
            // register the login callback
            R5Client.registerLoginListener(this);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void success(R5LoginResponse response) {
        Log.i("Activity", "r5LoginViewCallback response=" + response.toString());
        ((TextView) findViewById(R.id.textView)).setText(response.toString());
        if (simSDKInitProgressBar != null) {
            simSDKInitProgressBar.dismiss();
            simSDKInitProgressBar = null;
        }
    }

    @Override
    public void failure(R5Exception error) {
        showError(error);
        if (simSDKInitProgressBar != null) {
            simSDKInitProgressBar.dismiss();
            simSDKInitProgressBar = null;
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        R5Client.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        R5Client.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    protected void onStop() {
        super.onStop();
        R5Client.onStop();
    }


    public void onTest1BtClick(View v) {
        Log.d("MainActivity", "onTest1BtClick");
        ((TextView) findViewById(R.id.textView)).setText(null);

        R5Client.displayLoginButtons(this, "homepage");
    }

    public void onTest2BtClick(View v) {
        ((TextView) findViewById(R.id.textView)).setText(null);
        try {
            switch (v.getId()) {
                case R.id.button2:
                    R5Client.loginWithProvider(this, ProviderEnum.facebook, "homepage");
                    break;
                case R.id.button3:
                    simSDKInitProgressBar = ProgressDialog.show(MainActivity.this, "Please wait ...", "Contacting Google...", true);
                    R5Client.loginWithProvider(this, ProviderEnum.google, "homepage");
                    break;
                case R.id.button4:
                    simSDKInitProgressBar = ProgressDialog.show(MainActivity.this, "Please wait ...", "Contacting Twitter...", true);
                    R5Client.loginWithProvider(this, ProviderEnum.twitter, null);
                    break;
                case R.id.button5:
                    simSDKInitProgressBar = ProgressDialog.show(MainActivity.this, "Please wait ...", "Contacting Paypal...", true);
                    R5Client.loginWithProvider(this, ProviderEnum.paypal, "homepage");
                    break;
                case R.id.btn_logout:
                    R5Client.logout();
                    break;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showError(R5Exception error) {
        Log.e("Activity", error.getMessage(), error);
        ((TextView) findViewById(R.id.textView))
                .setText(String.format("R5 SDK Error !\n%s (code=%s)", error.getMessage(), error.getCode()));

    }

}
