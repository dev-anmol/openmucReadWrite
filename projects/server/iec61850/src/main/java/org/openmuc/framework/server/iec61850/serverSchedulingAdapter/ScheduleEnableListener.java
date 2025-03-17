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

import org.openmuc.framework.data.Record;
import org.openmuc.framework.dataaccess.RecordListener;
import org.openmuc.framework.server.iec61850.scheduling.IEC61850ScheduleController;
import org.openmuc.framework.server.iec61850.scheduling.IEC61850ScheduleController.InvalidScheduleException;
import org.openmuc.framework.server.iec61850.scheduling.Schedule;
import org.openmuc.framework.server.iec61850.serverSchedulingAdapter.IEC61850ScheduleNodeSnapshot.IEC61850ServerConfigException;
import org.slf4j.Logger;

import com.beanit.iec61850bean.Fc;
import com.beanit.iec61850bean.ModelNode;
import com.beanit.iec61850bean.ServerModel;

/**
 * RecordListener which listens to an openMUC channel corresponding to the EnaReq value of an IEC61850FSCHNode Whenever
 * the value is set to true AND the complementary DsaReq value of the node is false, it creates a schedule-node and adds
 * it to the controller
 */
public class ScheduleEnableListener implements RecordListener {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(ScheduleEnableListener.class);
    private final ServerMappingAnalyser analyser;

    private final IEC61850ScheduleController controller;

    private final ServerModel serverModel;

    public ScheduleEnableListener(IEC61850ScheduleController controller, ServerModel serverModel,
            ServerMappingAnalyser analyser) {
        this.controller = controller;
        this.serverModel = serverModel;
        this.analyser = analyser;
    }

    @Override
    public void newRecord(Record newEnableValue) {
        log.trace("Schedule enable listener for channel {} called with new value {}", analyser.getScheduleName(),
                newEnableValue);
        if (newEnableValue.getValue().asBoolean()) {
            final String disableNode = analyser.getServerName() + "/" + analyser.getScheduleName()
                    + ".DsaReq.Oper.ctlVal";
            final Fc fc = Fc.CO;
            log.trace("Searching for node {} with FC={}", disableNode, fc);
            ModelNode modelNode = serverModel.findModelNode(disableNode, fc);
            if (modelNode == null) {
                log.error("Required node {} not found on server. Not enabling schedule {}.", disableNode,
                        analyser.getScheduleName());
                return;
            }
            boolean scheduleDisabled = Boolean.parseBoolean(modelNode.getBasicDataAttributes().get(0).getValueString());
            if (!scheduleDisabled) {
                try {
                    Schedule action = getAction();
                    controller.addNewControlAction(action);
                    log.debug("New schedule enabled.");
                } catch (InvalidScheduleException | IEC61850ServerConfigException e) {
                    log.error("Error when enabling schedule {}. Schedule seems to be invalid",
                            analyser.getScheduleName());
                }
                log.debug("Enabled schedule {}", analyser.getScheduleName());
            }
            else {
                log.error("Bad schedule state. Both EnaReq and DsaReq are set to true.");
            }
        }
        else {
            // The IEC61850 norm specifies that only an active schedule_disable should set the state of a schedule to
            // notReady => therefore nothing happens here
            log.debug("Enable schedule {} set to false", analyser.getScheduleName());
        }
    }

    public IEC61850ScheduleNodeSnapshot getAction() throws IEC61850ServerConfigException {
        return new IEC61850ScheduleNodeSnapshot(analyser, serverModel);
    }

    public IEC61850ScheduleController getController() {
        return controller;
    }

    public String getScheduleName() {
        return analyser.getScheduleName();
    }
}
