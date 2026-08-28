package co.reachfive.identity.sdk.demo

import android.view.View
import android.view.animation.AnimationUtils
import android.webkit.WebView
import co.reachfive.identity.sdk.core.ReachFiveWebViewClient
import co.reachfive.identity.sdk.core.RedirectionActivity

class CustomWebViewClient(activity: RedirectionActivity, codeVerifier: String?): ReachFiveWebViewClient(activity, codeVerifier) {
    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        activity.binding.webview.visibility = View.VISIBLE
        val fadeIn = AnimationUtils.loadAnimation(activity, R.anim.fade_in)
        activity.binding.webview.startAnimation(fadeIn)
    }
}