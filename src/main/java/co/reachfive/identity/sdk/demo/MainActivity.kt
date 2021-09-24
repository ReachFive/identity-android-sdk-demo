package co.reachfive.identity.sdk.demo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import co.reachfive.identity.sdk.core.ReachFive
import co.reachfive.identity.sdk.core.RedirectionActivity.Companion.REDIRECTION_REQUEST_CODE
import co.reachfive.identity.sdk.core.models.AuthToken
import co.reachfive.identity.sdk.core.models.ReachFiveError
import co.reachfive.identity.sdk.core.models.SdkConfig
import co.reachfive.identity.sdk.core.models.requests.ProfileSignupRequest
import co.reachfive.identity.sdk.core.models.requests.ProfileWebAuthnSignupRequest
import co.reachfive.identity.sdk.core.models.requests.webAuthn.WebAuthnLoginRequest
import co.reachfive.identity.sdk.demo.AuthenticatedActivity.Companion.AUTH_TOKEN
import co.reachfive.identity.sdk.demo.AuthenticatedActivity.Companion.SDK_CONFIG
import co.reachfive.identity.sdk.demo.databinding.ActivityMainBinding
import co.reachfive.identity.sdk.demo.databinding.CallbackLoginBinding
import co.reachfive.identity.sdk.demo.databinding.WebauthnLoginBinding
import co.reachfive.identity.sdk.demo.databinding.WebauthnSignupBinding
import co.reachfive.identity.sdk.facebook.FacebookProvider
import co.reachfive.identity.sdk.google.GoogleProvider
import co.reachfive.identity.sdk.webview.WebViewProvider
import io.github.cdimascio.dotenv.dotenv

class MainActivity : AppCompatActivity() {

    private lateinit var webAuthnLoginBinding: WebauthnLoginBinding
    private lateinit var webAuthnSignupBinding: WebauthnSignupBinding
    private lateinit var loginCallbackBinding: CallbackLoginBinding
    private lateinit var mainActivityBinding: ActivityMainBinding

    private val TAG = "Reach5_MainActivity"

    private val dotenv = dotenv {
        directory = "/assets"
        filename = "env"
    }
    private val domain =
        dotenv["DOMAIN"]
            ?: throw IllegalArgumentException("The ReachFive domain is undefined! Check your `env` file.")
    private val clientId =
        dotenv["CLIENT_ID"]
            ?: throw IllegalArgumentException("The ReachFive client ID is undefined! Check your `env` file.")
    private val scheme =
        dotenv["SCHEME"]
            ?: throw IllegalArgumentException("The ReachFive redirect URI is undefined! Check your `env` file.")

    // This variable is only mandatory for the FIDO2 login flow
    private val origin = dotenv["ORIGIN"] ?: ""

    private val sdkConfig = SdkConfig(domain, clientId, scheme)

    private val assignedScope = setOf(
        "openid",
        "email",
        "profile",
        "phone_number",
        "offline_access",
        "events",
        "full_write"
    )

    private lateinit var reach5: ReachFive

    private lateinit var providerAdapter: ProvidersAdapter

    private lateinit var webAuthnId: String

    companion object {
        const val WEBAUTHN_LOGIN_REQUEST_CODE = 2
        const val WEBAUTHN_SIGNUP_REQUEST_CODE = 3
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        webAuthnLoginBinding = WebauthnLoginBinding.inflate(layoutInflater)
        webAuthnSignupBinding = WebauthnSignupBinding.inflate(layoutInflater)
        loginCallbackBinding = CallbackLoginBinding.inflate(layoutInflater)
        mainActivityBinding = ActivityMainBinding.inflate(layoutInflater)

        val providersCreators = listOf(
            GoogleProvider(),
            FacebookProvider(),
            WebViewProvider()
        )

        this.reach5 = ReachFive(
            sdkConfig = sdkConfig,
            providersCreators = providersCreators,
            activity = this
        ).initialize({ providers ->
            providerAdapter.refresh(providers)
        }, {
            Log.d(TAG, "ReachFive init ${it.message}")
            showToast("ReachFive init ${it.message}")
        })

        providerAdapter = ProvidersAdapter(applicationContext, reach5.getProviders())

        mainActivityBinding.providers.adapter = providerAdapter

        mainActivityBinding.providers.setOnItemClickListener { _, _, position, _ ->
            val provider = reach5.getProviders()[position]
            this.reach5.loginWithProvider(
                name = provider.name,
                origin = "home",
                scope = assignedScope,
                activity = this
            )
        }

        val emailBinding = mainActivityBinding.email
        val phoneNumberBinding = mainActivityBinding.phoneNumber
        val passwordBinding = mainActivityBinding.password
        val redirectUrlBinding = mainActivityBinding.redirectUrl

        mainActivityBinding.passwordSignup.setOnClickListener {
            val signupRequest = when {
                ((emailBinding.text.toString().isNotEmpty()) && (phoneNumberBinding.text.toString()
                    .isEmpty())) -> ProfileSignupRequest(
                    email = emailBinding.text.toString(),
                    password = passwordBinding.text.toString()
                )
                ((emailBinding.text.toString().isEmpty()) && (phoneNumberBinding.text.toString()
                    .isNotEmpty())) -> ProfileSignupRequest(
                    phoneNumber = phoneNumberBinding.text.toString(),
                    password = passwordBinding.text.toString()
                )

                else ->
                    ProfileSignupRequest(
                        email = emailBinding.text.toString(),
                        phoneNumber = phoneNumberBinding.text.toString(),
                        password = passwordBinding.text.toString()
                    )
            }

            this.reach5.signup(
                profile = signupRequest,
                redirectUrl = redirectUrlBinding.text.toString().ifEmpty { null },
                success = { handleLoginSuccess(it) },
                failure = {
                    Log.d(TAG, "signup error=$it")
                    showErrorToast(it)
                }
            )
        }

        mainActivityBinding.passwordLogin.setOnClickListener {
            this.reach5.loginWithPassword(
                username = emailBinding.text.trim().toString()
                    .ifEmpty { phoneNumberBinding.text.trim().toString() },
                password = passwordBinding.text.trim().toString(),
                success = { handleLoginSuccess(it) },
                failure = {
                    Log.d(TAG, "loginWithPassword error=$it")
                    showErrorToast(it)
                }
            )
        }

        mainActivityBinding.startPasswordless.setOnClickListener {
            val redirectUri = mainActivityBinding.redirectUriInput.text.toString()

            if (emailBinding.text.toString().isNotEmpty()) {
                if (redirectUri != "") {
                    this.reach5.startPasswordless(
                        email = emailBinding.text.toString(),
                        redirectUrl = redirectUri,
                        successWithNoContent = { showToast("Email sent - Check your email box") },
                        failure = {
                            Log.d(TAG, "signup error=$it")
                            showErrorToast(it)
                        }
                    )
                } else {
                    this.reach5.startPasswordless(
                        email = emailBinding.text.toString(),
                        successWithNoContent = { showToast("Email sent - Check your email box") },
                        failure = {
                            Log.d(TAG, "signup error=$it")
                            showErrorToast(it)
                        }
                    )
                }
            } else {
                if (redirectUri != "") {
                    this.reach5.startPasswordless(
                        phoneNumber = phoneNumberBinding.text.toString(),
                        redirectUrl = redirectUri,
                        successWithNoContent = { showToast("Sms sent - Please enter the validation code below") },
                        failure = {
                            Log.d(TAG, "signup error=$it")
                            showErrorToast(it)
                        }
                    )
                } else {
                    this.reach5.startPasswordless(
                        phoneNumber = phoneNumberBinding.text.toString(),
                        successWithNoContent = { showToast("Sms sent - Please enter the validation code below") },
                        failure = {
                            Log.d(TAG, "signup error=$it")
                            showErrorToast(it)
                        }
                    )
                }
            }
        }

        mainActivityBinding.phoneNumberPasswordless.setOnClickListener {
            this.reach5.verifyPasswordless(
                phoneNumber = phoneNumberBinding.text.toString(),
                verificationCode = mainActivityBinding.verificationCode.text.toString(),
                success = { handleLoginSuccess(it) },
                failure = {
                    Log.d(TAG, "verifyPasswordless error=$it")
                    showErrorToast(it)
                }
            )
        }

        webAuthnSignupBinding.signupWithWebAuthn.setOnClickListener {
            this.reach5
                .signupWithWebAuthn(
                    profile = ProfileWebAuthnSignupRequest(
                        email = webAuthnSignupBinding.signupWebAuthnEmail.text.toString(),
                        givenName = webAuthnSignupBinding.signupWebAuthnGivenName.text.toString(),
                        familyName = webAuthnSignupBinding.signupWebAuthnFamilyName.text.toString()
                    ),
                    origin = origin,
                    friendlyName = webAuthnSignupBinding.signupWebAuthnNewFriendlyName.text.toString(),
                    signupRequestCode = WEBAUTHN_SIGNUP_REQUEST_CODE,
                    successWithWebAuthnId = { this.webAuthnId = it },
                    failure = {
                        Log.d(TAG, "signupWithWebAuthn error=$it")
                        showErrorToast(it)
                    }
                )
        }

        webAuthnLoginBinding.loginWithWebAuthn.setOnClickListener {
            val email = webAuthnLoginBinding.webAuthnEmail.text.toString()
            val webAuthnLoginRequest: WebAuthnLoginRequest =
                if (email.isNotEmpty())
                    WebAuthnLoginRequest.EmailWebAuthnLoginRequest(origin, email, assignedScope)
                else
                    WebAuthnLoginRequest.PhoneNumberWebAuthnLoginRequest(
                        origin,
                        webAuthnLoginBinding.webAuthnPhoneNumber.text.toString(),
                        assignedScope
                    )

            this.reach5
                .loginWithWebAuthn(
                    loginRequest = webAuthnLoginRequest,
                    loginRequestCode = WEBAUTHN_LOGIN_REQUEST_CODE,
                    failure = {
                        Log.d(TAG, "loginWithWebAuthn error=$it")
                        showErrorToast(it)
                    }
                )
        }

        loginCallbackBinding.loginWithCallback.setOnClickListener {
            reach5.loginCallback(
                tkn = loginCallbackBinding.tkn.text.toString(),
                scope = assignedScope
            )
        }

        val authorizationCode: String? = intent?.data?.getQueryParameter("code")
        if (authorizationCode != null) {
            this.reach5.exchangeCodeForToken(
                authorizationCode,
                success = { handleLoginSuccess(it) },
                failure = {
                    Log.d(TAG, "loginWithPassword error=$it")
                    showErrorToast(it)
                }
            )
        }
    }

    @SuppressWarnings("deprecation")
    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "MainActivity.onActivityResult requestCode=$requestCode resultCode=$resultCode")

        when (requestCode) {
            WEBAUTHN_LOGIN_REQUEST_CODE -> {
                when (resultCode) {
                    RESULT_OK -> {
                        if (data == null) Log.d(TAG, "The data is null")
                        else handleWebAuthnLoginResponse(data)
                    }
                    RESULT_CANCELED -> Log.d(TAG, "Operation is cancelled")
                    else -> Log.e(TAG, "Operation failed, with resultCode: $resultCode")
                }
            }

            WEBAUTHN_SIGNUP_REQUEST_CODE -> {
                when (resultCode) {
                    RESULT_OK -> {
                        if (data == null) Log.d(TAG, "The data is null")
                        else handleWebAuthnSignupResponse(data)
                    }
                    RESULT_CANCELED -> Log.d(TAG, "Operation is cancelled")
                    else -> Log.e(TAG, "Operation failed, with resultCode: $resultCode")
                }
            }

            REDIRECTION_REQUEST_CODE -> {
                if (data == null) Log.d(TAG, "The data is null")
                else handleLoginCallbackResponse(data, resultCode)
            }

            // Handle webview and native provider login
            else -> {
                this.reach5.onActivityResult(
                    requestCode = requestCode,
                    resultCode = resultCode,
                    data = data,
                    success = { authToken -> handleLoginSuccess(authToken) },
                    failure = { error ->
                        Log.d(TAG, "onActivityResult error=$error")
                        error.exception?.printStackTrace()
                        showErrorToast(error)
                    }
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d(
            TAG,
            "MainActivity.onRequestPermissionsResult requestCode=$requestCode permissions=$permissions grantResults=$grantResults"
        )
        reach5.onRequestPermissionsResult(requestCode, permissions, grantResults, failure = {})
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_java -> {
                this.startActivity(Intent(this, JavaMainActivity::class.java))
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        menu?.findItem(R.id.menu_logout)?.isVisible = false

        return super.onCreateOptionsMenu(menu)
    }

    override fun onStop() {
        super.onStop()
        reach5.onStop()
    }

    private fun handleLoginSuccess(authToken: AuthToken) {
        try {
            val intent = Intent(this, AuthenticatedActivity::class.java)
            intent.putExtra(AUTH_TOKEN, authToken)
            intent.putExtra(SDK_CONFIG, sdkConfig)

            startActivity(intent)
        } catch (e: Exception) {
            Log.d(TAG, "Login error=$authToken")
            showToast("Login error=$authToken")
        }
    }

    private fun handleWebAuthnLoginResponse(intent: Intent) {
        reach5.onLoginWithWebAuthnResult(
            intent = intent,
            failure = {
                Log.d(TAG, "onLoginWithWebAuthnResult error=$it")
                showErrorToast(it)
            }
        )
    }

    private fun handleWebAuthnSignupResponse(intent: Intent) {
        reach5.onSignupWithWebAuthnResult(
            intent = intent,
            webAuthnId = this.webAuthnId,
            scope = assignedScope,
            failure = {
                Log.d(TAG, "onSignupWithWebAuthnResult error=$it")
                showErrorToast(it)
            }
        )
    }

    private fun handleLoginCallbackResponse(intent: Intent, resultCode: Int) {
        reach5.onLoginCallbackResult(
            intent,
            resultCode = resultCode,
            success = { handleLoginSuccess(it) },
            failure = {
                Log.d(TAG, "onLoginWithCallbackResult error=$it")
                showErrorToast(it)
            }
        )
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun showErrorToast(error: ReachFiveError) {
        showToast(
            error.data?.errorUserMsg ?: (error.data?.errorDetails?.get(0)?.message
                ?: (error.data?.errorDescription
                    ?: error.message))
        )
    }
}
