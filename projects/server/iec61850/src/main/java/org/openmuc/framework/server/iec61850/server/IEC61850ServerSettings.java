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

import org.openmuc.framework.lib.osgi.config.GenericSettings;
import org.openmuc.framework.lib.osgi.config.PropertyHandler;
import org.openmuc.framework.lib.osgi.config.ServiceProperty;

/**
 * The server settings for the IEC61850 server. Clarifies the port, address and the cid file from which the server
 * should start
 */
class IEC61850ServerSettings extends GenericSettings {

    public static final String PID = Iec61850Server.class.getName();

    private static final String PORT = "port";
    private static final String SCL_PATH = "sclPath";
    private static final String SCHEDULING_ENABLED = "schedulingEnabled";

    private final static Long DEFAULT_PORT = 10003l;
    private final static String DEFAULT_SCL_FILE = "conf/der_scheduler.cid";
    public static final Boolean DEFAULT_SCHEDULING_ENABLED = true;

    public IEC61850ServerSettings() {
        super();
        properties.put(PORT, new ServiceProperty(PORT, "port for iec 61850 Server", DEFAULT_PORT.toString(), true));
        properties.put(SCL_PATH, new ServiceProperty(SCL_PATH, "path to scl file", DEFAULT_SCL_FILE, true));
        properties.put(SCHEDULING_ENABLED, new ServiceProperty(SCHEDULING_ENABLED,
                "use IEC 61850 scheduling functionality", DEFAULT_SCHEDULING_ENABLED.toString(), true));
    }

    public PropertyHandler getPropertyHandler() {
        return new PropertyHandler(this, PID);
    }

    public static boolean isSchedulingEnabled(PropertyHandler propertyHandler) {
        return propertyHandler.getBoolean(SCHEDULING_ENABLED);
    }

    public static String getSclPath(PropertyHandler propertyHandler) {
        return propertyHandler.getString(SCL_PATH);
    }

    public static int getPort(PropertyHandler propertyHandler) {
        return propertyHandler.getInt(PORT);
    }
}
