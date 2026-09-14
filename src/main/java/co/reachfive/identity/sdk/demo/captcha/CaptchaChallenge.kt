package co.reachfive.identity.sdk.demo.captcha

/**
 * How a token is obtained for the configured provider.
 *
 * A [Web] challenge runs the provider's own JavaScript in a WebView and so needs an origin the site
 * key is registered for; a [Native] one is a call into the reCAPTCHA Android SDK, which has no page
 * and no origin, only a key.
 */
sealed interface CaptchaChallenge {

    data class Web(val conf: CaptchaConf) : CaptchaChallenge

    data class Native(val siteKey: String) : CaptchaChallenge

    companion object {
        /** Reads the `env` settings. Null unless the named provider has what it needs. */
        fun of(provider: String?, siteKey: String?, siteUrl: String?): CaptchaChallenge? = when {
            provider.isNullOrBlank() || siteKey.isNullOrBlank() -> null
            provider == CaptchaConf.RECAPTCHA_ENTERPRISE -> Native(siteKey)
            else -> CaptchaConf.of(provider, siteKey, siteUrl)?.let(::Web)
        }
    }
}
