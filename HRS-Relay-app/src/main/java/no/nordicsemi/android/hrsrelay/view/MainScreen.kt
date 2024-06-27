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

package no.nordicsemi.android.hrsrelay.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import no.nordicsemi.android.common.permissions.ble.RequireBluetooth
import no.nordicsemi.android.common.permissions.ble.RequireLocation
import no.nordicsemi.android.common.theme.view.NordicAppBar
import no.nordicsemi.android.hrsrelay.viewmodel.MainViewModel
import no.nordicsemi.android.hrsrelay.HRSDestinationId
import no.nordicsemi.android.hrsrelay.R
import no.nordicsemi.android.kotlin.ble.core.data.GattConnectionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {

    Scaffold(
        topBar = {
            NordicAppBar(stringResource(id = R.string.app_name))
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Box(modifier = Modifier.padding(it).padding(top = 16.dp)) {
            RequireBluetooth {
                RequireLocation {
                    val viewModel = hiltViewModel<MainViewModel>()
                    val hrsServerState by viewModel.stateHrsServer.collectAsStateWithLifecycle()
                    val hrsClientState by viewModel.stateHrsClient.collectAsStateWithLifecycle()
                    //val device by viewModel.device.collectAsStateWithLifecycle()

                    //device?.let { DeviceView(it) }

                    Column {
                        Spacer(modifier = Modifier.size(16.dp))

                        FeatureButton(
                            R.drawable.ic_hrs,
                            R.string.hrs_module,
                            R.string.hrs_module_full,
                            hrsClientState.connectionState?.state == GattConnectionState.STATE_CONNECTED,
                            R.string.heart_rate,
                            hrsClientState.heartRates.lastOrNull().toString()
                        ) {
                            viewModel.openProfile(HRSDestinationId)
                            //viewModel.logEvent(ProfileOpenEvent(Profile.HRS))
                        }

                        Spacer(modifier = Modifier.size(16.dp))

                        StateView(state = hrsServerState, viewModel = viewModel)

                        Spacer(modifier = Modifier.size(16.dp))

                        AdvertiseView(state = hrsServerState, viewModel = viewModel)

                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    MainScreen()
}
