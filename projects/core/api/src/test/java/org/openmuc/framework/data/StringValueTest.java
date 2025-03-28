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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class StringValueTest {
    @ParameterizedTest
    @MethodSource("params")
    public void testBooleanConvert(String value, boolean expected) throws Exception {
        assertEquals(expected, new StringValue(value).asBoolean());
    }

    public static Object[] params() {
        return new Object[][] { { "false", false }, { "true", true }, { "jhbvce", false }, { "TRUE", true },
                { "TRuE", true } };
    }

    @Test
    public void testException() throws Exception {
        Assertions.assertThrows(TypeConversionException.class, () -> new StringValue("98394kdbk").asDouble());
    }

}
