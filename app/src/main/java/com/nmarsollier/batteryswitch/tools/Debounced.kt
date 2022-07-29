package com.nmarsollier.batteryswitch.tools

import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import java.util.concurrent.atomic.AtomicBoolean

private const val DEFAULT_DEBOUNCE: Long = 500

/**
 * Debounce a function call.
 * This will call the function after or before an interval, calls inside the interval
 * window are ignored.
 *
 * Usage: Create field
 *
 * val debouncedFunction = debounce {
 *      //do something
 * }
 *
 * Call the method like normal function debouncedFunction();
 *
 * @param strategy defines if the method should execute and wait, or wait then execute
 */
fun debounce(
    strategy: DebounceStrategy = DebounceStrategy.WAIT_RUN,
    interval: Long = DEFAULT_DEBOUNCE,
    process: () -> Unit
): DebouncedCall {
    return DebouncedCall(strategy, interval, process)
}

/**
 * Debounce strategy :
 *
 * RUN_WAIT: Run first time called and ignore sequent calls in the time window
 * WAIT_RUN: Receives calls in the time window, and call the last
 * BOTH: Run at the first call, wait the window, and it it was called again the the
 *       time frame, then call the function again at the end.
 */
enum class DebounceStrategy {
    RUN_WAIT, WAIT_RUN, BOTH
}

/**
 * Not need to create and instance directly of this class, use the function.
 */
class DebouncedCall(
    private val strategy: DebounceStrategy = DebounceStrategy.WAIT_RUN,
    private val interval: Long = DEFAULT_DEBOUNCE,
    private val process: () -> Unit
) {
    private var debounceTimer: CountDownTimer? = null
    private var isExecuting = AtomicBoolean(false)
    private var calledWhileExecuting = false
    private var runAfterInit = false

    init {
        Handler(Looper.getMainLooper()).post {
            debounceTimer = object : CountDownTimer(interval, interval) {
                override fun onTick(millisUntilFinished: Long) {
                    System.out.println("internal")
                }

                override fun onFinish() {
                    isExecuting.set(false)

                    if (strategy == DebounceStrategy.WAIT_RUN) {
                        process()
                    }
                    if (strategy == DebounceStrategy.BOTH && calledWhileExecuting) {
                        process()
                    }
                }
            }
            if (runAfterInit) {
                runAfterInit = false
                invoke()
            }
        }
    }

    operator fun invoke() {
        val timer = debounceTimer ?: let {
            runAfterInit = true
            return
        }
        if (isExecuting.compareAndSet(false, true)) {
            calledWhileExecuting = false
            if (strategy == DebounceStrategy.RUN_WAIT || strategy == DebounceStrategy.BOTH) {
                process()
            }
            timer.start()
        } else {
            calledWhileExecuting = true
        }
    }
}