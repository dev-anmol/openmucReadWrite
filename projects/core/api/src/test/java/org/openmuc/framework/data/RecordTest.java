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

import java.util.HashSet;
import java.util.Objects;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RecordTest {

    @Test
    void hashCodeWorks() {
        HashSet<Record> records = new HashSet<>();
        Assertions.assertEquals(0, records.size());

        Record someRecord = new Record(new StringValue("value"), 123l);
        records.add(someRecord);
        Assertions.assertEquals(1, records.size());

        // add identical record
        records.add(someRecord);
        Assertions.assertEquals(1, records.size());

        // add a record that has the same hashcode
        Record recordWithSameHashcode = new Record(new StringValue("value"), 123l);
        records.add(recordWithSameHashcode);
        Assertions.assertEquals(1, records.size());

        // add a different record
        records.add(new Record(new IntValue(123), 123l));
        Assertions.assertEquals(2, records.size());
    }

    @Test
    void equalsWorks_recordsEqual() {
        Record r1 = new Record(new StringValue("value"), 123l);
        Record r2 = new Record(new StringValue("value"), 123l);

        Assertions.assertTrue(r1.equals(r2));
        Assertions.assertTrue(r2.equals(r1));
        Assertions.assertTrue(Objects.deepEquals(r1, r2));
    }

    @Test
    void equalsWorks_recordsNotEqual_sameType() {
        Record r1 = new Record(new StringValue("VALUE"), 123l);
        Record r2 = new Record(new StringValue("value"), 123l);

        Assertions.assertFalse(r1.equals(r2));
        Assertions.assertFalse(r2.equals(r1));
        Assertions.assertFalse(Objects.deepEquals(r1, r2));
    }

    @Test
    void equalsWorks_recordsNotEqual_differentType() {
        Record s = new Record(new StringValue("VALUE"), 123l);
        Record s2 = new Record(new StringValue("VALUE"), 2l);

        // different value types are different!
        Assertions.assertFalse(s.equals(s2));
        Assertions.assertFalse(s.equals(new Record(new BooleanValue(false), 123l)));
        Assertions.assertFalse(s.equals(new Record(new ByteArrayValue(s.getValue().asByteArray()), 123l)));
        Assertions.assertFalse(s.equals(new Record(new ByteValue("42"), 123l)));
        Assertions.assertFalse(s.equals(new Record(new DoubleValue(123), 123l)));
        Assertions.assertFalse(s.equals(new Record(new FloatValue(123), 123l)));
        Assertions.assertFalse(s.equals(new Record(new IntValue(123), 123l)));
        Assertions.assertFalse(s.equals(new Record(new LongValue(123), 123l)));
        Assertions.assertFalse(s.equals(new Record(new ShortValue("1"), 123l)));
    }
}
