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

import org.openmuc.framework.lib.osgi.config.ServiceProperty;

public class TestIEC61850ServerSettings extends IEC61850ServerSettings {

    void setPort(int port) {
        properties.put("port", new ServiceProperty("port", "port for iec 61850 Server", Integer.toString(port), true));
    }

    void setSclPath(String path) {
        properties.put("sclPath", new ServiceProperty("sclPath", "path to scl file", path, true));
    }
}
