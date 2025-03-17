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

import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.lib.mqtt.MqttSettings;

/**
 * Holds parameters for MQTT driver
 */
public class MqttDriverSettings extends MqttSettings {

    /**
     * This parameter makes it possible to optimize the performance of listening and logging huge amounts of records.
     * The driver waits until the configured number of records is collected, before returning the list to the data
     * manager. This decreases the number of needed tasks e.g. for writing to a database.
     */
    private final int recordCollectionSize;

    public static MqttDriverSettings parse(String host, String settings) throws ArgumentSyntaxException {
        settings = settings.replaceAll(";", "\n");
        Properties mqttSettings = new Properties();
        try {
            mqttSettings.load(new StringReader(settings));
        } catch (IOException e) {
            throw new ArgumentSyntaxException("Could not read settings string");
        }

        int port = Integer.parseInt(mqttSettings.getProperty("port"));
        int localPort = Integer.parseInt(mqttSettings.getProperty("localPort", "0"));
        String localAddress = mqttSettings.getProperty("localAddress", null);
        String username = mqttSettings.getProperty("username");
        String password = mqttSettings.getProperty("password");
        boolean ssl = Boolean.parseBoolean(mqttSettings.getProperty("ssl"));
        long maxBufferSize = Long.parseLong(mqttSettings.getProperty("maxBufferSize", "0"));
        long maxFileSize = Long.parseLong(mqttSettings.getProperty("maxFileSize", "0"));
        int maxFileCount = Integer.parseInt(mqttSettings.getProperty("maxFileCount", "1"));
        int connectionRetryInterval = Integer.parseInt(mqttSettings.getProperty("connectionRetryInterval", "10"));
        int connectionAliveInterval = Integer.parseInt(mqttSettings.getProperty("connectionAliveInterval", "10"));
        String persistenceDirectory = mqttSettings.getProperty("persistenceDirectory", "data/driver/mqtt");
        String lastWillTopic = mqttSettings.getProperty("lastWillTopic", "");
        byte[] lastWillPayload = mqttSettings.getProperty("lastWillPayload", "").getBytes();
        boolean lastWillAlways = Boolean.parseBoolean(mqttSettings.getProperty("lastWillAlways", "false"));
        String firstWillTopic = mqttSettings.getProperty("firstWillTopic", "");
        byte[] firstWillPayload = mqttSettings.getProperty("firstWillPayload", "").getBytes();
        boolean webSocket = Boolean.parseBoolean(mqttSettings.getProperty("webSocket", "false"));
        boolean retainedMessages = Boolean.parseBoolean(mqttSettings.getProperty("retainedMessages", "false"));
        int recordCollectionSize = Integer.parseInt(mqttSettings.getProperty("recordCollectionSize", "1"));
        String proxy = mqttSettings.getProperty("proxyConfiguration", "");
        String parser = mqttSettings.getProperty("parser", "");

        return new MqttDriverSettings(host, port, localPort, localAddress, username, password, ssl, maxBufferSize,
                maxFileSize, maxFileCount, connectionRetryInterval, connectionAliveInterval, persistenceDirectory,
                lastWillTopic, lastWillPayload, lastWillAlways, firstWillTopic, firstWillPayload, 0, 0, webSocket,
                retainedMessages, proxy, parser, recordCollectionSize);
    }

    public MqttDriverSettings(String host, int port, int localPort, String localAddress, String username,
            String password, boolean ssl, long maxBufferSize, long maxFileSize, int maxFileCount,
            int connectionRetryInterval, int connectionAliveInterval, String persistenceDirectory, String lastWillTopic,
            byte[] lastWillPayload, boolean lastWillAlways, String firstWillTopic, byte[] firstWillPayload,
            int recoveryChunkSize, int recoveryDelay, boolean webSocket, boolean retainedMessages,
            String httpProxyConfig, String parser, int recordCollectionSize) {
        super(host, port, localPort, localAddress, username, password, ssl, maxBufferSize, maxFileSize, maxFileCount,
                connectionRetryInterval, connectionAliveInterval, persistenceDirectory, lastWillTopic, lastWillPayload,
                lastWillAlways, firstWillTopic, firstWillPayload, recoveryChunkSize, recoveryDelay, webSocket,
                retainedMessages, httpProxyConfig, parser);
        this.recordCollectionSize = recordCollectionSize;
    }

    public int getRecordCollectionSize() {
        return recordCollectionSize;
    }

}
