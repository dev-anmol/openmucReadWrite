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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class ValueTest {

    @Test
    void booleanValueEqualsHashCodeTest() {
        Value v1 = new BooleanValue(true);
        Value v2 = new BooleanValue(true);

        assertEquals(v1.hashCode(), v2.hashCode());
        assertTrue(v1.equals(v2));
        assertTrue(v2.equals(v1));
    }

    @Test
    void byteArrayValueEqualsHashCodeTest() {
        Value v1 = new ByteArrayValue(new byte[] { 0x01 });
        Value v2 = new ByteArrayValue(new byte[] { 0x01 });

        assertEquals(v1.hashCode(), v2.hashCode());
        assertTrue(v1.equals(v2));
        assertTrue(v2.equals(v1));
    }

    @Test
    void byteValueEqualsHashCodeTest() {
        Value v1 = new ByteValue(Byte.valueOf("42"));
        Value v2 = new ByteValue(Byte.valueOf("42"));

        assertEquals(v1.hashCode(), v2.hashCode());
        assertTrue(v1.equals(v2));
        assertTrue(v2.equals(v1));
    }

    @Test
    void doubleValueEqualsHashCodeTest() {
        Value v1 = new DoubleValue(12.34);
        Value v2 = new DoubleValue(12.34);

        assertEquals(v1.hashCode(), v2.hashCode());
        assertTrue(v1.equals(v2));
        assertTrue(v2.equals(v1));
    }

    @Test
    void floatValueEqualsHashCodeTest() {
        Value v1 = new FloatValue(12.34f);
        Value v2 = new FloatValue(12.34f);

        assertEquals(v1.hashCode(), v2.hashCode());
        assertTrue(v1.equals(v2));
        assertTrue(v2.equals(v1));
    }

    @Test
    void intValueEqualsHashCodeTest() {
        Value v1 = new IntValue(42);
        Value v2 = new IntValue(42);

        assertEquals(v1.hashCode(), v2.hashCode());
        assertTrue(v1.equals(v2));
        assertTrue(v2.equals(v1));
    }

    @Test
    void longValueEqualsHashCodeTest() {
        Value v1 = new LongValue(42);
        Value v2 = new LongValue(42);

        assertEquals(v1.hashCode(), v2.hashCode());
        assertTrue(v1.equals(v2));
        assertTrue(v2.equals(v1));
    }

    @Test
    void shortValueEqualsHashCodeTest() {
        Value v1 = new ShortValue("1");
        Value v2 = new ShortValue("1");

        assertEquals(v1.hashCode(), v2.hashCode());
        assertTrue(v1.equals(v2));
        assertTrue(v2.equals(v1));
    }

    @Test
    void stringValueEqualsHashCodeTest() {
        Value v1 = new StringValue("utini!");
        Value v2 = new StringValue("utini!");

        assertEquals(v1.hashCode(), v2.hashCode());
        assertTrue(v1.equals(v2));
        assertTrue(v2.equals(v1));
    }
}
