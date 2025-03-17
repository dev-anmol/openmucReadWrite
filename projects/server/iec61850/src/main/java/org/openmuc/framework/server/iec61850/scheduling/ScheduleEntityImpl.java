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

import java.time.Instant;

public class ScheduleEntityImpl implements RunningSchedule {
    double currentOutputValue;
    String scheduleName;
    int prio;
    Instant start;
    boolean lastInterval;
    boolean reserveSchedule;
    Instant enabledAt;

    public ScheduleEntityImpl(double currentOutputValue, String scheduleName, int prio, Instant start,
            boolean lastInterval, boolean reserveSchedule, Instant enabledAt) {
        this.currentOutputValue = currentOutputValue;
        this.scheduleName = scheduleName;
        this.prio = prio;
        this.start = start;
        this.lastInterval = lastInterval;
        this.reserveSchedule = reserveSchedule;
        this.enabledAt = enabledAt;
    }

    @Override
    public void setLastInterval() {
        lastInterval = true;
    }

    @Override
    public boolean isInLastExecutionInterval() {
        return lastInterval;
    }

    @Override
    public boolean isReserveSchedule() {
        return reserveSchedule;
    }

    @Override
    public String toString() {
        return "ScheduleEntityImpl{" + "currentOutputValue=" + currentOutputValue + ", scheduleName='" + scheduleName
                + '\'' + ", prio=" + prio + ", start=" + start + ", lastInterval=" + lastInterval + ", reserveSchedule="
                + reserveSchedule + ", enabledAt=" + enabledAt + '}';
    }

    @Override
    public double getCurrentOutputValue() {
        return this.currentOutputValue;
    }

    @Override
    public String getScheduleName() {
        return this.scheduleName;
    }

    @Override
    public int getPrio() {
        return this.prio;
    }

    @Override
    public Instant getStart() {
        return this.start;
    }

    @Override
    public Instant getEnabledAt() {
        return this.enabledAt;
    }
}
