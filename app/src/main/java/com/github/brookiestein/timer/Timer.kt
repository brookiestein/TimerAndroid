package com.github.brookiestein.timer

import android.content.Context
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.widget.ProgressBar
import android.widget.TextView

class Timer(
    private val context: Context,
    private val activity: MainActivity,
    private val remainingTimeTextView: TextView,
    private val progressBar: ProgressBar,
    private var hours: Int,
    private var minutes: Int,
    private var seconds: Int
) : Runnable {
    private var running = false
    private var end = false
    private var hasBeenStarted = false
    private val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
    private val mediaPlayer = MediaPlayer.create(context, uri)
    private val preferences = context.getSharedPreferences(
        context.getString(R.string.preferences),
        Context.MODE_PRIVATE
    )
    private var totalDuration: Int

    init {
        mediaPlayer.isLooping = true
        totalDuration = hours * 3600000 + minutes * 60000 + seconds * 1000
        progressBar.max = totalDuration
    }

    fun setNewHours(newHours: Int) {
        hours = newHours
    }

    fun setNewMinutes(newMinutes: Int) {
        minutes = newMinutes
    }

    fun setNewSeconds(newSeconds: Int) {
        seconds = newSeconds
    }

    fun start() {
        if (hasBeenStarted) {
            return
        }
        running = true
        end = false
        hasBeenStarted = true
    }

    fun pause() {
        running = false
        hasBeenStarted = false
    }

    fun stop() {
        end = true
        running = false
        activity.runOnUiThread {
            remainingTimeTextView.text = context.getString(R.string.empty)
        }
    }

    fun isRunning() = running
    fun atEnd() = end

    private fun startRingtone() {
        mediaPlayer.start()
    }

    fun stopRingtone() {
        mediaPlayer.stop()
        mediaPlayer.release()
    }

    fun isPlaying() : Boolean {
        val result = try {
            mediaPlayer.isPlaying
        } catch (e: IllegalStateException) {
            false
        }
        return result
    }

    override fun run() {
        while (running) {
            Thread.sleep(1000)

            if (seconds == 0) {
                if (minutes > 0) {
                    --minutes
                    seconds = 60
                } else {
                    if (hours > 0) {
                        --hours
                        minutes = 59
                        seconds = 60
                    }
                }
            }

            seconds -= 1

            if (hours == 0 && minutes == 0 && seconds == 0) {
                stop()
                if (preferences.getBoolean(context.getString(R.string.playSoundPreference), false)) {
                    startRingtone()
                }

                break
            }

            /* Last check because timer could be stopped while thread was sleeping */
            if (running) {
                activity.runOnUiThread {
                    remainingTimeTextView.text = String.format(
                        context.getString(R.string.format),
                        hours,
                        minutes,
                        seconds
                    )

                    progressBar.progress = totalDuration - (hours * 3600000 + minutes * 60000 + seconds * 1000)
                }
            }
        }
    }
}