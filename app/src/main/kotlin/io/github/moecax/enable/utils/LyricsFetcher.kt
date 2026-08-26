/*
    This file is part of AbleMusicPlayer.
    AbleMusicPlayer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, version 3 of the License.
    AbleMusicPlayer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.
    You should have received a copy of the GNU General Public License
    along with AbleMusicPlayer.  If not, see <https://www.gnu.org/licenses/>.
*/

package io.github.moecax.enable.utils

import android.util.Log
import io.github.moecax.enable.model.song.Song
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.File
import java.net.URLEncoder

/**
 * Fetches time-synced lyrics from LRCLIB (https://lrclib.net), keyed on
 * song title/artist, caching results to [Constants.lyricsDir] so repeat
 * lookups (and songs known to have no match) don't hit the network again.
 */
object LyricsFetcher {
    private const val TAG = "LyricsFetcher"
    private const val USER_AGENT = "En-able Android Music Player (https://github.com/moecax/AbleMusicPlayer)"
    private const val NO_LYRICS_SENTINEL = "NO_LYRICS"

    private val LRC_LINE = Regex("""\[(\d{2}):(\d{2})\.(\d{2,3})](.*)""")

    data class LyricLine(val timeMs: Long, val text: String)

    /** The on-disk cache file for [song]'s lyrics. */
    fun getCacheFile(song: Song): File {
        val key = if (song.youtubeLink.isNotBlank())
            Shared.getIdFromLink(song.youtubeLink)
        else
            File(song.filePath).nameWithoutExtension

        return File(Constants.lyricsDir, "$key.lrc")
    }

    /**
     * Returns the synced lyric lines for [song], fetching from LRCLIB and
     * caching to disk on first lookup. Returns an empty list if no synced
     * lyrics are available (including on network/parse failure).
     *
     * Must be called off the main thread.
     */
    fun fetch(song: Song): List<LyricLine> {
        val cacheFile = getCacheFile(song)

        if (cacheFile.exists()) {
            val cached = cacheFile.readText()
            return if (cached == NO_LYRICS_SENTINEL) emptyList() else parseLrc(cached)
        }

        return try {
            val synced = search(song)
            Constants.lyricsDir.mkdirs()
            if (synced != null) {
                cacheFile.writeText(synced)
                parseLrc(synced)
            } else {
                cacheFile.writeText(NO_LYRICS_SENTINEL)
                emptyList()
            }
        } catch (e: Exception) {
            // Transient failure (network, parsing, ...) - don't cache a
            // sentinel so this is retried on the next play.
            Log.e(TAG, "Failed to fetch lyrics for ${song.name}", e)
            emptyList()
        }
    }

    /** Queries LRCLIB's search endpoint, returning the first result's raw syncedLyrics, if any. */
    private fun search(song: Song): String? {
        val url = "${Constants.LRCLIB_SEARCH_API}?track_name=${URLEncoder.encode(song.name, "UTF-8")}" +
                "&artist_name=${URLEncoder.encode(song.artist, "UTF-8")}"

        val request = Request.Builder()
            .url(url)
            .get()
            .addHeader("User-Agent", USER_AGENT)
            .build()

        OkHttpClient().newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val results = JSONArray(response.body.string())
            for (i in 0 until results.length()) {
                val result = results.getJSONObject(i)
                val synced = result.optString("syncedLyrics", "")
                if (synced.isNotBlank()) return synced
            }
        }

        return null
    }

    /** Parses raw LRC-format text (`[mm:ss.xx]line`) into sorted [LyricLine]s. */
    fun parseLrc(raw: String): List<LyricLine> {
        return raw.lineSequence()
            .mapNotNull { line -> LRC_LINE.find(line) }
            .map { match ->
                val (min, sec, frac, text) = match.destructured
                val fracMs = if (frac.length == 2) frac.toLong() * 10 else frac.toLong()
                val timeMs = min.toLong() * 60_000 + sec.toLong() * 1000 + fracMs
                LyricLine(timeMs, text.trim())
            }
            .sortedBy { it.timeMs }
            .toList()
    }
}
