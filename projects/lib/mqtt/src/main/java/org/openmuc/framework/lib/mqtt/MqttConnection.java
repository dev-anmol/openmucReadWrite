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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.openmuc.framework.security.SslManagerInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hivemq.client.mqtt.MqttClientSslConfig;
import com.hivemq.client.mqtt.MqttClientTransportConfig;
import com.hivemq.client.mqtt.MqttClientTransportConfigBuilder;
import com.hivemq.client.mqtt.datatypes.MqttClientIdentifier;
import com.hivemq.client.mqtt.lifecycle.MqttClientConnectedListener;
import com.hivemq.client.mqtt.lifecycle.MqttClientDisconnectedListener;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client;
import com.hivemq.client.mqtt.mqtt3.Mqtt3ClientBuilder;
import com.hivemq.client.mqtt.mqtt3.message.connect.Mqtt3Connect;
import com.hivemq.client.mqtt.mqtt3.message.connect.Mqtt3ConnectBuilder;

/**
 * Represents a connection to a MQTT broker
 */
public class MqttConnection {
    private static final Logger logger = LoggerFactory.getLogger(MqttConnection.class);
    private final MqttSettings settings;

    private final List<MqttClientConnectedListener> connectedListeners = new ArrayList<>();
    private final List<MqttClientDisconnectedListener> disconnectedListeners = new ArrayList<>();

    private boolean sslReady = false;

    private Mqtt3ClientBuilder clientBuilder;
    private Mqtt3AsyncClient client;

    private SslManagerInterface sslManager = null;

    private Timer connectionWatch = new Timer();

    /**
     * A connection to a MQTT broker
     *
     * @param settings
     *            connection details {@link MqttSettings}
     */
    public MqttConnection(MqttSettings settings) {
        this.settings = settings;
        logger.trace("Init with settings {}", settings);
        clientBuilder = getClientBuilder();
        client = buildClient();
        addDisconnectedListener(context -> {
            logger.debug("Disconnection (UUID={}) cause: {} / source {}. Will reconnect: {}",
                    context.getClientConfig().getClientIdentifier().map(MqttClientIdentifier::toString).orElse("none"),
                    context.getCause(), context.getSource(), context.getReconnector().isReconnect());
        });
        addConnectedListener(context -> logger.debug("Reconnected (UUID={})",
                context.getClientConfig().getClientIdentifier().map(MqttClientIdentifier::toString).orElse("none")));
    }

    private Mqtt3ClientBuilder getClientBuilder() {
        Mqtt3ClientBuilder clientBuilder = Mqtt3Client.builder()
                .identifier(UUID.randomUUID().toString())
                .automaticReconnect()
                .initialDelay(settings.getConnectionRetryInterval(), TimeUnit.SECONDS)
                .maxDelay(settings.getConnectionRetryInterval(), TimeUnit.SECONDS)
                .applyAutomaticReconnect();
        MqttClientTransportConfigBuilder transportConfigBuilder = getTransportConfig();
        final MqttClientTransportConfig transportConfig;
        if (settings.isSsl() && sslManager != null) {
            transportConfig = addSslConfig(transportConfigBuilder).build();
        }
        else {
            transportConfig = transportConfigBuilder.build();
        }
        clientBuilder.transportConfig(transportConfig);
        if (settings.isWebSocket()) {
            clientBuilder.webSocketWithDefaultConfig();
        }
        return clientBuilder;
    }

    private MqttClientTransportConfigBuilder getTransportConfig() {
        MqttClientTransportConfigBuilder transportConfigBuilder = MqttClientTransportConfig.builder();
        settings.applyProxy(transportConfigBuilder);
        return transportConfigBuilder.serverHost(settings.getHost())
                .serverPort(settings.getPort())
                .localPort(settings.getLocalPort())
                .localAddress(settings.getLocalAddress());
    }

    private Mqtt3AsyncClient buildClient() {
        return clientBuilder.buildAsync();
    }

    private MqttClientTransportConfigBuilder addSslConfig(MqttClientTransportConfigBuilder transportConfigBuilder) {
        MqttClientSslConfig sslConfig = MqttClientSslConfig.builder()
                .keyManagerFactory(sslManager.getKeyManagerFactory())
                .trustManagerFactory(sslManager.getTrustManagerFactory())
                .handshakeTimeout(10, TimeUnit.SECONDS)
                .build();
        transportConfigBuilder.sslConfig(sslConfig);
        return transportConfigBuilder;
    }

    public boolean isReady() {
        if (settings.isSsl()) {
            return sslReady;
        }
        return true;
    }

    // TODO: tests for both cases: ssl is set up first and also second!
    private void sslUpdate() {
        logger.warn("SSL configuration changed, reconnecting.");
        sslReady = true;
        client.disconnect().whenComplete((ack, e) -> {
            clientBuilder.transportConfig(addSslConfig(getTransportConfig()).build());
            clientBuilder.identifier(UUID.randomUUID().toString());
            connect();
        });
    }

    private Mqtt3Connect getConnect() {
        Mqtt3ConnectBuilder connectBuilder = Mqtt3Connect.builder();
        connectBuilder.keepAlive(settings.getConnectionAliveInterval());
        if (settings.isLastWillSet()) {
            connectBuilder.willPublish()
                    .topic(settings.getLastWillTopic())
                    .payload(settings.getLastWillPayload())
                    .applyWillPublish();
        }
        if (settings.getUsername() != null) {
            connectBuilder.simpleAuth()
                    .username(settings.getUsername())
                    .password(settings.getPassword().getBytes())
                    .applySimpleAuth();
        }
        return connectBuilder.build();
    }

    /**
     * Connect to the MQTT broker
     */
    public void connect() {
        client = buildClient();
        String uuid = client.getConfig().getClientIdentifier().map(MqttClientIdentifier::toString).orElse("<no uuid>");
        logger.trace("Client {} connecting to server {}", uuid, settings.getHost());
        LocalDateTime time = LocalDateTime.now();
        client.connect(getConnect()).whenComplete((ack, e) -> {
            if (e != null) {
                if (uuid.equals(client.getConfig().getClientIdentifier().toString())) {
                    logger.error("Error with connection initiated at {}: {}", time, e.getMessage());
                }
                else {
                    logger.warn("Error with some old connection with UUID={}", uuid, e);
                }
            }
            else {
                logger.debug("connect successfully");
            }
        });

        watchConnection();
    }

    private void watchConnection() {
        // stop tasks that were running before
        logger.trace("Resetting previous connection watch tasks (if present)");
        connectionWatch.cancel();
        connectionWatch = new Timer();

        final long periodMillis = settings.getConnectionRetryInterval() * 1000;
        TimerTask connectionWatchTask = new TimerTask() {
            AtomicInteger disconnectedCount = new AtomicInteger(0);

            @Override
            public void run() {
                boolean connected = client.getState().isConnected();
                boolean connectedOrReconnect = client.getState().isConnectedOrReconnect();
                String clientIdentifier = client.getConfig()
                        .getClientIdentifier()
                        .map(MqttClientIdentifier::toString)
                        .orElse("<none>");

                logger.debug("Client (identifier={}, host={}) state: connected={}, connectedOrReconnect={}",
                        clientIdentifier, settings.getHost(), connected, connectedOrReconnect);

                if (connectedOrReconnect) {
                    logger.debug("Is connectedOrReconnect");
                    disconnectedCount.set(0);
                }
                else {
                    int disconnectedSince = disconnectedCount.incrementAndGet();
                    logger.debug("Is now disconnected since {} runs", disconnectedSince);
                }

                final long disconnectedCnt = 10;
                if (disconnectedCount.get() > disconnectedCnt) {
                    logger.info(
                            "Was disconnected for more than {}ms. Starting manual reconnect by creating a new client, disconnecting old client",
                            periodMillis * disconnectedCnt);
                    client.disconnect();
                    String newIdentifier = UUID.randomUUID().toString();
                    clientBuilder.identifier(newIdentifier);
                    connect();
                }
            }
        };
        connectionWatch.scheduleAtFixedRate(connectionWatchTask, 0, periodMillis);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            connectionWatch.cancel();
            logger.debug("Shut down connection watch timer");
        }));
        logger.trace("Watching connection");
    }

    /**
     * Disconnect from the MQTT broker
     */
    public void disconnect() {
        if (settings.isLastWillAlways()) {
            client.publishWith()
                    .topic(settings.getLastWillTopic())
                    .payload(settings.getLastWillPayload())
                    .send()
                    .whenComplete((publish, e) -> {
                        client.disconnect();
                    });
        }
        else {
            client.disconnect();
        }
    }

    void addConnectedListener(MqttClientConnectedListener listener) {
        logger.trace("addConnectedListener ");
        if (clientBuilder == null) {
            connectedListeners.add(listener);
        }
        else {
            clientBuilder.addConnectedListener(listener);
            if (!connectedListeners.contains(listener)) {
                connectedListeners.add(listener);
            }
        }
    }

    void addDisconnectedListener(MqttClientDisconnectedListener listener) {
        logger.trace("addDisconnectedListener");
        if (clientBuilder == null) {
            disconnectedListeners.add(listener);
        }
        else {
            clientBuilder.addDisconnectedListener(listener);
            if (!disconnectedListeners.contains(listener)) {
                disconnectedListeners.add(listener);
            }
        }
    }

    Mqtt3AsyncClient getClient() {
        return client;
    }

    /**
     * @return the settings {@link MqttSettings} this connection was constructed with
     */
    public MqttSettings getSettings() {
        return settings;
    }

    public void setSslManager(SslManagerInterface instance) {
        if (!settings.isSsl()) {
            return;
        }
        sslManager = instance;
        clientBuilder = getClientBuilder();
        for (MqttClientConnectedListener listener : connectedListeners) {
            addConnectedListener(listener);
        }
        connectedListeners.clear();
        for (MqttClientDisconnectedListener listener : disconnectedListeners) {
            addDisconnectedListener(listener);
        }
        disconnectedListeners.clear();
        sslManager.listenForConfigChange(this::sslUpdate);
        addDisconnectedListener(context -> {
            logger.debug("Handling disconnect");
            String disconnectedClientId = context.getClientConfig()
                    .getClientIdentifier()
                    .map(MqttClientIdentifier::toString)
                    .orElse("<no id from context>");
            String thisClientIdentifier = client.getConfig()
                    .getClientIdentifier()
                    .map(MqttClientIdentifier::toString)
                    .orElse("<no id from client>");

            if (!disconnectedClientId.equals(thisClientIdentifier)) {
                logger.debug("Old client was disconnected. Preventing further reconnects.");
                context.getReconnector().reconnect(false);
            }
            else if (context.getReconnector().getAttempts() >= 3) {
                // if the reconnect was not successful for 3 times, then it probably will be never!
                // just create a new connection instead!
                // this case is only interesting for ssl, did not without ssl according to martin

                logger.debug("Shutting down old client");
                context.getReconnector().reconnect(false);
                client.disconnect();
                logger.info("Disconnected old client {}, starting new client", thisClientIdentifier);

                String newIdentifier = UUID.randomUUID().toString();
                clientBuilder.identifier(newIdentifier);
                connect();
                logger.debug("Connected to new client {}", newIdentifier);
            }
        });
        client = buildClient();
        if (sslManager.isLoaded()) {
            sslReady = true;
        }
    }
}
