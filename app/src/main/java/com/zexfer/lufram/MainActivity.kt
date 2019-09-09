package com.zexfer.lufram

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.Navigation
import androidx.navigation.ui.NavigationUI
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    private var drawer: DrawerLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.app_bar))
        drawer = findViewById(R.id.dl_root)

        NavigationUI.setupActionBarWithNavController(
            this,
            Navigation.findNavController(findViewById(R.id.nav_host_fragment)),
            drawer
        )
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
            CREATE_DISCRETE_WALLPAPER -> return
            else ->
                super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onBackPressed() {
        if (drawer!!.isDrawerOpen(GravityCompat.START)) {
            drawer!!.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return NavigationUI.navigateUp(
            Navigation.findNavController(this, R.id.nav_host_fragment),
            drawer
        ) || super.onSupportNavigateUp()
    }

    companion object {
        @JvmStatic
        val CREATE_DISCRETE_WALLPAPER = 1
    }
}
