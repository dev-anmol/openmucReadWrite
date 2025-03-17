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

import java.io.File;
import java.lang.reflect.Field;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.felix.framework.util.MapToDictionary;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openmuc.framework.core.datamanager.DataManager;
import org.openmuc.framework.core.datamanager.DataManagerAccessor;
import org.openmuc.framework.lib.osgi.config.DictionaryPreprocessor;
import org.openmuc.framework.lib.osgi.config.PropertyFileValidator;
import org.openmuc.framework.lib.osgi.config.PropertyHandler;
import org.openmuc.framework.lib.osgi.config.ServiceProperty;
import org.openmuc.framework.lib.osgi.config.ServicePropertyException;
import org.openmuc.framework.server.spi.ServerService;

public class ConfigParsingTest {

    @Test
    void channelXmlHasSeveralFSCCs() {
        DataManager dataManager = IEC61850ServerReadWriteAccessTest.startServer("src/test/resources/Test.cid",
                "channels-several-fsccs.xml");

        ScheduleAdapter scheduleAdapter = getScheduleAdapter(dataManager);
        List<String> channelStrings = dataManager.getAllIds();
        Assertions.assertEquals(scheduleAdapter.controllers.size(),
                channelStrings.stream().filter(e -> e.contains("FSCC")).count());
        Assertions.assertNotNull(scheduleAdapter.controllers.get("ActPow"));
        Assertions.assertNotNull(scheduleAdapter.controllers.get("ReactPow"));
    }

    @Test
    void serverCfgIsParsedCorrectly()
            throws NoSuchFieldException, IllegalAccessException, ServicePropertyException, InterruptedException {

        PropertyHandler propertyHandler = read61850ServerPropertiesFromTestResourcesFolder();

        // the actual test: verify values from
        // test/resources/org.openmuc.framework.server.iec61850.server.Iec61850Server.cfg file have been read
        Map<String, ServiceProperty> properties = propertyHandler.getCurrentProperties();
        assertThat(properties).containsOnlyKeys("port", "sclPath", "schedulingEnabled");
        assertThat(propertyHandler.getInt("port")).isEqualTo(1337);
        assertThat(propertyHandler.getString("sclPath")).isEqualTo("conf/my-very-own-config.cid");
        assertThat(propertyHandler.getString("schedulingEnabled")).isEqualTo("false");
    }

    private static ScheduleAdapter getScheduleAdapter(DataManager dataManager) {
        Optional<ServerService> maybe61850Server = DataManagerAccessor.getServerServices(dataManager)
                .stream()
                .filter(s -> s instanceof Iec61850Server)
                .findFirst();

        try {
            Field scheduleAdapterField = Iec61850Server.class.getDeclaredField("scheduleAdapter");
            scheduleAdapterField.setAccessible(true);
            ScheduleAdapter scheduleAdapter = (ScheduleAdapter) scheduleAdapterField.get(maybe61850Server.get());
            return scheduleAdapter;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Unable to find or access schedule adapter member. Was it renamed?");
        }
    }

    private static PropertyHandler read61850ServerPropertiesFromTestResourcesFolder()
            throws NoSuchFieldException, IllegalAccessException, ServicePropertyException, InterruptedException {

        // SET THE PATH FIRST!, e.g. PropertyFileValidator will only read once from Sys property, then cache the value!
        System.setProperty("felix.fileinstall.dir", new File("src/test/resources").getAbsolutePath());

        String pid = Iec61850Server.class.getName();
        IEC61850ServerSettings settings = new IEC61850ServerSettings();
        PropertyHandler propertyHandler = new PropertyHandler(settings, pid);

        PropertyFileValidator propertyFileValidator = new PropertyFileValidator();
        propertyFileValidator.initServiceProperties(settings.getProperties(), pid);

        Field existingPropertiesList = PropertyFileValidator.class.getDeclaredField("existingProperties");
        existingPropertiesList.setAccessible(true);
        propertyFileValidator.initServiceProperties(settings.getProperties(), pid);
        List<String> newPropsFromFile = (List<String>) existingPropertiesList.get(propertyFileValidator);
        Map<Object, Object> newValuesMap = newPropsFromFile.stream().filter(s -> !s.startsWith("#")).map(s -> {
            String[] split = s.split("=");
            if (split.length == 2) {
                return new AbstractMap.SimpleImmutableEntry(split[0], split[1]);
            }
            else {
                return null;
            }
        }).filter(e -> e != null).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        MapToDictionary mapToDictionary = new MapToDictionary(newValuesMap);
        propertyHandler.processConfig(new DictionaryPreprocessor(mapToDictionary));
        return propertyHandler;
    }
}
