package com.garan.wearwind

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.garan.wearwind.databinding.ActivityAboutBinding
import com.google.android.wearable.intent.RemoteIntent

class AboutActivity : FragmentActivity() {
    private lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.text.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
                .addCategory(Intent.CATEGORY_BROWSABLE)
                .setData(Uri.parse(ABOUT_URL))
            RemoteIntent.startRemoteActivity(
                this,
                intent, null
            )
        }
    }

    companion object {
        const val ABOUT_URL = "https://github.com/garanj/wearwind/blob/main/README.md"
    }
}