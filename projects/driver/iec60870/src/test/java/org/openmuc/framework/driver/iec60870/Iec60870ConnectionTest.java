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
package org.openmuc.framework.driver.iec60870;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.driver.iec60870.settings.DeviceAddress;
import org.openmuc.framework.driver.iec60870.settings.DeviceSettings;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.openmuc.j60870.ASduType;
import org.openmuc.j60870.Connection;
import org.openmuc.j60870.ConnectionEventListener;
import org.openmuc.j60870.Server;
import org.openmuc.j60870.ServerEventListener;

class Iec60870ConnectionTest {

    private TestableClientConnectionBuilder clientSap;
    private DeviceSettings deviceSettings;
    private Server server;
    private int port;
    final private String addressString = "127.0.0.1";

    @BeforeEach
    void setUp() throws ArgumentSyntaxException, IOException, ConnectionException {
        port = getAvailablePort();
        setupServer(port);
        clientSap = new TestableClientConnectionBuilder(addressString);
        clientSap.setPort(port);
        deviceSettings = new DeviceSettings("");
    }

    @AfterEach
    void stop() {
        server.stop();
    }

    private Iec60870Connection getConnection() {
        Iec60870Connection connection;
        try {
            System.out.println("Port = " + port);
            connection = new Iec60870Connection(new DeviceAddress("h=" + addressString + ";p=" + port), deviceSettings,
                    "iec60870");
        } catch (ConnectionException | ArgumentSyntaxException e) {
            throw new RuntimeException(e);
        }
        return connection;
    }

    private void setupServer(int port) throws IOException {
        InetAddress bindAddress = InetAddress.getByName(addressString);
        Server.Builder builder = Server.builder().setPort(port).setBindAddr(bindAddress);
        server = builder.build();
        server.start(new ServerEventListener() {
            @Override
            public ConnectionEventListener connectionIndication(Connection connection) {
                System.out.println("TestClient has connected.");
                return null;
            }

            @Override
            public void serverStoppedListeningIndication(IOException e) {
                System.out.println("TestClient has stopped.");
            }

            @Override
            public void connectionAttemptFailed(IOException e) {
                System.out.println("TestClient connection has failed.");
            }
        });
    }

    @Test
    void testSetupClientSap_WithCommonAddressFieldLength() throws ArgumentSyntaxException {
        deviceSettings = new DeviceSettings("cafl=1");

        Iec60870Connection connection = getConnection();
        connection.setupClientSap(clientSap, deviceSettings);
        assertEquals(1, clientSap.getTestableCommonAddressFieldLength());
    }

    // @Test
    void testSetupClientSap_WithCotFieldLength() throws ArgumentSyntaxException {
        deviceSettings = new DeviceSettings("cafl=1;cfl=10");

        Iec60870Connection connection = getConnection();
        connection.setupClientSap(clientSap, deviceSettings);

        assertEquals(10, clientSap.getTestableCotFieldLength());
    }

    // Fügen Sie hier weitere Tests für die anderen Felder ein, die durch setupClientSap gesetzt werden können

    @Test
    void testSetupClientSap_AllowedTypes_ValidIds() throws ArgumentSyntaxException {
        deviceSettings = new DeviceSettings("at=1,2,3");

        Iec60870Connection connection = getConnection();
        connection.setupClientSap(clientSap, deviceSettings);

        List<ASduType> allowedASduTypes = clientSap.getTestableAllowedTypes();
        assertEquals(3, allowedASduTypes.size());
        assertTrue(allowedASduTypes.contains(ASduType.typeFor(1)));
        assertTrue(allowedASduTypes.contains(ASduType.typeFor(2)));
        assertTrue(allowedASduTypes.contains(ASduType.typeFor(3)));
    }

    @Test
    void testSetupClientSap_AllowedTypes_WithInvalidId() throws ArgumentSyntaxException {
        deviceSettings = new DeviceSettings("at=1,256"); // 256 ist ungültig

        Iec60870Connection connection = getConnection();
        connection.setupClientSap(clientSap, deviceSettings);

        List<ASduType> allowedASduTypes = clientSap.getTestableAllowedTypes();
        assertEquals(1, allowedASduTypes.size());
        assertTrue(allowedASduTypes.contains(ASduType.typeFor(1)));
        assertFalse(allowedASduTypes.contains(ASduType.typeFor(256))); // 256 sollte nicht vorhanden sein
    }

    private static int MIN_PORT_NUMBER = 1024;
    private static int MAX_PORT_NUMBER = 65535;
    private static final int MAX_TRIES = 20;

    public static int getAvailablePort() {
        for (int i = 0; i < MAX_TRIES; i++) {
            int port = new Random().nextInt((MAX_PORT_NUMBER - MIN_PORT_NUMBER) + 1) + MIN_PORT_NUMBER;
            try (ServerSocket ss = new ServerSocket(port)) {
                ss.setReuseAddress(true);
                if (ss.isBound()) {
                    return port;
                }
            } catch (IOException e) {
                // port is not available
            }
        }
        throw new RuntimeException("No available port found after " + MAX_TRIES + " tries");
    }

}
