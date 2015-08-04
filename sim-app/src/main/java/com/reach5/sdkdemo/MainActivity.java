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
import com.sim.sdk.model.R5LoginResponse;

public class MainActivity extends Activity implements R5Client.R5LoginListener{

    private ProgressDialog simSDKInitProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        try {
            //initialize SDK
            R5Client.init(this, "BvnynWPdlUOGwC3yERXi", "reach5.og4.me", null);
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
        ((TextView) findViewById(R.id.textView))
                .setText(String.format("Sim SDK Response Error !\n%s (code=%s)", error.getMessage(), error.getCode()));
        if (simSDKInitProgressBar != null) {
            simSDKInitProgressBar.dismiss();
            simSDKInitProgressBar = null;
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        R5Client.onActivityResult(requestCode, resultCode, data);
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
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
