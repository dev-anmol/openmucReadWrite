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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import org.openmuc.framework.server.iec61850.scheduling.IEC61850ScheduleController;
import org.openmuc.framework.server.iec61850.serverSchedulingAdapter.ScheduleDisableListener;
import org.openmuc.framework.server.iec61850.serverSchedulingAdapter.ScheduleEnableListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValidityChecker {

    private static final Logger log = LoggerFactory.getLogger(ValidityChecker.class);

    private ValidityChecker() {
        // Util class: only static methods
    }

    public static void checkAllScheduleValuesAreAvailable(Collection<ScheduleEnableListener> collect) {
        for (ScheduleEnableListener potentialSchedule : collect) {
            try {
                potentialSchedule.getAction();
                log.debug("Schedule {} ok: all values available", potentialSchedule.getScheduleName());
            } catch (Exception e) {
                log.warn("Schedule {} is missing required values!", potentialSchedule.getScheduleName());
            }
        }
    }

    public static void validityCheck(Map<String, IEC61850ScheduleController> controllers,
            Collection<ScheduleEnableListener> enableListener, Collection<ScheduleDisableListener> disableListener) {

        ConcurrentMap<IEC61850ScheduleController, List<ScheduleEnableListener>> enableListenersByController = enableListener
                .stream()
                .collect(Collectors.groupingByConcurrent(ScheduleEnableListener::getController));

        ConcurrentMap<IEC61850ScheduleController, List<ScheduleDisableListener>> disableListenersByController = disableListener
                .stream()
                .collect(Collectors.groupingByConcurrent(ScheduleDisableListener::getController));

        for (Map.Entry<String, IEC61850ScheduleController> controllerEntry : controllers.entrySet()) {
            IEC61850ScheduleController controller = controllerEntry.getValue();
            List<ScheduleEnableListener> enableListeners = enableListenersByController.get(controller);
            List<ScheduleDisableListener> disableListeners = disableListenersByController.get(controller);
            if (enableListeners != null && disableListeners != null
                    && enableListeners.size() == disableListeners.size()) {
                if (enableListeners.size() > 0) {
                    log.info("Configuration ok for FSCC defined in channel {}: found {} associated schedules",
                            controllerEntry.getKey(), enableListeners.size());
                }
                else {
                    log.warn("Did not find any schedules assigned to FSCC node {}", controllerEntry.getKey());
                }
            }
            else {
                log.warn(
                        "Bad configuration: found {} schedule enable channels but {} schedule disable channels for FSCC {}. Equal amount enable/disable schedule channels required.",
                        enableListeners == null ? 0 : enableListener.size(),
                        disableListeners == null ? 0 : disableListeners.size(), controllerEntry.getKey());
            }
        }

        for (IEC61850ScheduleController controller : controllers.values()) {
            enableListenersByController.remove(controller);
            disableListenersByController.remove(controller);
        }

        for (List<ScheduleEnableListener> listenerWithoutController : enableListenersByController.values()) {
            log.warn("Found schedule enable channels that do not belong to a FSCC schedule controller: {}",
                    listenerWithoutController);
        }
        for (List<ScheduleDisableListener> listenerWithoutController : disableListenersByController.values()) {
            log.warn("Found schedule disable channels that do not belong to a FSCC schedule controller: {}",
                    listenerWithoutController);
        }
    }
}
