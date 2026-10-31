package com.sentrix.agent.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.sentrix.agent.R

/**
 * AboutActivity — author info, links.
 *
 * ── TO PERSONALISE ───────────────────────────────────────────────────────────
 * Replace the placeholder strings in res/values/strings.xml:
 *   about_author_name   → Innocent I
 *   about_github_url    → https://github.com/1024cyber
 *   about_linkedin_url  → https://linkedin.com/in/yourusername
 *   about_x_url         → https://x.com/yourusername
 *   about_version       → current version string
 * ─────────────────────────────────────────────────────────────────────────────
 */
class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        supportActionBar?.apply {
            title = "About Sentrix"
            setDisplayHomeAsUpEnabled(true)
        }

        findViewById<TextView>(R.id.tvAuthorName).text =
            getString(R.string.about_author_name)

        findViewById<TextView>(R.id.tvVersion).text =
            "Version ${getString(R.string.about_version)}"

        // GitHub
        findViewById<TextView>(R.id.btnGithub).setOnClickListener {
            openUrl(getString(R.string.about_github_url))
        }

        // LinkedIn
        findViewById<TextView>(R.id.btnLinkedin).setOnClickListener {
            openUrl(getString(R.string.about_linkedin_url))
        }

        // X / Twitter
        findViewById<TextView>(R.id.btnX).setOnClickListener {
            openUrl(getString(R.string.about_x_url))
        }
    }

    private fun openUrl(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}