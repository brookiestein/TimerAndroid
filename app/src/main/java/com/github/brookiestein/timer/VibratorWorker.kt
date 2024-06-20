package com.github.brookiestein.timer

import android.content.Context
import android.content.SharedPreferences
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.VibratorManager
import androidx.work.Worker
import androidx.work.WorkerParameters

class VibratorWorker(private val context: Context, workerParams: WorkerParameters)
    : Worker(context, workerParams)
{
    private val vm = applicationContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager

    private val preferences: SharedPreferences = context.getSharedPreferences(
        context.getString(R.string.preferences),
        Context.MODE_PRIVATE
    )

    private var vibrate = preferences.getBoolean(
        context.getString(R.string.vibratePreference),
        false
    )

    override fun doWork(): Result {
        val end = preferences.getInt(context.getString(R.string.vibrateTimePreference), 0) * 60
        if (!vibrate || end == 0) {
            return Result.success()
        }

        var i = 0
        while (i < end) {
            val vibrationEffect = VibrationEffect.createOneShot(
                500, VibrationEffect.DEFAULT_AMPLITUDE
            )

            val a = VibrationAttributes.Builder().setUsage(VibrationAttributes.USAGE_ALARM).build()
            vm.defaultVibrator.vibrate(vibrationEffect, a)

            Thread.sleep(1000)
            ++i

            if (!vibrate) {
                break
            }
        }

        return Result.success()
    }

    override fun onStopped() {
        vibrate = false
    }
}