/*
 * Copyright (c) 2022, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list
 * of conditions and the following disclaimer in the documentation and/or other materials
 * provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be
 * used to endorse or promote products derived from this software without specific prior
 * written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package no.nordicsemi.android.kotlin.ble.server

import android.annotation.SuppressLint
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import no.nordicsemi.android.common.navigation.DestinationId
import no.nordicsemi.android.common.navigation.Navigator
import no.nordicsemi.android.kotlin.ble.core.data.util.DataByteArray
import no.nordicsemi.android.kotlin.ble.advertiser.BleAdvertiser
import no.nordicsemi.android.kotlin.ble.advertiser.callback.OnAdvertisingSetStarted
import no.nordicsemi.android.kotlin.ble.advertiser.callback.OnAdvertisingSetStopped
import no.nordicsemi.android.kotlin.ble.core.advertiser.BleAdvertisingConfig
import no.nordicsemi.android.kotlin.ble.core.advertiser.BleAdvertisingData
import no.nordicsemi.android.kotlin.ble.core.advertiser.BleAdvertisingSettings
import no.nordicsemi.android.kotlin.ble.core.data.BleGattPermission
import no.nordicsemi.android.kotlin.ble.core.data.BleGattProperty
import no.nordicsemi.android.kotlin.ble.server.main.ServerBleGatt
import no.nordicsemi.android.kotlin.ble.server.main.ServerConnectionEvent
import no.nordicsemi.android.kotlin.ble.server.main.service.ServerBleGattCharacteristic
import no.nordicsemi.android.kotlin.ble.server.main.service.ServerBleGattCharacteristicConfig
import no.nordicsemi.android.kotlin.ble.server.main.service.ServerBleGattService
import no.nordicsemi.android.kotlin.ble.server.main.service.ServerBleGattServiceConfig
import no.nordicsemi.android.kotlin.ble.server.main.service.ServerBleGattServiceType
import java.util.*
import javax.inject.Inject
import no.nordicsemi.android.hrs.service.HRSRepository
import no.nordicsemi.android.kotlin.ble.profile.hrs.HRSDataParser

object HRSeverSpecifications {
    /** HR Service UUID. */
    val HRS_SERVICE_UUID: UUID = UUID.fromString("0000180D-0000-1000-8000-00805f9b34fb")

    /** HR measurement characteristic UUID. */
    val HEART_RATE_MEASUREMENT_CHARACTERISTIC_UUID: UUID = UUID.fromString("00002A37-0000-1000-8000-00805f9b34fb")

    /** BUTTON characteristic UUID. */
    val UUID_BUTTON_CHAR: UUID = UUID.fromString("00001524-1212-efde-1523-785feabcd123")
}

data class ServerState(
    val isAdvertising: Boolean = false,
    val beatsPerMin: Int = 60,
    val isButtonPressed: Boolean = false,
    val isHRSModuleRunning: Boolean = false,
    val refreshToggle: Boolean = false
) {

    fun copyWithRefresh(): ServerState {
        return copy(refreshToggle = !refreshToggle)
    }
}

@SuppressLint("MissingPermission")
@HiltViewModel
class ServerViewModel @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val navigationManager: Navigator,
    hrsRepository: HRSRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ServerState())
    val state = _state.asStateFlow()

    init {
        hrsRepository.isRunning.onEach {
            _state.value = _state.value.copy(isHRSModuleRunning = it)
        }.launchIn(viewModelScope)
    }

    fun openProfile(destination: DestinationId<Unit, Unit>) {
        navigationManager.navigateTo(destination)
    }

    private var heartRateMeasurementCharacteristic: ServerBleGattCharacteristic? = null
    private var buttonCharacteristic: ServerBleGattCharacteristic? = null

    private var advertisementJob: Job? = null

    fun advertise() {
        advertisementJob = viewModelScope.launch {
            //Define hr measurement characteristic
            val heartRateMeasurementCharacteristic = ServerBleGattCharacteristicConfig(
                HRSeverSpecifications.HEART_RATE_MEASUREMENT_CHARACTERISTIC_UUID,
                listOf(BleGattProperty.PROPERTY_NOTIFY),
                listOf(BleGattPermission.PERMISSION_READ),
                initialValue = DataByteArray.from(0x01)
            )

            //Define button characteristic
            val buttonCharacteristic = ServerBleGattCharacteristicConfig(
                HRSeverSpecifications.UUID_BUTTON_CHAR,
                listOf(BleGattProperty.PROPERTY_READ, BleGattProperty.PROPERTY_NOTIFY),
                listOf(BleGattPermission.PERMISSION_READ, BleGattPermission.PERMISSION_WRITE)
            )

            //Put hr measurement and button characteristics inside a service
            val serviceConfig = ServerBleGattServiceConfig(
                HRSeverSpecifications.HRS_SERVICE_UUID,
                ServerBleGattServiceType.SERVICE_TYPE_PRIMARY,
                listOf(heartRateMeasurementCharacteristic, buttonCharacteristic)
            )

            val server = ServerBleGatt.create(context, viewModelScope, serviceConfig)

            val advertiser = BleAdvertiser.create(context)
            val advertiserConfig = BleAdvertisingConfig(
                settings = BleAdvertisingSettings(
                    deviceName = "HR-Relay", // Advertise a device name,
                    legacyMode = true,
                    scannable = true
                ),
                advertiseData = BleAdvertisingData(
                    ParcelUuid(HRSeverSpecifications.HRS_SERVICE_UUID), //Advertise main service uuid.
                    includeDeviceName = true,
                )
            )

            advertiser.advertise(advertiserConfig) //Start advertising
                .cancellable()
                .catch { it.printStackTrace() }
                .onEach { Log.d("ADVERTISER", "New event: $it") }
                .onEach { //Observe advertiser lifecycle events
                    if (it is OnAdvertisingSetStarted) { //Handle advertising start event
                        _state.value = _state.value.copy(isAdvertising = true)
                    }
                    if (it is OnAdvertisingSetStopped) { //Handle advertising top event
                        _state.value = _state.value.copy(isAdvertising = false)
                    }
                }.launchIn(viewModelScope)

            server.connectionEvents
                .mapNotNull { it as? ServerConnectionEvent.DeviceConnected }
                .map { it.connection }
                .onEach {
                    it.services.findService(HRSeverSpecifications.HRS_SERVICE_UUID)?.let {
                        setUpServices(it)
                    }
                }.launchIn(viewModelScope)
        }
    }

    fun stopAdvertise() {
        advertisementJob?.cancelChildren()
        _state.value = _state.value.copy(isAdvertising = false)
    }

    private fun setUpServices(services: ServerBleGattService) {
        val heartRateMeasurementCharacteristic = services.findCharacteristic(HRSeverSpecifications.HEART_RATE_MEASUREMENT_CHARACTERISTIC_UUID)!!
        val buttonCharacteristic = services.findCharacteristic(HRSeverSpecifications.UUID_BUTTON_CHAR)!!

        //notifyHRSData(DataByteArray.from(99))
        heartRateMeasurementCharacteristic.value.onEach {
            _state.value = _state.value.copy(beatsPerMin = HRSDataParser.parse(it).heartRate)
        }.launchIn(viewModelScope)

        buttonCharacteristic.value.onEach {
            _state.value = _state.value.copy(isButtonPressed = it != DataByteArray.from(0x00))
        }.launchIn(viewModelScope)

        this.heartRateMeasurementCharacteristic = heartRateMeasurementCharacteristic
        this.buttonCharacteristic = buttonCharacteristic
    }

    suspend fun notifyHRSMeasurement(hrsMeasurement: DataByteArray) {
        heartRateMeasurementCharacteristic?.setValueAndNotifyClient(hrsMeasurement)
    }

    fun onButtonPressedChanged(isButtonPressed: Boolean) = viewModelScope.launch {
        val value = if (isButtonPressed) {
            DataByteArray.from(0x01)
        } else {
            DataByteArray.from(0x00)
        }
        buttonCharacteristic?.setValueAndNotifyClient(value)
    }
}
