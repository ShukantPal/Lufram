package com.zexfer.lufram

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProviders
import com.zexfer.lufram.Lufram.Companion.EXTRA_WALLPAPER

class DiscreteWallpaperCreatorActivity : AppCompatActivity(), DiscreteWallpaperEditorFragment.OnSubmitClickListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_discrete_wallpaper_creator)

        if (savedInstanceState === null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.content, DiscreteWallpaperEditorFragment())
                .commit()
        }
    }

    override fun onSubmitClick() {
        setResult(RESULT_OK, Intent().also {
            it.putExtra(
                EXTRA_WALLPAPER,
                ViewModelProviders.of(this)[WallpaperViewModel::class.java].targetWallpaper
            )
        })

        finish()
    }

}
