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

import static org.openmuc.framework.server.iec61850.serverSchedulingAdapter.ServerModelAccess.from;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openmuc.framework.server.iec61850.scheduling.Schedule;
import org.openmuc.framework.server.iec61850.scheduling.ScheduleState;
import org.openmuc.framework.server.iec61850.serverSchedulingAdapter.IEC61850ReserveScheduleNodeSnapshot;
import org.openmuc.framework.server.iec61850.serverSchedulingAdapter.IEC61850ScheduleNodeSnapshot;
import org.openmuc.framework.server.iec61850.serverSchedulingAdapter.IEC61850ScheduleNodeSnapshot.IEC61850ServerConfigException;
import org.openmuc.framework.server.iec61850.serverSchedulingAdapter.ServerMappingAnalyser;

import com.beanit.iec61850bean.SclParseException;
import com.beanit.iec61850bean.SclParser;
import com.beanit.iec61850bean.ServerModel;

public class IEC61850ScheduleNodeSnapshotTest {

    ClassLoader classLoader = getClass().getClassLoader();

    @Test
    void createFSCHNodeFromCID() throws SclParseException, IEC61850ServerConfigException {
        ServerModel serverModel = SclParser.parse(classLoader.getResource("Test.cid").getPath()).get(0);
        Schedule node = new IEC61850ScheduleNodeSnapshot("ActPow_Schedule_FSCH01", "DER_Test_Server",
                from(serverModel));

        Assertions.assertEquals(1, node.getPrio());
        Assertions.assertEquals(20, node.getInterval().getSeconds());
        Assertions.assertEquals(5, node.getNumEntr());
        Assertions.assertEquals("ActPow_Schedule_FSCH01", node.getScheduleName());
        Assertions.assertEquals(Instant.ofEpochSecond(0), node.getStart());
        Assertions.assertEquals(ScheduleState.notReady, node.getState());
        List<Float> values = new ArrayList(Arrays.asList(0f, 0f, 0f, 0f, 0f));
        Assertions.assertArrayEquals(values.toArray(), node.getValues().toArray());
    }

    @Test
    void properExceptionIsThrownOnWrongServerName() throws SclParseException {
        ServerModel serverModel = SclParser.parse(classLoader.getResource("Test.cid").getPath()).get(0);
        try {
            new IEC61850ScheduleNodeSnapshot("ActPow_Schedule_FSCH01", "this is not the server name",
                    from(serverModel));
            Assertions.fail("The above should have thrown");
        } catch (IEC61850ServerConfigException e) {
            Assertions.assertTrue(e.getMessage().contains("this is not the server name"));
        } catch (Exception e) {
            Assertions.fail("Expected dedicated exception, " + e.getClass() + " is too generic");
        }
    }

    @Test
    void properExceptionIsThrownOnWrongScheduleName() throws SclParseException {
        ServerModel serverModel = SclParser.parse(classLoader.getResource("Test.cid").getPath()).get(0);
        try {
            new IEC61850ScheduleNodeSnapshot("this is not the schedule name", "DER_Test_Server", from(serverModel));
            Assertions.fail("The above should have thrown");
        } catch (IEC61850ServerConfigException e) {
            Assertions.assertTrue(e.getMessage().contains("this is not the schedule name"));
        } catch (Exception e) {
            Assertions.fail("Expected dedicated exception, " + e.getClass() + " is too generic");
        }
    }

    @Test
    void throwsIfReserveScheduleDoesNotHaveMatchingPriority() throws SclParseException {
        ServerModel serverModel = SclParser.parse(classLoader.getResource("bad-reserve-schedules.cid").getPath())
                .get(0);

        org.assertj.core.api.Assertions
                .assertThatThrownBy(
                        () -> new IEC61850ReserveScheduleNodeSnapshot(new ServerMappingAnalyser("does_not_matter",
                                "DER_Test_Server/bad-priorityFSCH01.ValASG001.setMag.f"), from(serverModel)))//
                .hasMessageContaining("priority 10");
    }

    @Test
    void throwsIfReserveScheduleHasOnly1Value() throws SclParseException {
        ServerModel serverModel = SclParser.parse(classLoader.getResource("bad-reserve-schedules.cid").getPath())
                .get(0);

        org.assertj.core.api.Assertions
                .assertThatThrownBy(
                        () -> new IEC61850ReserveScheduleNodeSnapshot(new ServerMappingAnalyser("does_not_matter",
                                "DER_Test_Server/too-many-valuesFSCH01.ValASG001.setMag.f"), from(serverModel)))//
                .hasMessageContaining("exactly 1 value");
    }

    @Test
    void badSclValuesArrayTooShort() throws SclParseException {
        ServerModel serverModel = SclParser.parse(classLoader.getResource("bad-schedules.cid").getPath()).get(0);

        org.assertj.core.api.Assertions
                .assertThatThrownBy(() -> new IEC61850ReserveScheduleNodeSnapshot(
                        new ServerMappingAnalyser("does_not_matter",
                                "DER_Test_Server/too-many-values-declaredFSCH01.ValASG001.setMag.f"),
                        from(serverModel)))//
                .hasMessageContaining("6 control values");
    }
}
