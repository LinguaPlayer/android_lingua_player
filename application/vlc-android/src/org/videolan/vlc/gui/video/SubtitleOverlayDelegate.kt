package org.videolan.vlc.gui.video

import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.text.Spannable
import android.text.style.BackgroundColorSpan
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.core.text.toSpannable
import androidx.core.view.marginLeft
import androidx.core.view.marginRight
import androidx.core.view.marginTop
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.github.kazemihabib.cueplayer.util.EventObserver
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.videolan.tools.dp
import org.videolan.vlc.R
import org.videolan.vlc.gui.view.StrokedTextView
import org.videolan.vlc.media.ShowCaption
import org.videolan.vlc.media.ShowInfo
import org.videolan.vlc.mediadb.models.Subtitle
import org.videolan.vlc.repository.SubtitlesRepository
import org.videolan.vlc.util.toPixel

private const val TAG = "SubtitleOverlayDelegate"

class SubtitleOverlayDelegate(private val player: VideoPlayerActivity) {

    val subtitleObserver = Observer { subtitleList: List<Subtitle> ->
        player.lifecycleScope.launch { player.service?.playlistManager?.player?.parseSubtitles( subtitleList.map { it.subtitlePath.path!! } ) }
    }

    fun prepareSubtitles(videoUri: Uri) {
        SubtitlesRepository.getInstance(player.applicationContext).getSelectedSpuTracksLiveData(mediaPath = videoUri).observe(player, subtitleObserver)

        player.service?.playlistManager?.player?.subtitleCaption?.observe(player, showCaptionObserver)

        player.service?.playlistManager?.player?.subtitleInfo?.observe(player, EventObserver {
            Log.d(TAG, "prepareSubtitles: $it")
        } )
    }
    private var backgroundColorEnabled = true
    @ColorInt private var subtitleBackgroundColor: Int = 0

    private val showCaptionObserver = Observer<ShowCaption> {
        var caption: Spannable =
                if (!it.isTouchable) HtmlCompat.fromHtml(it.caption, HtmlCompat.FROM_HTML_MODE_LEGACY).toSpannable()
                //TODO("implement isTouchable version")
                else HtmlCompat.fromHtml(it.caption, HtmlCompat.FROM_HTML_MODE_LEGACY).toSpannable()
        if (backgroundColorEnabled)
            caption.setBackgroundColor(subtitleBackgroundColor)

        player.findViewById<StrokedTextView>(R.id.subtitleTextView).text = caption
    }

    fun updateSubtitleTextViewStyle() {
        // not in settings yet
        val strokeColor = PreferenceManager.getDefaultSharedPreferences(player.applicationContext).getInt("subtitle_stroke_color", ContextCompat.getColor(player.applicationContext, R.color.black))
        val strokeWidth = PreferenceManager.getDefaultSharedPreferences(player.applicationContext).getInt("subtitle_stroke_width", 3)
        //////////////////////
        val color = PreferenceManager.getDefaultSharedPreferences(player.applicationContext).getString("subtitles_color", ContextCompat.getColor(player.applicationContext, R.color.white).toString())?.toInt() ?: ContextCompat.getColor(player.applicationContext, R.color.white)
        val size = PreferenceManager.getDefaultSharedPreferences(player.applicationContext).getString("subtitle_size", "25")?.toInt() ?: 25
        val bold = PreferenceManager.getDefaultSharedPreferences(player.applicationContext).getBoolean("subtitles_bold", false)
        backgroundColorEnabled = PreferenceManager.getDefaultSharedPreferences(player.applicationContext).getBoolean("subtitles_background", false)
        Log.d(TAG, "updateSubtitleTextViewStyle: $color $size $bold $backgroundColorEnabled")
        Log.d(TAG, "updateSubtitleTextViewStyle: stroke $strokeColor $strokeWidth")
        player.findViewById<StrokedTextView>(R.id.subtitleTextView).apply {
            setStrokeColor(Color.parseColor("#" + Integer.toHexString(strokeColor)))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, size.toFloat())
            setStrokeWidth(TypedValue.COMPLEX_UNIT_DIP, strokeWidth)
            setTextColor(Color.parseColor("#" + Integer.toHexString(color)))
            if (bold) setTypeface(null, Typeface.BOLD)
        }
    }

    fun Spannable.setBackgroundColor( @ColorInt color: Int): Spannable {
        this.setSpan (BackgroundColorSpan (color), 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        return this
    }

    private var playerControllerHeight = 0
    private var subtitleBottomMargin = 4.dp.toPixel()

    // height is in pixels
    fun setOverlayHeight(height: Int) {
        playerControllerHeight = height

        updateSubtitlePosition()
    }

    private fun updateSubtitlePosition() {
        val subtitleBottomPosition = playerControllerHeight + if (playerControllerHeight == 0) subtitleBottomMargin else 0
        val subtitleTextView = player.findViewById<StrokedTextView>(R.id.subtitleTextView)
        subtitleTextView.setMargins(l = subtitleTextView.marginLeft, t = subtitleTextView.marginTop, r = subtitleTextView.marginRight, b = subtitleBottomPosition)
    }

    fun View.setMargins(l: Int, t: Int, r: Int, b: Int){
        if (this.layoutParams is ViewGroup.MarginLayoutParams){
            val p: ViewGroup.MarginLayoutParams = this.layoutParams as ViewGroup.MarginLayoutParams
            p.setMargins(l, t, r, b)
            this.requestLayout()
        }
    }

}