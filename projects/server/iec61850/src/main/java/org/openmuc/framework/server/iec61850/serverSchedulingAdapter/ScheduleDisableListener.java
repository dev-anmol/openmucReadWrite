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
import org.openmuc.framework.dataaccess.DataAccessService;
import org.openmuc.framework.dataaccess.RecordListener;
import org.openmuc.framework.server.iec61850.scheduling.IEC61850ScheduleController;
import org.slf4j.Logger;

/**
 * RecordListener which listens on an openMUC-channel corresponding to the DsaReq of an IEC61850FSCHNode. Whenever the
 * value is set to true, it calls the disableSchedule method of the schedule controller
 */
public class ScheduleDisableListener implements RecordListener {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(ScheduleDisableListener.class);

    IEC61850ScheduleController controller;
    private final DataAccessService dataAccessService;
    private final String scheduleName;

    public ScheduleDisableListener(String scheduleName, IEC61850ScheduleController controller,
            DataAccessService dataAccessService) {
        this.controller = controller;
        this.scheduleName = scheduleName;
        this.dataAccessService = dataAccessService;
    }

    @Override
    public void newRecord(Record record) {
        log.trace("Schedule enable listener for channel {} called with new value {}", scheduleName, record);
        if (record.getValue().asBoolean()) {
            controller.disableSchedule(scheduleName);
        }
        else {
            // nothing happens here, since schedule are only enabled when the EnaReq is set to true
        }
    }

    public IEC61850ScheduleController getController() {
        return controller;
    }
}
