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

import static org.openmuc.framework.lib.mqtt.MqttSettings.ProxySettings.PROXY_HTTP;
import static org.openmuc.framework.lib.mqtt.MqttSettings.ProxySettings.PROXY_HTTP_WITH_USER;
import static org.openmuc.framework.lib.mqtt.MqttSettings.ProxySettings.PROXY_SOCKS4;
import static org.openmuc.framework.lib.mqtt.MqttSettings.ProxySettings.PROXY_SOCKS5;

import org.openmuc.framework.lib.osgi.config.GenericSettings;
import org.openmuc.framework.lib.osgi.config.PropertyHandler;
import org.openmuc.framework.lib.osgi.config.ServiceProperty;

/**
 * Holds default values that are written into the property file, if it does not exist.
 */
public class MqttLoggerProperties extends GenericSettings {

    static final String PORT = "port";
    static final String HOST = "host";
    static final String LOCAL_PORT = "localPort";
    static final String LOCAL_ADDRESS = "localAddress";
    static final String SSL = "ssl";
    static final String USERNAME = "username";
    static final String PASSWORD = "password";
    static final String PARSER = "parser";
    static final String MULTIPLE = "multiple";
    static final String MAX_FILE_COUNT = "maxFileCount";
    static final String MAX_FILE_SIZE = "maxFileSize";
    static final String MAX_BUFFER_SIZE = "maxBufferSize";
    static final String CONNECTION_RETRY_INTERVAL = "connectionRetryInterval";
    static final String CONNECTION_ALIVE_INTERVAL = "connectionAliveInterval";
    static final String PERSISTENCE_DIRECTORY = "persistenceDirectory";
    static final String LAST_WILL_TOPIC = "lastWillTopic";
    static final String LAST_WILL_PAYLOAD = "lastWillPayload";
    static final String LAST_WILL_ALWAYS = "lastWillAlways";
    static final String FIRST_WILL_TOPIC = "firstWillTopic";
    static final String FIRST_WILL_PAYLOAD = "firstWillPayload";
    static final String RECOVERY_CHUNK_SIZE = "recoveryChunkSize";
    static final String RECOVERY_DELAY = "recoveryDelay";
    static final String WEB_SOCKET = "webSocket";
    static final String PROXY_CONFIG = "proxyConfiguration";

    public MqttLoggerProperties() {
        super();
        // properties for connection
        properties.put(PORT, new ServiceProperty(PORT, "port for MQTT communication", "1883", true));
        properties.put(HOST, new ServiceProperty(HOST, "URL of MQTT broker", "localhost", true));
        properties.put(SSL, new ServiceProperty(SSL, "usage of ssl true/false", "false", true));
        properties.put(USERNAME, new ServiceProperty(USERNAME, "name of your MQTT account", null, false));
        properties.put(PASSWORD, new ServiceProperty(PASSWORD, "password of your MQTT account", null, false));
        properties.put(LOCAL_PORT, new ServiceProperty(LOCAL_PORT, "local port for MQTT communication", "0", false));
        properties.put(LOCAL_ADDRESS,
                new ServiceProperty(LOCAL_ADDRESS, "local address for MQTT communication", null, false));
        properties.put(PARSER,
                new ServiceProperty(PARSER, "identifier of needed parser implementation", "openmuc", true));
        properties.put(WEB_SOCKET, new ServiceProperty(WEB_SOCKET, "usage of WebSocket true/false", "false", true));

        // properties for recovery / file buffering
        properties.put(CONNECTION_RETRY_INTERVAL,
                new ServiceProperty(CONNECTION_RETRY_INTERVAL, "connection retry interval in s", "10", true));
        properties.put(CONNECTION_ALIVE_INTERVAL,
                new ServiceProperty(CONNECTION_ALIVE_INTERVAL, "connection alive interval in s", "10", true));
        properties.put(PERSISTENCE_DIRECTORY, new ServiceProperty(PERSISTENCE_DIRECTORY,
                "directory for file buffered messages", "data/logger/mqtt", false));
        properties.put(MULTIPLE, new ServiceProperty(MULTIPLE,
                "if true compose log records of different channels to one mqtt message", "false", true));
        properties.put(MAX_FILE_COUNT,
                new ServiceProperty(MAX_FILE_COUNT, "file buffering: number of files to be created", "2", true));
        properties.put(MAX_FILE_SIZE,
                new ServiceProperty(MAX_FILE_SIZE, "file buffering: file size in kB", "5000", true));
        properties.put(MAX_BUFFER_SIZE,
                new ServiceProperty(MAX_BUFFER_SIZE, "file buffering: buffer size in kB", "1000", true));
        properties.put(RECOVERY_CHUNK_SIZE, new ServiceProperty(RECOVERY_CHUNK_SIZE,
                "number of messages which will be recovered simultaneously, 0 = disabled", "0", false));
        properties.put(RECOVERY_DELAY, new ServiceProperty(RECOVERY_DELAY,
                "delay between recovery chunk sending in ms, 0 = disabled", "0", false));

        // properties for LAST WILL / FIRST WILL
        properties.put(LAST_WILL_TOPIC,
                new ServiceProperty(LAST_WILL_TOPIC, "topic on which lastWillPayload will be published", "", false));
        properties.put(LAST_WILL_PAYLOAD, new ServiceProperty(LAST_WILL_PAYLOAD,
                "payload which will be published after (unwanted) disconnect", "", false));
        properties.put(LAST_WILL_ALWAYS, new ServiceProperty(LAST_WILL_ALWAYS,
                "send the last will also on planned disconnects", "false", false));
        properties.put(FIRST_WILL_TOPIC,
                new ServiceProperty(FIRST_WILL_TOPIC, "topic on which firstWillPayload will be published", "", false));
        properties.put(FIRST_WILL_PAYLOAD,
                new ServiceProperty(FIRST_WILL_PAYLOAD, "payload which will be published after connect", "", false));
        properties.put(PROXY_CONFIG,
                new ServiceProperty(PROXY_CONFIG, "Proxy to use to connect to broker. Supported formats: "
                        + PROXY_HTTP_WITH_USER + " or " + PROXY_HTTP + " or " + PROXY_SOCKS4 + " or " + PROXY_SOCKS5,
                        "", false));
    }

    public static MqttLoggerSettings parse(PropertyHandler propertyHandler) {
        // @formatter:off
        // do not use retained messages for logging (reason: caused the logger not to log anymore on first try, also
        // should not be necessary for logging because we have buffering)
        boolean noRetainedMessagesSupportedForLogging = false;
        return new MqttLoggerSettings(
                propertyHandler.getString(MqttLoggerProperties.HOST),
                propertyHandler.getInt(MqttLoggerProperties.PORT),
                propertyHandler.getInt(MqttLoggerProperties.LOCAL_PORT),
                propertyHandler.getString(MqttLoggerProperties.LOCAL_ADDRESS),
                propertyHandler.getString(MqttLoggerProperties.USERNAME),
                propertyHandler.getString(MqttLoggerProperties.PASSWORD),
                propertyHandler.getBoolean(MqttLoggerProperties.SSL),
                propertyHandler.getInt(MqttLoggerProperties.MAX_BUFFER_SIZE),
                propertyHandler.getInt(MqttLoggerProperties.MAX_FILE_SIZE),
                propertyHandler.getInt(MqttLoggerProperties.MAX_FILE_COUNT),
                propertyHandler.getInt(MqttLoggerProperties.CONNECTION_RETRY_INTERVAL),
                propertyHandler.getInt(MqttLoggerProperties.CONNECTION_ALIVE_INTERVAL),
                propertyHandler.getString(MqttLoggerProperties.PERSISTENCE_DIRECTORY),
                propertyHandler.getString(MqttLoggerProperties.LAST_WILL_TOPIC),
                propertyHandler.getString(MqttLoggerProperties.LAST_WILL_PAYLOAD).getBytes(),
                propertyHandler.getBoolean(MqttLoggerProperties.LAST_WILL_ALWAYS),
                propertyHandler.getString(MqttLoggerProperties.FIRST_WILL_TOPIC),
                propertyHandler.getString(MqttLoggerProperties.FIRST_WILL_PAYLOAD).getBytes(),
                propertyHandler.getInt(MqttLoggerProperties.RECOVERY_CHUNK_SIZE),
                propertyHandler.getInt(MqttLoggerProperties.RECOVERY_DELAY),
                propertyHandler.getBoolean(MqttLoggerProperties.WEB_SOCKET),
                noRetainedMessagesSupportedForLogging,
                propertyHandler.getString(MqttLoggerProperties.PROXY_CONFIG),
                propertyHandler.getString(MqttLoggerProperties.PARSER),
                propertyHandler.getBoolean(MqttLoggerProperties.MULTIPLE));
        // @formatter:on
    }
}
