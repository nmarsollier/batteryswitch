package com.nmarsollier.batteryswitch

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Context.BATTERY_SERVICE
import android.content.Intent
import android.os.BatteryManager
import android.view.View
import android.widget.RemoteViews
import com.nmarsollier.batteryswitch.api.TasmotaApi
import com.nmarsollier.batteryswitch.settings.SettingsActivity
import com.nmarsollier.batteryswitch.tools.detachedLaunch
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger

private const val OPEN_SETTINGS = "OPEN_SETTINGS"
const val MAX_BATTERY = 90
const val MIN_BATTERY = 20

class BatterySwitch : AppWidgetProvider() {
    override fun onEnabled(context: Context?) {
        super.onEnabled(context)

        context ?: return
        startScheduler(context)
    }

    override fun onDisabled(context: Context?) {
        super.onDisabled(context)

        context ?: return
        stopScheduler(context)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        logger.info("onUpdate called")
        updateAllWidgets(context)
    }

    override fun onReceive(context: Context?, intent: Intent) {
        super.onReceive(context, intent)
        context ?: return

        logger.info("onReceive called ${Date()}")
        updateAllWidgets(context)

        if (intent.action == OPEN_SETTINGS) {
            logger.info("onReceive open settings")
            openSettingsScreen(context)
        }

        logger.info("onReceive refresh")
        updateAllWidgets(context)
    }

    private fun openSettingsScreen(context: Context) {
        try {
            logger.info("openSettings called")
            val intent = Intent(context, SettingsActivity::class.java).also {
                it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "openSettings error", e)
        }
    }

    companion object {
        private val logger: Logger = Logger.getLogger("BatterySwitch")

        /**
         * Update all widgets on screen
         */
        fun updateAllWidgets(
            context: Context?
        ) {
            logger.info("companion refresh called")

            context ?: return

            try {
                val bm = context.getSystemService(BATTERY_SERVICE) as BatteryManager
                val percent = bm.batteryPercent
                val charging = bm.charging
                updateChargerStatus(context, percent, charging)

                logger.info("companion refreshing ${percent}% Charging: $charging")

                val widgetManager = AppWidgetManager.getInstance(context)
                val widgetComponent = ComponentName(context, BatterySwitch::class.java)
                val widgetIds = widgetManager.getAppWidgetIds(widgetComponent)

                logger.info(
                    "companion refreshing widgets ${widgetIds.joinToString(", ")}"
                )

                widgetIds.forEach { appWidgetId ->
                    updateWidget(context, widgetManager, appWidgetId, percent, charging)
                }
            } catch (e: Exception) {
                logger.log(Level.SEVERE, "companion refreshing error", e)
            }
        }

        private fun updateChargerStatus(
            context: Context,
            level: Int,
            charging: Boolean
        ) {
            logger.info("updating charger")

            detachedLaunch {
                when {
                    charging && level >= MAX_BATTERY -> {
                        TasmotaApi(context).switchOff()
                        updateAllWidgets(context)
                    }
                    !charging && level <= MIN_BATTERY -> {
                        TasmotaApi(context).switchOn()
                        updateAllWidgets(context)
                    }
                }
            }
        }

        /**
         * Update one widget
         */
        private fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            percent: Int,
            charging: Boolean
        ) {
            val views = RemoteViews(context.packageName, R.layout.battery_switch)
            views.setTextViewText(R.id.percent, "${percent}%")

            when {
                charging && percent > MAX_BATTERY -> {
                    views.setViewVisibility(R.id.battery_charging, View.GONE)
                    views.setViewVisibility(R.id.battery_using, View.GONE)
                    views.setViewVisibility(R.id.battery_error, View.VISIBLE)
                }
                !charging && percent < MIN_BATTERY -> {
                    views.setViewVisibility(R.id.battery_charging, View.GONE)
                    views.setViewVisibility(R.id.battery_using, View.GONE)
                    views.setViewVisibility(R.id.battery_error, View.VISIBLE)
                }
                charging -> {
                    views.setViewVisibility(R.id.battery_charging, View.VISIBLE)
                    views.setViewVisibility(R.id.battery_using, View.GONE)
                    views.setViewVisibility(R.id.battery_error, View.GONE)
                }
                else -> {
                    views.setViewVisibility(R.id.battery_charging, View.GONE)
                    views.setViewVisibility(R.id.battery_using, View.VISIBLE)
                    views.setViewVisibility(R.id.battery_error, View.GONE)
                }
            }

            views.setOnClickPendingIntent(R.id.widget_layout, getSettingsIntent(context))

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        /**
         * Sends a broadcast to this widget to open settings screen.
         * Called from service where no ui context can be accessed.
         */
        private fun getSettingsIntent(context: Context?): PendingIntent? {
            val intent = Intent(context, BatterySwitch::class.java)
            intent.action = OPEN_SETTINGS
            return PendingIntent.getBroadcast(
                context, 0, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        // ALARM SERVICE
        private const val INTERVAL = 5 * 60 * 1000L

        @SuppressLint("UnspecifiedImmutableFlag")
        private fun startScheduler(context: Context) {
            logger.info("start alarm service called ${Date()}")

            if (isAlarmSet(context)) return

            val pendingIntent = PendingIntent.getBroadcast(
                context, 1001, context.widgetIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )

            val alarmManager =
                (context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager) ?: return
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis(),
                INTERVAL,
                pendingIntent
            )

            logger.info("start alarm service executed ${Date()}")
        }

        @SuppressLint("UnspecifiedImmutableFlag")
        private fun stopScheduler(context: Context) {
            logger.info("stop alarm service called ${Date()}")

            if (!isAlarmSet(context)) return

            val pendingIntent = PendingIntent.getBroadcast(
                context, 1001, context.widgetIntent,
                PendingIntent.FLAG_CANCEL_CURRENT
            )

            (context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager)?.cancel(pendingIntent)
            pendingIntent.cancel()
            logger.info("stop alarm service executed ${Date()}")
        }

        @SuppressLint("UnspecifiedImmutableFlag")
        private fun isAlarmSet(context: Context): Boolean {
            val alarmUp = PendingIntent.getBroadcast(
                context, 1001,
                context.widgetIntent,
                PendingIntent.FLAG_NO_CREATE
            ) != null

            logger.info("alarm service up $alarmUp")
            return alarmUp
        }
    }
}

private val BatteryManager.batteryPercent
    get() = getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

private val BatteryManager.charging
    get() = getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS) == BatteryManager.BATTERY_STATUS_CHARGING

private val Context.widgetIntent
    get() = Intent(this, BatterySwitch::class.java).also {
        it.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
    }
