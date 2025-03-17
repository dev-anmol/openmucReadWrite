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

import static org.openmuc.framework.server.iec61850.server.IEC61850TypeConversionUtility.cast61850TypeToOpenmucType;
import static org.openmuc.framework.server.iec61850.server.IEC61850TypeConversionUtility.setBdaValue;

import java.util.Collections;
import java.util.Dictionary;
import java.util.List;
import java.util.UUID;

import org.openmuc.framework.dataaccess.DataAccessService;
import org.openmuc.framework.lib.osgi.config.DictionaryPreprocessor;
import org.openmuc.framework.lib.osgi.config.PropertyHandler;
import org.openmuc.framework.lib.osgi.config.ServicePropertyException;
import org.openmuc.framework.server.iec61850.serverSchedulingAdapter.IEC61850ScheduleNodeSnapshot;
import org.openmuc.framework.server.spi.ServerMappingContainer;
import org.openmuc.framework.server.spi.ServerService;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beanit.iec61850bean.BasicDataAttribute;
import com.beanit.iec61850bean.Fc;
import com.beanit.iec61850bean.ModelNode;
import com.beanit.iec61850bean.SclParseException;
import com.beanit.iec61850bean.SclParser;
import com.beanit.iec61850bean.ServerEventListener;
import com.beanit.iec61850bean.ServerModel;
import com.beanit.iec61850bean.ServerSap;
import com.beanit.iec61850bean.ServiceError;

/**
 * This starts an IEC61850 server corresponding to the cid File given via the Properties. It also matches the
 * openMUC-channels to the server Node by using the serverMappings configured in the channels.xml file
 */
@Component
public class Iec61850Server implements ServerService, ManagedService {
    static List<ServerMappingContainer> storedMappings = null;
    static ServerModel serverModel;
    private static Logger logger = LoggerFactory.getLogger(Iec61850Server.class);
    private final PropertyHandler propertyHandler;
    private ServerSap serverSap = null;

    private final String uuid = UUID.randomUUID().toString();
    @Reference
    DataAccessService dataAccessService;
    private ScheduleAdapter scheduleAdapter;

    /**
     * Marker that the mappings (IEC 61850 BDAs to OpenMUC channels) where Set By OpenMUC.
     */
    private boolean mappingsSet = false;

    /**
     * Marker that the server config (port, server config, ...) where Set By OpenMUC.
     */
    private boolean serverConfigSet = false;

    // no args constructor required for OSGi
    public Iec61850Server() {
        this(new IEC61850ServerSettings());
    }

    public Iec61850Server(IEC61850ServerSettings settings) {
        propertyHandler = settings.getPropertyHandler();
        logger.debug("Created new instance with settings {}", settings);
    }

    public static BasicDataAttribute getBasicDataAttribute(String serverAddress) {
        Fc fc;
        String bdaAddress;
        try {
            int indexOfDots = serverAddress.indexOf(":");
            fc = Fc.fromString(serverAddress.substring(indexOfDots + 1));
            bdaAddress = serverAddress.substring(0, indexOfDots);
        } catch (Exception e) {
            // possibly, the serverAddress does not contain : to indicate the FC, then simply search for any BDA with no
            // (=null) constraints on FC
            fc = null; // no constraint
            bdaAddress = serverAddress; // assumes the serverAddress does not include a Function Constraint
            // FIXME: add a warning here?
        }
        return ((BasicDataAttribute) serverModel.findModelNode(bdaAddress, fc));
    }

    /**
     * Skips initialization checks when starting the server. Only use in tests.
     */
    @Deprecated
    void skipChecksStartServer() {
        logger.warn("Skipping OpenMUC initialization test and starting server");
        this.serverConfigSet = true;
        this.mappingsSet = true;
        startServer();
    }

    public void startServer() {

        if (!(serverConfigSet && mappingsSet)) {
            logger.debug("Server {} not fully configured, delaying start configSet={},mappingsSet={}", uuid,
                    serverConfigSet, mappingsSet);
            return;
        }
        logger.info("Starting server {} @ port: {} with scl: {} scheduling enabled: {}", uuid,
                IEC61850ServerSettings.getPort(propertyHandler), IEC61850ServerSettings.getSclPath(propertyHandler),
                IEC61850ServerSettings.isSchedulingEnabled(propertyHandler));

        try {
            serverModel = SclParser.parse(IEC61850ServerSettings.getSclPath(propertyHandler)).get(0);
        } catch (SclParseException e) {
            throw new RuntimeException(e);
        }
        if (IEC61850ServerSettings.isSchedulingEnabled(propertyHandler)) {
            this.scheduleAdapter = new ScheduleAdapter(dataAccessService, this);
        }
        serverSap = new ServerSap(IEC61850ServerSettings.getPort(propertyHandler), 0, null, serverModel, null);
        logger.info("Successfully started IEC 61850 server");
        try {
            serverSap.startListening(new EventListener());
        } catch (Exception e) {
            throw new RuntimeException("Unable to start IEC 61850 server", e);
        }

        initialReadFromServer(storedMappings);
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        stopServer();
    }

    private void stopServer() {
        logger.info("Deactivating 61850 Server {}", uuid);
        if (serverSap != null) {
            serverSap.stop();
            serverSap = null;
        }
        if (this.scheduleAdapter != null) {
            this.scheduleAdapter.stop();
        }
    }

    @Override
    public String getId() {
        return "iec61850";
    }

    @Override
    public void serverMappings(List<ServerMappingContainer> mappings) {
        logger.trace("Got initial mappings {}", mappings);
        updatedConfiguration(mappings);
    }

    @Override
    public void updatedConfiguration(List<ServerMappingContainer> mappings) {
        try {
            logger.trace("Mappings updated: {}", mappings);
            if (storedMappings != null && storedMappings.equals(mappings)) {
                logger.info("No updates in mappings. Leaving server unchanged");
                return;
            }
            stopServer();
            checkServerMappings(mappings);
            this.mappingsSet = true;
            startServer();
        } catch (Exception e) {
            stopServer();
        }
    }

    private void checkServerMappings(List<ServerMappingContainer> mappings) {

        logger.info("updated server mappings for {}", uuid);

        storedMappings = mappings;

        mappings.stream()
                .filter(mapping -> mapping.getServerMapping().getServerAddress().contains("\n")
                        || mapping.getServerMapping().getServerAddress().contains("\r") //
                        || mapping.getServerMapping().getServerAddress().contains(" "))
                .findAny()
                .ifPresent(badMapping -> {
                    logger.error(
                            "Error in the channels xml. The serverMappings should not contain whitespaces or line breaks");
                });
    }

    /**
     * Reads the initial values of all stored mappings from the server
     *
     * @throws IEC61850ScheduleNodeSnapshot.IEC61850ServerConfigException
     *             if a mapping can not be found
     */
    private void initialReadFromServer(List<ServerMappingContainer> mappings) {
        for (ServerMappingContainer mapping : mappings) {
            String serverAddress = mapping.getServerMapping().getServerAddress();

            try {
                BasicDataAttribute bda = getBasicDataAttribute(serverAddress);
                if (bda == null) {
                    throw new IEC61850ScheduleNodeSnapshot.IEC61850ServerConfigException(
                            "Unable to find Basic Data Attribute '" + serverAddress + "' of channel with id="
                                    + mapping.getChannel().getId() + " in server.");
                }
                mapping.getChannel().write(cast61850TypeToOpenmucType(bda));
            } catch (Exception e) {
                logger.error(
                        "Unable to initially read from IEC 61850 server node '{}' and update channel '{}'. "
                                + "Is the server not running yet? Ran into {}: {}",
                        serverAddress, mapping.getChannel().getId(), e.getClass(), e.getMessage());
            }

            // This updates the 61850 Model node when the channel value is changed
            logger.trace("Listener added for: {}", mapping.getChannel().getId());
            mapping.getChannel().addListener(record -> {
                BasicDataAttribute bda = getBasicDataAttribute(serverAddress);

                if (bda != null && record.getValue() != null
                        && !record.getValue().toString().equals(cast61850TypeToOpenmucType(bda).toString())) {
                    logger.trace("Writing to 61850 model node, due to new channel value: {}", record);
                    setBdaValue(bda, record.getValue().toString());
                }
                else {
                    if (bda == null) {
                        logger.warn("Bad mapping in channel {}: server address {} not found on server",
                                mapping.getChannel().getId(), serverAddress);
                    }
                    // else, we have no value from the channel yet, so we can safely ignore this case
                }
            });
        }
    }

    @Override
    public void updated(Dictionary<String, ?> newPropertiesDict) throws ConfigurationException {
        logger.info("Server {} got update with {}", uuid, newPropertiesDict);

        DictionaryPreprocessor dictProcessor = new DictionaryPreprocessor(newPropertiesDict);
        try {
            propertyHandler.processConfig(dictProcessor);
            if (propertyHandler.configChanged()) {
                stopServer();
                logger.trace("starting server {} with props {}", this.uuid, propertyHandler.getCurrentProperties());
                startServer();
            }
            else {
                logger.info("Config unchanged.");
            }
        } catch (ServicePropertyException e) {
            logger.error("Update properties failed", e);
            stopServer();
        }
        this.serverConfigSet = true;
        startServer();
    }

    private static class EventListener implements ServerEventListener {
        @Override
        public void serverStoppedListening(ServerSap serverSap) {
            logger.info("The SAP stopped listening");
        }

        /**
         * Handles writes to the server, updating OpenMUC channels if a server mapping exists
         */
        @Override
        public List<ServiceError> write(List<BasicDataAttribute> bdas) {

            for (BasicDataAttribute bda : bdas) {
                if (logger.isTraceEnabled()) {
                    logger.trace("got a write request: {} with value: {}", bda.getReference(), bda.getValueString());
                }
                ModelNode modelNode = serverModel.findModelNode(bda.getReference(), bda.getFc());
                // Execute the write request
                setBdaValue((BasicDataAttribute) modelNode, bda);
                updateChannelValueIfMappingAvailable(bda);
            }
            return Collections.emptyList();
        }

        /**
         * Check if a mapping for the bda in channels.xml exists, and set the channel value.
         *
         * @param bda
         *            Basic Data Attribute to be updated, if mapping in channels.xml exists.
         */
        private void updateChannelValueIfMappingAvailable(BasicDataAttribute bda) {
            for (ServerMappingContainer mapping : storedMappings) {
                if (bda.getReference()
                        .equals(getBasicDataAttribute(mapping.getServerMapping().getServerAddress()).getReference())) {
                    mapping.getChannel().write(cast61850TypeToOpenmucType(bda));
                    logger.debug("Updated OpenMUC channel {} with value from server bda {}",
                            mapping.getChannel().getId(), mapping.getServerMapping().getServerAddress());
                }
            }
        }
    }
}
