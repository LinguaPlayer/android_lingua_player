package org.videolan.vlc.gui.onboarding

import android.animation.ValueAnimator
import android.graphics.drawable.ClipDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import org.videolan.vlc.R

private const val LEVEL_INCREMENT = 100;
private const val MAX_LEVEL = 10000

class OnboardingTranslateFragment : Fragment() {

    var currentLevel = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_onboarding_translate, container, false)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val subtitleTextOneBackground = view.findViewById<View>(R.id.subtitle_text_1).background as LayerDrawable
        val clipDrawable: ClipDrawable = subtitleTextOneBackground.findDrawableByLayerId(R.id.clip_drawable) as ClipDrawable
        animateTextSelection(clipDrawable)
    }

    fun animateTextSelection(clipDrawable: ClipDrawable) {

        ValueAnimator.ofInt(0, 10000).apply {
            duration = 800
            interpolator = LinearInterpolator()
            addUpdateListener { clipDrawable.level = animatedValue as Int }
            start()
        }
    }

    companion object {
        fun newInstance() = OnboardingTranslateFragment()
    }
}