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

import org.openmuc.framework.lib.mqtt.MqttSettings;
import org.openmuc.framework.lib.osgi.config.PropertyHandler;

/**
 * Configuration for MQTT logging, parsed from property file by {@link MqttLoggerProperties#parse(PropertyHandler)}
 */
public class MqttLoggerSettings extends MqttSettings {

    private final boolean multiple;

    public MqttLoggerSettings(String host, int port, int localPort, String localAddress, String username,
            String password, boolean ssl, long maxBufferSize, long maxFileSize, int maxFileCount,
            int connectionRetryInterval, int connectionAliveInterval, String persistenceDirectory, String lastWillTopic,
            byte[] lastWillPayload, boolean lastWillAlways, String firstWillTopic, byte[] firstWillPayload,
            int recoveryChunkSize, int recoveryDelay, boolean webSocket, boolean retainedMessages,
            String httpProxyConfig, String parser, boolean multiple) {
        super(host, port, localPort, localAddress, username, password, ssl, maxBufferSize, maxFileSize, maxFileCount,
                connectionRetryInterval, connectionAliveInterval, persistenceDirectory, lastWillTopic, lastWillPayload,
                lastWillAlways, firstWillTopic, firstWillPayload, recoveryChunkSize, recoveryDelay, webSocket,
                retainedMessages, httpProxyConfig, parser);
        this.multiple = multiple;
    }

    /**
     * if true compose log records of different channels to one mqtt message
     */
    public boolean getMultiple() {
        return multiple;
    }
}
