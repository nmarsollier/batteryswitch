package com.nmarsollier.batteryswitch.api

import android.content.Context
import com.google.gson.annotations.SerializedName
import com.nmarsollier.batteryswitch.settings.SettingsProperties
import retrofit2.http.GET
import retrofit2.http.Query

interface TasmotaApiInterface {
    @GET("cm")
    suspend fun command(
        @Query("cmnd") command: String
    ): CommandResponse
}

class TasmotaApi(context: Context) {
    private val props = SettingsProperties(context)

    suspend fun switchOn() = RetrofitConfig(props.serverName)
        .retrofit.create(TasmotaApiInterface::class.java)
        .command("Power On")

    suspend fun switchOff() = RetrofitConfig(props.serverName)
        .retrofit.create(TasmotaApiInterface::class.java)
        .command("Power Off")
}

data class CommandResponse(
    @SerializedName("POWER")
    val power: String? = null
)
