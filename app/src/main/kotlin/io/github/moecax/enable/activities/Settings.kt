package io.github.moecax.enable.activities

import android.os.Bundle
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.appbar.MaterialToolbar
import com.takisoft.preferencex.PreferenceFragmentCompat
import io.github.moecax.enable.R
import io.github.moecax.enable.services.UpdateCheckWorker
import io.github.moecax.enable.utils.Shared
/**
 * The settings page.
 */
class Settings: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        setContentView(R.layout.settings_view)

        val toolbar = findViewById<MaterialToolbar>(R.id.settings_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.settings)

        if (Shared.isFirstOpen) Shared.isFirstOpen = false
        supportFragmentManager.beginTransaction()
            .replace(
                R.id.content,
                SettingsFragment()
            )
            .commit()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        return true
    }
}

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferencesFix(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, null)

        findPreference<SwitchPreferenceCompat>("check_updates_key")?.setOnPreferenceChangeListener { _, newValue ->
            val context = requireContext()
            // Persist immediately so scheduleOrCancel (which re-reads the
            // preference) sees the new value rather than the stale one —
            // the default Preference persistence happens after this
            // listener returns, which would otherwise be too late.
            PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putBoolean("check_updates_key", newValue as Boolean)
                .apply()
            UpdateCheckWorker.scheduleOrCancel(context)
            true
        }
    }
}
