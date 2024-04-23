package co.reachfive.identity.sdk.demo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import co.reachfive.identity.sdk.core.ReachFive
import co.reachfive.identity.sdk.core.WebAuthnDeviceAddResult
import co.reachfive.identity.sdk.core.models.AuthToken
import co.reachfive.identity.sdk.core.models.ReachFiveError
import co.reachfive.identity.sdk.core.models.SdkConfig
import co.reachfive.identity.sdk.core.models.responses.MfaCredential
import co.reachfive.identity.sdk.core.models.responses.webAuthn.DeviceCredential
import co.reachfive.identity.sdk.demo.databinding.ActivityAuthenticatedBinding
import co.reachfive.identity.sdk.demo.databinding.MfaCredentialsBinding
import co.reachfive.identity.sdk.demo.databinding.WebauthnDevicesBinding
import io.github.cdimascio.dotenv.dotenv

class AuthenticatedActivity : AppCompatActivity() {
    private val TAG = "Reach5_AuthActivity"

    private val dotenv = dotenv {
        directory = "/assets"
        filename = "env"
    }

    // This variable is only mandatory for the FIDO2 login flow
    private val origin = dotenv["ORIGIN"] ?: ""

    private lateinit var reach5: ReachFive
    private lateinit var authToken: AuthToken

    private lateinit var authenticatedActivityBinding: ActivityAuthenticatedBinding
    private lateinit var mfaCredentialsBinding: MfaCredentialsBinding

    private lateinit var mfaCredentialsAdapter: MfaCredentialsAdapter
    private lateinit var mfaCredentials: List<MfaCredential>

    private lateinit var deviceAdapter: DevicesAdapter
    private lateinit var devicesDisplayed: List<DeviceCredential>

    private lateinit var devicesBinding: WebauthnDevicesBinding

    companion object {
        const val AUTH_TOKEN = "AUTH_TOKEN"
        const val SDK_CONFIG = "SDK_CONFIG"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authenticatedActivityBinding = ActivityAuthenticatedBinding.inflate(layoutInflater)
        devicesBinding = authenticatedActivityBinding.webauthnDevices
        mfaCredentialsBinding = authenticatedActivityBinding.mfaCredentials

        setContentView(authenticatedActivityBinding.root)


        this.authToken = intent.getParcelableExtra(AUTH_TOKEN)!!
        this.devicesDisplayed = listOf()

        this.mfaCredentials = listOf()

        val sdkConfig = intent.getParcelableExtra<SdkConfig>(SDK_CONFIG)!!
        this.reach5 = ReachFive(
            sdkConfig = sdkConfig,
            providersCreators = listOf(),
        )

        val givenNameTextView = findViewById<View>(R.id.user_given_name) as TextView
        givenNameTextView.text = this.authToken.user?.givenName

        val familyNameTextView = findViewById<View>(R.id.user_family_name) as TextView
        familyNameTextView.text = this.authToken.user?.familyName

        val emailTextView = findViewById<View>(R.id.user_email) as TextView
        emailTextView.text = this.authToken.user?.email

        val phoneNumberTextView = findViewById<View>(R.id.user_phone_number) as TextView
        phoneNumberTextView.text = this.authToken.user?.phoneNumber

        devicesBinding.registerPasskey.setOnClickListener {
            this.reach5.registerNewPasskey(
                authToken = this.authToken,
                friendlyName = devicesBinding.newFriendlyName.text.trim().toString(),
                success = {
                    showToast("New Passkey device registered")
                    refreshDevicesDisplayed()
                },
                failure = {
                    Log.d(TAG, "registerNewPasskey error=$it")
                    showToast(it.data?.errorUserMsg ?: it.message)
                },
                activity = this
            )

        }

        devicesBinding.newFriendlyName.setText(android.os.Build.MODEL)
        devicesBinding.addNewDevice.setOnClickListener {
            this.reach5.addNewWebAuthnDevice(
                authToken = this.authToken,
                originWebauthn = origin,
                friendlyName = devicesBinding.newFriendlyName.text.trim().toString(),
                failure = {
                    Log.d(TAG, "addNewWebAuthnDevice error=$it")
                    showToast(it.data?.errorUserMsg ?: it.message)
                },
                activity = this
            )

        }

        deviceAdapter =
            DevicesAdapter(applicationContext, this.devicesDisplayed, object : ButtonCallbacks {
                override fun removeDeviceCallback(position: Int) {
                    val device = deviceAdapter.getItem(position)

                    reach5.removeWebAuthnDevice(
                        authToken = authToken,
                        deviceId = device.id,
                        success = {
                            showToast("The FIDO2 device '${device.friendlyName}' is removed")
                            refreshDevicesDisplayed()
                        },
                        failure = {
                            Log.d(TAG, "removeWebAuthnDevice error=$it")
                            showErrorToast(it)
                        }
                    )
                }
            })
        devicesBinding.devices.adapter = deviceAdapter

        mfaCredentialsAdapter = MfaCredentialsAdapter(applicationContext, this.mfaCredentials)
        mfaCredentialsBinding.credentials.adapter = mfaCredentialsAdapter

        mfaCredentialsBinding.startMfaEmailRegistration.setOnClickListener {
            val redirectUri = mfaCredentialsBinding.redirectUriInput.text.toString().let {
                if (it.isBlank())
                    null
                else
                    it
            }
            this.reach5.startMfaEmailRegistration(authToken,
                redirectUri = redirectUri,
                success = { showToast("Start MFA email registration")},
                failure = {
                    Log.d(TAG, "mfa email registration error = $it")
                    showErrorToast(it)
                })

        }

        mfaCredentialsBinding.startMfaPhoneNumberRegistration.setOnClickListener {
            val phoneNumber = mfaCredentialsBinding.phoneNumberRegistration.text.toString()
            this.reach5.startMfaPhoneNumberRegistration(authToken,
                phoneNumber,
                success = { showToast("Start MFA Phone number $phoneNumber registration")},
                failure = {
                    Log.d(TAG, "mfa email registration error = $it")
                    showErrorToast(it)
                })

        }

        mfaCredentialsBinding.verifyMfaEmailRegistration.setOnClickListener {
            val verificationCode = mfaCredentialsBinding.emailMfaVerificationCode.text.toString()
            this.reach5.verifyMfaEmailRegistration(
                authToken,
                verificationCode,
                success = {
                    refreshMfaCredentialsDisplayed()
                    showToast("MFA email registered")
                          },
                failure = {
                    Log.d(TAG, "mfa email registration error = $it")
                    showErrorToast(it)
                })
        }

        mfaCredentialsBinding.verifyMfaPhoneNumberRegistration.setOnClickListener {
            val verificationCode = mfaCredentialsBinding.phoneNumberMfaVerificationCode.text.toString()
            this.reach5.verifyMfaPhoneNumberRegistration(
                authToken,
                verificationCode,
                success = {
                    refreshMfaCredentialsDisplayed()
                    showToast("MFA Phone number registered")
                          },
                failure = {
                    Log.d(TAG, "mfa email registration error = $it")
                    showErrorToast(it)
                })
        }
        refreshMfaCredentialsDisplayed()
        refreshDevicesDisplayed()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "onActivityResult - requestCode: $requestCode, resultCode: $resultCode")

        val handler = reach5.resolveResultHandler(requestCode, resultCode, data)

        if (handler is WebAuthnDeviceAddResult) {
            Log.d(TAG, "onActivityResult - is webetc")

            handler.handle(
                success = {
                    showToast("New FIDO2 device registered")
                    refreshDevicesDisplayed()
                },
                failure = {
                    Log.d(TAG, "onAddNewWebAuthnDeviceResult error=$it")
                    showErrorToast(it)
                }
            )
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_logout -> {
                Log.d("MainActivity", "Logging out...")
                reach5.logout({}, {})
                finish()
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        menu?.findItem(R.id.menu_java)?.isVisible = false
        return super.onCreateOptionsMenu(menu)
    }

    private fun showDevicesTitle() {
        devicesBinding.devicesTitle.visibility =
            if (this.devicesDisplayed.isEmpty()) View.INVISIBLE else View.VISIBLE
    }

    private fun refreshDevicesDisplayed() {
        reach5.listWebAuthnDevices(
            authToken,
            success = {
                this.devicesDisplayed = it
                this.deviceAdapter.refresh(this.devicesDisplayed)
                showDevicesTitle()
            },
            failure = {
                Log.d(TAG, "listWebAuthnDevices error=$it")
                showErrorToast(it)
            }
        )
    }

    private fun refreshMfaCredentialsDisplayed() {
        reach5.listMfaCredentials(
            authToken = authToken,
            success = {
                this.mfaCredentials = it.credentials
                this.mfaCredentialsAdapter.refresh(this.mfaCredentials)
            },
            failure = {
                Log.d(TAG, "listMfaCredentials error=$it")
                showErrorToast(it)
            }
        )
    }

//    private fun handleWebAuthnRegisterResponse(intent: Intent) {
//        reach5.onAddNewWebAuthnDeviceResult(
//            authToken,
//            intent = intent,
//            success = {
//                showToast("New FIDO2 device registered")
//                refreshDevicesDisplayed()
//            },
//            failure = {
//                Log.d(TAG, "onAddNewWebAuthnDeviceResult error=$it")
//                showErrorToast(it)
//            }
//        )
//    }

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
