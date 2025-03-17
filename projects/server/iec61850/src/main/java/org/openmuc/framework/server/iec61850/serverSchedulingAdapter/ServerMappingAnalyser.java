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

import org.openmuc.framework.server.spi.ServerMappingContainer;

/**
 * Handles IEC 61850 channel naming conventions as described in the OpenMUC user manual. Also takes care of finding the
 * server name and schedule name from a given ServerMapping.
 */
public class ServerMappingAnalyser {

    private final String scheduleNameOnly;
    private final String serverName;
    private final String channelId;

    /**
     * Throws if no schedule
     */
    public ServerMappingAnalyser(ServerMappingContainer serverMappingContainer) throws NoScheduleMappingException {
        this(serverMappingContainer.getChannel().getId(),
                serverMappingContainer.getServerMapping().getServerAddress().toString());
    }

    public ServerMappingAnalyser(String channelId, String serverMappingString) throws NoScheduleMappingException {
        this.channelId = channelId;
        try {
            String scheduleNameOnly = serverMappingString.replaceAll(".+/", "");
            this.scheduleNameOnly = scheduleNameOnly.substring(0, scheduleNameOnly.indexOf("."));
            this.serverName = serverMappingString.substring(0, serverMappingString.indexOf("/"));

            getControllerGroup();// check it can be extracted. Rather throw early!
        } catch (Exception e) {
            throw new NoScheduleMappingException(channelId);
        }

    }

    public static class NoScheduleMappingException extends Exception {
        NoScheduleMappingException(String channelId) {
            super("Is no schedule mapping in channelId=" + channelId);
        }
    }

    public String getScheduleName() {
        return scheduleNameOnly;
    }

    public String getServerName() {
        return serverName;
    }

    public String getChannelId() {
        return channelId;
    }

    public String getControllerGroup() {
        try {
            int underscoreIndex = getChannelId().indexOf("_");
            String controllerNode = getChannelId().substring(0, underscoreIndex);
            return controllerNode;
        } catch (Exception e) {
            throw new RuntimeException("Unable to extract control group from " + getChannelId());
        }
    }

    public static boolean isControllerOutputMapping(ServerMappingContainer serverMappingContainer) {
        try {
            return new ServerMappingAnalyser(serverMappingContainer).isControllerOutputMapping();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isControllerOutputMapping() {
        return getChannelId().endsWith("SCHEDULE_CONTROLLER_OUTPUT");
    }

    public boolean isReserveSchedule() {
        return getChannelId().endsWith("RESERVE_SCHEDULE_VALUE");
    }

    public boolean isScheduleEnableMapping() {
        return getChannelId().endsWith("SCHEDULE_ENABLE");
    }

    public boolean isScheduleDisableMapping() {
        return getChannelId().endsWith("SCHEDULE_DISABLE");
    }
}
