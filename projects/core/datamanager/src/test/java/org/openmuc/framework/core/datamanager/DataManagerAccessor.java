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
package org.openmuc.framework.core.datamanager;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.openmuc.framework.config.ParseException;
import org.openmuc.framework.datalogger.spi.DataLoggerService;
import org.openmuc.framework.driver.spi.DriverService;
import org.openmuc.framework.server.spi.ServerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a test util to make {@link DataManager} usable in tests with custom configurations
 */
public class DataManagerAccessor {

    private static final Logger log = LoggerFactory.getLogger(DataManagerAccessor.class);

    private DataManagerAccessor() {
        // hide constructor, only static methods here
    }

    public static void activateWithConfig(DataManager dm, String configInTestResources)
            throws IOException, ParserConfigurationException, TransformerException, ParseException {
        File configFile = null;
        try {
            ClassLoader classLoader = DataManagerAccessor.class.getClassLoader();
            configFile = new File(classLoader.getResource(configInTestResources).toURI());
            log.info("Using file {}", configFile.getAbsolutePath());
        } catch (URISyntaxException e) {
            throw new IOException("file not found" + configInTestResources);
        }
        activateWithConfig(dm, configFile);
    }

    public static void activateWithConfig(DataManager dm, File configFile)
            throws IOException, ParserConfigurationException, TransformerException, ParseException {
        dm.activateWithConfig(configFile);
    }

    public static void bindDriverService(DataManager dataManager, DriverService driver) {
        dataManager.bindDriverService(driver);
    }

    public static void bindDataLoggerService(DataManager dataManager, DataLoggerService dataLogger) {
        dataManager.bindDataLoggerService(dataLogger);
    }

    public static void bindServerService(DataManager dataManager, ServerService serverService) {
        dataManager.bindServerService(serverService);
    }

    public static Collection<ServerService> getServerServices(DataManager dataManager) {
        return dataManager.serverServices.values();
    }
}
