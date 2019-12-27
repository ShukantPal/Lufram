package com.zexfer.lufram

import android.content.Intent
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.Navigation
import androidx.navigation.ui.NavigationUI
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.zexfer.lufram.viewmodels.WCViewModel

class MainActivity : AppCompatActivity(),
    NavigationView.OnNavigationItemSelectedListener,
    NavController.OnDestinationChangedListener {

    private var dlRoot: DrawerLayout? = null
    private var navController: NavController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.app_bar))

        dlRoot = findViewById(R.id.dl_root)
        navController = Navigation.findNavController(this, R.id.nav_host_fragment)

        findViewById<NavigationView>(R.id.nav_view)
            .setNavigationItemSelectedListener(this)

        NavigationUI.setupActionBarWithNavController(
            this,
            Navigation.findNavController(findViewById(R.id.nav_host_fragment)),
            dlRoot
        )

        ViewModelProviders.of(this)[WCViewModel::class.java]
            .activeWallpaperBitmap
            .observe(this, Observer { wp: Bitmap? ->
                if (wp === null) {
                    return@Observer
                }

                val blurredWp =
                    Bitmap.createBitmap(wp.width, wp.height, Bitmap.Config.ARGB_8888)
                val finalWp =
                    Bitmap.createBitmap(wp.width, wp.height, Bitmap.Config.ARGB_8888)

                Lufram.applyBlur(wp, blurredWp)

                Canvas(finalWp)
                    .drawBitmap(blurredWp, 0f, 0f,
                        Paint().apply {
                            colorFilter =
                                PorterDuffColorFilter(0x88ffffff.toInt(), PorterDuff.Mode.SRC_OVER)
                        })

                blurredWp.recycle()

                dlRoot!!.background = BitmapDrawable(resources, finalWp)
            })
    }

    override fun onStart() {
        super.onStart()
        navController!!.addOnDestinationChangedListener(this)

        if (!LuframRepository.isUpdaterAlive()) {
            Snackbar.make(
                findViewById(android.R.id.content),
                "Your wallpaper was killed by Android! We've fixed that now :)",
                Snackbar.LENGTH_SHORT
            )
                .show()

            LuframRepository.restartWallpaper()
        }
    }

    override fun onStop() {
        super.onStop()
        navController!!.removeOnDestinationChangedListener(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            CREATE_DISCRETE_WALLPAPER -> return
            else ->
                super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onBackPressed() {
        if (dlRoot!!.isDrawerOpen(GravityCompat.START)) {
            dlRoot!!.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestinationChanged(
        controller: NavController,
        destination: NavDestination,
        arguments: Bundle?
    ) {
        if (destination.id != R.id.mainFragment) {
            dlRoot!!.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.START)
        } else {
            dlRoot!!.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, GravityCompat.START)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.option_about ->
                navController!!.navigate(R.id.action_mainFragment_to_aboutFragment)
            R.id.option_settings ->
                navController!!.navigate(R.id.action_mainFragment_to_settingsFragment)
            R.id.option_support_development ->
                navController!!.navigate(R.id.action_mainFragment_to_donateFragment)
            else -> return false
        }

        dlRoot!!.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        return NavigationUI.navigateUp(
            Navigation.findNavController(this, R.id.nav_host_fragment),
            dlRoot
        ) || super.onSupportNavigateUp()
    }

    companion object {
        @JvmStatic
        val CREATE_DISCRETE_WALLPAPER = 1
    }
}
