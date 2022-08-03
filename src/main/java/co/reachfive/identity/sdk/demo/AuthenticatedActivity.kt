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
import co.reachfive.identity.sdk.core.WebLogoutHandler
import co.reachfive.identity.sdk.core.models.AuthToken
import co.reachfive.identity.sdk.core.models.ReachFiveError
import co.reachfive.identity.sdk.core.models.SdkConfig
import co.reachfive.identity.sdk.core.models.responses.webAuthn.DeviceCredential
import co.reachfive.identity.sdk.demo.databinding.ActivityAuthenticatedBinding
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

        setContentView(authenticatedActivityBinding.root)


        this.authToken = intent.getParcelableExtra(AUTH_TOKEN)!!
        this.devicesDisplayed = listOf()

        val sdkConfig = intent.getParcelableExtra<SdkConfig>(SDK_CONFIG)!!
        this.reach5 = ReachFive(
            sdkConfig = sdkConfig,
            providersCreators = listOf()
        )

        val givenNameTextView = findViewById<View>(R.id.user_given_name) as TextView
        givenNameTextView.text = this.authToken.user?.givenName

        val familyNameTextView = findViewById<View>(R.id.user_family_name) as TextView
        familyNameTextView.text = this.authToken.user?.familyName

        val emailTextView = findViewById<View>(R.id.user_email) as TextView
        emailTextView.text = this.authToken.user?.email

        val phoneNumberTextView = findViewById<View>(R.id.user_phone_number) as TextView
        phoneNumberTextView.text = this.authToken.user?.phoneNumber

        devicesBinding.newFriendlyName.setText(android.os.Build.MODEL)
        devicesBinding.addNewDevice.setOnClickListener {
            this.reach5.addNewWebAuthnDevice(
                authToken = this.authToken,
                origin = origin,
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

        refreshDevicesDisplayed()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "onActivityResult - requestCode: $requestCode, resultCode: $resultCode")

        val handler = reach5.resolveResultHandler(requestCode, resultCode, data)

        if (handler is WebLogoutHandler)
            handler.handle {
                Log.d("AuthenticatedActivity", "Logout success")
                showToast("Successful logout!")
                finish()
            }
        else if (handler is WebAuthnDeviceAddResult) {
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
