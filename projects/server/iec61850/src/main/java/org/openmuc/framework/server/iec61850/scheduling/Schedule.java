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

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Represents all relevant parameters of a schedule as it is stored in IEC 61850 nodes
 */
public interface Schedule {

    /**
     * Number of control values (see {@link #getValues()}) in the schedule.
     */
    int getNumEntr();

    /**
     * Control values of the schedule executed in the right execution order
     */
    List<? extends Number> getValues(); //

    /**
     * Time duration between the values of a schedule
     */
    Duration getInterval();

    String getScheduleName();

    /**
     * start time of the schedule
     */
    Instant getStart();

    /**
     * the point in time when the schedule was enabled
     */
    Instant getEnabledAt();

    /**
     * priority of the schedule
     */
    int getPrio();

    ScheduleState getState(); // state of the schedule (could be running, ready, notReady, scheduleDone or
                              // startTimeRequired)

    void setState(ScheduleState scheduleState);

}
