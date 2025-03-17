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
package org.openmuc.framework.server.iec61850.serverSchedulingAdapter;

import java.time.Instant;

import org.openmuc.framework.server.iec61850.scheduling.IEC61850ScheduleController;
import org.openmuc.framework.server.iec61850.scheduling.RunningSchedule;
import org.openmuc.framework.server.iec61850.scheduling.ScheduleEntityImpl;
import org.slf4j.Logger;

/**
 * A reserve schedule has special requirements that are checked here
 */
public class IEC61850ReserveScheduleNodeSnapshot extends IEC61850ScheduleNodeSnapshot {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(IEC61850ReserveScheduleNodeSnapshot.class);

    public IEC61850ReserveScheduleNodeSnapshot(ServerMappingAnalyser serverMappingContainer,
            ServerModelAccess serverModelAccess) throws IEC61850ServerConfigException {
        super(serverMappingContainer, serverModelAccess);
        if (getNumEntr() != 1) {
            throw new IllegalArgumentException("Expecting exactly 1 value for reserve schedule");
        }
        if (getStart().getEpochSecond() != 1) {
            log.info("Expected reserve schedule to start at {}. Ignoring start {}.", Instant.ofEpochSecond(1),
                    getStart());
        }
        if (getPrio() != 10) {
            throw new IllegalArgumentException("Reserve schedules must have priority 10");

        }
        log.debug("Created reserve schedule from {}", getScheduleName());
    }

    public RunningSchedule asRunningSchedule() {
        return new ScheduleEntityImpl(getValues().get(0).doubleValue(),
                IEC61850ScheduleController.RESERVE_SCHEDULE_NAME, getPrio(), Instant.now(), false, true, Instant.MAX);
    }
}
