package io.github.moecax.enable.activities

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import io.github.inflationx.viewpump.ViewPumpContextWrapper
import io.github.moecax.enable.AbleApplication
import io.github.moecax.enable.R
import io.github.moecax.enable.BuildConfig
import io.github.moecax.enable.databinding.AboutBinding
import io.github.moecax.enable.utils.UpdateChecker
import kotlin.concurrent.thread
/**
 * The about page.
 */
class About: AppCompatActivity() {
    private lateinit var binding: AboutBinding

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        binding = AboutBinding.inflate(layoutInflater)

        setContentView(binding.root)

        binding.versionString.text = BuildConfig.VERSION_NAME
        binding.flavorString.text = BuildConfig.BUILD_TYPE.replaceFirstChar { it.uppercase() }
        binding.codenameString.text = BuildConfig.CODENAME

        binding.support.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Moecax/EnAbleMusicPlayer")))
        }

        binding.checkUpdatesButton.setOnClickListener {
            binding.checkUpdatesButton.isEnabled = false
            binding.checkUpdatesProgress.visibility = View.VISIBLE
            thread {
                // Manual checks bypass the 2-day gate — this is user-initiated,
                // not the automated background poll.
                val isNewer = UpdateChecker.checkNow(this@About)
                runOnUiThread {
                    binding.checkUpdatesButton.isEnabled = true
                    binding.checkUpdatesProgress.visibility = View.GONE
                    if (isNewer) {
                        UpdateChecker.showDialogNow(this@About)
                    } else {
                        Toast.makeText(this@About, R.string.up_to_date, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase!!, AbleApplication.viewPump))
    }
}
