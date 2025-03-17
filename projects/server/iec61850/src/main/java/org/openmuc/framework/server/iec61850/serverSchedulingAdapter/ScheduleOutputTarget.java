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

import java.util.Objects;

import org.openmuc.framework.data.FloatValue;
import org.openmuc.framework.dataaccess.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Takes care of forwarding the result calculated by the scheduling to the IEC 61850 nodes.
 * <p>
 * Also takes care that the logging can be done in a non-verbose way: only value changes are logged by default.
 */
public class ScheduleOutputTarget {

    private static final Logger log = LoggerFactory.getLogger(ScheduleOutputTarget.class);

    private final Channel channel;

    private Integer lastPrio = null;
    private Float lastValue = null;

    public ScheduleOutputTarget(Channel channel) {
        this.channel = channel;
    }

    public void newOutputTarget(Float outputvalue, Integer outputPrio) {
        if (!Objects.equals(this.lastValue, outputvalue)) {
            if (!Objects.equals(this.lastPrio, outputPrio)) {
                log.info("Output value for {} changed to {}, new prio {}", channel.getId(), outputvalue, outputPrio);
            }
            else {
                log.info("Output value for {} changed to {}, prio still at {}", channel.getId(), outputvalue,
                        outputPrio);
            }
        }
        else {
            if (!Objects.equals(this.lastPrio, outputPrio)) {
                log.debug("Output value for {} remains to {}, new prio {}", channel.getId(), outputvalue, outputPrio);
            }
            else {
                log.trace("Output value for {} remains at {}, prio still at {}", channel.getId(), outputvalue,
                        outputPrio);
            }
        }

        try {
            channel.write(new FloatValue(outputvalue));
        } catch (Exception e) {
            log.error("Unable to write new output value {} to channel {}", outputvalue, channel);
        } finally {
            this.lastValue = outputvalue;
            this.lastPrio = outputPrio;
        }
    }
}
