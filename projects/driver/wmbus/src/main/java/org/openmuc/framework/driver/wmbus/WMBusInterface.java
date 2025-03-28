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
package org.openmuc.framework.driver.wmbus;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

//import javax.xml.bind.DatatypeConverter;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.data.DoubleValue;
import org.openmuc.framework.data.LongValue;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.data.StringValue;
import org.openmuc.framework.data.Value;
import org.openmuc.framework.driver.spi.ChannelRecordContainer;
import org.openmuc.framework.driver.spi.Connection;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.openmuc.framework.driver.spi.RecordsReceivedListener;
import org.openmuc.jmbus.Bcd;
import org.openmuc.jmbus.DataRecord;
import org.openmuc.jmbus.DecodingException;
import org.openmuc.jmbus.SecondaryAddress;
import org.openmuc.jmbus.VariableDataStructure;
import org.openmuc.jmbus.wireless.WMBusConnection;
import org.openmuc.jmbus.wireless.WMBusConnection.WMBusManufacturer;
import org.openmuc.jmbus.wireless.WMBusListener;
import org.openmuc.jmbus.wireless.WMBusMessage;
import org.openmuc.jmbus.wireless.WMBusMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class representing an MBus Connection.<br>
 * This class will bind to the local com-interface.<br>
 * 
 */
public class WMBusInterface {
    private static final Logger logger = LoggerFactory.getLogger(WMBusInterface.class);

    private static final Map<String, WMBusInterface> interfaces = new HashMap<>();
    private final HashMap<SecondaryAddress, DriverConnection> connectionsBySecondaryAddress = new HashMap<>();
    RecordsReceivedListener listener;

    private final WMBusConnection con;
    private final String connectionName;
    private final String transceiverString;
    private final String modeString;

    public class Receiver implements WMBusListener {

        @Override
        public void discardedBytes(byte[] bytes) {
            if (logger.isDebugEnabled()) {
                String bytesAsHexStr = Hex.encodeHexString(bytes);
                logger.debug("received bytes that will be discarded: {}", bytesAsHexStr);
            }
        }

        @Override
        public void newMessage(WMBusMessage message) {

            try {
                message.getVariableDataResponse().decode();
            } catch (DecodingException e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Unable to decode header of received message: " + message, e);
                }
                return;
            }

            synchronized (this) {
                DriverConnection connection = connectionsBySecondaryAddress.get(message.getSecondaryAddress());

                if (connection == null) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("WMBus: connection is null, from device: {} with HashCode: {}",
                                message.getSecondaryAddress().getDeviceId().toString(), message.getSecondaryAddress());
                    }
                    return;
                }

                List<ChannelRecordContainer> channelContainers = connection.getContainersToListenFor();

                if (channelContainers.isEmpty()) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("WMBus: channelContainers.size == 0, from device: "
                                + message.getSecondaryAddress().getDeviceId().toString());
                    }
                    return;
                }

                VariableDataStructure variableDataStructure = message.getVariableDataResponse();

                try {
                    variableDataStructure.decode();
                } catch (DecodingException e) {
                    if (logger.isWarnEnabled()) {
                        logger.warn("Unable to decode header of variable data response or received message: {}",
                                message, e);
                    }
                    return;
                }

                List<DataRecord> dataRecords = message.getVariableDataResponse().getDataRecords();
                String[] dibvibs = new String[dataRecords.size()];

                int i = 0;
                for (DataRecord dataRecord : dataRecords) {
                    String dibHexStr = Hex.encodeHexString(dataRecord.getDib());
                    String vibHexStr = Hex.encodeHexString(dataRecord.getVib());
                    dibvibs[i++] = MessageFormat.format("{0}:{1}", dibHexStr, vibHexStr);
                }

                List<ChannelRecordContainer> containersReceived = new LinkedList<>();

                final long timestamp = System.currentTimeMillis();

                for (ChannelRecordContainer container : channelContainers) {
                    i = 0;
                    setRecords(dataRecords, dibvibs, i, containersReceived, timestamp, container);
                }
                listener.newRecords(containersReceived);
            }
        }

        private void setRecords(List<DataRecord> dataRecords, String[] dibvibs, int i,
                List<ChannelRecordContainer> containersReceived, final long timestamp,
                ChannelRecordContainer container) {
            for (DataRecord dataRecord : dataRecords) {

                if (dibvibs[i++].equalsIgnoreCase(container.getChannelAddress())) {

                    Value value = null;
                    switch (dataRecord.getDataValueType()) {
                    case DATE:
                        value = new DoubleValue(((Date) dataRecord.getDataValue()).getTime());
                        container.setRecord(new Record(value, timestamp));
                        break;
                    case STRING:
                        value = new StringValue((String) dataRecord.getDataValue());
                        container.setRecord(new Record(value, timestamp));
                        break;
                    case DOUBLE:
                        value = new DoubleValue(dataRecord.getScaledDataValue());
                        container.setRecord(new Record(value, timestamp));
                        break;
                    case LONG:
                        if (dataRecord.getMultiplierExponent() == 0) {
                            value = new LongValue((Long) dataRecord.getDataValue());
                            container.setRecord(new Record(value, timestamp));
                        }
                        else {
                            value = new DoubleValue(dataRecord.getScaledDataValue());
                            container.setRecord(new Record(value, timestamp));
                        }
                        break;
                    case BCD:
                        if (dataRecord.getMultiplierExponent() == 0) {
                            value = new LongValue(((Bcd) dataRecord.getDataValue()).longValue());
                            container.setRecord(new Record(value, timestamp));
                        }
                        else {
                            value = new DoubleValue(((Bcd) dataRecord.getDataValue()).longValue()
                                    * Math.pow(10, dataRecord.getMultiplierExponent()));
                            container.setRecord(new Record(value, timestamp));
                        }
                        break;
                    case NONE:
                        logger.warn("Received data record with <dib>:<vib> = {} has value type NONE.", dibvibs[i]);
                        continue;
                    }
                    if (logger.isTraceEnabled()) {
                        String channelId = container.getChannel().getId();
                        logger.trace("WMBus: Value from channel {} is: {}.", channelId, value);
                    }
                    containersReceived.add(container);

                    break;
                }
            }
        }

        @Override
        public void stoppedListening(IOException e) {
            WMBusInterface.this.stoppedListening();
        }
    }

    public static WMBusInterface getSerialInstance(String serialPortName, String transceiverString, String modeString)
            throws ConnectionException, ArgumentSyntaxException {
        WMBusInterface wmBusInterface;

        synchronized (interfaces) {
            wmBusInterface = interfaces.get(serialPortName);

            if (wmBusInterface == null) {
                wmBusInterface = new WMBusInterface(serialPortName, transceiverString, modeString);
                interfaces.put(serialPortName, wmBusInterface);
            }
            else {
                if (!wmBusInterface.modeString.equals(modeString)
                        || !wmBusInterface.transceiverString.equals(transceiverString)) {
                    throw new ConnectionException(
                            "Connections serial interface is already in use with a different transceiver or mode");
                }
            }
        }

        return wmBusInterface;
    }

    public static WMBusInterface getTCPInstance(String host, int port, String transceiverString, String modeString)
            throws ConnectionException, ArgumentSyntaxException {
        WMBusInterface wmBusInterface;
        String hostAndPort = host + ':' + port;

        synchronized (interfaces) {
            wmBusInterface = interfaces.get(hostAndPort);

            if (wmBusInterface == null) {
                wmBusInterface = new WMBusInterface(host, port, transceiverString, modeString);
                interfaces.put(hostAndPort, wmBusInterface);
            }
            else {
                if (!wmBusInterface.modeString.equals(modeString)
                        || !wmBusInterface.transceiverString.equals(transceiverString)) {
                    throw new ConnectionException(
                            "Connections TCP interface is already in use with a different transceiver or mode");
                }
            }
        }

        return wmBusInterface;
    }

    private WMBusInterface(String serialPortName, String transceiverString, String modeString)
            throws ArgumentSyntaxException, ConnectionException {

        this.connectionName = serialPortName;
        this.transceiverString = transceiverString;
        this.modeString = modeString;

        WMBusMode mode = getWMBusModeFromString(modeString);
        WMBusManufacturer wmBusManufacturer = getWMBusManufactureFromString(transceiverString);

        try {
            con = new WMBusConnection.WMBusSerialBuilder(wmBusManufacturer, new Receiver(), serialPortName)
                    .setMode(mode)
                    .build();
        } catch (IOException e) {
            throw new ConnectionException("Failed to open serial interface", e);
        }
    }

    public WMBusInterface(String host, int port, String transceiverString, String modeString)
            throws ArgumentSyntaxException, ConnectionException {
        this.connectionName = host + ':' + port;
        this.transceiverString = transceiverString;
        this.modeString = modeString;

        WMBusMode mode = getWMBusModeFromString(modeString);
        WMBusManufacturer wmBusManufacturer = getWMBusManufactureFromString(transceiverString);

        try {
            con = new WMBusConnection.WMBusTcpBuilder(wmBusManufacturer, new Receiver(), host, port).setMode(mode)
                    .build();
        } catch (IOException e) {
            throw new ConnectionException("Failed to open TCP interface", e);
        }
    }

    private WMBusMode getWMBusModeFromString(String modeString) throws ArgumentSyntaxException {
        WMBusMode mode;
        try {
            mode = WMBusMode.valueOf(modeString.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ArgumentSyntaxException(
                    "The wireless M-Bus mode is not correctly specified in the device's parameters string. Should be S, T or C but is: "
                            + modeString);
        }
        return mode;
    }

    private static WMBusManufacturer getWMBusManufactureFromString(String transceiverString)
            throws ArgumentSyntaxException {
        try {
            return WMBusManufacturer.valueOf(transceiverString.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ArgumentSyntaxException(
                    "The type of transceiver is not correctly specified in the device's parameters string. Should be amber, imst or rc but is: "
                            + transceiverString);
        }
    }

    public void connectionClosedIndication(SecondaryAddress secondaryAddress) {
        connectionsBySecondaryAddress.remove(secondaryAddress);
        if (connectionsBySecondaryAddress.size() == 0) {
            close();
        }
    }

    public void close() {
        synchronized (interfaces) {
            try {
                con.close();
            } catch (IOException e) {
                logger.warn("Failed to close connection properly", e);
            }
            interfaces.remove(connectionName);
        }
    }

    public Connection connect(SecondaryAddress secondaryAddress, String keyString)
            throws ArgumentSyntaxException, DecoderException {
        DriverConnection connection = new DriverConnection(con, secondaryAddress, keyString, this);
        if (logger.isTraceEnabled()) {
            logger.trace("WMBus: connect device with ID {} and HashCode {}", secondaryAddress.getDeviceId(),
                    secondaryAddress);
        }
        connectionsBySecondaryAddress.put(secondaryAddress, connection);
        return connection;
    }

    public void stoppedListening() {
        synchronized (interfaces) {
            interfaces.remove(connectionName);
        }
        synchronized (this) {
            for (DriverConnection connection : connectionsBySecondaryAddress.values()) {
                listener.connectionInterrupted("wmbus", connection);
            }
            connectionsBySecondaryAddress.clear();
        }
    }

}
