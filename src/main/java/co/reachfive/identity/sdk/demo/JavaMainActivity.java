package co.reachfive.identity.sdk.demo;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import co.reachfive.identity.sdk.core.JavaReachFive;
import co.reachfive.identity.sdk.core.Provider;
import co.reachfive.identity.sdk.core.models.AuthToken;
import co.reachfive.identity.sdk.core.models.OpenIdUser;
import co.reachfive.identity.sdk.core.models.SdkConfig;
import co.reachfive.identity.sdk.core.models.requests.ProfileSignupRequest;
import co.reachfive.identity.sdk.facebook.FacebookProvider;
import co.reachfive.identity.sdk.google.GoogleProvider;
import co.reachfive.identity.sdk.webview.WebViewProvider;
import io.github.cdimascio.dotenv.Dotenv;

public class JavaMainActivity extends AppCompatActivity {
    private static String TAG = "Reach5_MainActivity";
    private Dotenv dotenv = Dotenv.configure().directory("/assets").filename("env").load();
    private JavaReachFive reach5;
    private ProvidersAdapter providerAdapter;

  final int WEBAUTHN_LOGIN_REQUEST_CODE = 2;
  final int WEBAUTHN_SIGNUP_REQUEST_CODE = 3;
  final int REDIRECTION_REQUEST_CODE = 52558;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    setSupportActionBar(findViewById(R.id.toolbar));

        SdkConfig sdkConfig = new SdkConfig(
                Objects.requireNonNull(dotenv.get("DOMAIN")),
                Objects.requireNonNull(dotenv.get("CLIENT_ID")),
                Objects.requireNonNull(dotenv.get("SCHEME"))
        );

        Objects.requireNonNull(getSupportActionBar()).setTitle("Java Sdk Example");

    reach5 = new JavaReachFive(
        this,
        sdkConfig,
        Arrays.asList(new GoogleProvider(), new WebViewProvider(), new FacebookProvider())
    );

        reach5.initialize(providers ->
                        providerAdapter.refresh(providers)
                , error -> {
                    Log.d(TAG, "ReachFive init " + error.getMessage());
                    showToast("ReachFive init " + error.getMessage());
                });

    findViewById(R.id.weblogin).setOnClickListener(view -> {
      Set<String> scope = new HashSet<>(Arrays.asList("openid", "email", "profile", "phone_number", "offline_access", "events", "full_write"));
      reach5.loginWithWeb(scope,"state", "origin", "nonce");
    });

    providerAdapter = new ProvidersAdapter(getApplicationContext(), reach5.getProviders());

        ListView providers = findViewById(R.id.providers);
        providers.setAdapter(providerAdapter);

        providers.setOnItemClickListener((parent, view, position, id) -> {
            Provider provider = reach5.getProviders().get(position);
            Set<String> scope = new HashSet<>(Arrays.asList("openid", "email", "profile", "phone_number", "offline_access", "events", "full_write"));
            reach5.loginWithNativeProvider(provider.getName(), scope, "home", this);
        });

        EditText emailEditText = findViewById(R.id.email);
        EditText phoneNumberEditText = findViewById(R.id.phoneNumber);
        EditText passwordEditText = findViewById(R.id.password);

        findViewById(R.id.passwordSignup).setOnClickListener(view -> {
            ProfileSignupRequest signupRequest = (!emailEditText.getText().toString().isEmpty()) ?
                    new ProfileSignupRequest(
                            emailEditText.getText().toString(),
                            passwordEditText.getText().toString()
                    ) : new ProfileSignupRequest(
                    phoneNumberEditText.getText().toString(),
                    passwordEditText.getText().toString()
            );
            reach5.signup(
                    signupRequest,
                    this::handleLoginSuccess,
                    failure -> {
                        Log.d(TAG, "signup error=" + failure.getMessage());
                        showToast("Signup With Password Error " + failure.getMessage());
                    }
            );
        });

        findViewById(R.id.passwordLogin).setOnClickListener(view -> {
            String username = (!emailEditText.toString().isEmpty())
                    ? emailEditText.getText().toString() : phoneNumberEditText.getText().toString();
            reach5.loginWithPassword(
                    username,
                    passwordEditText.getText().toString(),
                    this::handleLoginSuccess,
                    failure -> {
                        Log.d(TAG, "loginWithPassword error=" + failure.getMessage());
                        showToast("Login With Password Error " + failure.getMessage());
                    }
            );
        });

    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void handleLoginSuccess(AuthToken authToken) {
        OpenIdUser user = authToken.getUser();
        Objects.requireNonNull(getSupportActionBar()).setTitle(user.getEmail());
        showToast("Login success " + authToken.getAccessToken());
    }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    switch (requestCode) {
      case REDIRECTION_REQUEST_CODE:
        reach5.onLoginCallbackResult(data, resultCode, this::handleLoginSuccess, it -> {
          showToast("LoginCallback error=" + it.getMessage());
        });
        break;
      default:
        reach5.onActivityResult(requestCode, resultCode, data, this::handleLoginSuccess, it -> {
        showToast("LoginProvider error=" + it.getMessage());
      });
    }
  }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_logout:
                reach5.logout(success -> showToast("Logout success"), failure -> {
                    Log.d(TAG, "logout error=" + failure.getMessage());
                    showToast("Logout Error " + failure.getMessage());
                });
                return true;
            case R.id.menu_java:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onStop() {
        super.onStop();
        reach5.onStop();
    }
}
