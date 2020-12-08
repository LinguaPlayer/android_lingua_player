package org.videolan.tools

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.arthenica.mobileffmpeg.Config.RETURN_CODE_CANCEL
import com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS
import com.arthenica.mobileffmpeg.FFmpeg
import com.arthenica.mobileffmpeg.FFprobe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*


private const val TAG = "FFmpegUtils"

private val pendingExtractionTracks: MutableSet<SubtitleStream> = mutableSetOf()
private val _pendingExtractionTracksLiveData = MutableLiveData<Set<SubtitleStream>>()
val pendingExtractionTracksLiveData: LiveData<Set<SubtitleStream>>
    get() = _pendingExtractionTracksLiveData

suspend fun getSubtitleStreams(videoUri: Uri): List<SubtitleStream> = withContext(Dispatchers.IO) {
    val info = FFprobe.getMediaInformation(videoUri.toString())
    val result = info?.streams?.filter { it.type == "subtitle" }?.map {
        var language = ""
        try {
            language = it.tags.getString("language")
        } catch (e: Exception) {
        }

        SubtitleStream(videoUri, it.index.toInt(), language)
    } ?: listOf()

    result
}

private fun generatePathForExtractedSubtitle(context: Context): File {
    val subsDir = context.getDir("subs", Context.MODE_PRIVATE)
    val fileName = generateRandomFileName() + ".srt"
    return File(subsDir, fileName)
}

suspend fun extractSubtitles(context: Context, videoUri: Uri, index: Int): FFmpegResult {
    val extractedPath = generatePathForExtractedSubtitle(context).path
    val command = "-i ${videoUri.path} -map 0:${index} $extractedPath"
    Log.d(TAG, "extractSubtitles: $command")

    val subtitleStream = getSubtitleStreams(videoUri).find { it.index == index }
            ?: return FFmpegResult(videoUri = videoUri, index = index, extractedPath = Uri.parse(""), language = "", message = "Asked sub index not found", returnCode = 10)
    pendingExtractionTracks.add(subtitleStream)
    _pendingExtractionTracksLiveData.postValue(pendingExtractionTracks)

    return suspendCancellableCoroutine { cont ->
        FFmpeg.executeAsync(command) { _, returnCode ->
            pendingExtractionTracks.remove(subtitleStream)
            _pendingExtractionTracksLiveData.postValue(pendingExtractionTracks)

            when (returnCode) {
                RETURN_CODE_SUCCESS -> {
                    cont.resumeWith(Result.success(FFmpegResult(videoUri = videoUri, index = index, extractedPath = Uri.parse(extractedPath), language = subtitleStream.language, message = "Success", returnCode = returnCode)))
                }
                RETURN_CODE_CANCEL -> {
                    cont.resumeWith(Result.failure(FFmpegUserCancelledException(FFmpegResult(videoUri = videoUri, index = index, extractedPath = Uri.parse(""), language = subtitleStream.language, message = "Cancelled by user", returnCode = returnCode))))
                }
                else -> {
                    cont.resumeWith(Result.failure(FFmpegFailedException(FFmpegResult(videoUri = videoUri, index = index, extractedPath = Uri.parse(""),language = subtitleStream.language, message = "Failed", returnCode = returnCode))))
                }
            }
        }
    }
}

private fun generateRandomFileName() = UUID.randomUUID().toString()

open class FFmpegException : Exception()
class FFmpegUserCancelledException(val ffmpegResult: FFmpegResult) : FFmpegException()
class FFmpegFailedException(val ffmpegResult: FFmpegResult) : FFmpegException()
data class FFmpegResult(val videoUri: Uri, val index: Int, val extractedPath: Uri, val language: String, val message: String, val returnCode: Int)
data class SubtitleStream(val videoUri: Uri, val index: Int, val language: String)