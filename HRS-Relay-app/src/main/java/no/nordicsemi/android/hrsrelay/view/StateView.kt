package no.nordicsemi.android.hrsrelay.view

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import no.nordicsemi.android.common.theme.view.SectionTitle
import no.nordicsemi.android.hrsrelay.service.ServerState
import no.nordicsemi.android.hrsrelay.viewmodel.MainViewModel
import no.nordicsemi.android.hrsrelay.R

@Composable
fun StateView(state: ServerState, viewModel: MainViewModel) {
    OutlinedCard(modifier = Modifier.padding(horizontal = 16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionTitle(
                icon = Icons.Default.Bluetooth,
                title = stringResource(id = R.string.characteristics)
            )

            val interactionSource = remember { MutableInteractionSource() }
            val isPressed = interactionSource.collectIsPressedAsState().value

            Spacer(modifier = Modifier.size(16.dp))

            val color = if (60!=10) {
                colorResource(id = R.color.yellow)
            } else {
                colorResource(id = R.color.gray)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = "",
                    tint = color,
                    modifier = Modifier.size(40.dp)
                )

                Spacer(modifier = Modifier.size(16.dp))

                Text(
                    //text = stringResource(id = R.string.led_state, state.beatsPerMin),
                    "test",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Spacer(modifier = Modifier.size(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = stringResource(
                        id = R.string.button_state,
                        state.isButtonPressed.toDisplayString()
                    ),
                    style = MaterialTheme.typography.bodyLarge
                )

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = { },
                    interactionSource = interactionSource
                ) {
                    Text(stringResource(id = R.string.button))
                }
            }

        }
    }
}

@Composable
fun Boolean.toDisplayString(): String {
    return when (this) {
        true -> stringResource(id = R.string.state_on)
        false -> stringResource(id = R.string.state_off)
    }
}
