package no.nordicsemi.android.kotlin.ble.profile.hts

import no.nordicsemi.android.kotlin.ble.profile.common.Data
import java.util.*

object HTSDataParser {

    fun parse(byteArray: ByteArray): HTSData? {
        val data = Data(byteArray)

        if (data.size() < 5) {
            return null
        }

        var offset = 0
        val flags: Int = data.getIntValue(Data.FORMAT_UINT8, offset) ?: return null

        val unit: TemperatureUnit = TemperatureUnit.create(flags and 0x01) ?: return null

        val timestampPresent = flags and 0x02 != 0
        val temperatureTypePresent = flags and 0x04 != 0
        offset += 1

        if (data.size() < 5 + (if (timestampPresent) 7 else 0) + (if (temperatureTypePresent) 1 else 0)) {
            return null
        }

        val temperature: Float = data.getFloatValue(Data.FORMAT_FLOAT, 1) ?: return null
        offset += 4

        var calendar: Calendar? = null
        if (timestampPresent) {
            calendar = DateTimeParser.parse(data, offset)
            offset += 7
        }

        var type: Int? = null
        if (temperatureTypePresent) {
            type = data.getIntValue(Data.FORMAT_UINT8, offset)
            // offset += 1;
        }

        return HTSData(temperature, unit, calendar, type)
    }
}
