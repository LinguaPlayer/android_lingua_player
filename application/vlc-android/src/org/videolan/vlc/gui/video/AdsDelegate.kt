package org.videolan.vlc.gui.video

import android.content.res.Configuration
import android.util.Log
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import ir.tapsell.sdk.bannerads.TapsellBannerType
import ir.tapsell.sdk.bannerads.TapsellBannerView
import ir.tapsell.sdk.bannerads.TapsellBannerViewEventListener
import org.videolan.tools.dp
import org.videolan.tools.setInvisible
import org.videolan.tools.setVisible
import org.videolan.vlc.R

private const val TAG = "AdsDelegate"

class AdsDelegate (val player: VideoPlayerActivity) {

    private var banner: TapsellBannerView? = null
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
        adContainer?.let {container ->
            val layoutParams: RelativeLayout.LayoutParams = container.layoutParams as RelativeLayout.LayoutParams
            layoutParams.removeRule(RelativeLayout.CENTER_IN_PARENT)
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE)
            layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE)
            layoutParams.setMargins(0.dp, 25.dp, 0.dp, 0.dp)
            container.layoutParams = layoutParams
        }

    }

    private fun goToPortraiteMode() {
        adContainer?.let {container ->
            val layoutParams: RelativeLayout.LayoutParams = container.layoutParams as RelativeLayout.LayoutParams
            layoutParams.removeRule(RelativeLayout.ALIGN_PARENT_TOP)
            layoutParams.removeRule(RelativeLayout.CENTER_HORIZONTAL)
            layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE)
            layoutParams.setMargins(0.dp, 0.dp, 0.dp, 0.dp)
            container.layoutParams = layoutParams
        }
    }

    fun initAds() {
        banner = player.findViewById(R.id.banner)
        adCloseButton = player.findViewById(R.id.close_ad)
        adContainer = player.findViewById(R.id.ad_container)
        requestNewAd()

        banner?.setEventListener(object : TapsellBannerViewEventListener {
            override fun onNoAdAvailable() {
                Log.d(TAG, "onNoAdAvailable: ")
            }

            override fun onNoNetwork() {
                Log.d(TAG, "onNoNetwork: ")
            }

            override fun onError(error: String?) {
                Log.d(TAG, "onError: $error")
            }

            override fun onRequestFilled() {
                Log.d(TAG, "onRequestFilled")
                requestFilled = true
                player.service?.let {
                    if (it.isPlaying || !shouldShowAds()) hideAds()
                    else adCloseButton?.setVisible()
                }
            }

            override fun onHideBannerView() {
                Log.d(TAG, "onHideBannerView: ")
            }
        })

        adCloseButton?.setOnClickListener { hideAds() }
    }

    fun playerStateChanged(isPlaying: Boolean) {
        if (isPlaying) hideAds()
        else showAds()
    }

    private fun requestNewAd() {
        banner?.loadAd(player.applicationContext, "5fd7a4556ccd5c000137994c", TapsellBannerType.BANNER_320x100)
    }

    private fun hideAds() {
        Log.d(TAG, "hideAds")
        banner?.hideBannerView()
        adCloseButton?.setInvisible()
    }

    private var numberOfTimesShowAdsIsCalled: Int = 1
    private var shouldRequestNewAd = false

    private fun shouldShowAds(): Boolean {
        if (isInPictureInPictureMode) return false

        if (numberOfTimesShowAdsIsCalled % 4 != 0) {
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

        Log.d(TAG, "showAds")
        banner?.showBannerView()

        if (requestFilled) adCloseButton?.setVisible()
    }

    private var isInPictureInPictureMode: Boolean = false
    fun onPictureInPictureModeChanged(inPictureInPictureMode: Boolean) {
        isInPictureInPictureMode = inPictureInPictureMode
        if (inPictureInPictureMode) hideAds()
    }
}