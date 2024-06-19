package com.github.brookiestein.timer

import android.content.Context
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.VibrationEffect
import android.os.VibratorManager
import android.widget.NumberPicker
import kotlin.concurrent.thread

/* Timer modifies NumberPicker's values, but not the view itself.
 * That's not possible because Android doesn't allow to modify the view in another thread.
 */
class Timer(
    private val context: Context,
    private val hoursPicker: NumberPicker,
    private val minutesPicker: NumberPicker,
    private val secondsPicker: NumberPicker
) : Runnable {
    private var running = false
    private var end = false
    private var hasBeenStarted = false
    private var hours = hoursPicker.value
    private var minutes = minutesPicker.value
    private var seconds = secondsPicker.value
    private val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
    private val mediaPlayer = MediaPlayer.create(context, uri)
    private var vibrating = false
    private val preferences = context.getSharedPreferences(
        context.getString(R.string.preferences),
        Context.MODE_PRIVATE
    )

    init {
        mediaPlayer.isLooping = true
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
    }

    fun stop() {
        end = true
        running = false
        hoursPicker.value = 0
        minutesPicker.value = 0
        secondsPicker.value = 0
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

    private fun startVibration() {
        val vibrator = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        val vibrationEffect = VibrationEffect.createOneShot(
            500, VibrationEffect.DEFAULT_AMPLITUDE
        )
        vibrator.defaultVibrator.vibrate(vibrationEffect)
    }

    fun stopVibration() {
        vibrating = false
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

                    if (preferences.getBoolean(context.getString(R.string.vibratePreference), false)) {
                        vibrating = true
                        thread {
                            while (vibrating) {
                                startVibration()
                                Thread.sleep(1000)
                            }
                        }
                    }
                }

                break
            }

            /* Last check because timer could be stopped while thread was sleeping */
            if (running) {
                hoursPicker.value = hours
                minutesPicker.value = minutes
                secondsPicker.value = seconds
            }
        }
    }
}