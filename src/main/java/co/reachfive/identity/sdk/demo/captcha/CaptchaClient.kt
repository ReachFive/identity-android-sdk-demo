package co.reachfive.identity.sdk.demo.captcha

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.Button
import android.widget.LinearLayout
import co.reachfive.identity.sdk.core.models.CaptchaFoxToken
import co.reachfive.identity.sdk.core.models.CaptchaToken
import co.reachfive.identity.sdk.core.models.ReCaptchaToken

/**
 * Produces a captcha token by running the provider's JavaScript widget in a [WebView].
 */
class CaptchaClient(
    private val activity: Activity,
    private val conf: CaptchaConf,
) {
    /**
     * Calls [onToken] with a fresh token. Failures go to [onError] and no token is produced.
     */
    fun token(action: String, onToken: (CaptchaToken) -> Unit, onError: (String) -> Unit) {
        val siteKey = conf.siteKey
        // Only CaptchaFox asks the user for anything; reCAPTCHA v3 is score-only.
        val interactive = conf.provider == CaptchaConf.CAPTCHAFOX

        val html: String
        val wrap: (String) -> CaptchaToken
        when (conf.provider) {
            CaptchaConf.CAPTCHAFOX -> {
                html = captchaFoxPage(siteKey)
                wrap = ::CaptchaFoxToken
            }

            CaptchaConf.RECAPTCHA -> {
                html = reCaptchaV3Page(siteKey, action)
                wrap = ::ReCaptchaToken
            }

            else -> {
                onError("Unknown captcha provider '${conf.provider}'")
                return
            }
        }

        render(html, interactive, { value -> onToken(wrap(value)) }, onError)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun render(
        html: String,
        interactive: Boolean,
        onToken: (String) -> Unit,
        onError: (String) -> Unit,
    ) {
        val webView = WebView(activity)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        // The provider's own diagnostics land here and nowhere else.
        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                Log.d(TAG, "captcha page: ${message.message()}")
                return true
            }
        }

        var dialog: Dialog? = null
        var settled = false
        val handler = Handler(Looper.getMainLooper())

        fun settle(block: () -> Unit) {
            if (settled) return
            settled = true
            handler.removeCallbacksAndMessages(null)
            dialog?.dismiss()
            // Destroying the WebView while one of its JavaScript callbacks is still on the stack
            // crashes; let the current dispatch drain first.
            webView.post {
                (webView.parent as? ViewGroup)?.removeView(webView)
                webView.destroy()
            }
            block()
        }

        val deliverToken = onToken
        val deliverError = onError
        webView.addJavascriptInterface(object {
            @JavascriptInterface
            fun onToken(token: String) {
                handler.post { settle { deliverToken(token) } }
            }

            @JavascriptInterface
            fun onError(message: String) {
                handler.post { settle { deliverError(message) } }
            }
        }, BRIDGE)

        // The provider checks the page origin against the domains the site key is registered for,
        // and a WebView page has none of its own — hence the base URL.
        Log.d(TAG, "captcha challenge as ${conf.baseUrl}")
        webView.loadDataWithBaseURL(conf.baseUrl, html, "text/html", "utf-8", null)

        if (interactive) {
            // Not an AlertDialog: AlertController re-adds a custom view with its own
            // wrap_content params, so a WebView shrinks to whatever it happens to be showing.
            // A challenge is a page in its own right — a slider, an image grid — so size the
            // window itself and give the WebView every row the Cancel button does not need.
            val content = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                addView(
                    webView,
                    LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f),
                )
                addView(
                    Button(activity).apply {
                        text = "Cancel"
                        setOnClickListener { settle { onError("Captcha cancelled") } }
                    },
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    ),
                )
            }

            dialog = Dialog(activity).apply {
                setContentView(content)
                // A slider challenge is a drag; an outside-touch dismissal would fight it.
                setCanceledOnTouchOutside(false)
                setOnCancelListener { settle { onError("Captcha cancelled") } }
                show()
                window?.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    (activity.resources.displayMetrics.heightPixels * INTERACTIVE_HEIGHT_RATIO)
                        .toInt(),
                )
            }
        } else {
            // Not shown, but still laid out: reCAPTCHA will not run in a WebView of zero size.
            activity.addContentView(webView, ViewGroup.LayoutParams(1, 1))
        }

        handler.postDelayed({ settle { onError("Captcha timed out") } }, TIMEOUT_MS)
    }

    private fun reCaptchaV3Page(siteKey: String, action: String) = """
        <!DOCTYPE html><html><head><meta name="viewport" content="width=device-width"></head>
        <body>
        <script src="https://www.google.com/recaptcha/api.js?render=$siteKey"></script>
        <script>
          grecaptcha.ready(function () {
            grecaptcha.execute('$siteKey', { action: '$action' })
              .then(function (t) { $BRIDGE.onToken(t); })
              .catch(function (e) { $BRIDGE.onError('' + e); });
          });
        </script>
        </body></html>
    """.trimIndent()

    // CaptchaFox reports why it gave up — invalid-sitekey, site-not-allowed, network-error,
    // rate-limited and friends — so pass the code straight through instead of flattening it.
    private fun captchaFoxPage(siteKey: String) = """
        <!DOCTYPE html><html><head><meta name="viewport" content="width=device-width"></head>
        <body>
        <div class="captchafox" data-sitekey="$siteKey" data-mode="inline" data-start="auto"
             data-callback="captchaSolved" data-error-callback="captchaError"
             data-fail-callback="captchaFail" data-expired-callback="captchaExpired"></div>
        <script>
          function captchaSolved(t) { $BRIDGE.onToken(t); }
          function captchaError(e) { $BRIDGE.onError('captchafox error: ' + e); }
          function captchaFail(e) { $BRIDGE.onError('captchafox verification failed: ' + e); }
          function captchaExpired() { $BRIDGE.onError('captchafox token expired'); }
        </script>
        <script src="https://cdn.captchafox.com/api.js" async defer></script>
        </body></html>
    """.trimIndent()

    private companion object {
        const val TAG = "Captcha"
        const val BRIDGE = "AndroidCaptcha"
        const val TIMEOUT_MS = 60_000L
        const val INTERACTIVE_HEIGHT_RATIO = 0.75f
    }
}
