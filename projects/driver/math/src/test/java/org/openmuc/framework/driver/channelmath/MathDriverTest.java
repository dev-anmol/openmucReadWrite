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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.openmuc.framework.config.ParseException;
import org.openmuc.framework.core.datamanager.DataManager;
import org.openmuc.framework.core.datamanager.DataManagerAccessor;
import org.openmuc.framework.data.DoubleValue;
import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.dataaccess.Channel;
import org.openmuc.framework.dataaccess.ChannelChangeListener;
import org.openmuc.framework.dataaccess.DataAccessService;
import org.openmuc.framework.dataaccess.LogicalDevice;
import org.openmuc.framework.dataaccess.LogicalDeviceChangeListener;
import org.openmuc.framework.dataaccess.ReadRecordContainer;
import org.openmuc.framework.dataaccess.WriteValueContainer;
import org.openmuc.framework.driver.csv.CsvDriver;
import org.openmuc.testing.util.TestLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class MathDriverTest {

    private static final Logger log = LoggerFactory.getLogger(MathDriverTest.class);

    @Test
    void patternWorks_1Channel() throws MathChannelParseException {
        Set<String> channelIdetifiers = MathEvaluator.getChannelIdentifiers("2*§Const_5§-§Const_3§");

        assertEquals(2, channelIdetifiers.size());
        Assertions.assertTrue(channelIdetifiers.contains("Const_5"));
        Assertions.assertTrue(channelIdetifiers.contains("Const_3"));
    }

    @Test
    void patternWorks_1Channel_2() throws MathChannelParseException {
        Set<String> channelIdetifiers = MathEvaluator.getChannelIdentifiers("§Const_5§-2");
        assertEquals(1, channelIdetifiers.size());
        Assertions.assertTrue(channelIdetifiers.contains("Const_5"));
    }

    @Test
    void patternWorks_1Channel_3() throws MathChannelParseException {
        Set<String> channelIdetifiers = MathEvaluator.getChannelIdentifiers("1337*§Const_5§-42");
        assertEquals(1, channelIdetifiers.size());
        Assertions.assertTrue(channelIdetifiers.contains("Const_5"));
    }

    @Test
    void patternWorks_1Channel_4() throws MathChannelParseException {
        Set<String> channelIdetifiers = MathEvaluator.getChannelIdentifiers("-§Const_5§");
        assertEquals(1, channelIdetifiers.size());
        Assertions.assertTrue(channelIdetifiers.contains("Const_5"));
    }

    @Test
    void patternWorks_oneChannel2() throws MathChannelParseException {
        Set<String> channelIdetifiers = MathEvaluator.getChannelIdentifiers("42-§Const_5§");
        assertEquals(1, channelIdetifiers.size());
        Assertions.assertTrue(channelIdetifiers.contains("Const_5"));
    }

    @Test
    void patternWorks_fiveChannels() throws MathChannelParseException {
        Set<String> channelIdetifiers = MathEvaluator
                .getChannelIdentifiers("123+§Const_1§-§Const_2§-§chan_123§-§chan42§-§chan5§-2");
        assertEquals(5, channelIdetifiers.size());
        Assertions.assertTrue(channelIdetifiers.contains("Const_1"));
        Assertions.assertTrue(channelIdetifiers.contains("Const_2"));
        Assertions.assertTrue(channelIdetifiers.contains("chan_123"));
        Assertions.assertTrue(channelIdetifiers.contains("chan42"));
        Assertions.assertTrue(channelIdetifiers.contains("chan5"));
    }

    @Test
    void test_channelIdentifierExtraction_badChannel() {
        String invalidChannelDefinition_missingEnd = "§Const_5-2";
        Assertions.assertThrows(MathChannelParseException.class,
                () -> MathEvaluator.getChannelIdentifiers(invalidChannelDefinition_missingEnd));
    }

    @Test
    void test_channelIdentifierExtraction_badChannel2() {
        String invalidChannelDefinition_missingEnd = "123+§Const_1§-§Const_2§-§chan_123§-§chan42-§chan5§-2";
        Channel mockChannel = Mockito.mock(Channel.class);
        Mockito.doReturn(new Record(new DoubleValue(123), 123l)).when(mockChannel).getLatestRecord();
        Mockito.doReturn("not-" + MathDriver.DRIVER_ID).when(mockChannel).getDriverName();
        Function<String, Channel> channelProvider = channelId -> {
            switch (channelId) {
            case "Const_1":
            case "Const_2":
            case "chan_123":
            case "chan42":
            case "chan5":
                // simply return some channel that provides some lastValue
                return mockChannel;
            default:
                // looks odd, but this is the behaviour of the datamanager in case the channel is not defined
                return null;
            }
        };
        Assertions.assertThrows(MathChannelParseException.class,
                () -> MathEvaluator.parseContainerReplaceChannelsByValues(invalidChannelDefinition_missingEnd,
                        "test-channel-id", channelRefresherFomChannelProvider(channelProvider), null));
    }

    ChannelFinder channelRefresherFomChannelProvider(Function<String, Channel> channelProvider) {
        return (calculationFormulaWithChannelReferences, channelId) -> {
            MathDriver mathDriver = new MathDriver();
            mathDriver.setDataAccessService(dataAccessServiceFromChannelProvider(channelProvider));
            return mathDriver.findChannelsRequiredToEvaluateFormula(calculationFormulaWithChannelReferences, channelId);
        };
    }

    DataAccessService dataAccessServiceFromChannelProvider(Function<String, Channel> channelProvider) {
        return new DataAccessService() {
            @Override
            public Channel getChannel(String id) {
                return channelProvider.apply(id);
            }

            @Override
            public Channel getChannel(String id, ChannelChangeListener channelChangeListener) {
                throw new NotImplementedException();
            }

            @Override
            public List<String> getAllIds() {
                System.err.println("NOT IMPLEMENTED");
                return new LinkedList<>();
            }

            @Override
            public List<LogicalDevice> getLogicalDevices(String type) {
                throw new NotImplementedException();
            }

            @Override
            public List<LogicalDevice> getLogicalDevices(String type,
                    LogicalDeviceChangeListener logicalDeviceChangeListener) {
                throw new NotImplementedException();
            }

            @Override
            public void read(List<ReadRecordContainer> values) {
                throw new NotImplementedException();
            }

            @Override
            public void write(List<WriteValueContainer> values) {
                throw new NotImplementedException();
            }
        };
    }

    @Test
    public void testAddingTwoChannels() throws IOException, ParserConfigurationException, ParseException,
            TransformerException, InterruptedException {

        DataManager dataManager = new DataManager();
        DataManagerAccessor.activateWithConfig(dataManager, "sum.xml");

        MathDriver driver = new MathDriver();
        DataManagerAccessor.bindDriverService(dataManager, driver);
        driver.setDataAccessService(dataManager);

        Channel sumChannel = dataManager.getChannel("SUM");
        Channel const3Channel = dataManager.getChannel("Const_3");
        Channel const_5Channel = dataManager.getChannel("Const_5");
        const3Channel.setLatestRecord(new Record(new DoubleValue(3), System.currentTimeMillis()));
        const_5Channel.setLatestRecord(new Record(new DoubleValue(5), System.currentTimeMillis()));

        assertEquals(1_000, sumChannel.getSamplingInterval());

        // sleeping 1.5 second should be enough
        Thread.sleep(1_500);

        assertEquals(8, sumChannel.getLatestRecord().getValue().asInt());
    }

    @Test
    public void testSubstractingTwoChannels() throws IOException, ParserConfigurationException, ParseException,
            TransformerException, InterruptedException {

        DataManager dataManager = new DataManager();
        DataManagerAccessor.activateWithConfig(dataManager, "difference.xml");

        MathDriver driver = new MathDriver();
        DataManagerAccessor.bindDriverService(dataManager, driver);
        driver.setDataAccessService(dataManager);

        Channel diffChannel = dataManager.getChannel("DIFF");
        Channel const3Channel = dataManager.getChannel("Const_3");
        Channel const_5Channel = dataManager.getChannel("Const_5");
        const3Channel.setLatestRecord(new Record(new DoubleValue(3), System.currentTimeMillis()));
        const_5Channel.setLatestRecord(new Record(new DoubleValue(5), System.currentTimeMillis()));

        assertEquals(1_000, diffChannel.getSamplingInterval());

        // sleeping 2 seconds should be enough
        Thread.sleep(2_000);

        assertEquals(2, diffChannel.getLatestRecord().getValue().asInt());
    }

    @Test
    public void test_stackingChannels_is_not_allowed() throws IOException, ParserConfigurationException, ParseException,
            TransformerException, InterruptedException {

        // Stacking match channels would require to create a (minimal time) delay between the different math channels
        // which seems overly complicated the developer. Also, the workaround is pretty easy: just copy your duplicated
        // formula into the channel address!

        DataManager dataManager = new DataManager();
        DataManagerAccessor.activateWithConfig(dataManager, "stacked-sum.xml");

        DataManagerAccessor.bindDriverService(dataManager, new CsvDriver());
        MathDriver driver = new MathDriver();
        DataManagerAccessor.bindDriverService(dataManager, driver);
        driver.setDataAccessService(dataManager);

        Channel sumChannel = dataManager.getChannel("SUM");
        Channel stackedSumChannel = dataManager.getChannel("STACKED_SUM");
        Channel const3Channel = dataManager.getChannel("Const_3");
        Channel const_5Channel = dataManager.getChannel("Const_5");
        Channel const_7Channel = dataManager.getChannel("Const_7");
        const3Channel.setLatestRecord(new Record(new DoubleValue(3), System.currentTimeMillis()));
        const_5Channel.setLatestRecord(new Record(new DoubleValue(5), System.currentTimeMillis()));
        const_7Channel.setLatestRecord(new Record(new DoubleValue(7), System.currentTimeMillis()));

        assertEquals(1_000, stackedSumChannel.getSamplingInterval());
        assertEquals(1_000, sumChannel.getSamplingInterval());

        // sleeping 1.5 second should be enough for the first sampling task to collect records from the math driver
        Thread.sleep(2_500);

        Assertions.assertNotEquals(Flag.VALID, stackedSumChannel.getLatestRecord().getFlag());
        assertEquals(null, stackedSumChannel.getLatestRecord().getValue());

    }

    @Test
    void oldMeasurementsAreNotMergedWithNewMeasurements() throws InterruptedException, IOException,
            ParserConfigurationException, ParseException, TransformerException {

        DataManager dataManager = new DataManager();
        DataManagerAccessor.activateWithConfig(dataManager, "difference.xml");

        MathDriver driver = new MathDriver();
        DataManagerAccessor.bindDriverService(dataManager, driver);
        driver.setDataAccessService(dataManager);

        Channel diffChannel = dataManager.getChannel("DIFF");
        Channel const3Channel = dataManager.getChannel("Const_3");
        Channel const_5Channel = dataManager.getChannel("Const_5");

        long channel3Timestamp = System.currentTimeMillis();
        long channel5Timestamp = channel3Timestamp - diffChannel.getSamplingInterval() * 2;

        const3Channel.setLatestRecord(new Record(new DoubleValue(3), channel3Timestamp));
        const_5Channel.setLatestRecord(new Record(new DoubleValue(5), channel5Timestamp));

        assertEquals(1_000, diffChannel.getSamplingInterval());

        // sleeping 2 seconds should be enough
        Thread.sleep(2_000);

        assertEquals(Flag.DRIVER_ERROR_READ_FAILURE, diffChannel.getLatestRecord().getFlag());
        assertEquals(null, diffChannel.getLatestRecord().getValue());
        assertEquals(null, diffChannel.getLatestRecord().getTimestamp());
    }

    @Test
    void parsingIsNotDoneEveryTime() throws InterruptedException, IOException, ParserConfigurationException,
            ParseException, TransformerException {

        DataManager dataManager = new DataManager();
        DataManagerAccessor.activateWithConfig(dataManager, "sum.xml");

        MathDriver driver = Mockito.spy(new MathDriver());
        DataManagerAccessor.bindDriverService(dataManager, driver);
        driver.setDataAccessService(dataManager);

        Channel sumChannel = dataManager.getChannel("SUM");
        Channel const3Channel = dataManager.getChannel("Const_3");
        Channel const_5Channel = dataManager.getChannel("Const_5");
        const3Channel.setLatestRecord(new Record(new DoubleValue(3), System.currentTimeMillis()));
        const_5Channel.setLatestRecord(new Record(new DoubleValue(5), System.currentTimeMillis()));

        assertEquals(1_000, sumChannel.getSamplingInterval());

        // sleeping 1.5 second should be enough to do one processing
        Thread.sleep(5000);

        assertEquals(8, sumChannel.getLatestRecord().getValue().asInt());

        Mockito.verify(driver, Mockito.times(1)).findChannelsRequiredToEvaluateFormula(anyString(), anyString());

        // wait again one more round
        Thread.sleep(1_500);
        // still should not be re-parsed
        Mockito.verify(driver, Mockito.times(1)).findChannelsRequiredToEvaluateFormula(anyString(), anyString());
    }

    @Test
    void regressionTest() throws IOException, ParserConfigurationException, ParseException, TransformerException,
            InterruptedException {

        DataManager dataManager = new DataManager();
        DataManagerAccessor.activateWithConfig(dataManager, "mantissaExponent.xml");

        MathDriver driver = Mockito.spy(new MathDriver());
        DataManagerAccessor.bindDriverService(dataManager, driver);
        driver.setDataAccessService(dataManager);

        CsvDriver csvDriver = new CsvDriver();
        DataManagerAccessor.bindDriverService(dataManager, csvDriver);

        Channel mantissa = dataManager.getChannel("Meter_01_raw_p_tot_mant");
        Channel exponent = dataManager.getChannel("Meter_01_raw_p_tot_exp");
        Channel output = dataManager.getChannel("Meter_01_p_tot");

        assertEquals(1_000, mantissa.getSamplingInterval());
        assertEquals(0, mantissa.getSamplingTimeOffset());
        assertEquals(1_000, exponent.getSamplingInterval());
        assertEquals(0, exponent.getSamplingTimeOffset());
        assertEquals(1_000, output.getSamplingInterval());
        assertEquals(1_000, output.getSamplingTimeOffset());

        Thread.sleep(2200);

        assertEquals(2, mantissa.getLatestRecord().getValue().asInt());
        assertEquals(1, exponent.getLatestRecord().getValue().asInt());
        assertEquals(20, output.getLatestRecord().getValue().asInt());
    }

    @Test
    void badFormulasAreLoggedToErrorLog() throws IOException, ParserConfigurationException, ParseException,
            TransformerException, InterruptedException {

        TestLogger testLogger = new TestLogger(MathDriver.class);

        DataManager dataManager = new DataManager();
        DataManagerAccessor.activateWithConfig(dataManager, "mantissaExponent-wrong-formula.xml");

        MathDriver driver = new MathDriver();
        DataManagerAccessor.bindDriverService(dataManager, driver);
        driver.setDataAccessService(dataManager);

        CsvDriver csvDriver = new CsvDriver();
        DataManagerAccessor.bindDriverService(dataManager, csvDriver);

        Channel output = dataManager.getChannel("Meter_01_p_tot");

        assertEquals(1_000, output.getSamplingInterval());
        assertEquals(0, output.getSamplingTimeOffset());

        Thread.sleep(1200);

        boolean formulaIncludedInErrorMessages = testLogger.getLoggedLinesWithLevel(Level.ERROR)
                .stream()
                .anyMatch(msg -> msg.contains("§Meter_01_raw_p_tot_mant§*(10^§Meter_01_raw_p_tot_exp)§"));
        boolean channelNameIncludedInErrorMessages = testLogger.getLoggedLinesWithLevel(Level.ERROR)
                .stream()
                .anyMatch(message -> message.contains("Meter_01_p_tot"));

        assertTrue(formulaIncludedInErrorMessages);
        assertTrue(channelNameIncludedInErrorMessages);
    }
}
