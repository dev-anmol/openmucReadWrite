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

import static java.lang.Integer.parseInt;
import static java.lang.Long.parseLong;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.openmuc.framework.config.ParseException;
import org.openmuc.framework.server.iec61850.scheduling.Schedule;
import org.openmuc.framework.server.iec61850.scheduling.ScheduleState;
import org.openmuc.framework.server.iec61850.serverSchedulingAdapter.ServerModelAccess.IEC61850NodeNotFoundException;
import org.slf4j.Logger;

import com.beanit.iec61850bean.Fc;
import com.beanit.iec61850bean.ServerModel;

/**
 * Implementation of the @IEC61850FSCHNode. Whenever an EnaReq value of anode is set to true in the IEC61850-Server,
 * this reads the current values of the node in the server. Uses @validityCheck method to check if schedule values are
 * valid
 */
public class IEC61850ScheduleNodeSnapshot implements Schedule {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(IEC61850ScheduleNodeSnapshot.class);
    private final int prio;
    private final List<Float> values;
    private final Duration interval;
    private final Instant start;
    private final String serverName;
    private final String scheduleName;
    private ScheduleState state;
    private final int numEntr;
    private final Instant enabledAt;

    public IEC61850ScheduleNodeSnapshot(ServerMappingAnalyser serverMappingAnalyser, ServerModel serverModel)
            throws IEC61850ServerConfigException {
        this(serverMappingAnalyser, ServerModelAccess.from(serverModel));
    }

    public IEC61850ScheduleNodeSnapshot(ServerMappingAnalyser serverMappingAnalyser, ServerModelAccess serverModel)
            throws IEC61850ServerConfigException {
        this(serverMappingAnalyser.getScheduleName(), serverMappingAnalyser.getServerName(), serverModel);
    }

    public IEC61850ScheduleNodeSnapshot(String scheduleName, String serverName, ServerModelAccess serverModel)
            throws IEC61850ServerConfigException {
        this.enabledAt = Instant.now();

        this.serverName = serverName;
        this.scheduleName = scheduleName;

        final String serverSchedule = serverName + "/" + scheduleName;

        if (!serverModel.checkNodeExists(serverName)) {
            throw new IEC61850ServerConfigException("Server node with name '" + serverName
                    + "' not found in IEC 61850 server configuration. Aborting.");
        }

        if (!serverModel.checkNodeExists(serverSchedule)) {
            throw new IEC61850ServerConfigException("Schedule node with name '" + scheduleName
                    + "' not found in IEC 61850 server configuration. Checking child elements of server '" + serverName
                    + "'. Aborting.");
        }

        prio = parseInt(serverModel.readModelNode(serverSchedule + ".SchdPrio.stVal", Fc.CF));
        interval = Duration.ofSeconds(parseInt(serverModel.readModelNode(serverSchedule + ".SchdIntv.stVal", Fc.CF)));
        long startEpochSecond = parseLong(
                serverModel.readModelNode(serverSchedule + ".StrTm.t.SecondSinceEpoch", Fc.CF));
        start = Instant.ofEpochSecond(startEpochSecond);
        if (Boolean.parseBoolean(serverModel.readModelNode(serverSchedule + ".EnaReq.Oper.ctlVal", Fc.CO))) {
            state = ScheduleState.ready;
        }
        else {
            state = ScheduleState.notReady;
        }

        this.numEntr = parseInt(serverModel.readModelNode(serverSchedule + ".NumEntr.stVal", Fc.CF));
        this.values = new ArrayList<>();

        try {
            for (int i = 1; i <= numEntr; i++) {

                final String objectReference = serverName + "/" + scheduleName + ".ValASG" + String.format("%03d", i)
                        + ".setMag";
                log.trace("processing {}, looking for node {}", i, objectReference);
                Float value = Float.parseFloat(serverModel.readModelNode(objectReference, Fc.SP));
                values.add(value);
                log.debug("Found value {} for {} (total {} entries)", value, objectReference, numEntr);
            }
        } catch (IEC61850NodeNotFoundException e) {
            throw new IEC61850ServerConfigException("Error occured reading " + numEntr + " control values from '"
                    + serverSchedule + "'. Node not found:" + e.nodeName);
        }
        log.debug("Read schedule {}. Start {} interval {} prio {} values {}", scheduleName, start, interval, prio,
                values);
    }

    @Override
    public String getScheduleName() {
        return scheduleName;
    }

    @Override
    public ScheduleState getState() {
        return state;
    }

    @Override
    public void setState(ScheduleState scheduleState) {
        this.state = scheduleState;
    }

    @Override
    public int getNumEntr() {
        return numEntr;
    }

    @Override
    public String toString() {
        return "IEC61850ScheduleNodeSnapshot{" + "serverName='" + serverName + '\'' + ", prio=" + prio + ", values="
                + values + ", interval=" + interval + ", scheduleName='" + scheduleName + '\'' + ", start=" + start
                + ", state=" + state + ", numEntr=" + numEntr + ", enabledAt=" + enabledAt + '}';
    }

    @Override
    public int getPrio() {
        return this.prio;
    }

    @Override
    public List<Float> getValues() {
        return this.values;
    }

    @Override
    public Duration getInterval() {
        return this.interval;
    }

    @Override
    public Instant getStart() {
        return this.start;
    }

    public String getServerName() {
        return this.serverName;
    }

    @Override
    public Instant getEnabledAt() {
        return this.enabledAt;
    }

    /**
     * Shall be thrown when there is a configuration either in the Server config file or a missmatch of channels.xml and
     * the server configuration file.
     */
    public static class IEC61850ServerConfigException extends ParseException {
        public IEC61850ServerConfigException(String message) {
            super(message);
        }
    }

}
