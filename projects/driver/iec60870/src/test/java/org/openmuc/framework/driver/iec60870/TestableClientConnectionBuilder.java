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
package org.openmuc.framework.driver.iec60870;

import java.net.UnknownHostException;
import java.util.List;

import org.openmuc.j60870.ASduType;
import org.openmuc.j60870.ClientConnectionBuilder;

public class TestableClientConnectionBuilder extends ClientConnectionBuilder {

    private int testableCommonAddressFieldLength;
    private int testableCotFieldLength;
    private int testableIoaFieldLength;
    private int testableMaxTimeNoAckReceived;
    private int testableMaxTimeNoAckSent;
    private int testableMaxIdleTime;
    private int testableConnectionTimeout;
    private int testableMaxUnconfirmedIPdusReceived;
    private int testableMaxNumOfOutstandingIPdus;
    private List<ASduType> testableAllowedTypes;

    public TestableClientConnectionBuilder(String hostAddress) throws UnknownHostException {
        super(hostAddress);
    }

    @Override
    public ClientConnectionBuilder setCommonAddressFieldLength(int length) {
        this.testableCommonAddressFieldLength = length;
        return super.setCommonAddressFieldLength(length);
    }

    @Override
    public ClientConnectionBuilder setCotFieldLength(int length) {
        this.testableCotFieldLength = length;
        return super.setCotFieldLength(length);
    }

    @Override
    public ClientConnectionBuilder setIoaFieldLength(int length) {
        this.testableIoaFieldLength = length;
        return super.setIoaFieldLength(length);
    }

    @Override
    public ClientConnectionBuilder setMaxTimeNoAckReceived(int time) {
        this.testableMaxTimeNoAckReceived = time;
        return super.setMaxTimeNoAckReceived(time);
    }

    @Override
    public ClientConnectionBuilder setMaxTimeNoAckSent(int time) {
        this.testableMaxTimeNoAckSent = time;
        return super.setMaxTimeNoAckSent(time);
    }

    @Override
    public ClientConnectionBuilder setMaxIdleTime(int time) {
        this.testableMaxIdleTime = time;
        return super.setMaxIdleTime(time);
    }

    @Override
    public ClientConnectionBuilder setConnectionTimeout(int timeout) {
        this.testableConnectionTimeout = timeout;
        return super.setConnectionTimeout(timeout);
    }

    @Override
    public ClientConnectionBuilder setMaxUnconfirmedIPdusReceived(int max) {
        this.testableMaxUnconfirmedIPdusReceived = max;
        return super.setMaxUnconfirmedIPdusReceived(max);
    }

    @Override
    public ClientConnectionBuilder setMaxNumOfOutstandingIPdus(int max) {
        this.testableMaxNumOfOutstandingIPdus = max;
        return super.setMaxNumOfOutstandingIPdus(max);
    }

    @Override
    public ClientConnectionBuilder setAllowedASduTypes(List<ASduType> setAllowedASduTypes) {
        this.testableAllowedTypes = setAllowedASduTypes;
        return super.setAllowedASduTypes(setAllowedASduTypes);
    }

    public int getTestableCommonAddressFieldLength() {
        return testableCommonAddressFieldLength;
    }

    public int getTestableCotFieldLength() {
        return testableCotFieldLength;
    }

    public int getTestableIoaFieldLength() {
        return testableIoaFieldLength;
    }

    public int getTestableMaxTimeNoAckReceived() {
        return testableMaxTimeNoAckReceived;
    }

    public int getTestableMaxTimeNoAckSent() {
        return testableMaxTimeNoAckSent;
    }

    public int getTestableMaxIdleTime() {
        return testableMaxIdleTime;
    }

    public int getTestableConnectionTimeout() {
        return testableConnectionTimeout;
    }

    public int getTestableMaxUnconfirmedIPdusReceived() {
        return testableMaxUnconfirmedIPdusReceived;
    }

    public int getTestableMaxNumOfOutstandingIPdus() {
        return testableMaxNumOfOutstandingIPdus;
    }

    public List<ASduType> getTestableAllowedTypes() {
        return testableAllowedTypes;
    }

}
