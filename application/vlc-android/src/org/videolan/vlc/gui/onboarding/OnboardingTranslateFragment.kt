package org.videolan.vlc.gui.onboarding

import android.animation.ValueAnimator
import android.graphics.drawable.ClipDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.transition.ChangeBounds
import androidx.transition.TransitionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.videolan.vlc.R


private const val LEVEL_INCREMENT = 100;
private const val MAX_LEVEL = 10000

class OnboardingTranslateFragment : Fragment() {

    lateinit var layout: ConstraintLayout

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        layout = inflater.inflate(R.layout.fragment_onboarding_translate, container, false) as ConstraintLayout
        return layout

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val subtitleTextOneBackground = view.findViewById<View>(R.id.subtitle_text_1).background as LayerDrawable
        val dictionary = view.findViewById<View>(R.id.dictionary)
        val clipDrawable: ClipDrawable = subtitleTextOneBackground.findDrawableByLayerId(R.id.clip_drawable) as ClipDrawable
        activity?.lifecycleScope?.launch {
            delay(200)
            animateTextSelection(clipDrawable)
        }
    }

    private fun animateTextSelection(clipDrawable: ClipDrawable) {
        animateFinger()
        ValueAnimator.ofInt(0, 10000).apply {
            duration = 800
            interpolator = LinearInterpolator()
            addUpdateListener {
                clipDrawable.level = animatedValue as Int
                if (animatedValue == 10000)
                    animateDictionary()
            }

            start()
        }

    }

    private fun animateFinger() {
        val constraintSet = ConstraintSet()
        constraintSet.clone(this.context, R.layout.fragment_onboarding_translate_finger_animated)
        val transition = ChangeBounds()
        transition.interpolator = LinearInterpolator()
        transition.duration = 800
        TransitionManager.beginDelayedTransition(layout, transition)
        constraintSet.applyTo(layout)
    }

    private fun animateDictionary() {
        val constraintSet = ConstraintSet()
        constraintSet.clone(this.context, R.layout.fragment_onboarding_translate_dictionary_animated)
        val transition = ChangeBounds()
        transition.interpolator = OvershootInterpolator(2.0f)
        transition.duration = 800
        TransitionManager.beginDelayedTransition(layout, transition)
        constraintSet.applyTo(layout)
    }

    companion object {
        fun newInstance() = OnboardingTranslateFragment()
    }
}