package com.github.brookiestein.timer

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var runningForTextView: TextView
    private lateinit var statusText: TextView
    private lateinit var hoursPicker: NumberPicker
    private lateinit var minutesPicker: NumberPicker
    private lateinit var secondsPicker: NumberPicker
    private lateinit var startButton: Button
    private lateinit var pauseButton: Button
    private lateinit var stopRingtoneButton: Button
    private lateinit var timer: Timer
    private lateinit var started: Date
    private lateinit var checkForTimerFinished: CheckForTimerFinished
    private val notificationChannelID = 0
    private var endAtText = ""
    private var totalDuration: Long = 0

    private inner class CheckForTimerFinished(
        private val setEnabled: (Boolean) -> Unit,
        private var stoppedByButton: Boolean,
        private var firstTimeRunningTimer: Boolean) : Runnable
    {
        private val workManager = WorkManager.getInstance(this@MainActivity)
        private var workRequest: OneTimeWorkRequest? = null
        private val preferences = getSharedPreferences(getString(R.string.preferences), Context.MODE_PRIVATE)
        private val playSoundPreference = preferences.getBoolean(getString(R.string.playSoundPreference), false)
        private val vibratePreference = preferences.getBoolean(getString(R.string.vibratePreference), false)

        fun firstTimeRunningTimer() = firstTimeRunningTimer
        fun workRequest() = workRequest
        fun workManager() = workManager
        fun stoppedByButton(newVal: Boolean) {
            stoppedByButton = newVal
        }

        private fun sendStopNotification() {
            val stopSoundIntent = Intent(getString(R.string.stopSoundAction))
            val stopSoundPendingIntent = PendingIntent.getBroadcast(
                this@MainActivity,
                2,
                stopSoundIntent,
                PendingIntent.FLAG_IMMUTABLE
            )

            val stopVibrationIntent = Intent(getString(R.string.stopVibrationAction))
            val stopVibrationPendingIntent = PendingIntent.getBroadcast(
                this@MainActivity,
                3,
                stopVibrationIntent,
                PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(
                this@MainActivity,
                getString(R.string.app_name)
            )
                .setSmallIcon(R.drawable.logo)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.timerFinished))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setAutoCancel(true)

            if (playSoundPreference) {
                builder.addAction(
                    R.drawable.logo,
                    getString(R.string.stopRingtone),
                    stopSoundPendingIntent
                )
            } else if (vibratePreference) {
                builder.addAction(
                    R.drawable.logo,
                    getString(R.string.stopVibration),
                    stopVibrationPendingIntent
                )
            }

            val mainActivityIntent = Intent(this@MainActivity, MainActivity::class.java)
            val mainActivityPendingIntent = PendingIntent.getActivity(
                this@MainActivity, 0,
                mainActivityIntent,
                PendingIntent.FLAG_IMMUTABLE
            )
            builder.setContentIntent(mainActivityPendingIntent)

            with(NotificationManagerCompat.from(this@MainActivity)) {
                val allowed = ActivityCompat.checkSelfPermission(this@MainActivity,
                    Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                if (!allowed) {
                    return@with
                }

                notify(notificationChannelID, builder.build())
            }
        }

        override fun run() {
            var firstTime = true
            var sentLastNotification = false
            setStatusText(
                false,
                hoursPicker.value,
                minutesPicker.value,
                secondsPicker.value,
                statusText
            )

            while (!timer.atEnd()) {
                if (timer.isRunning() || !sentLastNotification) {
                    sentLastNotification = !timer.isRunning()

                    sendNotification(
                        hoursPicker,
                        minutesPicker,
                        secondsPicker,
                        firstTime,
                        sentLastNotification
                    )
                }

                firstTime = false

                Thread.sleep(1000)
            }

            runOnUiThread {
                runningForTextView.text = getString(R.string.empty)
                startButton.text = getString(R.string.start)
                setEnabled(true)
                setStatusText(true, 0, 0, 0, statusText)

                val text = if (stoppedByButton) {
                    getString(R.string.timerStopped)
                } else {
                    getString(R.string.timerFinished)
                }

                Toast
                    .makeText(
                        this@MainActivity,
                        text,
                        Toast.LENGTH_LONG
                    )
                    .show()

                if (!stoppedByButton) {
                    val vibrateFor = preferences.getInt(getString(R.string.vibrateTimePreference), 0)
                    if (vibratePreference) {
                        workRequest = OneTimeWorkRequestBuilder<VibratorWorker>().build()
                        workManager.enqueue(workRequest!!)
                    }

                    if (playSoundPreference) {
                        stopRingtoneButton.isVisible = true
                        stopRingtoneButton.text = getString(R.string.stopRingtone)
                        thread {
                            var i = 0
                            val stopAfter = preferences.getInt(
                                getString(R.string.playSoundForPreference), 5) * 60
                            while (timer.isPlaying()) {
                                Thread.sleep(1000)
                                if (++i > stopAfter) {
                                    runOnUiThread {
                                        stopRingtoneButton.callOnClick()
                                    }
                                    break
                                }
                            }
                        }
                    } else if (vibratePreference && vibrateFor != 0) {
                        stopRingtoneButton.text = getString(R.string.stopVibration)
                        stopRingtoneButton.isVisible = true

                        thread {
                            while (true) {
                                if (workManager.getWorkInfoById(workRequest!!.id).get().state == WorkInfo.State.SUCCEEDED) {
                                    break
                                }

                                Thread.sleep(1000)
                            }

                            runOnUiThread {
                                stopRingtoneButton.isVisible = false
                            }
                        }
                    }
                }
            }

            firstTimeRunningTimer = true
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(notificationChannelID)
            sendStopNotification()
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action

            when (action) {
                getString(R.string.stopTimerAction) -> {
                    startButton.callOnClick()
                }
                getString(R.string.pauseTimerAction) -> {
                    pauseButton.callOnClick()
                }
                getString(R.string.resumeTimerAction) -> {
                    pauseButton.callOnClick()
                }
                getString(R.string.stopSoundAction) -> {
                    stopRingtoneButton.callOnClick()
                    val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    nm.cancel(notificationChannelID)
                }
                getString(R.string.stopVibrationAction) -> {
                    stopRingtoneButton.callOnClick()
                    val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    nm.cancel(notificationChannelID)
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                val intent = Intent(this, Settings::class.java)
                startActivity(intent)
                true
            } else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val intentFilter = IntentFilter().apply {
            addAction(getString(R.string.stopTimerAction))
            addAction(getString(R.string.pauseTimerAction))
            addAction(getString(R.string.resumeTimerAction))
            addAction(getString(R.string.stopSoundAction))
            addAction(getString(R.string.stopVibrationAction))
        }
        ContextCompat.registerReceiver(
            this,
            receiver,
            intentFilter,
            ContextCompat.RECEIVER_EXPORTED
        )

        val preferences = getSharedPreferences(getString(R.string.preferences), Context.MODE_PRIVATE)
        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) {
                Toast.makeText(
                    this,
                    getString(R.string.permissionDenied),
                    Toast.LENGTH_LONG
                ).show()
            }

            with (preferences.edit()) {
                putInt(getString(R.string.permissionAsked), 1)
                apply()
            }
        }

        createNotificationChannel()

        hoursPicker = findViewById(R.id.hoursPicker)
        minutesPicker = findViewById(R.id.minutesPicker)
        secondsPicker = findViewById(R.id.secondsPicker)
        runningForTextView = findViewById(R.id.runningForTextView)
        statusText = findViewById(R.id.statusTextView)

        val setEnabled: (Boolean) -> Unit = {
            hoursPicker.isEnabled = it
            minutesPicker.isEnabled = it
            secondsPicker.isEnabled = it
        }

        /* This property controls whether or not running for text view gets updated. */
        var firstTimeRunningTimer = true
        /* Timer can be stopped by itself or by user.
         * This property controls whether or not started playing sound or vibrating.
         */
        var stoppedByButton: Boolean
        var threadCheckForTimerFinished: Thread
        /* This initialization is for firstTimeRunningTimer() to work properly actually the first time.
         * See conditional on startButton.setOnClickListener.
         * This state is overwritten afterwards.
         */
        checkForTimerFinished = CheckForTimerFinished(
            setEnabled,
            stoppedByButton = false,
            firstTimeRunningTimer = true
        )

        hoursPicker.minValue = 0
        hoursPicker.maxValue = 23
        hoursPicker.value = 0

        minutesPicker.minValue = 0
        minutesPicker.maxValue = 59
        minutesPicker.value = 0

        secondsPicker.minValue = 0
        secondsPicker.maxValue = 59
        secondsPicker.value = 0

        startButton = findViewById(R.id.startButton)
        pauseButton = findViewById(R.id.pauseButton)
        stopRingtoneButton = findViewById(R.id.stopRingtoneButton)
        stopRingtoneButton.isVisible = false

        timer = Timer(this, hoursPicker, minutesPicker, secondsPicker)
        var thread: Thread

        startButton.setOnClickListener {
            if (hoursPicker.value == 0 && minutesPicker.value == 0 && secondsPicker.value == 0) {
                Toast.makeText(
                    this,
                    getString(R.string.allZeroes),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (startButton.text == getString(R.string.stop)) {
                startButton.text = getString(R.string.start)
                timer.stop()
                stoppedByButton = true
                checkForTimerFinished.stoppedByButton(true)
                return@setOnClickListener
            }

            stoppedByButton = false
            started = Date()

            if (checkForTimerFinished.firstTimeRunningTimer()) {
                setRunningForText()
                firstTimeRunningTimer = false
            }

            /* Make new thread every time user starts new timer because
             * threads cannot be restarted.
             */
            timer = Timer(this, hoursPicker, minutesPicker, secondsPicker)
            timer.start()
            thread = Thread(timer)
            thread.start()

            startButton.text = getString(R.string.stop)
            pauseButton.text = getString(R.string.pause)
            setEnabled(false)

            checkForTimerFinished = CheckForTimerFinished(
                setEnabled,
                stoppedByButton,
                firstTimeRunningTimer
            )

            threadCheckForTimerFinished = Thread(checkForTimerFinished)
            threadCheckForTimerFinished.start()
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
                timer.pause()
                pauseButton.text = getString(R.string.resume)
                Toast
                    .makeText(
                        this,
                        getString(R.string.timerPaused),
                        Toast.LENGTH_SHORT
                    ).show()
            } else {
                setStatusText(
                    false,
                    hoursPicker.value,
                    minutesPicker.value,
                    secondsPicker.value,
                    statusText
                )
                timer = Timer(this, hoursPicker, minutesPicker, secondsPicker)
                timer.start()
                thread = Thread(timer)
                thread.start()
                pauseButton.text = getString(R.string.pause)
                /* For progress bar to work as expected. */
                started = Date()
                totalDuration = (hours * 3600000 + minutes * 60000 + seconds * 1000).toLong()

                Toast
                    .makeText(
                        this,
                        getString(R.string.timerResumed),
                        Toast.LENGTH_SHORT
                    )
                    .show()
            }
        }

        stopRingtoneButton.setOnClickListener {
            if (stopRingtoneButton.text == getString(R.string.stopRingtone)) {
                timer.stopRingtone()
            }

            stopRingtoneButton.isVisible = false
            checkForTimerFinished.workRequest()?.let { it1 ->
                checkForTimerFinished.workManager().cancelWorkById(it1.id)
            }
        }
    }

    override fun onDestroy() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancelAll()
        unregisterReceiver(receiver)
        super.onDestroy()
    }

    private fun setRunningForText() {
        val hours = hoursPicker.value
        val minutes = minutesPicker.value
        val seconds = secondsPicker.value

        val hoursText = if (hours == 1) {
            getString(R.string.hour).lowercase()
        } else {
            getString(R.string.hours).lowercase()
        }

        val minutesText = if (minutes == 1) {
            getString(R.string.minute).lowercase()
        } else {
            getString(R.string.minutes).lowercase()
        }

        val secondsText = if (seconds == 1) {
            getString(R.string.second).lowercase()
        } else {
            getString(R.string.seconds).lowercase()
        }

        runningForTextView.text = String.format(
            getString(R.string.runningForText),
            hours,
            hoursText,
            minutes,
            minutesText,
            seconds,
            secondsText
        )

        totalDuration = (hoursPicker.value * 3600000
                + minutesPicker.value * 60000
                + secondsPicker.value * 1000).toLong()
    }

    private fun setStatusText(
        clear: Boolean,
        h: Int,
        m: Int, s
        : Int,
        statusText: TextView
    ) {
        if (clear) {
            statusText.text = getString(R.string.empty)
            return
        }

        val calendar = Calendar.getInstance()
        var hour = h + calendar.get(Calendar.HOUR)
        var minutes = m + calendar.get(Calendar.MINUTE)
        var seconds = s + calendar.get(Calendar.SECOND)
        var indicator = calendar.get(Calendar.AM_PM)
        val getIndicator: () -> String = {
            if (indicator == Calendar.AM) {
                indicator = Calendar.PM
                "AM"
            } else {
                indicator = Calendar.AM
                "PM"
            }
        }
        var amPM = getIndicator()

        while (minutes >= 60) {
            minutes -= 60
            ++hour
        }

        while (seconds >= 60) {
            seconds -= 60
            ++minutes
        }

        while (hour >= 12) {
            hour -= 12
            indicator = if (indicator == Calendar.AM) {
                Calendar.PM
            } else {
                Calendar.AM
            }

            amPM = getIndicator()
        }

        endAtText = getString(R.string.message)
        endAtText += String.format(
            Locale.getDefault(),
            " %d:%d:%d %s",
            hour,
            minutes,
            seconds,
            amPM
        )

        statusText.text = endAtText
    }

    private fun createNotificationChannel() {
        val name = getString(R.string.app_name)
        val descriptionText = getString(R.string.app_name)
        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel(name, name, importance).apply {
            description = descriptionText
        }

        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    private fun elapsedTimeInPercentage(): Long {
        val currentTime = Date()
        val elapsedMillis = currentTime.time - started.time
        val percentage = elapsedMillis * 100 / totalDuration
        return percentage
    }

    private fun sendNotification(hoursPicker: NumberPicker,
                                minutesPicker: NumberPicker,
                                secondsPicker: NumberPicker,
                                 firstTime: Boolean,
                                 lastNotification: Boolean)
    {
        val h = hoursPicker.value
        val m = minutesPicker.value
        val s = secondsPicker.value
        val content = String.format(getString(R.string.format), h, m, s)
        val preferences = getSharedPreferences(getString(R.string.preferences), Context.MODE_PRIVATE)

        val stopIntent = Intent(getString(R.string.stopTimerAction))
        val pauseOrResumeIntent = if (lastNotification) {
            Intent(getString(R.string.resumeTimerAction))
        } else {
            Intent(getString(R.string.pauseTimerAction))
        }

        val stopPendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val pausePendingIntent = PendingIntent.getBroadcast(
            this,
            1,
            pauseOrResumeIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val pauseOrResumeActionText = if (lastNotification) {
            getString(R.string.resume)
        } else {
            getString(R.string.pause)
        }
        val builder = NotificationCompat.Builder(
            this, getString(R.string.app_name)
        )
            .setSmallIcon(R.drawable.logo)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(content)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(String.format("%s\n%s", content, endAtText))
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setProgress(
                100,
                elapsedTimeInPercentage().toInt(),
                false
            )
            .addAction(
                R.drawable.logo,
                getString(R.string.stop),
                stopPendingIntent
            )
            .addAction(
                R.drawable.logo,
                pauseOrResumeActionText,
                pausePendingIntent
            )

        val mainActivityIntent = Intent(this, MainActivity::class.java)
        val mainActivityPendingIntent = PendingIntent.getActivity(
            this, 0,
            mainActivityIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        builder.setContentIntent(mainActivityPendingIntent)

        with(NotificationManagerCompat.from(this@MainActivity)) {
            val allowed = ActivityCompat.checkSelfPermission(this@MainActivity,
                Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            if (!allowed
                && preferences.getInt(getString(R.string.permissionAsked), 0) != 1
                && firstTime) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return@with
            }

            notify(notificationChannelID, builder.build())
        }
    }
}