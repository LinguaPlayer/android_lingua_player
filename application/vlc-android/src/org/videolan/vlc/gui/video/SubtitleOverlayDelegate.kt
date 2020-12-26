package org.videolan.vlc.gui.video

import android.content.DialogInterface
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.text.Spannable
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.core.text.toSpannable
import androidx.core.view.marginLeft
import androidx.core.view.marginRight
import androidx.core.view.marginTop
import androidx.lifecycle.*
import androidx.preference.PreferenceManager
import com.github.kazemihabib.cueplayer.util.EventObserver
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.videolan.tools.*
import org.videolan.vlc.R
import org.videolan.vlc.gui.helpers.UiTools
import org.videolan.vlc.gui.view.StrokedTextView
import org.videolan.vlc.media.ShowCaption
import org.videolan.vlc.mediadb.models.Subtitle
import org.videolan.vlc.repository.SubtitlesRepository
import org.videolan.vlc.util.*
import java.util.regex.Pattern


private const val TAG = "SubtitleOverlayDelegate"

class SubtitleOverlayDelegate(private val player: VideoPlayerActivity) {

    private val nextCaptionButton: ImageView? = player.findViewById(R.id.next_caption)
    private val prevCaptionButton: ImageView? = player.findViewById(R.id.prev_caption)
    private val subtitleContainer: ConstraintLayout? = player.findViewById(R.id.subtitle_container)
    private val subtitleTextView: StrokedTextView? = player.findViewById(R.id.subtitleTextView)
    private val smartSub: ImageButton? = player.findViewById(R.id.listening_mode)

    init {
        nextCaptionButton?.apply {
            setOnClickListener {
                player.service?.playlistManager?.player?.getNextCaption(false)
            }
            setOnLongClickListener {
                player.service?.playlistManager?.player?.getNextCaption(true)
                true
            }
        }

        prevCaptionButton?.apply {
            setOnClickListener {
                player.service?.playlistManager?.player?.getPreviousCaption(false)
            }

            setOnLongClickListener {
                player.service?.playlistManager?.player?.getPreviousCaption(true)
                true
            }

        }

        smartSub?.setOnClickListener {
            toggleSmartSubtitle()
        }


    }

    fun shadowingModeEnabled() {
        smartSub?.setGone()

        player.service?.playlistManager?.player?.apply {
            if (isSmartSubtitleEnabled()) this@SubtitleOverlayDelegate.disableSmartSubtitle()
        }
    }

    fun shadowingModeDisabled() {
        smartSub?.setVisible()
    }

    private fun toggleSmartSubtitle() {
        player.service?.playlistManager?.player?.run {
            if (isSmartSubtitleEnabled())  this@SubtitleOverlayDelegate.disableSmartSubtitle()
            else this@SubtitleOverlayDelegate.enableSmartSubtitle()
            updateCurrentCaption()
        }
    }

    private fun disableSmartSubtitle() {
        player.service?.playlistManager?.player?.disableSmartSubtitle()
        smartSub?.isSelected = false
        player.overlayDelegate.showInfo(R.string.smart_subtitle_disabled, 1000)
    }

    private fun enableSmartSubtitle() {
        player.service?.playlistManager?.player?.enableSmartSubtitle()
        smartSub?.isSelected = true
        player.overlayDelegate.showInfo(R.string.smart_subtitle_enabled, 1000)
    }


    val subtitleObserver = Observer { subtitleList: List<Subtitle> ->
        player.lifecycleScope.launch {
            player.service?.playlistManager?.player?.apply{
                parseSubtitles(subtitleList.map { it.subtitlePath.path!! })
                decideAboutCaptionButtonVisibility(isPlaying())
            }
        }
    }

    fun onConfigurationChanged(newConfig: Configuration) {
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            goToLandscapeMode()
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            goToPortraiteMode()
        }
    }

    private fun goToLandscapeMode() {
        val constraintLayout: ConstraintLayout = player.findViewById(R.id.subtitle_container)
        val constraintSet = ConstraintSet()
        constraintSet.clone(constraintLayout)
        constraintSet.connect(R.id.subtitleTextView, ConstraintSet.END, R.id.next_caption, ConstraintSet.START, 0)
        constraintSet.connect(R.id.subtitleTextView, ConstraintSet.START, R.id.prev_caption, ConstraintSet.END, 0)
        constraintSet.connect(R.id.next_caption, ConstraintSet.BOTTOM, R.id.subtitle_container, ConstraintSet.BOTTOM, 0)
        constraintSet.connect(R.id.prev_caption, ConstraintSet.BOTTOM, R.id.subtitle_container, ConstraintSet.BOTTOM, 0)

        constraintSet.connect(R.id.next_caption, ConstraintSet.TOP, R.id.subtitle_container, ConstraintSet.TOP, 0)
        constraintSet.connect(R.id.prev_caption, ConstraintSet.TOP, R.id.subtitle_container, ConstraintSet.TOP, 0)
        constraintSet.setVerticalBias(R.id.next_caption, 1f)
        constraintSet.setVerticalBias(R.id.prev_caption, 1f)

        constraintSet.clear(R.id.next_caption, ConstraintSet.TOP)
        constraintSet.clear(R.id.prev_caption, ConstraintSet.TOP)
        constraintSet.applyTo(constraintLayout)
    }

    private fun goToPortraiteMode() {
        val constraintLayout: ConstraintLayout = player.findViewById(R.id.subtitle_container)
        val constraintSet = ConstraintSet()
        constraintSet.clone(constraintLayout)
        constraintSet.connect(R.id.subtitleTextView, ConstraintSet.END, R.id.subtitle_container, ConstraintSet.END, 8.dp)
        constraintSet.connect(R.id.subtitleTextView, ConstraintSet.START, R.id.subtitle_container, ConstraintSet.START, 8.dp)
        constraintSet.connect(R.id.next_caption, ConstraintSet.TOP, R.id.subtitleTextView, ConstraintSet.BOTTOM)
        constraintSet.connect(R.id.prev_caption, ConstraintSet.TOP, R.id.subtitleTextView, ConstraintSet.BOTTOM)
        constraintSet.clear(R.id.next_caption, ConstraintSet.BOTTOM)
        constraintSet.clear(R.id.prev_caption, ConstraintSet.BOTTOM)

        constraintSet.setVerticalBias(R.id.next_caption, 0.5f)
        constraintSet.setVerticalBias(R.id.prev_caption, 0.5f)

        constraintSet.applyTo(constraintLayout)
    }


    fun prepareSubtitles(videoUri: Uri) {
        prepareEmbeddedSubtitles(videoUri)
        SubtitlesRepository.getInstance(player.applicationContext).getSelectedSpuTracksLiveData(mediaPath = videoUri).observe(player, subtitleObserver)

        player.service?.playlistManager?.player?.subtitleCaption?.observe(player, showCaptionObserver)

        player.lifecycleScope.launchWhenStarted {
            player.service?.playlistManager?.player?.numberOfParsedSubsFlow?.collect { numberOfSubs ->
                when {
                    player.service?.playlistManager?.player?.isShadowingModeEnabled == true -> smartSub.setGone()
                    numberOfSubs > 0 -> smartSub.setVisible()
                    else -> smartSub.setGone()
                }
            }
        }

        player.service?.playlistManager?.player?.subtitleInfo?.observe(player, EventObserver {
            UiTools.snacker(player, it.message)
        })
    }

    fun prepareEmbeddedSubtitles(videoUri: Uri) {

        //TODO: Does just checking unAttempted subs are enough?
        player.lifecycleScope.launch {
            player.service?.playlistManager?.player?.apply {
                val unAttemptedToExtractEmbeddedSubs = getEmbeddedSubsWhichAreUnattemptedToExtract(videoUri)
                val isThereAnyUnattemptedToExtractEmbeddedSubs = unAttemptedToExtractEmbeddedSubs.isNotEmpty()
                if (isThereAnyUnattemptedToExtractEmbeddedSubs) Snackbar.make(player.findViewById(R.id.player_root), R.string.video_has_embeddedspu, Snackbar.LENGTH_LONG)
                        .show()
                unAttemptedToExtractEmbeddedSubs.forEach {
                    player.service?.playlistManager?.player?.extractEmbeddedSubtitle(videoUri, it.index)
                }
                // If there are multiple subtitles user can select which subtitle to show at the moment
                if (isThereAnyUnattemptedToExtractEmbeddedSubs) {
                    delay(50)
                    if (SubtitlesRepository.getInstance(player.applicationContext).getSelectedSpuTracks(videoUri).size > 1)
                        player.overlayDelegate.showTracks()
                }

            }
        }
    }

    private var backgroundColorEnabled = false
    @ColorInt private var subtitleBackgroundColor: Int = ContextCompat.getColor(player.applicationContext, R.color.black)

    private val showCaptionObserver = Observer<ShowCaption> {
        var caption: Spannable = HtmlCompat.fromHtml(it.caption, HtmlCompat.FROM_HTML_MODE_LEGACY).toSpannable()
        if (!it.isTouchable) makeClickable(caption)

//        if (backgroundColorEnabled)
//            caption.setBackgroundColor(subtitleBackgroundColor)

        // TODO: add clickable span to previous spannable
        subtitleTextView?.text = caption
    }

    var color = Color.parseColor("#ffffff")

    fun updateSubtitleTextViewStyle() {
        // not in settings yet
//        val strokeColor = PreferenceManager.getDefaultSharedPreferences(player.applicationContext).getString("subtitle_stroke_color", ContextCompat.getColor(player.applicationContext, R.color.black))
        val strokeColor = ContextCompat.getColor(player.applicationContext, R.color.black)
        val strokeWidth = PreferenceManager.getDefaultSharedPreferences(player.applicationContext).getInt("subtitle_stroke_width", 3)
        //////////////////////
        color = Color.parseColor(PreferenceManager.getDefaultSharedPreferences(player.applicationContext).getString("subtitles_color", "#ffffff")
                ?: "#ffffff")

        val size = PreferenceManager.getDefaultSharedPreferences(player.applicationContext).getString("subtitles_size", "25")?.toInt() ?: 25
        val bold = PreferenceManager.getDefaultSharedPreferences(player.applicationContext).getBoolean("subtitles_bold", false)
        backgroundColorEnabled = PreferenceManager.getDefaultSharedPreferences(player.applicationContext).getBoolean("subtitles_background", false)
        val backgroundColor = ContextCompat.getColor(player.applicationContext, R.color.black_more_transparent)
//        Log.d(TAG, "updateSubtitleTextViewStyle: $color $size $bold $backgroundColorEnabled")
//        Log.d(TAG, "updateSubtitleTextViewStyle: stroke $strokeColor $strokeWidth")
        subtitleTextView?.apply {
            setStrokeColor(strokeColor)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, size.toFloat())
            setStrokeWidth(TypedValue.COMPLEX_UNIT_DIP, strokeWidth)
            setTextColor(color)
            if (bold) setTypeface(null, Typeface.BOLD)
            movementMethod = LinkTouchMovementMethod()
            if (backgroundColorEnabled)
                setBackgroundColor(backgroundColor)
        }
    }

    fun Spannable.setBackgroundColor(@ColorInt color: Int): Spannable {
        this.setSpan(BackgroundColorSpan(color), 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        return this
    }

    fun updateSubtitlePosition(playerControllerHeight: Int, calledFromOnLayoutChangeListener: Boolean) {
        val subtitleBottomPosition = playerControllerHeight + if (playerControllerHeight == 0) 4.dp.toPixel() else 0.dp.toPixel()
        subtitleContainer?.setMargins(l = subtitleContainer.marginLeft, t = subtitleContainer.marginTop, r = subtitleContainer.marginRight, b = subtitleBottomPosition)
        // TODO: fixme: dirty hack. when video starts if user pauses then taps on screen setMargin won't take effect unless clicking on a button or calling dimStatusBar
        if (calledFromOnLayoutChangeListener) {
            player.overlayDelegate.dimStatusBar(true)
            player.lifecycleScope.launch {
                delay(100)
                player.overlayDelegate.dimStatusBar(false)
            }
        }
    }

    fun View.setMargins(l: Int, t: Int, r: Int, b: Int) {
        if (this.layoutParams is ViewGroup.MarginLayoutParams){
            val p: ViewGroup.MarginLayoutParams = this.layoutParams as ViewGroup.MarginLayoutParams
            p.setMargins(l, t, r, b)
            this.requestLayout()
        }
    }

    private fun makeClickable(spannableText: Spannable): Spannable {
        val pattern = "[-!$%^&*()_+|~=`{}\\[\\]:\\\";'<>?,.\\/\\s+]"
        val r: Pattern = Pattern.compile(pattern)
        val words = spannableText.toString().split("(?=[-!$%^&*()_+|~=`{}\\[\\]:\\\";'<>?,.\\/\\s+])|(?<=[-!$%^&*()_+|~=`{}\\[\\]:\\\";'<>?,.\\/\\s+])".toRegex())
//        val words = spannableText.toString().split("\\/\\s+".toRegex())
        var start = 0
        var end = 0
        words.forEach { word ->
            end = start + word.length
//            if (!r.matcher(word).find()) {
                val clickableSpan: ForegroundColorSpan = SubTouchSpan(start = start, end = end, underlineText = false, normalTextColor = color, pressedTextColor = ContextCompat.getColor(player.applicationContext, R.color.orange500), bgColor = Color.TRANSPARENT, pressedBackgroundColor = Color.TRANSPARENT)
                spannableText.setSpan(clickableSpan, start, end, 0)
//            }
            start = end
        }

        return spannableText
    }

    fun decideAboutCaptionButtonVisibility(isPlaying: Boolean) {
        player.service?.playlistManager?.player?.let {
            if (it.isShadowingModeEnabled) {
                hideCaptionButtons()
            }
            else if (isPlaying) {
                hideCaptionButtons()
            } else {
                if (it.numberOfParsedSubs == 0) hideCaptionButtons()
                else showCaptionButtons()
            }
        }
    }

    private fun hideCaptionButtons() {
        nextCaptionButton.setGone()
        prevCaptionButton.setGone()
    }

    private fun showCaptionButtons() {
        nextCaptionButton.setVisible()
        prevCaptionButton.setVisible()
    }

    private fun showLoadingForTranslation(duration: Long) {
//        player.handler.sendEmptyMessage(VideoPlayerActivity.SHOW_WITING_FOR_TRANSLATION)
//        player.handler.sendEmptyMessageDelayed(VideoPlayerActivity.HIDE_WITING_FOR_TRANSLATION, duration)
    }

    private inner class LinkTouchMovementMethod : LinkMovementMethod() {
        private var touchedSpans: Array<SubTouchSpan>? = null
        private var startOffset: Int = -1
        var endOffset: Int = -1

        override fun onTouchEvent(textView: TextView, spannable: Spannable, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    onDown(textView, spannable, event)
                }

                MotionEvent.ACTION_MOVE -> {
                    onMove(textView, spannable, event)
                }

                MotionEvent.ACTION_UP -> {
                    onUp(textView.text.toString())
                }
            }
            return true
        }

        private fun onDown(textView: TextView, spannable: Spannable, event: MotionEvent) {
            player.service?.pause()
            startOffset = getOffset(textView, event)
        }

        private fun onMove(textView: TextView, spannable: Spannable, event: MotionEvent) {
            player.service?.pause()
            endOffset = getOffset(textView, event)

            // user redoed his highlights and went behind his/her first touch
            if (endOffset < startOffset) {
                touchedSpans?.forEach { it.setPressed(false) }
                touchedSpans = null
                return
            }

            // user redoed his highlights and went behind his/her first touch
            val newTouchedSpans = getTouchedSpans(spannable, startOffset, endOffset)
            if (!touchedSpans.isNullOrEmpty() && newTouchedSpans.isNotEmpty()) {
                getTouchedSpans(spannable, newTouchedSpans.last().end + 1, touchedSpans!!.last().end).forEach {
                    it.setPressed(false)
                }
            }
            touchedSpans = newTouchedSpans
            touchedSpans?.forEach { it.setPressed(true) }

        }

        private fun onUp(text: String) {
            touchedSpans?.let { selectedSpans ->
                selectedSpans.forEach {
                    it.setPressed(false)
                }

                if (selectedSpans.isNotEmpty()) {
                    translate(text.subSequence(selectedSpans.first().start, selectedSpans.last().end).toString())
                }

            }
            startOffset = -1
            endOffset = -1
            touchedSpans = null

        }

        private fun getOffset(textView: TextView, event: MotionEvent): Int {
            var x = event.x.toInt()
            var y = event.y.toInt()
            val layout = textView.layout
            val line = layout.getLineForVertical(y)
            return layout.getOffsetForHorizontal(line, x.toFloat())
        }

        private fun getTouchedSpans(spannable: Spannable, startOffset: Int, endOffset: Int): Array<SubTouchSpan> {
            return spannable.getSpans(startOffset, endOffset, SubTouchSpan::class.java)
        }

        private fun translate(text: String) {
            val isGoogleTranslateAvailable = translate(text, player)
            if (isGoogleTranslateAvailable) {
                showLoadingForTranslation(5000L)
            } else {
                UiTools.installGoogleTranslateDialog(player, DialogInterface.OnClickListener { _, _ ->
                    installGoogleTranslate(player.applicationContext)
                },
                        DialogInterface.OnClickListener { _, _ -> })
            }
        }


    }
}

private class SubTouchSpan(val start: Int, val end: Int, private val underlineText: Boolean, private val normalTextColor: Int, private val pressedTextColor: Int, private val bgColor: Int, private val pressedBackgroundColor: Int) : ForegroundColorSpan(Color.WHITE) {
    private var isPressed = false
    fun setPressed(isSelected: Boolean) {
        isPressed = isSelected
    }

    override fun updateDrawState(ds: TextPaint) {
        if (isPressed) {
            ds.strokeWidth = 0f
            ds.color = pressedTextColor
        }
        else {
            ds.color = ds.color
        }

        ds.bgColor = if (isPressed) pressedBackgroundColor else bgColor
        ds.isUnderlineText = underlineText
    }

}

