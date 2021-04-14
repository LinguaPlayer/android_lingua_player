package org.videolan.vlc.gui.video

import android.content.res.Configuration
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import ir.tapsell.plus.AdRequestCallback
import ir.tapsell.plus.TapsellPlus
import ir.tapsell.plus.TapsellPlusBannerType
import ir.tapsell.sdk.bannerads.TapsellBannerType
import ir.tapsell.sdk.bannerads.TapsellBannerView
import org.videolan.tools.*
import org.videolan.vlc.R


private const val TAG = "AdsDelegate"

class AdsDelegate(val player: VideoPlayerActivity) {

    private var banner: ViewGroup? = null
    private var adCloseButton: ImageView? = null
    private var adContainer: FrameLayout? = null
    private var requestFilled = false

    fun onConfigurationChanged(newConfig: Configuration) {
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            goToLandscapeMode()
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            goToPortraiteMode()
        }
    }

    private fun goToLandscapeMode() {
        adContainer?.let { container ->
            val layoutParams: RelativeLayout.LayoutParams = container.layoutParams as RelativeLayout.LayoutParams
            layoutParams.removeRule(RelativeLayout.CENTER_IN_PARENT)
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE)
            layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE)
            layoutParams.setMargins(0.dp, 25.dp, 0.dp, 0.dp)
            container.layoutParams = layoutParams
        }

    }

    private fun goToPortraiteMode() {
        adContainer?.let { container ->
            val layoutParams: RelativeLayout.LayoutParams = container.layoutParams as RelativeLayout.LayoutParams
            layoutParams.removeRule(RelativeLayout.ALIGN_PARENT_TOP)
            layoutParams.removeRule(RelativeLayout.CENTER_HORIZONTAL)
            layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE)
            layoutParams.setMargins(0.dp, 0.dp, 0.dp, 0.dp)
            container.layoutParams = layoutParams
        }
    }

    fun initAds() {
        adCloseButton = player.findViewById(R.id.close_ad)
        banner = player.findViewById(R.id.banner)
        adContainer = player.findViewById(R.id.ad_container)


        requestNewAd()

        adCloseButton?.setOnClickListener { hideAds() }
    }

    fun playerStateChanged(isPlaying: Boolean) {
        if (isPlaying) hideAds()
        else showAds()
    }

    private fun requestNewAd() {
        Log.d(TAG, "requestNewAd:")
        TapsellPlus.showBannerAd(
                player,
                banner,
                "5fd7a4556ccd5c000137994c",
                TapsellPlusBannerType.BANNER_320x100,
                object : AdRequestCallback() {
                    override fun response(res: String) {
                        Log.d(TAG, "response: $res")
                        requestFilled = true
                    }

                    override fun error(message: String?) {
                        Log.d(TAG, "error: $message")
                        requestFilled = false
                    }
                })
    }

    private fun hideAds() {
//        Log.d(TAG, "hideAds")
        banner.setGone()
        adCloseButton?.setInvisible()
    }

    private var numberOfTimesShowAdsIsCalled: Int = 1
    private var shouldRequestNewAd = false
    private fun shouldShowAds(): Boolean {
        if (isInPictureInPictureMode) return false

        if (numberOfTimesShowAdsIsCalled % 10 != 0) {
            if (shouldRequestNewAd) requestNewAd()
            shouldRequestNewAd = false
            return false
        }

        return true
    }

    private fun showAds() {
        numberOfTimesShowAdsIsCalled++
        if (!shouldShowAds()) return

        shouldRequestNewAd = true

//        Log.d(TAG, "showAds")
        banner.setVisible()

        adCloseButton?.setVisible()
    }

    private var isInPictureInPictureMode: Boolean = false
    fun onPictureInPictureModeChanged(inPictureInPictureMode: Boolean) {
        isInPictureInPictureMode = inPictureInPictureMode
        if (inPictureInPictureMode) hideAds()
    }
}