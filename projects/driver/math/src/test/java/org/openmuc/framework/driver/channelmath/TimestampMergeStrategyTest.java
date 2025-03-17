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
package org.openmuc.framework.driver.channelmath;

import static org.openmuc.framework.driver.channelmath.TimestampMergeStrategy.AVG;
import static org.openmuc.framework.driver.channelmath.TimestampMergeStrategy.NEWEST;
import static org.openmuc.framework.driver.channelmath.TimestampMergeStrategy.OLDEST;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.openmuc.framework.config.ParseException;
import org.openmuc.framework.core.datamanager.DataManager;
import org.openmuc.framework.core.datamanager.DataManagerAccessor;
import org.openmuc.framework.dataaccess.Channel;

public class TimestampMergeStrategyTest {

    @Test
    void allStrategies_creationOfTimestep_sameTs() {
        long sameTimestamp = 1234567l;
        for (TimestampMergeStrategy tms : TimestampMergeStrategy.values()) {
            Assertions.assertEquals(sameTimestamp, tms.merge(sameTimestamp, sameTimestamp, sameTimestamp));
        }
    }

    @Test
    void creationOfTimestep_avgTs() {
        Assertions.assertEquals(100_050l, AVG.merge(100_000l, 100_100l));
    }

    @Test
    void creationOfTimestep_newestTs() {
        Assertions.assertEquals(100_100l, NEWEST.merge(100_000l, 100_100l));
    }

    @Test
    void creationOfTimestep_oldestTs() {
        Assertions.assertEquals(100_000l, OLDEST.merge(100_000l, 100_100l));
    }

    @Test
    void parsing_test() throws IOException, ParserConfigurationException, ParseException, TransformerException {
        DataManager dataManager = new DataManager();
        DataManagerAccessor.activateWithConfig(dataManager, "settings_example.xml");
        MathDriver driver = new MathDriver();
        DataManagerAccessor.bindDriverService(dataManager, driver);
        driver.setDataAccessService(dataManager);

        Channel timestamp_averaging_channel = dataManager.getChannel("timestamp_averaging_channel");
        Assertions.assertEquals(AVG, TimestampMergeStrategy.parseDeviceSettings(timestamp_averaging_channel));

        Channel timestamp_no_settings = dataManager.getChannel("timestamp_no_settings");
        Assertions.assertEquals(OLDEST, TimestampMergeStrategy.parseDeviceSettings(timestamp_no_settings));

        Channel timestamp_newer_channel = dataManager.getChannel("timestamp_newer_channel");
        Assertions.assertEquals(NEWEST, TimestampMergeStrategy.parseDeviceSettings(timestamp_newer_channel));

        Channel timestamp_older_channel = dataManager.getChannel("timestamp_older_channel");
        Assertions.assertEquals(OLDEST, TimestampMergeStrategy.parseDeviceSettings(timestamp_older_channel));
    }

    @Test
    void parsing_bad_string() {
        Channel channelWithBadSettings = Mockito.mock(Channel.class);
        Mockito.doReturn("math-ts-strategy=use_random_timestamp").when(channelWithBadSettings).getSettings();
        Assertions.assertThrows(ParseException.class,
                () -> TimestampMergeStrategy.parseDeviceSettings(channelWithBadSettings));
    }
}
