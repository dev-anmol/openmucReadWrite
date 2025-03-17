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
package org.openmuc.framework.driver.channelmath;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.config.ChannelScanInfo;
import org.openmuc.framework.config.DriverInfo;
import org.openmuc.framework.config.ScanException;
import org.openmuc.framework.config.ScanInterruptedException;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.dataaccess.Channel;
import org.openmuc.framework.dataaccess.DataAccessService;
import org.openmuc.framework.driver.spi.ChannelRecordContainer;
import org.openmuc.framework.driver.spi.ChannelValueContainer;
import org.openmuc.framework.driver.spi.Connection;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.openmuc.framework.driver.spi.DriverDeviceScanListener;
import org.openmuc.framework.driver.spi.DriverService;
import org.openmuc.framework.driver.spi.RecordsReceivedListener;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Does simple calculations on channel values.
 * <p>
 * This class initiates reading channels.xml parsing, from other channels and the creation of {@link Record}s. This
 * class is only implementing the interfaces required for OpenMUC. The actual parsing is shifted into
 * {@link MathEvaluator} and {@link TimestampMergeStrategy}.
 */
@Component
public class MathDriver implements DriverService, Connection, ChannelFinder {

    public final static String DRIVER_ID = "math";
    public final static String DRIVER_DESCRIPTION = "Does simple calculations on channel values. Supports addition and subtraction of two channels. Math channels may be stacked ";
    private final static String noDeviceAddressSyntax = "not needed";
    private final static String noDeviceParametersSyntax = "not needed";
    private final static String channelAddressSyntax = "$<channelId>$[+-*/]$<channelId>$";
    private final static String deviceScanNotSupported = "not supported";
    private static final DriverInfo DRIVER_INFO = new DriverInfo(DRIVER_ID, DRIVER_DESCRIPTION, noDeviceAddressSyntax,
            noDeviceParametersSyntax, channelAddressSyntax, deviceScanNotSupported);
    private static final Logger logger = LoggerFactory.getLogger(MathDriver.class);
    private DataAccessService dataAccessService;

    public MathDriver() {
        // No-argument constructor
    }

    @Activate
    protected void activate(ComponentContext context) {
        logger.info("Activating {}", this.getClass().getSimpleName());
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        logger.info("Deactivating {}", this.getClass().getSimpleName());
    }

    @Override
    public DriverInfo getInfo() {
        return DRIVER_INFO;
    }

    @Override
    public Object read(List<ChannelRecordContainer> containers, Object containerListHandle, String samplingGroup)
            throws UnsupportedOperationException, ConnectionException {

        for (ChannelRecordContainer container : containers) {

            Channel channel = container.getChannel();
            String containerChannelId = channel.getId();
            try {
                logger.debug("Processing container of channelId '{}'", containerChannelId);

                Object channelHandle = container.getChannelHandle();
                MathEvaluator.RecordWithInputChannelCache result = MathEvaluator.evaluate(channel, this, channelHandle);
                container.setRecord(result.getRecord());

                Set<Channel> maybeUpdatedCache = result.getInputChannelCache();
                container.setChannelHandle(maybeUpdatedCache);
                logger.debug("Updated cache with channels={}", maybeUpdatedCache);

                logger.debug("Successfully set value {} for channelId '{}'",
                        new Object[] { result, containerChannelId });
            } catch (MathChannelReadExcpetion e) {
                logger.warn("Unable to read from channelId={} with reason {}:{}. Skipping value calculation.",
                        new Object[] { containerChannelId, e.getClass().getSimpleName(), e.getMessage() });
            } catch (MathChannelParseException e) {
                String message = "Unable to parse formula of channelId='" + containerChannelId + "'";
                logger.error("{}:{}", message, e.getMessage());
                throw new UnsupportedOperationException(message, e);
            } catch (Exception e) {
                logger.error("Unable to calculate result for channel='{}'", containerChannelId, e);
                throw e;
            }
        }
        return null;
    }

    @Override
    public Set<Channel> findChannelsRequiredToEvaluateFormula(String calculationFormulaWithChannelReferences,
            String channelId) throws MathChannelParseException {
        Set<Channel> inputChannels = new HashSet<>();

        Set<String> inputChannelIdentifiers = MathEvaluator
                .getChannelIdentifiers(calculationFormulaWithChannelReferences);

        for (String inputChannelId : inputChannelIdentifiers) {
            Channel channel = dataAccessService.getChannel(inputChannelId);

            if (channel == null) {
                throw new MathChannelParseException("Error parsing '" + calculationFormulaWithChannelReferences
                        + "': Channel '" + inputChannelId + "' not found.");
            }

            if (MathDriver.DRIVER_ID.equals(channel.getDriverName())) {
                throw new MathChannelParseException("Invalid formula for channel '" + channelId
                        + "': may not depend on other " + MathDriver.DRIVER_ID + " channels.");
            }
            inputChannels.add(channel);
        }
        return inputChannels;
    }

    /**
     * math driver depends on other channels only. So we do not need to connect to anything.
     */
    @Override
    public Connection connect(String deviceAddress, String settings)
            throws ArgumentSyntaxException, ConnectionException {
        return this;
    }

    /**
     * Does nothing because we do not need a connection here, see {@link #connect(String, String)}
     */
    @Override
    public void disconnect() {
        // noop
    }

    @Reference
    public void setDataAccessService(DataAccessService dataAccessService) {
        logger.debug("Set data access service with {0} channels", dataAccessService.getAllIds().size());
        this.dataAccessService = dataAccessService;
    }

    @Override
    public void startListening(List<ChannelRecordContainer> containers, RecordsReceivedListener listener)
            throws UnsupportedOperationException, ConnectionException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object write(List<ChannelValueContainer> containers, Object containerListHandle)
            throws UnsupportedOperationException, ConnectionException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void scanForDevices(String settings, DriverDeviceScanListener listener)
            throws UnsupportedOperationException, ArgumentSyntaxException, ScanException, ScanInterruptedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void interruptDeviceScan() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ChannelScanInfo> scanForChannels(String settings)
            throws UnsupportedOperationException, ArgumentSyntaxException, ScanException, ConnectionException {
        throw new UnsupportedOperationException();
    }
}
