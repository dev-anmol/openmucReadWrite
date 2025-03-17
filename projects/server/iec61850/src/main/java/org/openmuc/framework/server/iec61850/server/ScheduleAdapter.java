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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.openmuc.framework.dataaccess.DataAccessService;
import org.openmuc.framework.server.iec61850.scheduling.IEC61850ScheduleController;
import org.openmuc.framework.server.iec61850.serverSchedulingAdapter.IEC61850ReserveScheduleNodeSnapshot;
import org.openmuc.framework.server.iec61850.serverSchedulingAdapter.ScheduleDisableListener;
import org.openmuc.framework.server.iec61850.serverSchedulingAdapter.ScheduleEnableListener;
import org.openmuc.framework.server.iec61850.serverSchedulingAdapter.ScheduleOutputTarget;
import org.openmuc.framework.server.iec61850.serverSchedulingAdapter.ServerMappingAnalyser;
import org.openmuc.framework.server.iec61850.serverSchedulingAdapter.ServerModelAccess;
import org.openmuc.framework.server.spi.ServerMappingContainer;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;

import com.beanit.iec61850bean.ServerModel;

/**
 * Single instance as compared to server. Creates scheduling here to avoid duplicated schedule controllers. Links server
 * functionality with scheduling functionality
 */
public class ScheduleAdapter {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(ScheduleAdapter.class);

    Map<String, IEC61850ScheduleController> controllers = new HashMap<>();
    private final ServerModel serverModel;

    public ScheduleAdapter(DataAccessService dataAccessService, Iec61850Server server) {
        log.debug("Starting to set up scheduling");
        serverModel = Iec61850Server.serverModel;

        Collection<ScheduleEnableListener> enableListener = new LinkedList<>();
        Collection<ScheduleDisableListener> disableListener = new LinkedList<>();

        try {
            // create a controller for every FSCC node
            for (ServerMappingContainer mapping : Iec61850Server.storedMappings) {
                if (ServerMappingAnalyser.isControllerOutputMapping(mapping)) {
                    ServerMappingAnalyser analyser = new ServerMappingAnalyser(mapping);
                    ScheduleOutputTarget scheduleOutputTarget = new ScheduleOutputTarget(mapping.getChannel());

                    Optional<ServerMappingContainer> hasReserveSchedule = Iec61850Server.storedMappings.stream()
                            .filter(smc -> {
                                try {
                                    ServerMappingAnalyser serverMappingAnalyser = new ServerMappingAnalyser(smc);
                                    boolean reserveSchedule = serverMappingAnalyser.isReserveSchedule();
                                    boolean belongsToController = analyser.getControllerGroup()
                                            .equals(serverMappingAnalyser.getControllerGroup());
                                    return reserveSchedule && belongsToController;
                                } catch (ServerMappingAnalyser.NoScheduleMappingException e) {
                                    return false;
                                }
                            })
                            .findFirst();

                    IEC61850ScheduleController controller = null;
                    if (hasReserveSchedule.isPresent()) {
                        try {
                            log.info("Processing controller {} with reserve schedule {}", mapping.getChannel().getId(),
                                    hasReserveSchedule.get().getChannel().getId());
                            IEC61850ReserveScheduleNodeSnapshot reserveSchedule = new IEC61850ReserveScheduleNodeSnapshot(
                                    new ServerMappingAnalyser(hasReserveSchedule.get()),
                                    ServerModelAccess.from(serverModel));
                            controller = new IEC61850ScheduleController(scheduleOutputTarget::newOutputTarget,
                                    reserveSchedule.asRunningSchedule());
                            log.info("Found reserve schedule mapping with channelId={}, read reserve schedule {}",
                                    hasReserveSchedule.get().getChannel().getId(), reserveSchedule);
                        } catch (Exception e) {
                            log.error("Unable to read reserve schedule from channelId={} mapping: {}:{}",
                                    mapping.getChannel().getId(), e.getClass().getSimpleName(), e.getMessage(), e);
                            continue;
                        }
                    }
                    else {
                        controller = new IEC61850ScheduleController(scheduleOutputTarget::newOutputTarget);
                    }

                    if (controller != null) {
                        controllers.put(analyser.getControllerGroup(), controller);
                        log.debug("Added channel {} as iec 61850 controller output", analyser.getChannelId());
                    }
                }
            }
            // all controllers are created, now search for enable and disable channels
            for (ServerMappingContainer mapping : Iec61850Server.storedMappings) {
                try {
                    ServerMappingAnalyser analyser = new ServerMappingAnalyser(mapping);

                    // if channel is an enable channel we map it to the corresponding controller
                    if (analyser.isScheduleEnableMapping()) {
                        ScheduleEnableListener listener = new ScheduleEnableListener(
                                controllers.get(analyser.getControllerGroup()), serverModel, analyser);
                        mapping.getChannel().addListener(listener);
                        enableListener.add(listener);

                        log.debug("Added schedule enable listener for channel {}", analyser.getChannelId());
                    }
                    // if channel is an disable channel we map it to the corresponding controller
                    else if (analyser.isScheduleDisableMapping()) {
                        ScheduleDisableListener listener = new ScheduleDisableListener(analyser.getScheduleName(),
                                controllers.get(analyser.getControllerGroup()), dataAccessService);
                        mapping.getChannel().addListener(listener);
                        disableListener.add(listener);

                        log.debug("Added schedule disable listener for channel {}", analyser.getChannelId());
                    }
                    else {
                        if (!analyser.isReserveSchedule() && !analyser.isControllerOutputMapping()) {
                            log.info(
                                    "Ignoring channel {} for scheduling mapping. Assuming this is a normal server mapping.",
                                    analyser.getChannelId());
                        }
                    }
                } catch (ServerMappingAnalyser.NoScheduleMappingException e) {
                    // this is just a regular server mapping we can safely ignore
                }
            }
        } catch (Exception e) {
            stop();
            log.error("Unable to start IEC 61850 scheduling", e);
        }

        ValidityChecker.validityCheck(Collections.unmodifiableMap(controllers), enableListener, disableListener);
        ValidityChecker.checkAllScheduleValuesAreAvailable(enableListener);

        log.info("Done setting up scheduling");
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        stop();
    }

    public void stop() {
        for (Map.Entry<String, IEC61850ScheduleController> controller : controllers.entrySet()
                .stream()
                .collect(Collectors.toSet())) {
            if (controller.getValue() != null) {
                controller.getValue().shutdown();
                log.info("Shut down controller");
            }
        }
        log.info("Schedule adapter deactivated gracefully");
    }

}
