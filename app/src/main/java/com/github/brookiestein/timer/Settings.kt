package com.github.brookiestein.timer

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.widget.Toolbar
import androidx.appcompat.app.AppCompatActivity

class Settings : AppCompatActivity() {
    private lateinit var preferences: SharedPreferences
    private lateinit var playSoundForEdit: EditText
    private lateinit var vibrateForEdit: EditText
    private var playSoundFor = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val toolbar = findViewById<Toolbar>(R.id.settingsToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        preferences = getSharedPreferences(
            getString(R.string.preferences),
            Context.MODE_PRIVATE
        )

        if (preferences.getInt(getString(R.string.playSoundForPreference), -1) == -1) {
            playSoundFor = 5 /* 5 minutes by default */
            preferences.edit().putInt(getString(R.string.playSoundForPreference), playSoundFor).apply()
        } else {
            playSoundFor = preferences.getInt(getString(R.string.playSoundForPreference), -1)
        }

        playSoundForEdit = findViewById(R.id.playSoundForEdit)
        playSoundForEdit.setText(playSoundFor.toString())

        val playSound: androidx.appcompat.widget.SwitchCompat = findViewById(R.id.playSoundSwitch)
        playSound.isChecked = preferences.getBoolean(getString(R.string.playSoundPreference), false)

        playSound.setOnCheckedChangeListener { _, isChecked ->
            preferences.edit().putBoolean(getString(R.string.playSoundPreference), isChecked).apply()
            playSoundForEdit.isEnabled = playSound.isChecked
        }

        val vibrateSwitch: androidx.appcompat.widget.SwitchCompat = findViewById(R.id.vibrateSwitch)
        vibrateSwitch.isChecked = preferences.getBoolean(getString(R.string.vibratePreference), false)

        /* Same as playSound by default */
        val vibrateFor = preferences.getInt(getString(R.string.vibrateTimePreference), playSoundFor)
        vibrateForEdit = findViewById(R.id.vibrateForEdit)
        vibrateForEdit.setText(vibrateFor.toString())
        vibrateForEdit.isEnabled = vibrateSwitch.isChecked

        vibrateSwitch.setOnCheckedChangeListener { _, isChecked ->
            preferences.edit().putBoolean(getString(R.string.vibratePreference), isChecked).apply()
            vibrateForEdit.isEnabled = vibrateSwitch.isChecked
        }

        playSoundForEdit.isEnabled = playSound.isChecked
    }

    override fun onDestroy() {
        var number = playSoundForEdit.text.toString()
        if (number.isNotEmpty()) {
            if (number.toInt() != playSoundFor) {
                preferences.edit()
                    .putInt(getString(R.string.playSoundForPreference), number.toInt())
                    .apply()
            }
        }

        number = vibrateForEdit.text.toString()
        if (number.isNotEmpty()) {
            preferences.edit()
                .putInt(getString(R.string.vibrateTimePreference), number.toInt())
                .apply()
        }

        super.onDestroy()
    }
}