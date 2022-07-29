package com.nmarsollier.batteryswitch.tools

import android.os.Environment
import java.io.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.logging.Logger

private val logger: Logger = Logger.getLogger("FileLog")
private val logFile = File("${Environment.getExternalStorageDirectory()}/log.txt")

fun appendToLogFile(text: String) {
    logger.info(text)
    try {
        if (!logFile.exists()) {
            try {
                logFile.createNewFile()
            } catch (e: IOException) {
                logger.info(logFile.absolutePath)
                logger.severe(e.message)
            }
        }
        val buf = BufferedWriter(FileWriter(logFile, true))
        buf.append("${currentTime} : " + text)
        buf.newLine()
        buf.close()
    } catch (e: IOException) {
        logger.severe(e.message)
    }
}

private val currentTime
    get() = DateTimeFormatter.ofPattern("MMM-dd HH:mm:ss").format(LocalDateTime.now())

fun clearLogs() {
    try {
        if (logFile.exists()) {
            logFile.delete()
        }
    } catch (e: IOException) {
        logger.severe(e.message)
    }
}

fun readLogs(): String {
    var result = ""
    try {
        if (!logFile.exists()) {
            return "No Logs!"
        }

        val buf = BufferedReader(FileReader(logFile))

        do {
            val txt = buf.readLine().also {
                result += it ?: ""
                result += "\n"
            }
        } while (txt != null)
        buf.close()
    } catch (e: IOException) {
        logger.severe(e.message)
    }
    return result
}
