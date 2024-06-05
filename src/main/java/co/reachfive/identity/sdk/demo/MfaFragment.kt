package co.reachfive.identity.sdk.demo

import android.app.Activity
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import co.reachfive.identity.sdk.core.ReachFive
import co.reachfive.identity.sdk.core.models.AuthToken
import co.reachfive.identity.sdk.core.models.CredentialMfaType
import co.reachfive.identity.sdk.core.models.ReachFiveError
import co.reachfive.identity.sdk.core.models.SdkConfig
import co.reachfive.identity.sdk.core.models.responses.MfaCredential

class MfaFragment(private val reach5: ReachFive,
                  private var authToken: AuthToken,
                  private val assignedScope: Set<String>,
                  private val sdkConfig: SdkConfig,
                  private val anchor: Activity): Fragment() {
    private val TAG = "Reach5_mfa_fragment"

    private lateinit var mfaCredentialsAdapter: MfaCredentialsAdapter
    private var mfaCredentials: List<MfaCredential> = listOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.mfa_fragment, container, false)
        mfaCredentialsAdapter = MfaCredentialsAdapter(requireContext(), this.mfaCredentials, object: ButtonCredentialCallback {
            override fun removeCredentialCallback(position: Int) {
                val credential = mfaCredentialsAdapter.getItem(position)

                if(credential.type == CredentialMfaType.email) {
                    reach5.removeMfaEmail(
                        authToken,
                        success = {
                            showToast("Removed MFA email successfully")
                            refreshMfaCredentialsDisplayed()
                        },
                        failure = {
                            Log.d(TAG, "Removed mfa email error = $it")
                            showErrorToast(it)
                        }
                    )
                } else {
                    reach5.removeMfaPhoneNumber(
                        authToken,
                        phoneNumber = credential.phoneNumber!!,
                        success = {
                            showToast("Removed MFA phone number successfully")
                            refreshMfaCredentialsDisplayed()

                        },
                        failure = {
                            Log.d(TAG, "Removed mfa phone number error = $it")
                            showErrorToast(it)
                        }
                    )
                }
            }
        })
        view.findViewById<ListView>(R.id.credentials).adapter = mfaCredentialsAdapter

        view.findViewById<Button>(R.id.startMfaEmailRegistration).setOnClickListener {
            val redirectUri = view.findViewById<TextView>(R.id.redirectUriInput).text.toString().let {
                if (it.isBlank())
                    null
                else
                    it
            }
            reach5.startMfaEmailRegistration(authToken,
                redirectUri = redirectUri,
                success = { showToast("Start MFA email registration")},
                failure = {
                    Log.d(TAG, "mfa email registration error = $it")
                    showErrorToast(it)
                })

        }

        view.findViewById<Button>(R.id.startMfaPhoneNumberRegistration).setOnClickListener {
            val phoneNumber = view.findViewById<EditText>(R.id.phoneNumberRegistration).text.toString()
            this.reach5.startMfaPhoneNumberRegistration(authToken,
                phoneNumber,
                success = { showToast("Start MFA Phone number $phoneNumber registration")},
                failure = {
                    Log.d(TAG, "mfa email registration error = $it")
                    showErrorToast(it)
                })

        }

        view.findViewById<Button>(R.id.verifyMfaEmailRegistration).setOnClickListener {
            val verificationCode = view.findViewById<TextView>(R.id.emailMfaVerificationCode).text.toString()
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

        view.findViewById<Button>(R.id.verifyMfaPhoneNumberRegistration).setOnClickListener {
            val verificationCode = view.findViewById<TextView>(R.id.phoneNumberMfaVerificationCode).text.toString()
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

        view.findViewById<Button>(R.id.startMfaStepUp).setOnClickListener {
            val authType = if(view.findViewById<RadioButton>(R.id.emailCredentialType).isChecked) CredentialMfaType.email else CredentialMfaType.sms
            this.reach5.startStepUp(
                authToken,
                authType,
                scope = assignedScope,
                redirectUri = sdkConfig.scheme,
                success = {
                    val verificationCodeTextView = EditText(context)
                    var alert = androidx.appcompat.app.AlertDialog.Builder(requireContext());
                    alert.setTitle("Step up")
                    alert.setMessage("Please enter the code you received by $authType")
                    alert.setView(verificationCodeTextView)
                    alert.setPositiveButton("Complete step up", DialogInterface.OnClickListener { dialog: DialogInterface, which: Int ->
                        this.reach5.endStepUp(
                            challengeId = it.challengeId,
                            verificationCode = verificationCodeTextView.text.toString(),
                            success = {
                                authToken = it
                                showToast("MFA step up completed")
                            },
                            failure = {
                                showErrorToast(it)
                            },
                            activity = anchor)
                    })
                    alert.show()
                    showToast("MFA step up started")
                },
                failure = {
                    Log.d(TAG, "mfa start step up error = $it")
                    showErrorToast(it)
                },
                activity = anchor)
        }

        refreshMfaCredentialsDisplayed()

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

    private fun refreshMfaCredentialsDisplayed() {
        reach5.listMfaCredentials(
            authToken = authToken,
            success = {
                this.mfaCredentials = it.credentials
                Log.d(TAG, "listMfaCredentials ${this.mfaCredentials}")
                this.mfaCredentialsAdapter.refresh(this.mfaCredentials)
            },
            failure = {
                Log.d(TAG, "listMfaCredentials error=$it")
                showErrorToast(it)
            }
        )
    }


}