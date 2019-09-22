package com.zexfer.lufram

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.ui.NavigationUI
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

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
    }

    override fun onStart() {
        super.onStart()

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
