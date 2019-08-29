package com.zexfer.lufram

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.Navigation
import com.google.android.material.snackbar.Snackbar
import com.zexfer.lufram.database.models.DiscreteWallpaper

class MainActivity : AppCompatActivity(), WallpaperPreviewFragment.WallpaperListProvider {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.app_bar))
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
        Navigation.findNavController(this, R.id.nav_host_fragment)
            .navigate(R.id.action_discreteWallpaperPreviewFragment2_to_discreteWallpaperEditorFragment2)
    }

    override fun visibleWallpapers(adapterId: Int): LiveData<Any> {
        return ViewModelProviders.of(this)[WallpaperViewModel::class.java]
            .discreteWallpapers as LiveData<Any>
    }

    companion object {
        @JvmStatic
        val CREATE_DISCRETE_WALLPAPER = 1

        @JvmStatic
        val EDIT_DISCRETE_WALLPAPER = 2
    }
}
