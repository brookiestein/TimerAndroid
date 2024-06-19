package com.github.brookiestein.timer

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.widget.Toolbar
import androidx.appcompat.app.AppCompatActivity

class Settings : AppCompatActivity() {
    private lateinit var playSoundForEdit: EditText
    private var playSoundFor = 0
    private lateinit var preferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        preferences = getSharedPreferences(
            getString(R.string.preferences),
            Context.MODE_PRIVATE
        )

        val toolbar = findViewById<Toolbar>(R.id.settingsToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (preferences.getInt(getString(R.string.playSoundForPreference), -1) == -1) {
            playSoundFor = 5 /* 5 minutes by default */
            preferences.edit().putInt(getString(R.string.playSoundForPreference), playSoundFor).apply()
        } else {
            playSoundFor = preferences.getInt(getString(R.string.playSoundForPreference), -1)
        }

        val vibrateSwitch: androidx.appcompat.widget.SwitchCompat = findViewById(R.id.vibrateSwitch)
        playSoundForEdit = findViewById(R.id.playSoundForEdit)
        playSoundForEdit.setText(playSoundFor.toString())

        val playSound: androidx.appcompat.widget.SwitchCompat = findViewById(R.id.playSoundSwitch)
        playSound.isChecked = preferences.getBoolean(getString(R.string.playSoundPreference), false)

        playSound.setOnCheckedChangeListener { _, isChecked ->
            preferences.edit().putBoolean(getString(R.string.playSoundPreference), isChecked).apply()
            playSoundForEdit.isEnabled = playSound.isChecked
            vibrateSwitch.isEnabled = playSound.isChecked
        }

        vibrateSwitch.isEnabled = playSound.isChecked
        vibrateSwitch.isChecked = preferences.getBoolean(getString(R.string.vibratePreference), false)

        vibrateSwitch.setOnCheckedChangeListener { _, isChecked ->
            preferences.edit().putBoolean(getString(R.string.vibratePreference), isChecked).apply()
        }

        playSoundForEdit.isEnabled = playSound.isChecked

        playSoundForEdit.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                return@setOnFocusChangeListener
            }

            val number = playSoundForEdit.text.toString()
            if (number.isEmpty()) {
                return@setOnFocusChangeListener
            }

            if (number.toInt() != playSoundFor) {
                preferences.edit()
                    .putInt(getString(R.string.playSoundForPreference), number.toInt())
                    .apply()
            }
        }
    }

    override fun onDestroy() {
        val number = playSoundForEdit.text.toString()
        if (number.isNotEmpty()) {
            if (number.toInt() != playSoundFor) {
                preferences.edit()
                    .putInt(getString(R.string.playSoundForPreference), number.toInt())
                    .apply()
            }
        }

        super.onDestroy()
    }
}