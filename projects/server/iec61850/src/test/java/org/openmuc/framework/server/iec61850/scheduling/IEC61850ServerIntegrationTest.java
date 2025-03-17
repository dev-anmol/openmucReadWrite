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
package org.openmuc.framework.server.iec61850.scheduling;

import java.io.IOException;
import java.lang.reflect.Field;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openmuc.framework.core.datamanager.DataManager;
import org.openmuc.framework.core.datamanager.DataManagerAccessor;
import org.openmuc.framework.lib.osgi.config.PropertyHandler;
import org.openmuc.framework.server.iec61850.IEC61850ClientScheduleWriterExampleApp;
import org.openmuc.framework.server.iec61850.server.IEC61850ServerReadWriteAccessTest;
import org.openmuc.framework.server.iec61850.server.Iec61850Server;
import org.openmuc.framework.server.iec61850.server.TestIEC61850ServerSettings;

import com.beanit.iec61850bean.ServiceError;

public class IEC61850ServerIntegrationTest {

    final int port;
    final DataManager dataManager;

    public IEC61850ServerIntegrationTest() throws NoSuchFieldException, IllegalAccessException {
        dataManager = IEC61850ServerReadWriteAccessTest.startServer("src/test/resources/Test.cid", "channels.xml");
        port = getPortOfRunningServer(dataManager);
    }

    /**
     * Covers:
     * <p>
     * - parsing of channel mappings
     * <p>
     * - reading from server via channels OpenMUC channels
     * <p>
     * - simple scheduling
     */
    @Test
    void integrationTest() throws ServiceError, IOException, InterruptedException {

        // parsing works as expected: we have 4 mappings available
        Assertions.assertEquals(5, dataManager.getAllIds().size());

        // reading from the server via channel works as expected
        Assertions.assertEquals("OpenMUC dummy junit integration test server",
                dataManager.getChannel("test-channel-vendor")//
                        .getLatestRecord()//
                        .getValue()//
                        .asString());
        // also works via 61850 client
        Assertions.assertEquals("1.33.7",
                IEC61850ServerReadWriteAccessTest
                        .readFromLocalServerWithIEC61850Client(port, "DER_Test_Server/LLN0.NamPlt")
                        .getChild("swRev")
                        .getBasicDataAttributes()
                        .get(0)
                        .getValueString());

        // the default schedule should be active now with a value of 0 at the output
        double schedDefaultValue = 123d;
        Assertions.assertEquals(schedDefaultValue, readControllerOutput());
        // TODO: DER_Test_Server/ActPow_Schedule_FSCH01.ValMV.mag.f should be updated here, too (but not implemented
        // yet)

        try (IEC61850ClientScheduleWriterExampleApp.IEC61850ScheduleAccess scheduleAccess = new IEC61850ClientScheduleWriterExampleApp.IEC61850ScheduleAccess(
                "localhost", port)) {

            Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);

            Instant start = now.plusSeconds(1);
            Duration interval = Duration.ofSeconds(2);
            double schedValue1 = -42d;
            double schedValue2 = -1337d;
            double schedValue3 = 1337d;
            scheduleAccess.writeAndEnableSchedule(Arrays.asList(schedValue1, schedValue2, schedValue3),
                    "DER_Test_Server/ActPow_Schedule_FSCH01", interval, start, 22);

            Thread.sleep(Math.max(Duration.between(Instant.now(), start).toMillis(), 0));
            // now we should have the first value
            Assertions.assertEquals(schedValue1, readControllerOutput());
            // TODO: DER_Test_Server/ActPow_Schedule_FSCH01.ValMV.mag.f should be updated here, too (but not implemented
            // yet)

            Thread.sleep(Math.max(Duration.between(Instant.now(), start.plus(interval.multipliedBy(1))).toMillis(), 0));
            Assertions.assertEquals(schedValue2, readControllerOutput());
            // TODO: DER_Test_Server/ActPow_Schedule_FSCH01.ValMV.mag.f should be updated here, too (but not implemented
            // yet)

            Thread.sleep(Math.max(Duration.between(Instant.now(), start.plus(interval.multipliedBy(2))).toMillis(), 0));
            Assertions.assertEquals(schedValue3, readControllerOutput());
            // TODO: DER_Test_Server/ActPow_Schedule_FSCH01.ValMV.mag.f should be updated here, too (but not implemented
            // yet)

            Thread.sleep(Math.max(Duration.between(Instant.now(), start.plus(interval.multipliedBy(3))).toMillis(), 0));
            Assertions.assertEquals(schedDefaultValue, readControllerOutput());
            // TODO: DER_Test_Server/ActPow_Schedule_FSCH01.ValMV.mag.f should be updated here, too (but not implemented
            // yet)
        }
    }

    private double readControllerOutput() throws ServiceError, IOException {
        return IEC61850ServerReadWriteAccessTest.readDoubleFromLocalServerWithIEC61850Client(port,
                "DER_Test_Server/ActPow_FSCC1.ValMV.mag.f");
    }

    private static int getPortOfRunningServer(DataManager dataManager)
            throws NoSuchFieldException, IllegalAccessException {
        Iec61850Server serverService = (Iec61850Server) DataManagerAccessor.getServerServices(dataManager)
                .stream()
                .filter(s -> s instanceof Iec61850Server)
                .findFirst()
                .get();
        Field propertyHandlerField = Iec61850Server.class.getDeclaredField("propertyHandler");
        propertyHandlerField.setAccessible(true);
        PropertyHandler propertyHandler = (PropertyHandler) propertyHandlerField.get(serverService);
        return TestIEC61850ServerSettings.getPort(propertyHandler);
    }
}
