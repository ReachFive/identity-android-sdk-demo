package co.reachfive.identity.sdk.demo

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import co.reachfive.identity.sdk.core.ReachFive
import co.reachfive.identity.sdk.core.models.AuthToken
import co.reachfive.identity.sdk.core.models.CredentialMfaType
import co.reachfive.identity.sdk.core.models.SdkConfig
import co.reachfive.identity.sdk.core.models.requests.StartStepUpLoginFlow
import co.reachfive.identity.sdk.demo.databinding.MfaStepUpBinding

class StepUpActivity : AppCompatActivity() {
    private lateinit var mfaStepUpBinding: MfaStepUpBinding

    private val TAG = "Reach5_StepUpActivity"

    private lateinit var authToken: AuthToken

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
    private lateinit var sdkConfig: SdkConfig

    companion object {
        const val AUTH_TOKEN = "AUTH_TOKEN"
        const val SDK_CONFIG = "SDK_CONFIG"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.authToken = intent.getParcelableExtra(AUTH_TOKEN)!!
        sdkConfig = intent.getParcelableExtra<SdkConfig>(SDK_CONFIG)!!

        mfaStepUpBinding = MfaStepUpBinding.inflate(layoutInflater)
        setContentView(mfaStepUpBinding.root)
        this.reach5 = ReachFive(
            sdkConfig = sdkConfig,
            providersCreators = emptyList(),
        ).initialize({
            Log.d(TAG, "ReachFive init success")
        }, {
            Log.d(TAG, "ReachFive init ${it.message}")
        })
        mfaStepUpBinding.startMfaStepUp.setOnClickListener {
            val authType = if(mfaStepUpBinding.emailCredentialType.isChecked) CredentialMfaType.email else CredentialMfaType.sms
            this.reach5.startStepUp(
                StartStepUpLoginFlow(stepUpToken = authToken.stepUpToken!!),
                authType,
                scope = assignedScope,
                redirectUri = sdkConfig.scheme,
                success = {
                    val verificationCodeTextView = EditText(this.baseContext)
                    val alert = androidx.appcompat.app.AlertDialog.Builder(this);
                    alert.setTitle("Step up")
                    alert.setMessage("Please enter the code you received by $authType")
                    alert.setView(verificationCodeTextView)
                    alert.setPositiveButton("Complete step up", DialogInterface.OnClickListener { dialog: DialogInterface, which: Int ->
                        this.reach5.endStepUp(
                            challengeId = it.challengeId,
                            verificationCode = verificationCodeTextView.text.toString(),
                            success = {
                                handleLoginSuccess(it)
                            },
                            failure = {
                                Log.d(TAG, "error=$it")
                            },
                            activity = this)
                    })
                    alert.show()
                },
                failure = {
                    Log.d(TAG, "mfa start step up error = $it")
                })
        }
    }

    private fun handleLoginSuccess(authToken: AuthToken) {
        try {
            val intent = Intent(this, AuthenticatedActivity::class.java)
            intent.putExtra(AUTH_TOKEN, authToken)
            intent.putExtra(SDK_CONFIG, sdkConfig)

            startActivity(intent)
        } catch (e: Exception) {
            Log.d(TAG, "Login error=$authToken")
        }
    }
}