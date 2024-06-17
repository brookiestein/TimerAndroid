package com.github.brookiestein.timer

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
import java.util.Calendar
import kotlin.concurrent.thread

class Run(
    private val hoursPicker: NumberPicker,
    private val minutesPicker: NumberPicker,
    private val secondsPicker: NumberPicker,
    private val setStatusText: (Boolean, Int, Int, Int) -> Unit
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
        setStatusText(false, hours, minutes, seconds)
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
        setStatusText(true, 0, 0, 0)
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

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val hoursPicker: NumberPicker = findViewById(R.id.hoursPicker)
        val minutesPicker: NumberPicker = findViewById(R.id.minutesPicker)
        val secondsPicker: NumberPicker = findViewById(R.id.secondsPicker)

        hoursPicker.minValue = 0
        hoursPicker.maxValue = 23
        hoursPicker.value = 0

        minutesPicker.minValue = 0
        minutesPicker.maxValue = 59
        minutesPicker.value = 0

        secondsPicker.minValue = 0
        secondsPicker.maxValue = 59
        secondsPicker.value = 0

        val setEnabled: (Boolean) -> Unit = { enabled ->
            hoursPicker.isEnabled = enabled
            minutesPicker.isEnabled = enabled
            secondsPicker.isEnabled = enabled
        }

        val statusText: TextView = findViewById(R.id.statusTextView)
        val setStatusText: (Boolean, Int, Int, Int) -> Unit = { clear, h, m, s ->
            var text: String

            if (clear) {
                text = ""
            } else {
                val calendar = Calendar.getInstance()
                var hour = h
                var minutes = m
                var seconds = s
                var indicator = calendar.get(Calendar.AM_PM)

                val calculateTime: () -> Unit = {
                    while (seconds >= 60) {
                        ++minutes
                        seconds -= 60
                    }

                    while (minutes >= 60) {
                        ++hour
                        --minutes
                    }

                    while (hour >= 12) {
                        hour -= 12
                        indicator = if (indicator == Calendar.AM) {
                            Calendar.PM
                        } else {
                            Calendar.AM
                        }
                    }
                }

                calculateTime()

                hour += calendar.get(Calendar.HOUR)
                minutes += calendar.get(Calendar.MINUTE)
                seconds += calendar.get(Calendar.SECOND)

                calculateTime()

                val amPM = if (indicator == Calendar.AM) {
                    "AM"
                } else {
                    "PM"
                }

                text = getString(R.string.message)
                text += String.format(" %d:%d:%d %s", hour, minutes, seconds, amPM)
            }

            statusText.text = text
        }

        val startButton: Button = findViewById(R.id.startButton)
        val pauseButton: Button = findViewById(R.id.pauseButton)

        var runner = Run(
            hoursPicker, minutesPicker, secondsPicker,
            setStatusText
        )

        var thread: Thread

        startButton.setOnClickListener {
            val hours = hoursPicker.value
            val minutes = minutesPicker.value
            val seconds = secondsPicker.value

            if (hours == 0 && minutes == 0 && seconds == 0) {
                Toast.makeText(
                    this,
                    getString(R.string.allZeroes),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (startButton.text == getString(R.string.stop)) {
                startButton.text = getString(R.string.start)
                runner.stop()
                return@setOnClickListener
            }

            /* Make new thread every time user starts new timer because
             * threads cannot be restarted.
             */
            runner = Run(
                hoursPicker, minutesPicker, secondsPicker,
                setStatusText
            )
            runner.start()
            thread = Thread(runner)
            thread.start()
            startButton.text = getString(R.string.stop)
            pauseButton.text = getString(R.string.pause)
            setEnabled(false)

            val checkForTimerFinished = thread(false) {
                while (!runner.atEnd()) {
                    Thread.sleep(1000)
                }

                runOnUiThread {
                    startButton.text = getString(R.string.start)
                    setEnabled(true)
                }
            }
            checkForTimerFinished.start()
        }

        pauseButton.setOnClickListener {
            val hours = hoursPicker.value
            val minutes = minutesPicker.value
            val seconds = secondsPicker.value

            if ((hours == 0 && minutes == 0 && seconds == 0)) {
                Toast
                    .makeText(
                        this,
                        getString(R.string.noNeedToPause),
                        Toast.LENGTH_SHORT
                    ).show()
                return@setOnClickListener
            }

            if (pauseButton.text == getString(R.string.pause)) {
                runner.pause()
                pauseButton.text = getString(R.string.resume)
                Toast
                    .makeText(
                        this,
                        getString(R.string.timerPaused),
                        Toast.LENGTH_SHORT
                    ).show()
            } else {
                runner = Run(
                    hoursPicker, minutesPicker, secondsPicker,
                    setStatusText
                )
                runner.start()
                thread = Thread(runner)
                thread.start()
                pauseButton.text = getString(R.string.pause)

                Toast
                    .makeText(
                        this,
                        getString(R.string.timerResumed),
                        Toast.LENGTH_SHORT
                    ).show()
            }
        }
    }
}