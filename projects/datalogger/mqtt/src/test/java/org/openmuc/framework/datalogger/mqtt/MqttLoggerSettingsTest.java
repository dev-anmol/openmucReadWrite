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
package org.openmuc.framework.datalogger.mqtt;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openmuc.framework.lib.mqtt.MqttSettings;

public class MqttLoggerSettingsTest {

    @Test
    void settingsParsing() {
        Dictionary<String, String> dict = new Hashtable<>();
        dict.put("port", "8883");
        dict.put("host", "localhost");
        dict.put("localPort", "0");
        dict.put("localAddress", "localhost");
        dict.put("ssl", "false");
        dict.put("username", "padme");
        dict.put("password", "luke+leia");
        dict.put("parser", "custom-parser");
        dict.put("multiple", "false");
        dict.put("maxFileCount", "1");
        dict.put("maxFileSize", "2000");
        dict.put("maxBufferSize", "100");
        dict.put("webSocket", "false");
        dict.put("connectionRetryInterval", "123");
        dict.put("connectionAliveInterval", "456");
        dict.put("persistenceDirectory", "/somewhere");
        dict.put("recoveryChunkSize", "1337");
        dict.put("recoveryDelay", "42");
        dict.put("lastWillTopic", "utini");
        dict.put("lastWillPayload", "shootogawa");
        dict.put("lastWillAlways", "true");
        dict.put("firstWillTopic", "foo");
        dict.put("firstWillPayload", "bar");
        dict.put("proxyConfiguration", "http://me:my-secret123@myhost:1234");

        MqttLogger logger = new MqttLogger();
        logger.updated(dict);
        MqttLoggerSettings settingsParsedFromProperties = MqttLoggerProperties.parse(logger.propertyHandler);

        // logger-specific settings
        Assertions.assertEquals("custom-parser", settingsParsedFromProperties.getParser());
        Assertions.assertFalse(settingsParsedFromProperties.getMultiple());

        // general mqtt settings
        Assertions.assertEquals(8883, settingsParsedFromProperties.getPort());
        Assertions.assertEquals("localhost", settingsParsedFromProperties.getHost());
        Assertions.assertFalse(settingsParsedFromProperties.isSsl());
        Assertions.assertEquals("padme", settingsParsedFromProperties.getUsername());
        Assertions.assertEquals("luke+leia", settingsParsedFromProperties.getPassword());
        Assertions.assertEquals(1, settingsParsedFromProperties.getMaxFileCount());
        Assertions.assertEquals(2000, settingsParsedFromProperties.getMaxFileSize());
        Assertions.assertEquals(100, settingsParsedFromProperties.getMaxBufferSize());
        Assertions.assertFalse(settingsParsedFromProperties.isWebSocket());
        Assertions.assertEquals(123, settingsParsedFromProperties.getConnectionRetryInterval());
        Assertions.assertEquals(456, settingsParsedFromProperties.getConnectionAliveInterval());
        Assertions.assertEquals("/somewhere", settingsParsedFromProperties.getPersistenceDirectory());
        Assertions.assertEquals(1337, settingsParsedFromProperties.getRecoveryChunkSize());
        Assertions.assertEquals(42, settingsParsedFromProperties.getRecoveryDelay());
        Assertions.assertEquals("utini", settingsParsedFromProperties.getLastWillTopic());
        Assertions.assertEquals("shootogawa", new String(settingsParsedFromProperties.getLastWillPayload()));
        Assertions.assertTrue(settingsParsedFromProperties.isLastWillAlways());
        Assertions.assertEquals("foo", settingsParsedFromProperties.getFirstWillTopic());
        Assertions.assertEquals("bar", new String(settingsParsedFromProperties.getFirstWillPayload()));
        Assertions.assertEquals(Optional.of(new MqttSettings.ProxySettings(MqttSettings.ProxyProtocolMapping.HTTP, //
                "myhost", //
                1234, //
                Optional.of("me"), //
                Optional.of("my-secret123"))), //
                settingsParsedFromProperties.getProxySettings());
    }
}
