/*
 * Copyright 2011-2024 Fraunhofer ISE
 *
 * This file is part of OpenMUC.
 * For more information visit http://www.openmuc.org
 *
 * OpenMUC is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenMUC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenMUC. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.openmuc.framework.server.modbus.register;

import org.openmuc.framework.data.LongValue;
import org.openmuc.framework.data.Value;
import org.openmuc.framework.dataaccess.Channel;

public class LongMappingInputRegister extends MappingInputRegister {

    public LongMappingInputRegister(Channel channel, int byteHigh, int byteLow) {
        super(channel, byteHigh, byteLow);
    }

    @Override
    public byte[] toBytes() {
        byte[] bytes;
        if (useUnscaledValues) {
            Value value = channel.getLatestRecord().getValue();
            bytes = new LongValue(value.asLong() / (long) channel.getScalingFactor()).asByteArray();
        }
        else {
            bytes = new LongValue(channel.getLatestRecord().getValue().asLong()).asByteArray();
        }

        return new byte[] { bytes[highByte], bytes[lowByte] };
    }

}
