package com.nmarsollier.batteryswitch.settings

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.nmarsollier.batteryswitch.BatterySwitch
import com.nmarsollier.batteryswitch.databinding.ActivitySettingsBinding
import com.nmarsollier.batteryswitch.api.TasmotaApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivitySettingsBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeViews()
        setContentView(binding.root)
    }

    private fun initializeViews() {
        val props = SettingsProperties(this)
        binding.serverName.setText(props.serverName)

        binding.close.setOnClickListener {
            save(props)
            finish()
        }

        binding.tryOn.setOnClickListener {
            GlobalScope.launch {
                try {
                    save(props)
                    TasmotaApi(this@SettingsActivity).switchOn()
                    BatterySwitch.updateAllWidgets(applicationContext)
                } catch (e: Exception) {
                    Toast.makeText(applicationContext, e.message, Toast.LENGTH_LONG).show()
                }
            }
        }

        binding.tryOff.setOnClickListener {
            GlobalScope.launch {
                try {
                    save(props)
                    TasmotaApi(this@SettingsActivity).switchOff()
                    BatterySwitch.updateAllWidgets(applicationContext)
                } catch (e: Exception) {
                    Toast.makeText(applicationContext, e.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun save(props: SettingsProperties) {
        props.serverName = binding.serverName.text.toString()
        props.save()
    }
}