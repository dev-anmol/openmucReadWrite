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

import org.openmuc.framework.data.BooleanValue;
import org.openmuc.framework.data.ByteValue;
import org.openmuc.framework.data.DoubleValue;
import org.openmuc.framework.data.FloatValue;
import org.openmuc.framework.data.IntValue;
import org.openmuc.framework.data.LongValue;
import org.openmuc.framework.data.ShortValue;
import org.openmuc.framework.data.StringValue;
import org.openmuc.framework.data.Value;

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

/**
 * Holds methods to convert between iec61850bean and OpenMUC datatypes
 */
public class IEC61850TypeConversionUtility {

    private IEC61850TypeConversionUtility() {
        // only static methods
    }

    public static Value cast61850TypeToOpenmucType(BasicDataAttribute bda) {
        String valueString = bda.getValueString();
        if (bda instanceof BdaFloat32) {
            return new FloatValue(Float.parseFloat(valueString));
        }
        else if (bda instanceof BdaFloat64) {
            return new DoubleValue(Double.parseDouble(valueString));
        }
        else if (bda instanceof BdaInt8) {
            return new ByteValue(Byte.parseByte(valueString));
        }
        else if (bda instanceof BdaInt8U) {
            return new ShortValue(Short.parseShort(valueString));
        }
        else if (bda instanceof BdaInt16) {
            return new ShortValue(Short.parseShort(valueString));
        }
        else if (bda instanceof BdaInt16U) {
            return new IntValue(Integer.parseInt(valueString));
        }
        else if (bda instanceof BdaInt32) {
            return new IntValue(Integer.parseInt(valueString));
        }
        else if (bda instanceof BdaInt32U) {
            return new LongValue(Long.parseLong(valueString));
        }
        else if (bda instanceof BdaInt64) {
            return new LongValue(Long.parseLong(valueString));
        }
        else if (bda instanceof BdaBoolean) {
            return new BooleanValue(Boolean.parseBoolean(valueString));
        }
        else if (bda instanceof BdaVisibleString) {
            return new StringValue(valueString);
        }
        else if (bda instanceof BdaTimestamp) {
            return new LongValue(((BdaTimestamp) bda).getInstant().toEpochMilli());
        }
        else {
            throw new IllegalArgumentException();
        }
    }

    public static void setBdaValue(BasicDataAttribute modelNode, String valueString) {
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

        }
        else {
            throw new IllegalArgumentException();
        }

    }

    public static void setBdaValue(BasicDataAttribute modelNode, BasicDataAttribute bdaToBeWritten) {
        if (modelNode instanceof BdaFloat32) {
            ((BdaFloat32) modelNode).setValueFrom(bdaToBeWritten);
        }
        else if (modelNode instanceof BdaFloat64) {
            ((BdaFloat64) modelNode).setValueFrom(bdaToBeWritten);
        }
        else if (modelNode instanceof BdaInt8) {
            ((BdaInt8) modelNode).setValueFrom(bdaToBeWritten);
        }
        else if (modelNode instanceof BdaInt8U) {
            ((BdaInt8U) modelNode).setValueFrom(bdaToBeWritten);
        }
        else if (modelNode instanceof BdaInt16) {
            ((BdaInt16) modelNode).setValueFrom(bdaToBeWritten);
        }
        else if (modelNode instanceof BdaInt16U) {
            ((BdaInt16U) modelNode).setValueFrom(bdaToBeWritten);
        }
        else if (modelNode instanceof BdaInt32) {
            ((BdaInt32) modelNode).setValueFrom(bdaToBeWritten);
        }
        else if (modelNode instanceof BdaInt32U) {
            ((BdaInt32U) modelNode).setValueFrom(bdaToBeWritten);
        }
        else if (modelNode instanceof BdaInt64) {
            ((BdaInt64) modelNode).setValueFrom(bdaToBeWritten);
        }
        else if (modelNode instanceof BdaBoolean) {
            ((BdaBoolean) modelNode).setValueFrom(bdaToBeWritten);
        }
        else if (modelNode instanceof BdaVisibleString) {
            ((BdaVisibleString) modelNode).setValueFrom(bdaToBeWritten);
        }
        else if (modelNode instanceof BdaTimestamp) {
            ((BdaTimestamp) modelNode).setValueFrom(bdaToBeWritten);

        }
        else {
            throw new IllegalArgumentException();
        }
    }
}
