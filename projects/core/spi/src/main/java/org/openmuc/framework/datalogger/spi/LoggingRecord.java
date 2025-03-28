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

package org.openmuc.framework.datalogger.spi;

import org.openmuc.framework.data.Record;

public class LoggingRecord {

    private final String channelId;
    private final Record record;

    public LoggingRecord(String channelId, Record record) {
        this.channelId = channelId;
        this.record = record;
    }

    public String getChannelId() {
        return channelId;
    }

    public Record getRecord() {
        return record;
    }
}
