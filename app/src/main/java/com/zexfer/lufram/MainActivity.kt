package com.zexfer.lufram

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.Navigation
import androidx.navigation.ui.NavigationUI
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.app_bar))

        NavigationUI.setupActionBarWithNavController(
            this,
            Navigation.findNavController(findViewById(R.id.nav_host_fragment))
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
            }
        }

        return super.onOptionsItemSelected(item)
    }

    companion object {
        @JvmStatic
        val CREATE_DISCRETE_WALLPAPER = 1
    }
}
