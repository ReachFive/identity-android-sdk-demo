package co.reachfive.identity.sdk.demo.captcha

import co.reachfive.identity.sdk.core.models.CaptchaToken

data class CaptchaConf(
    val provider: String,
    val siteKey: String,
    val siteUrl: String,
) {
    val baseUrl: String =
        if (siteUrl.startsWith("http://") || siteUrl.startsWith("https://")) siteUrl
        else "https://$siteUrl"

    companion object {
        const val RECAPTCHA = CaptchaToken.RECAPTCHA
        const val CAPTCHAFOX = CaptchaToken.CAPTCHAFOX

        fun of(provider: String?, siteKey: String?, siteUrl: String?): CaptchaConf? =
            if (provider.isNullOrBlank() || siteKey.isNullOrBlank() || siteUrl.isNullOrBlank()) null
            else CaptchaConf(provider, siteKey, siteUrl)
    }
}

/**
 * The `action` reported to reCAPTCHA v3, https://developer.reachfive.com/docs/captcha/recaptcha.html
 */
object CaptchaAction {
    const val SIGNUP = "signup"
    const val LOGIN = "login"
    const val ACCOUNT_RECOVERY = "account_recovery"
    const val PASSWORD_RESET_REQUESTED = "password_reset_requested"
    const val UPDATE_EMAIL = "update_email"
    const val PASSWORDLESS_EMAIL = "passwordless_email"
    const val PASSWORDLESS_PHONE = "passwordless_phone"
}
