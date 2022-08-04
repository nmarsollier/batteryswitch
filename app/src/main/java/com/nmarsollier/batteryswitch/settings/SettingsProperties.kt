package com.nmarsollier.batteryswitch.settings

import android.content.Context
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger

private val logger: Logger = Logger.getLogger("SettingsProperties")

class SettingsProperties(context: Context) {
    private val fileName = "${context.filesDir}/settings.properties"

    var serverName: String = "192.168.0.228"

    init {
        try {
            val props = FileInputStream(fileName).use {
                Properties().apply { load(it) }
            }
            serverName = props.getProperty("serverName", serverName)
        } catch (err: Error) {
            logger.log(Level.SEVERE, " init ", err)
        } catch (e: Exception) {
            logger.log(Level.SEVERE, " init ", e)
        }
    }

    fun save() = try {
        val props = Properties().apply {
            setProperty("serverName", serverName)
        }

        FileOutputStream(fileName).use {
            props.store(it, "")
        }
    } catch (e: Exception) {
        logger.log(Level.SEVERE, " save ", e)
    }
}