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
package org.openmuc.framework.driver.csv.utils;

import org.mockito.Mockito;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.data.ValueType;
import org.openmuc.framework.dataaccess.Channel;
import org.openmuc.framework.driver.spi.ChannelRecordContainer;

public class CsvChannelRecordContainer implements ChannelRecordContainer {

    private Record record;
    private final String channelAddress;
    private final Channel channel;

    /**
     * Set up using a mock channel
     *
     * @param channelAddress
     */
    public CsvChannelRecordContainer(String channelAddress) {
        this(channelAddress, createMockChannel());
    }

    private static Channel createMockChannel() {
        Channel mock = Mockito.mock(Channel.class);
        Mockito.doReturn(ValueType.STRING).when(mock).getValueType();
        return mock;
    }

    public CsvChannelRecordContainer(String channelAddress, Channel channel) {
        this.channelAddress = channelAddress;
        this.channel = channel;
    }

    @Override
    public Record getRecord() {
        return record;
    }

    @Override
    public Channel getChannel() {
        return channel;
    }

    @Override
    public String getChannelAddress() {
        return this.channelAddress;
    }

    @Override
    public Object getChannelHandle() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void setChannelHandle(Object handle) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void setRecord(Record record) {
        this.record = record;
    }

    @Override
    public ChannelRecordContainer copy() {
        throw new RuntimeException("Not implemented");
    }

}
