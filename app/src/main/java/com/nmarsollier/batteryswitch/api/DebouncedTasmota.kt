package com.nmarsollier.batteryswitch.api

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.nmarsollier.batteryswitch.BatterySwitch
import com.nmarsollier.batteryswitch.MAX_BATTERY
import com.nmarsollier.batteryswitch.MIN_BATTERY
import com.nmarsollier.batteryswitch.tools.DebounceStrategy
import com.nmarsollier.batteryswitch.tools.appendToLogFile
import com.nmarsollier.batteryswitch.tools.debounce
import com.nmarsollier.batteryswitch.tools.detachedLaunch
import java.lang.ref.WeakReference

object DebouncedTasmota {
    private var context: WeakReference<Context> = WeakReference(null)
    private var level: Int = 0
    private var charging: Boolean = false

    val debouncedCall = debounce(strategy = DebounceStrategy.WAIT_RUN, interval = 1000) {
        appendToLogFile("refresh charger")
        when {
            charging && level >= MAX_BATTERY -> {
                detachedLaunch {
                    context.get()?.let {
                        appendToLogFile("updating charger to off")
                        TasmotaApi(it).switchOff()
                        BatterySwitch.updateAllWidgets(it)
                    }
                }
            }
            !charging && level <= MIN_BATTERY -> {
                detachedLaunch {
                    context.get()?.let {
                        appendToLogFile("updating charger to on")
                        TasmotaApi(it).switchOn()
                        BatterySwitch.updateAllWidgets(it)
                    }
                }
            }
        }
    }

    fun updateChargerStatus(
        context: Context,
        level: Int,
        charging: Boolean
    ) {
        this.context = WeakReference(context)
        this.level = level
        this.charging = charging
        debouncedCall()
    }
}