package uk.co.sixpillar.flicbridge

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import uk.co.sixpillar.flicbridge.service.FlicBridgeService

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Settings"

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings_container, SettingsFragment())
                .commit()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)

            // Show current values in summaries
            findPreference<EditTextPreference>(FlicBridgeService.PREF_DOWN_INTENT)?.apply {
                summaryProvider = EditTextPreference.SimpleSummaryProvider.getInstance()
            }

            findPreference<EditTextPreference>(FlicBridgeService.PREF_UP_INTENT)?.apply {
                summaryProvider = EditTextPreference.SimpleSummaryProvider.getInstance()
            }
        }
    }
}
