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
import androidx.viewpager.widget.ViewPager
import co.reachfive.identity.sdk.core.ReachFive
import co.reachfive.identity.sdk.core.models.AuthToken
import co.reachfive.identity.sdk.core.models.ReachFiveError
import co.reachfive.identity.sdk.core.models.SdkConfig
import co.reachfive.identity.sdk.demo.databinding.ActivityAuthenticatedBinding
import com.google.android.material.tabs.TabLayout
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

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager

    private lateinit var authenticatedActivityBinding: ActivityAuthenticatedBinding

    private lateinit var webauthnFragment: WebauthnFragment

    private lateinit var fragmentAdapter: AuthenticatedActivityPagerAdapter
    private val assignedScope = setOf(
        "openid",
        "email",
        "profile",
        "phone_number",
        "offline_access",
        "events",
        "full_write",
        "mfa"
    )

    companion object {
        const val AUTH_TOKEN = "AUTH_TOKEN"
        const val SDK_CONFIG = "SDK_CONFIG"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.authToken = intent.getParcelableExtra(AUTH_TOKEN)!!

        val sdkConfig = intent.getParcelableExtra<SdkConfig>(SDK_CONFIG)!!
        this.reach5 = ReachFive(
            sdkConfig = sdkConfig,
            providersCreators = listOf(),
        )

        authenticatedActivityBinding = ActivityAuthenticatedBinding.inflate(layoutInflater)

        setContentView(authenticatedActivityBinding.root)

        tabLayout = findViewById(R.id.tabLayout)
        viewPager = findViewById(R.id.viewPager)
        tabLayout.setupWithViewPager(viewPager)

        webauthnFragment = WebauthnFragment(reach5, authToken, origin, this)
        fragmentAdapter = AuthenticatedActivityPagerAdapter(supportFragmentManager)
        fragmentAdapter.addFragment(MfaFragment(reach5, authToken, assignedScope, sdkConfig,this), "MFA")
        fragmentAdapter.addFragment(webauthnFragment, "webauthn")
        viewPager.adapter = fragmentAdapter

        val givenNameTextView = findViewById<View>(R.id.user_given_name) as TextView
        givenNameTextView.text = this.authToken.user?.givenName

        val familyNameTextView = findViewById<View>(R.id.user_family_name) as TextView
        familyNameTextView.text = this.authToken.user?.familyName

        val emailTextView = findViewById<View>(R.id.user_email) as TextView
        emailTextView.text = this.authToken.user?.email

        val phoneNumberTextView = findViewById<View>(R.id.user_phone_number) as TextView
        phoneNumberTextView.text = this.authToken.user?.phoneNumber

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        webauthnFragment.onWebauthnAddingDevice(requestCode, resultCode, data)
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
