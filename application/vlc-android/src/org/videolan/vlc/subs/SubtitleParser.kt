package org.videolan.vlc.subs

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.videolan.vlc.R
import org.videolan.vlc.util.FileUtils
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.*

private const val TAG = "SubtitleParser"

@ObsoleteCoroutinesApi
@ExperimentalCoroutinesApi
class SubtitleParser {
    private val parsedSubtitles = TreeMap<String, TimedTextObject>()
    private var subtitleDelay = 0L

    fun setSubtitleDelay(delay: Long) {
        subtitleDelay = delay
    }

    private val _parsedSubtitlesFlow = MutableStateFlow(0)
    val parsedSubtitlesFlow = _parsedSubtitlesFlow
    fun getNumberOfParsedSubs(): Int = parsedSubtitles.size

    private fun parse(subtitlePath: String, subtitleLanguage: String? = null, subtitleManualEncoding: String = ""): TimedTextObject {
        val subtitlePathFile = File(subtitlePath)
        return when (subtitlePathFile.extension) {
            "srt" -> {
                return FormatSRT().parseFile(
                        subtitlePathFile.name,
                        SubUtils.inputstreamToCharsetString(
                                FileInputStream(File(subtitlePath)),
                                subtitleLanguage, subtitleManualEncoding
                        ).split("\n").toTypedArray())
            }
            "ssa", "ass" -> {
                return FormatASS().parseFile(
                        subtitlePathFile.name,
                        SubUtils.inputstreamToCharsetString(
                                FileInputStream(File(subtitlePath)),
                                subtitleLanguage, subtitleManualEncoding
                        ).split("\n").toTypedArray())
            }
            "vtt" -> {
                return FormatVTT().parseFile(
                        subtitlePathFile.name,
                        SubUtils.inputstreamToCharsetString(
                                FileInputStream(File(subtitlePath)),
                                subtitleLanguage, subtitleManualEncoding
                        ).split("\n").toTypedArray())
            }

            else -> TimedTextObject()
        }
    }

    fun parseAsTimedTextObject(
            context: Context,
            subtitlePaths: List<String>,
            subtitleLanguage: String? = null,
            subtitleManualEncoding: String = ""): Flow<SubtitleParsinginfo> = flow {
        // Remove unselected items from parsedSubtitles
        parsedSubtitles.filterNot { subtitlePaths.contains(it.key) }.forEach {
            parsedSubtitles.remove(it.key)
            _parsedSubtitlesFlow.value = parsedSubtitles.size
        }


        subtitlePaths.filterNot { parsedSubtitles.containsKey(it) }.forEach { subtitlePath ->
            try {
                parse(subtitlePath, subtitleLanguage, subtitleManualEncoding).also {
//                        it.captions.forEach { a ->
//                            Log.d(org.videolan.vlc.subs.TAG, "${a.key} ${a.value.start.milliseconds} --> ${a.value.end.milliseconds} : ${a.value.content}")
//                        }
                }
                        .run {
                            if (this.captions.isEmpty()) {
                                emit(
                                        SubtitleParsinginfo(
                                                subtitlePath,
                                                false,
                                                String.format(context.resources.getString(R.string.subtitle_parsing_error), FileUtils.getFileNameFromPath(subtitlePath))
                                        )
                                )

                            } else {
                                createDelayedCaptions()
                                parsedSubtitles[subtitlePath] = this
                                _parsedSubtitlesFlow.value = parsedSubtitles.size
                                emit(
                                        SubtitleParsinginfo(
                                                subtitlePath,
                                                true,
                                                ""
                                        )
                                )
                            }
                        }
            } catch (exception: IOException) {
                emit(
                        SubtitleParsinginfo(
                                subtitlePath,
                                false,
                                String.format(context.resources.getString(R.string.subtitle_not_found), FileUtils.getFileNameFromPath(subtitlePath))
                        )
                )
            } catch (exception: Exception) {
                Log.d(TAG, "exception: $exception")
                emit(
                        SubtitleParsinginfo(
                                subtitlePath,
                                false,
                                String.format(context.resources.getString(R.string.subtitle_parsing_error), FileUtils.getFileNameFromPath(subtitlePath))
                        )
                )
            }
        }

    }.flowOn(Dispatchers.IO)

    private var lastMinCaptionTime = 0L
    private var lastMaxCaptionTime = 0L

    // I'm returning CaptionData that contains all captions for a time
    // because later I might use that for example show the all list of
    // subs and jump to specific caption by pressing on it.

    fun getCaption(isSmartSubtitleEnabled: Boolean, currentTime: Long): List<CaptionsData> {
        return try {
            parsedSubtitles.map { mapEntry ->
                mapEntry.value.run {
                    if (isSmartSubtitleEnabled) getCaption(currentTime - subtitleDelay, smartCaptions)
                    else getCaption(currentTime - subtitleDelay, captions)
                }
            }.filterNotNull().apply {
                lastMaxCaptionTime = maxByOrNull { it.maxEndTime }?.maxEndTime ?: currentTime - subtitleDelay
                lastMinCaptionTime = minByOrNull { it.minStartTime }?.minStartTime ?: currentTime - subtitleDelay
            }
        } catch (e: ConcurrentModificationException) {
            Log.d(TAG, "getCaption: ConcurrentModificationException")
            listOf()
        } catch (e: Exception) {
            listOf()
        }
    }

    private val acceptableDelay = 500

    fun getNextCaption(isSmartSubtitleEnabled: Boolean): List<CaptionsData> {
        // show the nextCaption
        // if user selected multiple caption it will show multiple captions so I choose the min one
        // if there are other subs that are close to min I'll show them also
        // I save the min and max time of shown subs
        return parsedSubtitles.map { mapEntry ->
            mapEntry.value.run {
                if (isSmartSubtitleEnabled) getNextCaption(lastMaxCaptionTime, smartCaptions)
                else getNextCaption(lastMaxCaptionTime, captions)
            }
        }.filterNotNull().run {
            val minTime = minByOrNull { it.minStartTime }?.minStartTime
            if (minTime != null) {
                lastMinCaptionTime = minTime
                lastMaxCaptionTime = minTime
                filter { kotlin.math.abs(it.minStartTime - minTime) <= acceptableDelay }.also { captionList ->
                    captionList.maxByOrNull { it?.minStartTime }?.minStartTime?.let{ lastMaxCaptionTime = it }
                }
            }
            else  listOf()
        }
    }



    fun getPreviousCaption(isSmartSubtitleEnabled: Boolean): List<CaptionsData> {
        return parsedSubtitles.map { mapEntry ->
            mapEntry.value.run {
                if (isSmartSubtitleEnabled) getPreviousCaption(lastMinCaptionTime, smartCaptions)
                else getPreviousCaption(lastMinCaptionTime, captions)
            }
        }.filterNotNull().run {
            val maxTime = maxByOrNull { it.minStartTime}?.minStartTime
            if (maxTime != null) {
                lastMinCaptionTime = maxTime
                lastMaxCaptionTime = maxTime
                filter { kotlin.math.abs(it.minStartTime - maxTime) <= acceptableDelay }.also { captionList ->
                    captionList.minByOrNull { it?.minStartTime }?.minStartTime?.let{ lastMinCaptionTime = it }
                }
            }
            else  listOf()
        }
    }
}

class SubtitleParsinginfo(val subtitlePath: String, val successful: Boolean, val error: String)

class CaptionsData(val captionsOfThisTime: List<Caption>, val minStartTime: Long, val maxEndTime: Long)