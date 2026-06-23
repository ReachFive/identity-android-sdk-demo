package co.reachfive.identity.sdk.demo

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.Toast
import androidx.fragment.app.Fragment
import co.reachfive.identity.sdk.core.ReachFive
import co.reachfive.identity.sdk.core.models.AuthToken
import co.reachfive.identity.sdk.core.models.ReachFiveError
import co.reachfive.identity.sdk.core.models.responses.SessionDevice

class SessionDevicesFragment(private val reach5: ReachFive, private var authToken: AuthToken): Fragment() {
    private val TAG = "Reach5_session_device_fragment"

    private lateinit var sessionDeviceAdapter: SessionDevicesAdapter
    private var sessionDevices: List<SessionDevice> = listOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.session_devices_fragment, container, false)
        sessionDeviceAdapter = SessionDevicesAdapter(requireContext(), this.sessionDevices, object : ButtonSessionDeviceCallback {
            override fun removeSessionDeviceCallback(position: Int) {
                val sessionDevice = sessionDeviceAdapter.getItem(position) as SessionDevice
                reach5.deleteSessionDevice(sessionDevice.id, authToken,
                    success = {
                        Log.d(TAG, "deleteSessionDevice success")
                        refreshSessionDevicesDisplayed()
                    },
                    failure = {
                        Log.d(TAG, "deleteSessionDevice error $it")
                        showErrorToast(it)
                    })
            }
        })
        view.findViewById<ListView>(R.id.sessionDevices).adapter = sessionDeviceAdapter
        refreshSessionDevicesDisplayed()
        return view
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

    private fun refreshSessionDevicesDisplayed() {
        reach5.listSessionDevices(authToken,
            success = {
                this.sessionDevices = it
                Log.d(TAG, "listSessionDevices $sessionDevices")
                this.sessionDeviceAdapter.refresh(this.sessionDevices)
            },
            failure = {
                Log.d(TAG, "listSessionDevices error $it")
                showErrorToast(it)
            })
    }
}