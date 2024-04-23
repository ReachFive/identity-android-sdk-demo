package co.reachfive.identity.sdk.demo

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import co.reachfive.identity.sdk.core.ReachFive
import co.reachfive.identity.sdk.core.WebAuthnDeviceAddResult
import co.reachfive.identity.sdk.core.models.AuthToken
import co.reachfive.identity.sdk.core.models.ReachFiveError
import co.reachfive.identity.sdk.core.models.responses.webAuthn.DeviceCredential


class WebauthnFragment(private val reach5: ReachFive,
                       private var authToken: AuthToken,
                       private val origin: String,
                       private val anchor: Activity) : Fragment() {
    private val TAG = "Reach5_webauthn_fragment"

    private var devicesDisplayed: List<DeviceCredential> = listOf()
    private lateinit var deviceAdapter: DevicesAdapter


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.webauthn_fragment, container, false)
        view.findViewById<EditText>(R.id.newFriendlyName).setText(android.os.Build.MODEL)

        deviceAdapter = DevicesAdapter(requireContext(), devicesDisplayed,object : ButtonCallbacks {
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

        view.findViewById<ListView>(R.id.devices).adapter = deviceAdapter
        view.findViewById<Button>(R.id.addNewDevice).setOnClickListener {
            this.reach5.addNewWebAuthnDevice(
                authToken = this.authToken,
                originWebauthn = origin,
                friendlyName = view.findViewById<EditText>(R.id.newFriendlyName).text.trim().toString(),
                failure = {
                    Log.d(TAG, "addNewWebAuthnDevice error=$it")
                    showToast(it.data?.errorUserMsg ?: it.message)
                },
                activity = anchor
            )
        }

        view.findViewById<Button>(R.id.registerPasskey).setOnClickListener {
            this.reach5.registerNewPasskey(
                authToken = this.authToken,
                friendlyName = view.findViewById<EditText>(R.id.newFriendlyName).text.trim().toString(),
                success = {
                    showToast("New Passkey device registered")
                    refreshDevicesDisplayed()
                },
                failure = {
                    Log.d(TAG, "registerNewPasskey error=$it")
                    showToast(it.data?.errorUserMsg ?: it.message)
                },
                activity = anchor
            )

        }

        refreshDevicesDisplayed()

        return view
    }

    private fun showDevicesTitle() {
        requireView().findViewById<TextView>(R.id.devicesTitle).visibility =
            if (this.devicesDisplayed.isEmpty()) View.INVISIBLE else View.VISIBLE
    }

    fun onWebauthnAddingDevice(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d(TAG, "onWebauthnAddingDevice - requestCode: $requestCode, resultCode: $resultCode")

        val handler = reach5.resolveResultHandler(requestCode, resultCode, data)
        if (handler is WebAuthnDeviceAddResult) {
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

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    private fun showErrorToast(error: ReachFiveError) {
        showToast(
            error.data?.errorUserMsg ?: (error.data?.errorDetails?.get(0)?.message
                ?: (error.data?.errorDescription
                    ?: error.message))
        )
    }
}