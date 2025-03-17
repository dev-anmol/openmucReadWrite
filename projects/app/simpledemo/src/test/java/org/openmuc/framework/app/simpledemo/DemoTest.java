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
package org.openmuc.framework.app.simpledemo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.openmuc.framework.config.ParseException;
import org.openmuc.framework.core.datamanager.DataManager;
import org.openmuc.framework.core.datamanager.DataManagerAccessor;
import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.dataaccess.Channel;
import org.openmuc.framework.datalogger.ascii.AsciiLogger;
import org.openmuc.framework.driver.channelmath.MathDriver;
import org.openmuc.framework.driver.csv.CsvDriver;

public class DemoTest {

    private static final String TEST_CSV_RELATIVE_PATH = "./src/test/resources/example-csv-for-testing.csv";

    /**
     * This test uses the channel definitions in the channel.xml from the framework folder to make sure the math driver
     * formulas will work. A more basic csv file stored in {@value #TEST_CSV_RELATIVE_PATH} is used to simplify this
     * test (the csv holds only values of 0, 1 and 2).
     */
    @Tag("demoConfigTest")
    @Test
    void mathDriverFormulasWorkAsExpected() throws IOException, ParserConfigurationException, ParseException,
            TransformerException, InterruptedException {

        // This test makes sure that the channels.xml behaves just like intended.
        File modifiedChannelXmlReadyForTesting = fetchDemoChannelsXmlAndModifyForTesting();

        Instant start = Instant.now();
        // mock relevant components of OpenMUC
        DataManager dataManager = new DataManager();
        DataManagerAccessor.activateWithConfig(dataManager, modifiedChannelXmlReadyForTesting);

        MathDriver driver = Mockito.spy(new MathDriver());
        DataManagerAccessor.bindDriverService(dataManager, driver);
        driver.setDataAccessService(dataManager);

        CsvDriver csvDriver = new CsvDriver();
        DataManagerAccessor.bindDriverService(dataManager, csvDriver);

        AsciiLogger logger = new AsciiLogger();
        DataManagerAccessor.bindDataLoggerService(dataManager, logger);

        Channel gridChannel = dataManager.getChannel("power_grid"); // values: 0 1 2
        Channel photovoltaicsChannel = dataManager.getChannel("power_photovoltaics"); // values: 0 0 5
        Channel selfConsumptionChannel = dataManager.getChannel("power_self_consumption"); // expected values: 0 1 7

        // do some checks on the channels.xml (the below tests rely on these settings)
        assertEquals(5000, gridChannel.getSamplingInterval());
        assertEquals(0, gridChannel.getSamplingTimeOffset());
        assertEquals(0, gridChannel.getLoggingTimeOffset());
        assertEquals(5000, photovoltaicsChannel.getSamplingInterval());
        assertEquals(0, photovoltaicsChannel.getSamplingTimeOffset());
        assertEquals(0, photovoltaicsChannel.getLoggingTimeOffset());
        assertEquals(5000, selfConsumptionChannel.getSamplingInterval());
        assertEquals(1000, selfConsumptionChannel.getSamplingTimeOffset());
        assertEquals(2000, selfConsumptionChannel.getLoggingTimeOffset());

        // do tests on both sampling and logging behaviour
        Record latestRecord = null;
        do {
            latestRecord = gridChannel.getLatestRecord();
            Thread.sleep(200);
        } while (latestRecord.getFlag() != Flag.VALID
                && Duration.between(start, Instant.now()).minus(Duration.ofSeconds(10)).isNegative());

        assertEquals(Flag.VALID, gridChannel.getLatestRecord().getFlag(), "Should have received value");

        // wait for the first iteration: 5s + 1s sampling

        // sampling tests
        assertEquals(0, gridChannel.getLatestRecord().getValue().asInt());
        assertEquals(0, photovoltaicsChannel.getLatestRecord().getValue().asInt());
        Thread.sleep(1000); // give math driver time to do the calculation
        assertEquals(0, selfConsumptionChannel.getLatestRecord().getValue().asInt());
        // logging tests: 1s wait
        Thread.sleep(1_000); // 7s
        assertEquals(0, logger.getLatestLogRecord("power_self_consumption").getValue().asInt());

        // wait until second iteration
        Thread.sleep(4_000); // 11s
        // sampling tests
        assertEquals(1, gridChannel.getLatestRecord().getValue().asInt());
        assertEquals(0, photovoltaicsChannel.getLatestRecord().getValue().asInt());
        assertEquals(1, selfConsumptionChannel.getLatestRecord().getValue().asInt());
        // logging tests
        Thread.sleep(1_000); // 12s
        assertEquals(1, logger.getLatestLogRecord("power_self_consumption").getValue().asInt());

        // wait until third iteration
        Thread.sleep(4_000); // 16s
        // sampling tests
        assertEquals(2, gridChannel.getLatestRecord().getValue().asInt());
        assertEquals(5, photovoltaicsChannel.getLatestRecord().getValue().asInt());
        assertEquals(7, selfConsumptionChannel.getLatestRecord().getValue().asInt());
        // logging tests
        Thread.sleep(1_000); // 17s
        assertEquals(7, logger.getLatestLogRecord("power_self_consumption").getValue().asInt());

        // third iteration: rewind, start with first line again
        Thread.sleep(4_000); // 21s
        // sampling tests
        assertEquals(0, gridChannel.getLatestRecord().getValue().asInt());
        assertEquals(0, photovoltaicsChannel.getLatestRecord().getValue().asInt());
        assertEquals(0, selfConsumptionChannel.getLatestRecord().getValue().asInt());
        // logging tests
        Thread.sleep(1_000); // 22s
        assertEquals(0, logger.getLatestLogRecord("power_self_consumption").getValue().asInt());
    }

    private File fetchDemoChannelsXmlAndModifyForTesting() throws IOException {
        String demoChannelsXml = FileUtils.readFileToString(new File("../../../framework/conf/channels.xml"), "UTF-8");
        String modifiedChannelXmlStringForTesting = demoChannelsXml //
                .replaceAll("./csv-driver/home1.csv", TEST_CSV_RELATIVE_PATH) // use modified csv for testing
                .replaceAll("samplingmode=hhmmss", "samplingmode=line"); // always start from first line

        File modifiedChannelXmlReadyForTesting = File.createTempFile("openmuc-junit-test", ".xml");
        FileUtils.writeStringToFile(modifiedChannelXmlReadyForTesting, modifiedChannelXmlStringForTesting, "UTF-8");
        return modifiedChannelXmlReadyForTesting;
    }
}
