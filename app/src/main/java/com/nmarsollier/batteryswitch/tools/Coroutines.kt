package com.nmarsollier.batteryswitch.tools

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/**
 * This scope is used to execute a background process attached to a lifecycle.
 * If the lifecycle is destroyed, the process is cancelled, so the view never get updates.
 */
class DetachedCoroutineScope : CoroutineScope {
    private val job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job
}

/**
 * Function to launch a Job without scope
 */
fun detachedLaunch(
    block: suspend CoroutineScope.() -> Unit
): Job {
    val lifecycle = DetachedCoroutineScope()
    return lifecycle.launch(context = lifecycle.coroutineContext, block = block)
}
