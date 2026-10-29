package co.reachfive.identity.sdk.demo.captcha

import android.app.Application
import android.util.Log
import co.reachfive.identity.sdk.core.models.CaptchaToken
import co.reachfive.identity.sdk.core.models.ReCaptchaEnterpriseToken
import com.google.android.recaptcha.Recaptcha
import com.google.android.recaptcha.RecaptchaAction
import com.google.android.recaptcha.RecaptchaClient

/**
 * Obtains a token through the reCAPTCHA Android SDK.
 */
class RecaptchaEnterpriseClient(private val application: Application) {

    // A client is fetched once and reused.
    private var client: RecaptchaClient? = null
    private var clientSiteKey: String? = null

    suspend fun token(siteKey: String, action: String): Result<CaptchaToken> {
        val recaptcha = client?.takeIf { clientSiteKey == siteKey }
            ?: runCatching { Recaptcha.fetchClient(application, siteKey) }
                .onFailure { Log.w(TAG, "reCAPTCHA client could not be fetched", it) }
                .getOrElse { return Result.failure(it) }
                .also {
                    client = it
                    clientSiteKey = siteKey
                }

        return recaptcha
            .execute(RecaptchaAction.custom(action))
            .onFailure { Log.w(TAG, "reCAPTCHA execute failed", it) }
            .map { ReCaptchaEnterpriseToken(it) }
    }

    private companion object {
        const val TAG = "Captcha"
    }
}
