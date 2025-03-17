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
package org.openmuc.framework.driver.csv;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.junit.jupiter.api.Test;
import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.config.ParseException;
import org.openmuc.framework.core.datamanager.ConnectTask;
import org.openmuc.framework.core.datamanager.DataManager;
import org.openmuc.framework.core.datamanager.DataManagerAccessor;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.openmuc.testing.util.TestLogger;

import ch.qos.logback.classic.Level;

public class ConfigurationErrorHandlingTest {

    @Test
    void channelAddressTagIsMissing() throws InterruptedException, IOException, ParserConfigurationException,
            ParseException, TransformerException, ConnectionException, ArgumentSyntaxException {

        TestLogger testLogger = new TestLogger(CsvDeviceConnection.class);

        DataManager dataManager = new DataManager();
        DataManagerAccessor.activateWithConfig(dataManager, "channelAddressTagIsMissing.xml");

        CsvDriver driver = new CsvDriver();
        DataManagerAccessor.bindDriverService(dataManager, driver);

        Thread.sleep(500);

        List<String> warnMessages = testLogger.getLoggedLinesWithLevel(Level.WARN);
        assertFalse(warnMessages.isEmpty());
        assertTrue(warnMessages.stream().anyMatch(msg -> msg.contains("hhmmss")));
        assertTrue(warnMessages.stream().anyMatch(msg -> msg.contains("No ChannelAddress")));
    }

    @Test
    void channelAddressIsMissing() throws InterruptedException, IOException, ParserConfigurationException,
            ParseException, TransformerException, ConnectionException, ArgumentSyntaxException {

        TestLogger testLogger = new TestLogger(CsvDeviceConnection.class);

        DataManager dataManager = new DataManager();
        DataManagerAccessor.activateWithConfig(dataManager, "channelAddressIsMissing.xml");

        CsvDriver driver = new CsvDriver();
        DataManagerAccessor.bindDriverService(dataManager, driver);

        Thread.sleep(3_000);

        List<String> warnMessages = testLogger.getLoggedLinesWithLevel(Level.WARN);
        assertFalse(warnMessages.isEmpty());
        assertTrue(warnMessages.stream().anyMatch(msg -> msg.contains("hhmmss")));
        assertTrue(warnMessages.stream().anyMatch(msg -> msg.contains("No ChannelAddress")));
    }

    @Test
    void deviceAddressColumnMissingInCsv() throws IOException, ParserConfigurationException, ParseException,
            TransformerException, InterruptedException {

        TestLogger testLogger = new TestLogger(ConnectTask.class);

        DataManager dataManager = new DataManager();
        DataManagerAccessor.activateWithConfig(dataManager, "deviceAddressIsMissing.xml");

        CsvDriver driver = new CsvDriver();
        DataManagerAccessor.bindDriverService(dataManager, driver);

        Thread.sleep(500);

        List<String> warnMessages = testLogger.getLoggedLinesWithLevel(Level.WARN);
        assertFalse(warnMessages.isEmpty());
        assertTrue(warnMessages.stream().anyMatch(msg -> msg.contains("Unable to connect to device")));
        assertTrue(warnMessages.stream().anyMatch(msg -> msg.contains("home1")));
        assertTrue(warnMessages.stream().anyMatch(msg -> msg.contains("CSV driver - file not found")));
    }

    @Test
    void channelAddressColumnDoesNotExistInCsv() throws IOException, ParserConfigurationException, ParseException,
            TransformerException, InterruptedException {

        TestLogger testLogger = new TestLogger(CsvDeviceConnection.class);

        DataManager dataManager = new DataManager();
        DataManagerAccessor.activateWithConfig(dataManager, "channelAddressColumnDoesNotExistInCsv.xml");

        CsvDriver driver = new CsvDriver();
        DataManagerAccessor.bindDriverService(dataManager, driver);

        Thread.sleep(3_000);

        List<String> warnMessages = testLogger.getLoggedLinesWithLevel(Level.WARN);
        assertFalse(warnMessages.isEmpty());
        assertTrue(warnMessages.stream().anyMatch(msg -> msg.contains("this-column-does-not-exist")));
        assertTrue(warnMessages.stream().anyMatch(msg -> msg.contains("Invalid Channel Address:")));
    }

    @Test
    void deviceAddressColumnDoesNotExistInCsv() throws IOException, ParserConfigurationException, ParseException,
            TransformerException, InterruptedException {

        TestLogger testLogger = new TestLogger(ConnectTask.class);

        DataManager dataManager = new DataManager();
        DataManagerAccessor.activateWithConfig(dataManager, "deviceAddressDoesNotExist.xml");

        CsvDriver driver = new CsvDriver();
        DataManagerAccessor.bindDriverService(dataManager, driver);

        Thread.sleep(500);

        List<String> warnMessages = testLogger.getLoggedLinesWithLevel(Level.WARN);
        assertFalse(warnMessages.isEmpty());
        assertTrue(warnMessages.stream().anyMatch(msg -> msg.contains("Unable to connect to device")));
        assertTrue(warnMessages.stream().anyMatch(msg -> msg.contains("home1")));
        assertTrue(warnMessages.stream().anyMatch(msg -> msg.contains("CSV driver - file not found")));
    }

    @Test
    void settingsColumnDoesNotExistInCsv() throws IOException, ParserConfigurationException, ParseException,
            TransformerException, InterruptedException {

        TestLogger testLogger = new TestLogger(ConnectTask.class);

        DataManager dataManager = new DataManager();
        DataManagerAccessor.activateWithConfig(dataManager, "settingsColumnDoesNotExist.xml");

        CsvDriver driver = new CsvDriver();
        DataManagerAccessor.bindDriverService(dataManager, driver);

        Thread.sleep(500);

        List<String> warnMessages = testLogger.getLoggedLinesWithLevel(Level.WARN);
        assertFalse(warnMessages.isEmpty());
        assertTrue(warnMessages.stream().anyMatch(msg -> msg.contains("Unable to connect to device")));
        assertTrue(warnMessages.stream().anyMatch(msg -> msg.contains("home1")));
        assertTrue(warnMessages.stream().anyMatch(msg -> msg.contains("address or settings syntax is incorrect")));
    }

    @Test
    void settingsColumnMissingInCsv() throws IOException, ParserConfigurationException, ParseException,
            TransformerException, InterruptedException {

        TestLogger testLogger = new TestLogger(ConnectTask.class);

        DataManager dataManager = new DataManager();
        DataManagerAccessor.activateWithConfig(dataManager, "settingsIsMissing.xml");

        CsvDriver driver = new CsvDriver();
        DataManagerAccessor.bindDriverService(dataManager, driver);

        Thread.sleep(500);

        List<String> warnMessages = testLogger.getLoggedLinesWithLevel(Level.WARN);
        assertFalse(warnMessages.isEmpty());
        assertTrue(warnMessages.stream().anyMatch(msg -> msg.contains("Unable to connect to device")));
        assertTrue(warnMessages.stream().anyMatch(msg -> msg.contains("home1")));
        assertTrue(warnMessages.stream().anyMatch(msg -> msg.contains("address or settings syntax is incorrect")));
    }
}
