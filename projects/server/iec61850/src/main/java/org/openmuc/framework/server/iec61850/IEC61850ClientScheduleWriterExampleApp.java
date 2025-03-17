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
package org.openmuc.framework.server.iec61850;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beanit.iec61850bean.BasicDataAttribute;
import com.beanit.iec61850bean.BdaBoolean;
import com.beanit.iec61850bean.BdaFloat32;
import com.beanit.iec61850bean.BdaFloat64;
import com.beanit.iec61850bean.BdaInt16;
import com.beanit.iec61850bean.BdaInt16U;
import com.beanit.iec61850bean.BdaInt32;
import com.beanit.iec61850bean.BdaInt32U;
import com.beanit.iec61850bean.BdaInt64;
import com.beanit.iec61850bean.BdaInt8;
import com.beanit.iec61850bean.BdaInt8U;
import com.beanit.iec61850bean.BdaTimestamp;
import com.beanit.iec61850bean.BdaVisibleString;
import com.beanit.iec61850bean.ClientAssociation;
import com.beanit.iec61850bean.ClientSap;
import com.beanit.iec61850bean.Fc;
import com.beanit.iec61850bean.FcModelNode;
import com.beanit.iec61850bean.ModelNode;
import com.beanit.iec61850bean.ServerModel;
import com.beanit.iec61850bean.ServiceError;

/**
 * This is an example app that will set up and activate 2 IEC 61850 schedules {@value #SCHEDULE_NAME1} and
 * {@value #SCHEDULE_NAME2} with priorities {@value #SCHEDULE_PRIO1} and {@value #SCHEDULE_PRIO2}.
 * <p>
 * The schedules are set up such that they run in parallel and it can be observed how {@value #SCHEDULE_NAME2} overrides
 * {@value #SCHEDULE_NAME1} because of its higher priority
 */
public class IEC61850ClientScheduleWriterExampleApp {
    private static final String SCHEDULE_NAME1 = "IED_Controllable_DER/ActPow_FSCH01";
    private static final String SCHEDULE_NAME2 = "IED_Controllable_DER/ActPow_FSCH02";
    private static final int SCHEDULE_PRIO1 = 100;
    private static final int SCHEDULE_PRIO2 = 111;

    private static Logger log = LoggerFactory.getLogger(IEC61850ClientScheduleWriterExampleApp.class);

    public static void main(String[] args) {
        try (IEC61850ScheduleAccess scheduleAccess = new IEC61850ScheduleAccess("localhost", 10003)) {
            scheduleAccess.disableSchedule(SCHEDULE_NAME1);
            scheduleAccess.disableSchedule(SCHEDULE_NAME2);

            Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);

            scheduleAccess.writeAndEnableSchedule(Arrays.asList(-42d, -1337d, 1337d, 42d, 31d, 22d), SCHEDULE_NAME1,
                    Duration.ofSeconds(4), now.minusSeconds(4), SCHEDULE_PRIO1);

            scheduleAccess.writeAndEnableSchedule(Arrays.asList(9999d), SCHEDULE_NAME2, Duration.ofSeconds(3),
                    now.plusSeconds(5), SCHEDULE_PRIO2);

            // result should be
            /**
             * second active s1 val s2 val output schedule -4...-1 1 -42 - none, this part of the schedule is already
             * over at activation ------ start of observable values ------ 0 1 -1337 - -1337 1 - -1337 - -1337 2 - -1337
             * - -1337 3 1 -1337 - -1337 4 1 1337 - 1337 5 2 1337 9999 9999 6 2 1337 9999 9999 7 2 1337 9999 9999 8 2 42
             * - 42 9 1 42 - 42 10 1 42 - 42 11 1 42 - 42 12 1 123 - 123 13 1 123 - 123 14 1 123 - 123 15 1 123 - 123 16
             * 1 22 - 22 17 1 22 - 22 18 1 22 - 22 19 1 22 - 22 20 - (reserve schedule value, probably 123)
             */
        } catch (Exception e) {
            log.error("Unable to write schedules to server", e);
        }
    }

    public static class IEC61850ScheduleAccess implements Closeable {

        private final static Logger log = LoggerFactory.getLogger(IEC61850ScheduleAccess.class);

        private final ClientAssociation association;
        private final ServerModel serverModel;

        public IEC61850ScheduleAccess(String host, int port) throws UnknownHostException, IOException, ServiceError {
            this(InetAddress.getByName(host), port);
        }

        public IEC61850ScheduleAccess(InetAddress host, int port) throws IOException, ServiceError {
            log.info("Connecting to {}:{}", host, port);
            ClientSap clientSap = new ClientSap();

            this.association = clientSap.associate(host, port, null, null);
            log.debug("loading server model");
            this.serverModel = this.association.retrieveModel();
            log.debug("done loading server model");
        }

        private static void setBda(String valueString, BasicDataAttribute modelNode) {
            if (modelNode instanceof BdaFloat32) {
                float value = Float.parseFloat(valueString);
                ((BdaFloat32) modelNode).setFloat(value);
            }
            else if (modelNode instanceof BdaFloat64) {
                double value = Float.parseFloat(valueString);
                ((BdaFloat64) modelNode).setDouble(value);
            }
            else if (modelNode instanceof BdaInt8) {
                byte value = Byte.parseByte(valueString);
                ((BdaInt8) modelNode).setValue(value);
            }
            else if (modelNode instanceof BdaInt8U) {
                short value = Short.parseShort(valueString);
                ((BdaInt8U) modelNode).setValue(value);
            }
            else if (modelNode instanceof BdaInt16) {
                short value = Short.parseShort(valueString);
                ((BdaInt16) modelNode).setValue(value);
            }
            else if (modelNode instanceof BdaInt16U) {
                int value = Integer.parseInt(valueString);
                ((BdaInt16U) modelNode).setValue(value);
            }
            else if (modelNode instanceof BdaInt32) {
                int value = Integer.parseInt(valueString);
                ((BdaInt32) modelNode).setValue(value);
            }
            else if (modelNode instanceof BdaInt32U) {
                long value = Long.parseLong(valueString);
                ((BdaInt32U) modelNode).setValue(value);
            }
            else if (modelNode instanceof BdaInt64) {
                long value = Long.parseLong(valueString);
                ((BdaInt64) modelNode).setValue(value);
            }
            else if (modelNode instanceof BdaBoolean) {
                boolean value = Boolean.parseBoolean(valueString);
                ((BdaBoolean) modelNode).setValue(value);
            }
            else if (modelNode instanceof BdaVisibleString) {
                ((BdaVisibleString) modelNode).setValue(valueString);
            }
            else if (modelNode instanceof BdaTimestamp) {
                ((BdaTimestamp) modelNode).setInstant(Instant.ofEpochMilli(Long.parseLong(valueString)));
            }
            else {
                throw new IllegalArgumentException();
            }
        }

        @Override
        public void close() {
            IEC61850ClientScheduleWriterExampleApp.log.info("Closing connection");
            this.association.disconnect();
        }

        public void writeAndEnableSchedule(List<Double> values, String scheduleName, Duration interval, Instant start,
                int prio) throws ServiceError, IOException {

            writeScheduleValues(values, scheduleName);

            Long intervalInSeconds = interval.getSeconds();

            if (intervalInSeconds < 1) {
                throw new IllegalArgumentException("interval must be larger than one second");
            }

            // FIXME setVal instead of stVal?
            setDataValues(scheduleName + ".SchdIntv.stVal", null, intervalInSeconds.toString());
            setSchedulePrio(scheduleName, prio);

            setScheduleStart(scheduleName, start);
            BasicDataAttribute disableOp = findAndAssignValue(scheduleName + ".DsaReq.Oper.ctlVal", Fc.CO, "false");
            BasicDataAttribute enableOp = findAndAssignValue(scheduleName + ".EnaReq.Oper.ctlVal", Fc.CO, "true");
            operate((FcModelNode) disableOp.getParent().getParent());
            operate((FcModelNode) enableOp.getParent().getParent());
        }

        public void setScheduleStart(String scheduleName, Instant start) throws ServiceError, IOException {
            log.info("setting {} start to {}", scheduleName, start);
            // FIXME setVal instead of stVal?
            setDataValues(scheduleName + ".StrTm.t.SecondSinceEpoch", null, Long.toString(start.toEpochMilli() / 1000));
        }

        public void setSchedulePrio(String scheduleName, int prio) throws ServiceError, IOException {
            // FIXME setVal instead of stVal?
            setDataValues(scheduleName + ".SchdPrio.stVal", null, Long.toString(prio));
        }

        /**
         * Writes a previously specified Schedule to the device.
         */
        public String writeScheduleValues(Collection<Double> values, String scheduleName)
                throws ServiceError, IOException {

            if (values.size() < 1) {
                throw new IllegalArgumentException("At least one value required.");
            }

            int valueIndex = 1;
            for (double value : values) {
                String nodeName = String.format("%s.ValASG%03d.setMag.f", scheduleName, valueIndex++);
                setDataValues(nodeName, null, Double.toString(value));
            }

            // FIXME setVal instead of stVal?
            setDataValues(scheduleName + ".NumEntr.stVal", null, String.valueOf(values.size()));
            return scheduleName;
        }

        public void disableSchedule(String scheduleNames) throws ServiceError, IOException {

            BasicDataAttribute disableOp = findAndAssignValue(scheduleNames + ".DsaReq.Oper.ctlVal", Fc.CO, "true");
            BasicDataAttribute enableOp = findAndAssignValue(scheduleNames + ".EnaReq.Oper.ctlVal", Fc.CO, "false");

            operate((FcModelNode) disableOp.getParent().getParent());
            operate((FcModelNode) enableOp.getParent().getParent());
        }

        protected void operate(FcModelNode node) throws ServiceError, IOException {
            try {
                association.operate(node);
            } catch (ServiceError e) {
                throw new ServiceError(e.getErrorCode(), "Unable to operate " + node.getReference().toString(), e);
            }
        }

        public BasicDataAttribute setDataValues(String objectReference, Fc fc, String value)
                throws ServiceError, IOException {
            log.debug("Setting {} to {}", objectReference, value);
            BasicDataAttribute bda = findAndAssignValue(objectReference, fc, value);
            try {
                association.setDataValues(bda);
                return bda;
            } catch (ServiceError se) {
                throw new ServiceError(se.getErrorCode(),
                        String.format("Unable to set '%s' to '%s'", objectReference, value), se);
            }
        }

        protected BasicDataAttribute findAndAssignValue(String objectReference, Fc fc, String value) {
            ModelNode node = serverModel.findModelNode(objectReference, fc);
            if (node == null) {
                throw new RuntimeException("Could not find node with name " + objectReference);
            }
            if (!(node instanceof BasicDataAttribute)) {
                throw new RuntimeException(String.format("Unable to assign a value to node '%s' with type '%s'.",
                        node.getName(), node.getClass().getSimpleName()));
            }
            else {
                BasicDataAttribute attribute = (BasicDataAttribute) node;
                setBda(value, attribute);
                log.info("SET {}", serverModel.findModelNode(objectReference, fc));
                return attribute;
            }
        }
    }
}
