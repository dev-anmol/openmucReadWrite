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
package org.openmuc.framework.core.datamanager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.openmuc.framework.core.datamanager.ChannelConfigImpl.timeStringToMillis;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.openmuc.framework.config.ParseException;

public class ChannelConfigImplTest {

    @ParameterizedTest
    @CsvSource({ "99ms, 99", "100, 100", "1s, 1000", "1m, 60000", "0h, 0", "5h, 18000000", "24h, 86400000" })
    public void testTimeStringToMillis(String timeStr, Integer expTimeInMillis) throws Exception {
        Integer millis = timeStringToMillis(timeStr);
        assertEquals(expTimeInMillis, millis);
    }

    public static Object testTimeStringToMillisParams() {
        return new Object[][] { { "99ms", "99" }, { "100", "100" }, { "1s", "1000" }, { "1m", "60000" }, { "0h", "0" },
                { "5h", "18000000" }, { "24h", "86400000" } };
    }

    @Test
    public void testEmptyTimeStringToMillis() throws Exception {
        Integer millis = timeStringToMillis("");
        assertNull(millis);
    }

    @ParameterizedTest
    @ValueSource(strings = { "99w", "1y", "a77" })
    public void testTimeStringToMillisFail(String timeStr) throws Exception {
        Assertions.assertThrows(ParseException.class, () -> timeStringToMillis(timeStr));
    }

    @ParameterizedTest
    @CsvSource({ "99ms, 99", "5ms, 5", "100ms, 100", "1s, 1000", "59s, 59000", "59001ms,59001", "1m, 60000", "0, 0",
            "5h, 18000000", "24h, 86400000" })
    public void testTimeToString(String expectedTimeStr, int millis) throws Exception {
        String resTime = ChannelConfigImpl.millisToTimeString(millis);
        assertEquals(expectedTimeStr, resTime);
    }
}
