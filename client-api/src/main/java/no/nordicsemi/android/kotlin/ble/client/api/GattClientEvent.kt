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

package no.nordicsemi.android.kotlin.ble.client.api

import no.nordicsemi.android.kotlin.ble.core.data.BleGattConnectionStatus
import no.nordicsemi.android.kotlin.ble.core.data.BleGattOperationStatus
import no.nordicsemi.android.kotlin.ble.core.data.BleGattPhy
import no.nordicsemi.android.kotlin.ble.core.data.BondState
import no.nordicsemi.android.kotlin.ble.core.data.GattConnectionState
import no.nordicsemi.android.kotlin.ble.core.wrapper.IBluetoothGattCharacteristic
import no.nordicsemi.android.kotlin.ble.core.wrapper.IBluetoothGattDescriptor
import no.nordicsemi.android.kotlin.ble.core.wrapper.IBluetoothGattService

sealed interface GattClientEvent

data class OnServicesDiscovered(val services: List<IBluetoothGattService>, val status: BleGattOperationStatus) : GattClientEvent

data class OnConnectionStateChanged(
    val status: BleGattConnectionStatus,
    val newState: GattConnectionState
) : GattClientEvent

data class OnPhyRead(
    val txPhy: BleGattPhy,
    val rxPhy: BleGattPhy,
    val status: BleGattOperationStatus
) : GattClientEvent

data class OnPhyUpdate(
    val txPhy: BleGattPhy,
    val rxPhy: BleGattPhy,
    val status: BleGattOperationStatus
) : GattClientEvent

data class OnReadRemoteRssi(val rssi: Int, val status: BleGattOperationStatus) : GattClientEvent

data class OnBondStateChanged(val bondState: BondState) : GattClientEvent

class OnServiceChanged : GattClientEvent

sealed interface ServiceEvent : GattClientEvent

sealed interface CharacteristicEvent : ServiceEvent

sealed interface DescriptorEvent : ServiceEvent

data class OnMtuChanged(val mtu: Int, val status: BleGattOperationStatus) : GattClientEvent

data class OnCharacteristicChanged(
    val characteristic: IBluetoothGattCharacteristic,
    val value: ByteArray
) : CharacteristicEvent

data class OnCharacteristicRead(
    val characteristic: IBluetoothGattCharacteristic,
    val value: ByteArray,
    val status: BleGattOperationStatus
) : CharacteristicEvent

data class OnCharacteristicWrite(
    val characteristic: IBluetoothGattCharacteristic,
    val status: BleGattOperationStatus
) : CharacteristicEvent

data class OnDescriptorRead(
    val descriptor: IBluetoothGattDescriptor,
    val value: ByteArray,
    val status: BleGattOperationStatus
) : DescriptorEvent

data class OnDescriptorWrite(
    val descriptor: IBluetoothGattDescriptor,
    val status: BleGattOperationStatus
) : DescriptorEvent

data class OnReliableWriteCompleted(val status: BleGattOperationStatus) : ServiceEvent