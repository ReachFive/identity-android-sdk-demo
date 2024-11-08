package co.reachfive.identity.sdk.demo

import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import co.reachfive.identity.sdk.core.LoginResultHandler
import co.reachfive.identity.sdk.core.ReachFive
import co.reachfive.identity.sdk.core.models.AuthToken
import co.reachfive.identity.sdk.core.models.CredentialMfaType
import co.reachfive.identity.sdk.core.models.ReachFiveError
import co.reachfive.identity.sdk.core.models.SdkConfig
import co.reachfive.identity.sdk.core.models.requests.LoginMfaConf
import co.reachfive.identity.sdk.core.models.requests.ProfileSignupRequest
import co.reachfive.identity.sdk.core.models.requests.ProfileWebAuthnSignupRequest
import co.reachfive.identity.sdk.core.models.requests.StartStepUpLoginFlow
import co.reachfive.identity.sdk.core.models.requests.webAuthn.WebAuthnLoginRequest
import co.reachfive.identity.sdk.demo.AuthenticatedActivity.Companion.AUTH_TOKEN
import co.reachfive.identity.sdk.demo.AuthenticatedActivity.Companion.SDK_CONFIG
import co.reachfive.identity.sdk.demo.databinding.*
import co.reachfive.identity.sdk.facebook.FacebookProvider
import co.reachfive.identity.sdk.google.GoogleProvider
import co.reachfive.identity.sdk.webview.WebViewProvider
import co.reachfive.identity.sdk.wechat.WechatProvider
import io.github.cdimascio.dotenv.dotenv


class MainActivity : AppCompatActivity() {

    private lateinit var webAuthnLoginBinding: WebauthnLoginBinding
    private lateinit var webAuthnSignupBinding: WebauthnSignupBinding
    private lateinit var webAuthnResetBinding: WebauthnResetBinding
    private lateinit var passwordAuthBinding: PasswordAuthBinding
    private lateinit var passwordlessAuthBinding: PasswordlessAuthBinding
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

    private val sdkConfig = SdkConfig(domain, clientId, scheme, origin)

    private val assignedScope = setOf(
        "openid",
        "email",
        "profile",
        "phone_number",
        "offline_access",
        "events",
        "mfa",
        "full_write"
    )

    private lateinit var reach5: ReachFive

    private lateinit var providerAdapter: ProvidersAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainActivityBinding = ActivityMainBinding.inflate(layoutInflater)
        passwordAuthBinding = mainActivityBinding.passwordAuth
        passwordlessAuthBinding = mainActivityBinding.passwordlessAuth
        webAuthnLoginBinding = mainActivityBinding.webauthnLogin
        webAuthnSignupBinding = mainActivityBinding.webauthnSignup
        webAuthnResetBinding = mainActivityBinding.webauthnReset
        setContentView(mainActivityBinding.root)
        setSupportActionBar(findViewById(R.id.toolbar))


        val providersCreators =
            listOf(GoogleProvider(), FacebookProvider(), WebViewProvider(), WechatProvider())

        this.reach5 = ReachFive(
            sdkConfig = sdkConfig,
            providersCreators = providersCreators,
        ).initialize({
            Log.d(TAG, "ReachFive init success")
        }, {
            Log.d(TAG, "ReachFive init ${it.message}")
            showToast("ReachFive init ${it.message}")
        })

        reach5.loadSocialProviders(
            this,
            success = { providers -> providerAdapter.refresh(providers) },
            failure = { Log.d(TAG, "Loading providers failed ${it.message}") }
        )

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

        mainActivityBinding.weblogin.setOnClickListener {
            this.reach5.loginWithWeb(
                state = "state",
                nonce = "nonce",
                origin = "origin",
                activity = this
            )
        }

        mainActivityBinding.webviewlogin.setOnClickListener {
            this.reach5.loginWithWebView(
                state = "state",
                nonce = "nonce",
                origin = "origin",
                activity = this
            )
        }

        mainActivityBinding.webviewSsoData.setOnClickListener {
            val intent = Intent(this, WebviewActivity::class.java)
            intent.data = Uri.parse("https://$domain/identity/v1/sso/data")
            startActivity(intent)
        }

        val redirectUrlBinding = passwordAuthBinding.redirectUrl
        val emailPwdBinding = passwordAuthBinding.email
        fun emailPwd() = emailPwdBinding.text.toString().let {
            if (it.isBlank())
                null
            else
                it
        }

        val phoneNumberPwdBinding = passwordAuthBinding.phoneNumber
        fun phoneNumberPwd() = phoneNumberPwdBinding.text.toString().let {
            if (it.isBlank())
                null
            else
                it
        }

        val customIdentifierPwdBinding = passwordAuthBinding.customIdentifier
        fun customIdentifierPwd() = customIdentifierPwdBinding.text.toString().let {
            if (it.isBlank())
                null
            else
                it
        }

        val passwordBinding = passwordAuthBinding.password
        fun passwordValue() = passwordBinding.text.toString().trim()

        passwordAuthBinding.passwordSignup.setOnClickListener {
            val signupRequest = ProfileSignupRequest(
                email = emailPwd(),
                phoneNumber = phoneNumberPwd(),
                customIdentifier = customIdentifierPwd(),
                password = passwordValue()
            )

            Log.d("Signup", "Password: '${passwordValue()}'")

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

        passwordAuthBinding.passwordLogin.setOnClickListener {
            this.reach5.loginWithPassword(
                email = emailPwd(),
                phoneNumber = phoneNumberPwd(),
                customIdentifier = customIdentifierPwd(),
                password = passwordValue(),
                scope = assignedScope,
                mfaConf = LoginMfaConf(activity = this, redirectUri = sdkConfig.scheme),
                success = {
                    if(it.stepUpToken != null) {
                        val linearLayout = LinearLayout(this)
                        val checkboxAuthTypeSMS = CheckBox(this)
                        checkboxAuthTypeSMS.text = "SMS"
                        val checkboxAuthTypeEmail = CheckBox(this)
                        checkboxAuthTypeEmail.text = "Email"
                        linearLayout.addView(checkboxAuthTypeEmail)
                        linearLayout.addView(checkboxAuthTypeSMS)
                        val alert = androidx.appcompat.app.AlertDialog.Builder(this)
                        alert.setTitle("Mfa auth type")
                        alert.setMessage("Choose your mfa auth type")
                        alert.setView(linearLayout)
                        alert.setPositiveButton("Start Step up") { dialog: DialogInterface, which: Int ->
                            val secondFactorType =
                                if (checkboxAuthTypeSMS.isChecked)
                                    CredentialMfaType.sms
                                else if (checkboxAuthTypeEmail.isChecked)
                                    CredentialMfaType.email
                                else CredentialMfaType.email
                            this.reach5.startStepUp(
                                startStepUpFlow = StartStepUpLoginFlow(stepUpToken = it.stepUpToken!!),
                                authType = secondFactorType,
                                redirectUri = sdkConfig.scheme,
                                scope = assignedScope,
                                success = {
                                    val verificationCodeTextView = EditText(this)
                                    val alertEndStepUp =
                                        androidx.appcompat.app.AlertDialog.Builder(this)
                                    alertEndStepUp.setTitle("Step up")
                                    alertEndStepUp.setMessage("Please enter the code you received by $secondFactorType")
                                    alertEndStepUp.setView(verificationCodeTextView)
                                    alertEndStepUp.setPositiveButton(
                                        "Complete step up"
                                    ) { dialog: DialogInterface, which: Int ->
                                        this.reach5.endStepUp(
                                            challengeId = it.challengeId,
                                            trustDevice = true,
                                            verificationCode = verificationCodeTextView.text.toString(),
                                            success = {
                                                handleLoginSuccess(it)
                                                showToast("MFA step up completed")
                                            },
                                            failure = {
                                                showErrorToast(it)
                                            },
                                            activity = this
                                        )
                                    }
                                    alertEndStepUp.show()
                                    showToast("MFA step up started")
                                },
                                failure = {
                                    Log.d(TAG, "mfa start step up error = $it")
                                    showErrorToast(it)
                                },
                            )
                        }
                        alert.show()
                    } else {
                        handleLoginSuccess(it)
                    }
                },
                failure = {
                    Log.d(TAG, "loginWithPassword error=$it")
                    showErrorToast(it)
                }
            )
        }

        passwordAuthBinding.accountRecovery.setOnClickListener {
            this.reach5.requestAccountRecovery(
                email = emailPwd(),
                phoneNumber = phoneNumberPwd(),
                success = { showToast("Request Account Recovery: code sent") },
                failure = {
                    Log.d(TAG, "requestAccountRecovery error=$it")
                    showErrorToast(it)
                }
            )
        }


        val emailPwdlBinding = passwordlessAuthBinding.email
        val phoneNumberPwdlBinding = passwordlessAuthBinding.phoneNumber

        passwordlessAuthBinding.startPasswordless.setOnClickListener {
            val redirectUri = passwordlessAuthBinding.redirectUriInput.text.toString()

            if (emailPwdlBinding.text.toString().isNotEmpty()) {
                if (redirectUri != "") {
                    this.reach5.startPasswordless(
                        email = emailPwdlBinding.text.toString(),
                        redirectUrl = redirectUri,
                        success = { showToast("Email sent - Check your email box") },
                        failure = {
                            Log.d(TAG, "signup error=$it")
                            showErrorToast(it)
                        },
                        activity = this
                    )
                } else {
                    this.reach5.startPasswordless(
                        email = emailPwdlBinding.text.toString(),
                        success = { showToast("Email sent - Check your email box") },
                        failure = {
                            Log.d(TAG, "signup error=$it")
                            showErrorToast(it)
                        },
                        activity = this
                    )
                }
            } else {
                if (redirectUri != "") {
                    this.reach5.startPasswordless(
                        phoneNumber = phoneNumberPwdlBinding.text.toString(),
                        redirectUrl = redirectUri,
                        success = { showToast("Sms sent - Please enter the validation code below") },
                        failure = {
                            Log.d(TAG, "signup error=$it")
                            showErrorToast(it)
                        },
                        activity = this
                    )
                } else {
                    this.reach5.startPasswordless(
                        phoneNumber = phoneNumberPwdlBinding.text.toString(),
                        success = { showToast("Sms sent - Please enter the validation code below") },
                        failure = {
                            Log.d(TAG, "signup error=$it")
                            showErrorToast(it)
                        },
                        activity = this
                    )
                }
            }
        }

        passwordlessAuthBinding.phoneNumberPasswordless.setOnClickListener {
            this.reach5.verifyPasswordless(
                phoneNumber = phoneNumberPwdlBinding.text.toString(),
                verificationCode = passwordlessAuthBinding.verificationCode.text.toString(),
                success = { handleLoginSuccess(it) },
                failure = {
                    Log.d(TAG, "verifyPasswordless error=$it")
                    showErrorToast(it)
                },
                activity = this
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
                    originWebauthn = origin,
                    friendlyName = webAuthnSignupBinding.signupWebAuthnNewFriendlyName.text.toString(),
                    success = {},
                    failure = {
                        Log.d(TAG, "signupWithWebAuthn error=$it")
                        showErrorToast(it)
                    },
                    activity = this
                )
        }

        webAuthnSignupBinding.signupWithPasskey.setOnClickListener {
            val email = webAuthnSignupBinding.signupWebAuthnEmail.text.toString()
            val phone = webAuthnSignupBinding.signupWebAuthnPhone.text.toString()
            this.reach5
                .signupWithPasskey(
                    profile = if(email.isNotEmpty()) ProfileWebAuthnSignupRequest(email = email) else ProfileWebAuthnSignupRequest(phoneNumber = phone),
                    friendlyName = webAuthnSignupBinding.signupWebAuthnNewFriendlyName.text.toString(),
                    success = { handleLoginSuccess(it) },
                    failure = {
                        Log.d(TAG, "signupWithWebAuthn error=$it")
                        showErrorToast(it)
                    },
                    activity = this
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
                    failure = {
                        Log.d(TAG, "loginWithWebAuthn error=$it")
                        showErrorToast(it)
                    },
                    activity = this
                )
        }

        webAuthnLoginBinding.loginWithPasskey.setOnClickListener {

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
                .loginWithPasskey(
                    loginRequest = webAuthnLoginRequest,
                    success = { handleLoginSuccess(it) },
                    failure = {
                        Log.d(TAG, "loginWithWebAuthn error=$it")
                        showErrorToast(it)
                    },
                    scope = assignedScope,
                    activity = this
                )
        }

        webAuthnLoginBinding.discoverableLogin.setOnClickListener {

            this.reach5
                .discoverableLogin(
                    success = { handleLoginSuccess(it) },
                    failure = {
                        Log.d(TAG, "discoverableLogin error=$it")
                        showErrorToast(it)
                    },
                    scope = assignedScope,
                    activity = this
                )
        }

        webAuthnResetBinding.resetWebAuthn.setOnClickListener {
            this.reach5
                .resetPasskeys(
                    email = webAuthnResetBinding.resetWebAuthnEmail.text.toString().ifBlank { null },
                    phoneNumber = webAuthnResetBinding.resetWebAuthnPhone.text.toString().ifBlank { null },
                    friendlyName = webAuthnResetBinding.resetWebAuthnNewFriendlyName.text.toString(),
                    verificationCode = webAuthnResetBinding.resetWebAuthnVerificationCode.text.toString(),
                    success = { showToast("Reset Passkeys Success")},
                    failure = {
                        Log.d(TAG, "resetPasskeys error=$it")
                        showErrorToast(it)
                    },
                    activity = this
                )
        }

    }


    @Suppress("deprecation")
    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "MainActivity.onActivityResult requestCode=$requestCode resultCode=$resultCode")

        // Method A
//        reach5.onLoginActivityResult(requestCode, resultCode, data, {handleLoginSuccess(it)}, {
//            Log.d(TAG, "onLoginWithCallbackResult error=$it")
//            showErrorToast(it)
//        })

        // Method B
        val handler = reach5.resolveResultHandler(requestCode, resultCode, data)
        when (handler) {
            is LoginResultHandler -> handler.handle({ handleLoginSuccess(it) }, {
                Log.d(TAG, "onLoginWithCallbackResult error=$it")
                showErrorToast(it)
            }, activity = this)
            // TODO/cbu
            else -> {}
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
        reach5.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults,
            failure = {},
            activity = this
        )
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
