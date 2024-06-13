package com.github.brookiestein.timer

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
import java.util.Calendar

class Run(
    private var context: Context? = null,
    private var hoursPicker: NumberPicker? = null,
    private var minutesPicker: NumberPicker? = null,
    private var secondsPicker: NumberPicker? = null,
    private var startButton: Button? = null,
    private var setEnabled: (Boolean) -> Unit
) : Runnable {
    private var running = false

    fun start() {
        running = true
    }

    fun stop() {
        running = false
    }

    fun setContext(ctx: Context) {
        context = ctx
    }

    fun setHoursPicker(hp: NumberPicker) {
        hoursPicker = hp
    }

    fun setMinutesPicker(mp: NumberPicker) {
        minutesPicker = mp
    }

    fun setSecondsPicker(sp: NumberPicker) {
        secondsPicker = sp
    }

    fun setStartButton(sb: Button) {
        startButton = sb
    }

    fun setCallback(callback: (Boolean) -> Unit) {
        setEnabled = callback
    }

    override fun run() {
        var hours = hoursPicker?.value
        var minutes = minutesPicker?.value
        var seconds = secondsPicker?.value

        while (running) {
            if (seconds == 0) {
                if (minutes != null) {
                    if (minutes > 0) {
                        --minutes
                        minutesPicker?.value = minutes
                        seconds = 60
                    } else {
                        if (hours != null) {
                            if (hours > 0) {
                                --hours
                                hoursPicker?.value = hours
                                minutes = 59
                                minutesPicker?.value = minutes
                                seconds = 60
                            }
                        }
                    }
                }
            }

            seconds = seconds!! - 1
            secondsPicker?.value = seconds

            if (hours == 0 && minutes == 0 && seconds == 0) {
                break
            }

            Thread.sleep(1000)
        }

        /* FIXME: This makes the app to crash */
        startButton?.text = context?.getString(R.string.start)
        setEnabled(true)
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

                    if (hour >= 12) {
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

        val setEnabled: (Boolean) -> Unit = { enabled ->
            hoursPicker.isEnabled = enabled
            minutesPicker.isEnabled = enabled
            secondsPicker.isEnabled = enabled
        }

        val startButton: Button = findViewById(R.id.startButton)
        val pauseButton: Button = findViewById(R.id.pauseButton)

        val runner = Run(setEnabled = setEnabled)
        var thread: Thread

        startButton.setOnClickListener {
            val hours = hoursPicker.value
            val minutes = minutesPicker.value
            val seconds = secondsPicker.value

            if (hours == 0 && minutes == 0 && seconds == 0) {
                Toast.makeText(
                    this,
                    getString(R.string.allZeroes),
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            if (startButton.text == getString(R.string.stop)) {
                setEnabled(true)
                startButton.text = getString(R.string.start)
                runner.stop()
                hoursPicker.value = 0
                minutesPicker.value = 0
                secondsPicker.value = 0
                setStatusText(true, 0, 0, 0)
                return@setOnClickListener
            }

            runner.setContext(this)
            runner.setHoursPicker(hoursPicker)
            runner.setMinutesPicker(minutesPicker)
            runner.setSecondsPicker(secondsPicker)
            runner.setStartButton(startButton)
            runner.setCallback(setEnabled)
            runner.start()
            thread = Thread(runner)
            thread.start()
            startButton.text = getString(R.string.stop)
            setEnabled(false)
            setStatusText(false, hours, minutes, seconds)
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
                        Toast.LENGTH_LONG
                    ).show()
                return@setOnClickListener
            }

            if (pauseButton.text == getString(R.string.pause)) {
                runner.stop()
                pauseButton.text = getString(R.string.resume)
                Toast
                    .makeText(
                        this,
                        getString(R.string.timerPaused),
                        Toast.LENGTH_LONG
                    ).show()
            } else {
                runner.start()
                thread = Thread(runner)
                thread.start()
                pauseButton.text = getString(R.string.pause)
                Toast
                    .makeText(
                        this,
                        getString(R.string.timerResumed),
                        Toast.LENGTH_LONG
                    ).show()
            }
        }
    }
}