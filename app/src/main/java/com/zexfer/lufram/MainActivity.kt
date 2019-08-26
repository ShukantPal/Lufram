package com.zexfer.lufram

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.snackbar.Snackbar
import com.zexfer.lufram.database.models.DiscreteWallpaper

class MainActivity : AppCompatActivity(), WallpaperPreviewFragment.WallpaperListProvider {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.app_bar))

        if (savedInstanceState === null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.frame_content, DiscreteWallpaperPreviewFragment())
                .commit()
        }
    }

    override fun onStart() {
        super.onStart()

        if (!LuframRepository.isUpdaterAlive()) {
            Snackbar.make(
                findViewById(android.R.id.content),
                "Your wallpaper has been killed by the system!", Snackbar.LENGTH_SHORT
            )
                .show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            CREATE_DISCRETE_WALLPAPER -> {
                (data?.getParcelableExtra(Lufram.EXTRA_WALLPAPER) as DiscreteWallpaper?)
                    .let {
                        if (it !== null)
                            ViewModelProviders.of(this)[WallpaperViewModel::class.java]
                                .putDiscreteWallpaper(it)
                    }
            }
            else ->
                super.onActivityResult(requestCode, resultCode, data)
        }
    }

    fun onAddWallpaperClick(fab: View?) {
        startActivityForResult(
            Intent(this, DiscreteWallpaperCreatorActivity::class.java),
            CREATE_DISCRETE_WALLPAPER
        )
    }

    override fun visibleWallpapers(adapterId: Int): LiveData<Any> {
        return ViewModelProviders.of(this)[WallpaperViewModel::class.java]
            .discreteWallpapers as LiveData<Any>
    }

    companion object {
        @JvmStatic
        val CREATE_DISCRETE_WALLPAPER = 1
    }
}
