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
package org.openmuc.framework.server.iec61850.server;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Collection;
import java.util.HashSet;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.openmuc.framework.core.datamanager.DataManager;
import org.openmuc.framework.core.datamanager.DataManagerAccessor;
import org.openmuc.framework.data.DoubleValue;
import org.openmuc.framework.dataaccess.Channel;

import com.beanit.iec61850bean.ClientAssociation;
import com.beanit.iec61850bean.ClientSap;
import com.beanit.iec61850bean.FcModelNode;
import com.beanit.iec61850bean.ModelNode;
import com.beanit.iec61850bean.ServerModel;
import com.beanit.iec61850bean.ServiceError;

public class IEC61850ServerReadWriteAccessTest {

    // used constants (which could be hidden in methods but are easier to see up here...)
    private static final String channelIEC61850Reference = "NewIEDLDevice1/LLN0.TestOpenmuc.testValue";
    private static int port;
    final String channelOpenMUCReference = "some-server-node";

    private final Channel mappedChannel;

    public IEC61850ServerReadWriteAccessTest() {
        DataManager dataManager = startServer("src/test/resources/server-mappings.icd", "server-mappings.xml");
        mappedChannel = dataManager.getChannel(channelOpenMUCReference);
    }

    @Test
    void canWriteTo61850Server() throws IOException, ServiceError {

        Double originalValue = readFromLocalServerWithIEC61850Client();

        double updatedValue = originalValue - 1337;

        // write to a channel that has a mapping
        mappedChannel.write(new DoubleValue(updatedValue));

        Double updatedValueOnServer = readFromLocalServerWithIEC61850Client();
        assertThat(updatedValueOnServer).isEqualTo(updatedValue);
    }

    @Test
    void canReadFromServer() throws ServiceError, IOException {
        // write some value to the server
        writeToLocalServerWithIEC61850Client(42);

        // the value should now have been updated in the openmuc channel and set as latest record
        assertThat(42d).isEqualTo(mappedChannel.getLatestRecord().getValue().asDouble());
    }

    @Test
    void channelListenersAreCalled() throws InterruptedException, ServiceError, IOException {
        Collection<Double> newValues = new HashSet<>(); // FIXME: currently, the listeners are called twice (so a list
                                                        // would make this obvious, rather use a HashSet to hide this
                                                        // behaviour). Not sure why that is but it does not bother me
                                                        // enough just now, so maybe fix it or remove this comment
        mappedChannel.addListener(newRecord -> {
            double value = newRecord.getValue().asDouble();
            newValues.add(value);
        });

        writeToLocalServerWithIEC61850Client(42);
        writeToLocalServerWithIEC61850Client(1337);

        assertThat(newValues).contains(42d, 1337d); // add check on order?
    }

    public static DataManager startServer(String sclPath, String configInResources) {
        port = getAvailablePort();
        return startServer(sclPath, configInResources, port);
    }

    public static DataManager startServer(String sclPath, String configInResources, int port) {
        try {
            DataManager dataManager = new DataManager();
            DataManagerAccessor.activateWithConfig(dataManager, configInResources);
            // bind driver service in order to make our mqtt channel go into state 'Listening'
            TestIEC61850ServerSettings settings = new TestIEC61850ServerSettings(); // TODO: parse settings using
                                                                                    // openmuc
            settings.setSclPath(sclPath);
            settings.setPort(port);
            Iec61850Server iec61850Server = new Iec61850Server(settings);
            DataManagerAccessor.bindServerService(dataManager, iec61850Server);
            iec61850Server.skipChecksStartServer();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                iec61850Server.deactivate(null);
            }));
            // binding takes a bit (and DataManager runs on a different threat), so wait a bit
            Thread.sleep(2_000);
            return dataManager;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Double readFromLocalServerWithIEC61850Client() throws ServiceError, IOException {
        return readDoubleFromLocalServerWithIEC61850Client(port, channelIEC61850Reference);
    }

    public static Double readDoubleFromLocalServerWithIEC61850Client(int serverPort, String bdaToRead)
            throws ServiceError, IOException {
        return Double.valueOf(readFromLocalServerWithIEC61850Client(port, bdaToRead).getBasicDataAttributes()
                .get(0)
                .getValueString());
    }

    public static ModelNode readFromLocalServerWithIEC61850Client(int serverPort, String nodeToRead)
            throws IOException, ServiceError {
        ClientSap clientSap = new ClientSap();
        ClientAssociation association = clientSap.associate(InetAddress.getLoopbackAddress(), serverPort, null, null);

        ServerModel modelNodes = association.retrieveModel();
        assertThat(modelNodes).isNotNull();
        ModelNode channelReadVia61850Client = modelNodes.findModelNode(nodeToRead, null);
        assertThat(channelReadVia61850Client).describedAs("node is only null if not available").isNotNull();
        // actually read the values from the server!
        association.getDataValues((FcModelNode) channelReadVia61850Client);
        association.disconnect();
        return channelReadVia61850Client;
    }

    private void writeToLocalServerWithIEC61850Client(double value) throws IOException, ServiceError {
        try {
            ClientSap clientSap = new ClientSap();
            ClientAssociation association = clientSap.associate(InetAddress.getLoopbackAddress(), port, null, null);

            ServerModel modelNodes = association.retrieveModel();
            assertThat(modelNodes).isNotNull();
            ModelNode channelReadVia61850Client = modelNodes.findModelNode(channelIEC61850Reference, null);
            IEC61850TypeConversionUtility.setBdaValue(channelReadVia61850Client.getBasicDataAttributes().get(0),
                    Double.toString(value));
            association.setDataValues((FcModelNode) channelReadVia61850Client);
        } catch (Exception e) {
            throw new RuntimeException("Unable to set " + channelIEC61850Reference + " to " + value, e);
        }
    }

    private static int MIN_PORT_NUMBER = 2000;
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
