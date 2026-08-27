package io.github.moecax.enable.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.preference.PreferenceManager
import com.afollestad.materialdialogs.MaterialDialog
import io.github.moecax.enable.BuildConfig
import io.github.moecax.enable.R
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

/**
 * Checks GitHub Releases for newer En-able builds, caches the result to
 * the `able_prefs` SharedPreferences file, and shows a changelog dialog
 * gated to once every 2 days. See
 * docs/superpowers/specs/2026-08-27-update-checker-design.md.
 */
object UpdateChecker {
    private const val TAG = "UpdateChecker"
    private const val USER_AGENT = "En-able Android Music Player (https://github.com/Moecax/EnAbleMusicPlayer)"
    private const val PREFS_NAME = "able_prefs"
    private const val KEY_TAG = "update_cached_tag"
    private const val KEY_NOTES = "update_cached_notes"
    private const val KEY_URL = "update_cached_url"
    private const val KEY_NOTIFIED_TAG = "update_notified_tag"
    private const val KEY_DIALOG_SHOWN_AT = "update_dialog_shown_at"

    /** The changelog dialog is shown at most this often. */
    const val DIALOG_INTERVAL_MS = 2 * 24 * 60 * 60 * 1000L

    private data class CachedRelease(val tag: String, val notes: String, val url: String)

    /** Dev/local builds never participate in update checks. */
    fun isDevBuild(): Boolean = BuildConfig.VERSION_NAME == "dev"

    /** Whether the user has left automatic update checks enabled (default on). */
    fun isEnabled(context: Context): Boolean =
        PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean("check_updates_key", true)

    /**
     * Fetches the latest GitHub release and caches it regardless of
     * outcome (so stale cached data is refreshed even if it's not newer).
     * On any network/parse failure, the existing cache is left untouched.
     *
     * Must be called off the main thread. Returns true if the fetched
     * release is newer than the running build.
     */
    fun checkNow(context: Context): Boolean {
        return try {
            val request = Request.Builder()
                .url(Constants.GITHUB_RELEASES_API)
                .get()
                .addHeader("User-Agent", USER_AGENT)
                .addHeader("Accept", "application/vnd.github+json")
                .build()

            OkHttpClient().newCall(request).execute().use { response ->
                if (!response.isSuccessful) return false
                val json = JSONObject(response.body.string())
                val tag = json.getString("tag_name")
                val notes = json.optString("body", "")
                val url = json.getString("html_url")

                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                    .putString(KEY_TAG, tag)
                    .putString(KEY_NOTES, notes)
                    .putString(KEY_URL, url)
                    .apply()

                VersionComparator.isNewer(tag, BuildConfig.VERSION_NAME)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Update check failed", e)
            false
        }
    }

    /** Cache-only: is the last fetched release newer than the running build? */
    fun hasPendingUpdate(context: Context): Boolean {
        val cached = getCached(context) ?: return false
        return VersionComparator.isNewer(cached.tag, BuildConfig.VERSION_NAME)
    }

    /** Cache-only: the last fetched release tag, if any. */
    fun cachedTag(context: Context): String? = getCached(context)?.tag

    fun markNotified(context: Context, tag: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_NOTIFIED_TAG, tag)
            .apply()
    }

    fun alreadyNotified(context: Context, tag: String): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_NOTIFIED_TAG, null) == tag

    /**
     * Cache-only, no network. Shows the changelog dialog if a pending
     * update exists and it hasn't been shown in the last 2 days.
     */
    fun maybeShowDialog(activity: Activity) {
        if (isDevBuild() || !isEnabled(activity)) return
        if (!hasPendingUpdate(activity)) return

        val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastShown = prefs.getLong(KEY_DIALOG_SHOWN_AT, 0L)
        if (System.currentTimeMillis() - lastShown < DIALOG_INTERVAL_MS) return

        showDialogNow(activity)
    }

    /** Shows the changelog dialog unconditionally and stamps the shown-at timestamp. */
    fun showDialogNow(activity: Activity) {
        val cached = getCached(activity) ?: return

        activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putLong(KEY_DIALOG_SHOWN_AT, System.currentTimeMillis())
            .apply()

        MaterialDialog(activity).show {
            title(text = activity.getString(R.string.update_available_title, cached.tag))
            message(text = cached.notes.ifBlank { activity.getString(R.string.update_no_notes) })
            positiveButton(text = activity.getString(R.string.update_now)) {
                activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(cached.url)))
            }
            negativeButton(text = activity.getString(R.string.update_later))
        }
    }

    private fun getCached(context: Context): CachedRelease? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val tag = prefs.getString(KEY_TAG, null) ?: return null
        val notes = prefs.getString(KEY_NOTES, "") ?: ""
        val url = prefs.getString(KEY_URL, null) ?: return null
        return CachedRelease(tag, notes, url)
    }
}
