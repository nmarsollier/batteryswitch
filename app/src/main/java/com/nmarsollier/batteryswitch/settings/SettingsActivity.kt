package com.nmarsollier.batteryswitch.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.nmarsollier.batteryswitch.BatterySwitch
import com.nmarsollier.batteryswitch.api.TasmotaApi
import com.nmarsollier.batteryswitch.databinding.ActivitySettingsBinding
import com.nmarsollier.batteryswitch.tools.appendToLogFile
import com.nmarsollier.batteryswitch.tools.clearLogs
import com.nmarsollier.batteryswitch.tools.debounce
import com.nmarsollier.batteryswitch.tools.readLogs
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.logging.Logger

private val logger: Logger = Logger.getLogger("SettingsActivity: ")

class SettingsActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivitySettingsBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermissions()
        initializeViews()
        setContentView(binding.root)
    }

    override fun onResume() {
        super.onResume()
        refreshLogs()
    }

    private fun initializeViews() {
        val props = SettingsProperties(this)
        binding.serverName.setText(props.serverName)
        refreshLogs()

        binding.close.setOnClickListener {
            save(props)
            finish()
        }

        binding.tryOn.setOnClickListener {
            try {
                save(props)
                GlobalScope.launch {
                    appendToLogFile("manually turned on")
                    TasmotaApi(this@SettingsActivity).switchOn()
                    refreshLogs()
                    binding.logs.post {
                        BatterySwitch.updateAllWidgets(applicationContext)
                    }
                }
            } catch (e: Exception) {
                logger.severe(e.message)
                Toast.makeText(applicationContext, e.message, Toast.LENGTH_LONG).show()
            }
        }

        binding.tryOff.setOnClickListener {
            try {
                save(props)
                GlobalScope.launch {
                    appendToLogFile("manually turned off")
                    TasmotaApi(this@SettingsActivity).switchOff()
                    refreshLogs()
                    binding.logs.post {
                        BatterySwitch.updateAllWidgets(applicationContext)
                    }
                }
            } catch (e: Exception) {
                logger.severe(e.message)
                Toast.makeText(applicationContext, e.message, Toast.LENGTH_LONG).show()
            }
        }

        binding.clear.setOnClickListener {
            try {
                GlobalScope.launch {
                    clearLogs()
                    refreshLogs()
                }
            } catch (e: Exception) {
                logger.severe(e.message)
                Toast.makeText(applicationContext, e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun requestPermissions() {
        logger.info("requestPermissions")
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            logger.info("requesting permission")

            ActivityCompat.requestPermissions(
                this, arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ), 1001
            )
        }
    }

    private fun save(props: SettingsProperties) {
        props.serverName = binding.serverName.text.toString()
        props.save()
    }

    private val refreshLogs = debounce {
        binding.logs.post { binding.logs.text = readLogs() }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        try {
            refreshLogs()
        } catch (e: Exception) {
            logger.severe(e.message)
        }
    }
}