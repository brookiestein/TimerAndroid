package com.github.brookiestein.timer

import android.widget.NumberPicker

/* Timer modifies NumberPicker's values, but not the view itself.
 * That's not possible because Android doesn't allow to modify the view in another thread.
 */
class Timer(
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

    override fun run() {
        while (running) {
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
            hoursPicker.value = hours
            minutesPicker.value = minutes
            secondsPicker.value = seconds

            if (hours == 0 && minutes == 0 && seconds == 0) {
                stop()
                break
            }

            Thread.sleep(1000)
        }
    }
}