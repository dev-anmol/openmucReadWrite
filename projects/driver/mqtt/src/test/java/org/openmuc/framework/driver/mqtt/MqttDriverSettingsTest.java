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
package org.openmuc.framework.driver.mqtt;

import static java.util.Optional.empty;
import static org.openmuc.framework.lib.mqtt.MqttSettings.ProxyProtocolMapping.HTTP;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openmuc.framework.config.ParseException;
import org.openmuc.framework.core.datamanager.DataManager;
import org.openmuc.framework.core.datamanager.DataManagerAccessor;
import org.openmuc.framework.lib.mqtt.MqttSettings.ProxySettings;

public class MqttDriverSettingsTest {

    @Test
    void settingsAreParsedCorrectly() throws IOException, ParserConfigurationException, ParseException,
            TransformerException, InterruptedException {
        DataManager dataManager = new DataManager();
        DataManagerAccessor.activateWithConfig(dataManager, "framework/conf/channels.xml");
        MqttDriver mqttDriver = new MqttDriver();
        DataManagerAccessor.bindDriverService(dataManager, mqttDriver);
        Thread.sleep(5000);

        MqttDriverSettings driverSettings = driverSettings = mqttDriver.getSettings();
        Assertions.assertEquals("localhost", driverSettings.getHost());
        Assertions.assertEquals(1234, driverSettings.getPort());
        Assertions.assertEquals(4321, driverSettings.getLocalPort());
        Assertions.assertEquals(null, driverSettings.getLocalAddress());
        Assertions.assertEquals("anakin", driverSettings.getUsername());
        Assertions.assertEquals("padme", driverSettings.getPassword());
        Assertions.assertEquals(123, driverSettings.getRecordCollectionSize());
        Assertions.assertTrue(driverSettings.isSsl());
        Assertions.assertEquals(42, driverSettings.getMaxBufferSize());
        Assertions.assertEquals(1337, driverSettings.getMaxFileSize());
        Assertions.assertEquals(23, driverSettings.getMaxFileCount());
        Assertions.assertEquals(456, driverSettings.getConnectionRetryInterval());
        Assertions.assertEquals(777, driverSettings.getConnectionAliveInterval());
        Assertions.assertEquals("first-will-topic", driverSettings.getFirstWillTopic());
        Assertions.assertEquals("hello-world", new String(driverSettings.getFirstWillPayload()));
        Assertions.assertEquals("last-will-topic", driverSettings.getLastWillTopic());
        Assertions.assertEquals("goodbye", new String(driverSettings.getLastWillPayload()));
        Assertions.assertTrue(driverSettings.isLastWillAlways());
        Assertions.assertEquals("/tmp/mqtt-driver", driverSettings.getPersistenceDirectory());
        Assertions.assertTrue(driverSettings.isRetainedMessages());
        Assertions.assertEquals(new ProxySettings(HTTP, "1.2.3.4", 5678, empty(), empty()),
                driverSettings.getProxySettings().get());
    }
}
