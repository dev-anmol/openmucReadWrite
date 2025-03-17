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

package org.openmuc.framework.lib.mqtt;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hivemq.client.mqtt.MqttClientTransportConfigBuilder;
import com.hivemq.client.mqtt.MqttProxyConfig;
import com.hivemq.client.mqtt.MqttProxyConfigBuilder;
import com.hivemq.client.mqtt.MqttProxyProtocol;

/**
 * Holds parameters for MQTT lib that are mostly used to connect to the MQTT broker, but also some OpenMUC specifics,
 * like the {@link org.openmuc.framework.parser.spi.ParserService} to be used to transform OpenMUC
 * {@link org.openmuc.framework.data.Record}s in MQTT messages and vice versa, where to buffer messages in case the
 * connection to the broker is lost etc.
 */
public class MqttSettings {
    private static final Logger logger = LoggerFactory.getLogger(MqttSettings.class);

    private final String host;
    private final int port;
    private final int localPort;
    private final String localAddress;
    private final String username;
    private final String password;
    private final boolean ssl;
    private final long maxBufferSize;
    private final long maxFileSize;
    private final int maxFileCount;
    private final int connectionRetryInterval;
    private final int connectionAliveInterval;
    private final String persistenceDirectory;
    private final String lastWillTopic;
    private final byte[] lastWillPayload;
    private final boolean lastWillAlways;
    private final String firstWillTopic;
    private final byte[] firstWillPayload;
    private final int recoveryChunkSize;
    private final int recoveryDelay;
    private final boolean webSocket;
    private final boolean retainedMessages;
    private final Optional<ProxySettings> proxySettings;
    private final String parser;

    public MqttSettings(String host, int port, int localPort, String localAddress, String username, String password,
            boolean ssl, long maxBufferSize, long maxFileSize, int maxFileCount, int connectionRetryInterval,
            int connectionAliveInterval, String persistenceDirectory, String lastWillTopic, byte[] lastWillPayload,
            boolean lastWillAlways, String firstWillTopic, byte[] firstWillPayload, int recoveryChunkSize,
            int recoveryDelay, boolean webSocket, boolean retainedMessages, String httpProxyConfig, String parser) {
        this.host = host;
        this.port = port;
        this.localPort = localPort;
        this.localAddress = localAddress;
        this.username = username;
        this.password = password;
        this.ssl = ssl;
        this.webSocket = webSocket;
        this.maxBufferSize = maxBufferSize;
        this.maxFileSize = maxFileSize;
        this.maxFileCount = maxFileCount;
        this.connectionRetryInterval = connectionRetryInterval;
        this.connectionAliveInterval = connectionAliveInterval;
        this.persistenceDirectory = persistenceDirectory;
        this.lastWillTopic = lastWillTopic;
        this.lastWillPayload = lastWillPayload;
        this.lastWillAlways = lastWillAlways;
        this.firstWillTopic = firstWillTopic;
        this.firstWillPayload = firstWillPayload;
        this.recoveryChunkSize = recoveryChunkSize;
        this.recoveryDelay = recoveryDelay;
        this.retainedMessages = retainedMessages;
        this.proxySettings = ProxySettings.parse(httpProxyConfig);
        this.parser = parser;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public int getLocalPort() {
        return localPort;
    }

    public String getLocalAddress() {
        return localAddress;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public boolean isSsl() {
        return ssl;
    }

    /**
     * @return maximum buffer size in Kibibytes
     */
    public long getMaxBufferSize() {
        return maxBufferSize;
    }

    /**
     * @return maximum file buffer size in Kibibytes
     */
    public long getMaxFileSize() {
        return maxFileSize;
    }

    public int getMaxFileCount() {
        return maxFileCount;
    }

    public long getConnectionRetryInterval() {
        return connectionRetryInterval;
    }

    public int getConnectionAliveInterval() {
        return connectionAliveInterval;
    }

    public String getPersistenceDirectory() {
        return persistenceDirectory;
    }

    public String getLastWillTopic() {
        return lastWillTopic;
    }

    public byte[] getLastWillPayload() {
        return lastWillPayload;
    }

    public boolean isLastWillSet() {
        return !lastWillTopic.equals("") && lastWillPayload.length != 0;
    }

    public boolean isLastWillAlways() {
        return lastWillAlways && isLastWillSet();
    }

    public String getFirstWillTopic() {
        return firstWillTopic;
    }

    public byte[] getFirstWillPayload() {
        return firstWillPayload;
    }

    public boolean isFirstWillSet() {
        return !firstWillTopic.equals("") && lastWillPayload.length != 0;
    }

    public boolean isRecoveryLimitSet() {
        return recoveryChunkSize > 0 && recoveryDelay > 0;
    }

    public int getRecoveryChunkSize() {
        return recoveryChunkSize;
    }

    public int getRecoveryDelay() {
        return recoveryDelay;
    }

    public boolean isWebSocket() {
        return webSocket;
    }

    public boolean isRetainedMessages() {
        return retainedMessages;
    }

    public boolean hasSupportedProxyConfigured() {
        return this.proxySettings.isPresent();
    }

    public String getParser() {
        return parser;
    }

    /**
     * Returns a string of all settings, always uses '*****' as password string.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("host=").append(getHost()).append("\n");
        sb.append("port=").append(getPort()).append("\n");
        sb.append("localPort=").append(getLocalPort()).append("\n");
        sb.append("localAddress=").append(getLocalAddress()).append("\n");
        sb.append("username=").append(getUsername()).append("\n");
        sb.append("password=").append("*****").append("\n");
        sb.append("ssl=").append(isSsl()).append("\n");
        sb.append("webSocket=").append(isWebSocket()).append("\n");
        sb.append("retainedMessages=").append(isRetainedMessages()).append("\n");
        sb.append("persistenceDirectory=").append(getPersistenceDirectory()).append("\n");
        sb.append("maxBufferSize=").append(getMaxBufferSize()).append("\n");
        sb.append("maxFileCount=").append(getMaxFileCount()).append("\n");
        sb.append("maxFileSize=").append(getMaxFileSize()).append("\n");
        sb.append("connectionRetryInterval=").append(getConnectionRetryInterval()).append("\n");
        sb.append("connectionAliveInterval=").append(getConnectionAliveInterval()).append("\n");
        sb.append("lastWillTopic=").append(getLastWillTopic()).append("\n");
        sb.append("lastWillPayload=").append(new String(getLastWillPayload())).append("\n");
        sb.append("lastWillAlways=").append(isLastWillAlways()).append("\n");
        sb.append("firstWillTopic=").append(getFirstWillTopic()).append("\n");
        sb.append("firstWillPayload=").append(new String(getFirstWillPayload()));
        sb.append("recoveryChunkSize=").append(getRecoveryChunkSize()).append("\n");
        sb.append("recoveryDelay=").append(getRecoveryDelay()).append("\n");
        return sb.toString();
    }

    public Optional<ProxySettings> getProxySettings() {
        return proxySettings;
    }

    public void applyProxy(MqttClientTransportConfigBuilder transportConfigBuilder) {
        if (proxySettings.isPresent()) {
            transportConfigBuilder.proxyConfig(proxySettings.get().toMqttProxyConfig());
            logger.debug("Applied proxy settings");
        }
        else {
            logger.debug("No proxy set");
        }
    }

    /**
     * Holds information about the used proxy
     */
    public static class ProxySettings {

        // examples for valid proxies
        public static String PROXY_HTTP_WITH_USER = "http://user:password@host:port";
        public static String PROXY_HTTP = "http://host:port";
        public static String PROXY_SOCKS4 = "socks4://host:port";
        public static String PROXY_SOCKS5 = "socks5://host:port";

        private final ProxyProtocolMapping protocol;
        private final String hostname;
        private final int port;
        private final Optional<String> user;
        private final Optional<String> password;

        public ProxySettings(ProxyProtocolMapping protocol, String hostname, int port, Optional<String> user,
                Optional<String> password) {
            this.protocol = protocol;
            this.hostname = hostname;
            this.port = port;
            this.user = user;
            this.password = password;
        }

        private ProxySettings(String proxyConfig) {
            final String httpProxyStringWithSchema;
            if (proxyConfig.contains("://")) {
                httpProxyStringWithSchema = proxyConfig;
            }
            else {
                String defaultSchema = ProxyProtocolMapping.HTTP.getSchema() + "://";
                logger.debug("No proxy schema set. Assuming '{}'", defaultSchema);
                httpProxyStringWithSchema = defaultSchema + proxyConfig;
            }
            try {
                URI uri = new URI(httpProxyStringWithSchema);
                this.protocol = ProxyProtocolMapping.parse(uri);
                if (uri.getHost() == null || uri.getHost().isEmpty()) {
                    throw new IllegalArgumentException("Bad proxy config '" + proxyConfig + "': host is missing");
                }
                this.hostname = uri.getHost();
                this.port = uri.getPort();
                String userInfo = uri.getUserInfo();
                if (userInfo != null && !userInfo.isEmpty() && userInfo.contains(":")) {
                    String[] split = userInfo.split(":");
                    if (split.length == 2) {
                        this.user = Optional.of(split[0]);
                        this.password = Optional.of(split[1]);
                    }
                    else {
                        throw new IllegalArgumentException("Bad user/password in proxy settings");
                    }
                }
                else {
                    this.user = Optional.empty();
                    this.password = Optional.empty();
                }
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalArgumentException("Unable to parse proxy config from '" + proxyConfig + "'", e);
            }
        }

        public static Optional<ProxySettings> parse(String proxyConfig) throws IllegalArgumentException {
            if ((proxyConfig == null ? "" : proxyConfig).trim().isEmpty()) {
                return Optional.empty();
            }
            else {
                try {
                    ProxySettings settings = new ProxySettings(proxyConfig);
                    logger.debug("Parsed {}", settings);
                    return Optional.of(settings);
                } catch (Exception e) {
                    throw new IllegalArgumentException(
                            "Unable to parse MQTT Settings from " + proxyConfig + ". Defaulting to no proxy.", e);
                }
            }
        }

        public MqttProxyConfig toMqttProxyConfig() {
            MqttProxyConfigBuilder proxyConfigBuilder = MqttProxyConfig.builder();
            proxyConfigBuilder//
                    .host(this.hostname)//
                    .port(this.port)
                    .protocol(this.protocol.mqttProxyProtocol);

            if (user.isPresent()) {
                proxyConfigBuilder.username(user.get());
            }
            if (password.isPresent()) {
                proxyConfigBuilder.password(password.get());
            }

            return proxyConfigBuilder.build();
        }

        @Override
        public String toString() {
            return "ProxySettings{" + "protocol=" + protocol + ", hostname='" + hostname + '\'' + ", port=" + port
                    + ", user=" + user.orElse("<none>") + ", hasPassword=" + password.isPresent() + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ProxySettings settings = (ProxySettings) o;
            return port == settings.port && protocol == settings.protocol && Objects.equals(hostname, settings.hostname)
                    && Objects.equals(user, settings.user) && Objects.equals(password, settings.password);
        }

        @Override
        public int hashCode() {
            return Objects.hash(protocol, hostname, port, user, password);
        }
    }

    /**
     * Parses URI proxy schemes and maps them to {@link MqttProxyProtocol} values.
     */
    public enum ProxyProtocolMapping {
        HTTP("http", MqttProxyProtocol.HTTP),
        SOCKS_4("socks4", MqttProxyProtocol.SOCKS_4),
        SOCKS_5("socks5", MqttProxyProtocol.SOCKS_5);

        String schema;
        MqttProxyProtocol mqttProxyProtocol;

        ProxyProtocolMapping(String schema, MqttProxyProtocol protocol) {
            this.schema = schema;
            this.mqttProxyProtocol = protocol;
        }

        public String getSchema() {
            return schema;
        }

        static ProxyProtocolMapping parse(URI uri) {
            Optional<ProxyProtocolMapping> existingMapping = Arrays.stream(ProxyProtocolMapping.values())
                    .filter(mapping -> uri.getScheme().startsWith(mapping.getSchema()))
                    .findFirst();
            return existingMapping.orElseThrow(() -> new IllegalArgumentException("Unknown proxy schema '"
                    + uri.getScheme() + "'. Known schemes are: " + ProxyProtocolMapping.values()));
        }

        Collection<String> getKnownSchemas() {
            return Arrays.stream(ProxyProtocolMapping.values())
                    .map(ProxyProtocolMapping::getSchema)
                    .collect(Collectors.toSet());
        }

        @Override
        public String toString() {
            return getSchema();
        }
    }

    // TODO: maybe add method to make sure the proxy can be connected to? the error message is quite ok,
    // but could be more clear that the proxy is causing issue
}
