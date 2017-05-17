package com.reach5.sdkdemo;

import com.reach5.android.ReachFive;
import com.reach5.android.callback.R5InitCallback;
import com.reach5.android.callback.R5LoginCallback;
import com.reach5.android.exception.R5Exception;
import com.reach5.android.model.R5InitResponse;
import com.reach5.android.model.R5LoginResponse;
import com.reach5.android.model.R5NativeProviderEnum;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {

    private ProgressDialog mSimSDKInitProgressBar;

    private ReachFive mReachFive;

    private EditText mProviderEditText;

    private ScrollView mScrollView;

    private TextView mMessageTextView;

    private R5LoginCallback mR5LoginCallback = new R5LoginCallback() {
        @Override
        public void success(@NonNull R5LoginResponse response) {
            showMessage(response.toString());
            hideProgress();
        }

        @Override
        public void failure(@NonNull R5Exception error) {
            showError(error);
            hideProgress();
        }
    };

    private R5InitCallback mR5InitCallback = new R5InitCallback() {
        @Override
        public void success(@NonNull R5InitResponse response) {
            showMessage("Initialized providers = " + response.getProviderNameList());
            hideProgress();
        }

        @Override
        public void failure(@NonNull R5Exception error) {
            showError(error);
            hideProgress();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mScrollView = (ScrollView) findViewById(R.id.scrollView);
        mMessageTextView = (TextView) findViewById(R.id.textView);
        mProviderEditText = (EditText) findViewById(R.id.provider_text);

        showProgress();

        mReachFive = ReachFive.newClient()
                .withContext(this)
                .withDomain("reach5-stg.og4.me")
                .withLogs(true)
                .withCallback(mR5InitCallback)
                .init();

    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mReachFive.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        mReachFive.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    protected void onStop() {
        super.onStop();
        mReachFive.onStop();
    }

    public void onButtonClicked(View v) {
        try {
            switch (v.getId()) {
                case R.id.facebook_button:
                    mReachFive.newLoginRequest()
                            .withOrigin("homepage")
                            .withCallback(mR5LoginCallback)
                            .withProvider(R5NativeProviderEnum.FACEBOOK)
                            .withActivity(this)
                            .execute();
                    break;
                case R.id.google_button:
                    mReachFive.newLoginRequest()
                            .withCallback(mR5LoginCallback)
                            .withProvider(R5NativeProviderEnum.GOOGLE)
                            .withActivity(this)
                            .execute();
                    break;
                case R.id.provider_button:

                    String inputText = mProviderEditText.getText().toString().toLowerCase();

                    mReachFive.newLoginRequest()
                            .withOrigin("homepage")
                            .withCallback(mR5LoginCallback)
                            .withProvider(inputText)
                            .withActivity(this)
                            .execute();
                    break;
            }

        } catch (Exception e) {
            showMessage(e.getMessage());
        }
    }

    public void showProgress() {
        mSimSDKInitProgressBar = ProgressDialog.show(MainActivity.this, null, "Please wait...", true);
    }

    public void hideProgress() {
        if (mSimSDKInitProgressBar != null) {
            mSimSDKInitProgressBar.hide();
            mSimSDKInitProgressBar = null;
        }
    }

    private void showMessage(@NonNull String text) {

        String content = mMessageTextView.getText().toString();

        if (!content.isEmpty()) {
            content += "\n";
        }

        content += "-----------------\n";
        content += text;
        content += "\n-----------------";

        mMessageTextView.setText(content);

        mMessageTextView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mMessageTextView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                mScrollView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });


    }

    private void showError(R5Exception error) {
        Log.e("Activity", error.getMessage(), error);
        showMessage(String.format("R5 SDK Error !\n%s (code=%s)", error.getMessage(), error.getCode()));
    }

}
