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

package org.openmuc.framework.data;

import java.util.Arrays;
import java.util.Objects;

/**
 * ByteArrayValue is not immutable.
 */
public class ByteArrayValue implements Value {

    private final byte[] value;

    /**
     * Create a new ByteArrayValue whose internal byte array will be a reference to the <code>value</code> passed to
     * this constructor. That means the passed byte array is not copied. Therefore you should not change the contents of
     * value after calling this constructor. If you want ByteArrayValue to internally store a copy of the passed value
     * then you should use the other constructor of this class instead.
     *
     * @param value
     *            the byte array value.
     */
    public ByteArrayValue(byte[] value) {
        this.value = value;
    }

    /**
     * Creates a new ByteArrayValue copying the byte array passed if <code>copy</code> is true.
     *
     * @param value
     *            the byte array value.
     * @param copy
     *            if true it will internally store a copy of value, else it will store a reference to value.
     */
    public ByteArrayValue(byte[] value, boolean copy) {
        if (copy) {
            this.value = value.clone();
        }
        else {
            this.value = value;
        }
    }

    /**
     * Creates a new ByteArrayValue from a String in type [012345] or 0x012345.
     *
     * @param hexStringValue
     *            the byte array value as string.
     */
    public ByteArrayValue(String hexStringValue) {
        this.value = fromHexString(hexStringValue);
    }

    private static byte[] fromHexString(String hexString) throws NumberFormatException {
        validate(hexString);

        hexString = hexString.replaceAll("[^0-9a-fA-F]", "");
        int length = hexString.length();
        byte[] data = new byte[length / 2];

        for (int i = 0; i < length; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                    + Character.digit(hexString.charAt(i + 1), 16));
        }
        return data;
    }

    private static void validate(String s) {
        if (s == null) {
            throw new IllegalArgumentException("String may not be null");
        }

        if (s.isEmpty() || ((s.length() % 2) != 0)) {
            throw new NumberFormatException("String \"" + s + "\" is not a legal hex string.");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ByteArrayValue that = (ByteArrayValue) o;
        return Objects.deepEquals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(value);
    }

    @Override
    public double asDouble() {
        throw new TypeConversionException();
    }

    @Override
    public float asFloat() {
        throw new TypeConversionException();
    }

    @Override
    public long asLong() {
        throw new TypeConversionException();
    }

    @Override
    public int asInt() {
        throw new TypeConversionException();
    }

    @Override
    public short asShort() {
        throw new TypeConversionException();
    }

    @Override
    public byte asByte() {
        throw new TypeConversionException();
    }

    @Override
    public boolean asBoolean() {
        throw new TypeConversionException();
    }

    @Override
    public byte[] asByteArray() {
        return value;
    }

    @Override
    public String toString() {
        return Arrays.toString(this.value);
    }

    @Override
    public String asString() {
        return toString();
    }

    @Override
    public ValueType getValueType() {
        return ValueType.BYTE_ARRAY;
    }
}
