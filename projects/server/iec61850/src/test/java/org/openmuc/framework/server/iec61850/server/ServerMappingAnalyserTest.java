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

import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openmuc.framework.server.iec61850.serverSchedulingAdapter.ServerMappingAnalyser;

public class ServerMappingAnalyserTest {

    final String someServerMapping = "Test-server/ActPow_FSCC1.ValMV.mag.f:MX";

    @Test
    void testNamingConventionC() throws ServerMappingAnalyser.NoScheduleMappingException {
        ServerMappingAnalyser enable = new ServerMappingAnalyser("junit-testing-controller_SCHEDULE_ENABLE",
                someServerMapping);
        ServerMappingAnalyser disable = new ServerMappingAnalyser("junit-testing-controller_SCHEDULE_DISABLE",
                someServerMapping);
        ServerMappingAnalyser controller = new ServerMappingAnalyser(
                "junit-testing-controller_SCHEDULE_CONTROLLER_OUTPUT", someServerMapping);
        ServerMappingAnalyser reserveSched = new ServerMappingAnalyser(
                "junit-testing-controller_RESERVE_SCHEDULE_VALUE", someServerMapping);
        ServerMappingAnalyser whatever = new ServerMappingAnalyser("junit-testing-controller_DOES_NOT_MATTER_ANYWAYS",
                someServerMapping);

        for (ServerMappingAnalyser analyser : Arrays.asList(enable, disable, controller, reserveSched, whatever)) {
            Assertions.assertEquals("junit-testing-controller", analyser.getControllerGroup());
        }
    }

    @Test
    void testNamingConventionScheduleEnable() throws ServerMappingAnalyser.NoScheduleMappingException {
        ServerMappingAnalyser enable = new ServerMappingAnalyser("asd_SCHEDULE_ENABLE", someServerMapping);
        Assertions.assertTrue(enable.isScheduleEnableMapping());

        Assertions.assertFalse(enable.isControllerOutputMapping());
        Assertions.assertFalse(enable.isScheduleDisableMapping());
        Assertions.assertFalse(enable.isReserveSchedule());
    }

    @Test
    void testNamingConventionScheduleDisable() throws ServerMappingAnalyser.NoScheduleMappingException {
        ServerMappingAnalyser disable = new ServerMappingAnalyser("asd_SCHEDULE_DISABLE", someServerMapping);
        Assertions.assertTrue(disable.isScheduleDisableMapping());

        Assertions.assertFalse(disable.isScheduleEnableMapping());
        Assertions.assertFalse(disable.isControllerOutputMapping());
        Assertions.assertFalse(disable.isReserveSchedule());
    }

    @Test
    void testNamingConventionController() throws ServerMappingAnalyser.NoScheduleMappingException {
        ServerMappingAnalyser output = new ServerMappingAnalyser("asd_SCHEDULE_CONTROLLER_OUTPUT", someServerMapping);
        Assertions.assertTrue(output.isControllerOutputMapping());

        Assertions.assertFalse(output.isScheduleEnableMapping());
        Assertions.assertFalse(output.isScheduleDisableMapping());
        Assertions.assertFalse(output.isReserveSchedule());
    }

    @Test
    void testNamingConventionCReserveSchedule() throws ServerMappingAnalyser.NoScheduleMappingException {
        ServerMappingAnalyser reserveSchedule = new ServerMappingAnalyser("asd_RESERVE_SCHEDULE_VALUE",
                someServerMapping);
        Assertions.assertTrue(reserveSchedule.isReserveSchedule());

        Assertions.assertFalse(reserveSchedule.isScheduleEnableMapping());
        Assertions.assertFalse(reserveSchedule.isScheduleDisableMapping());
        Assertions.assertFalse(reserveSchedule.isControllerOutputMapping());
    }

    @Test
    void analysingServerAndScheduleNameWorksAsExpected() throws ServerMappingAnalyser.NoScheduleMappingException {
        String serverMapping = "Test-server/ActPow_FSCC1.ValMV.mag.f:MX";
        ServerMappingAnalyser serverMappingAnalyser = new ServerMappingAnalyser("what_ever", serverMapping);
        Assertions.assertEquals("Test-server", serverMappingAnalyser.getServerName());
        Assertions.assertEquals("ActPow_FSCC1", serverMappingAnalyser.getScheduleName());
    }

}
