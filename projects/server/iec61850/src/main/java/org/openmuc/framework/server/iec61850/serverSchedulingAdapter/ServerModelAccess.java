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
package org.openmuc.framework.server.iec61850.serverSchedulingAdapter;

import com.beanit.iec61850bean.Fc;
import com.beanit.iec61850bean.ModelNode;
import com.beanit.iec61850bean.ServerModel;

/**
 * Wraps access to {@link ServerModel} to make code more readable and testable ({@link ServerModel} is a final class
 * which makes it hard to test...)
 */
public interface ServerModelAccess {
    String readModelNode(String objectReference, Fc fc);

    boolean checkNodeExists(String serverSchedule);

    static ServerModelAccess from(ServerModel serverModel) {
        return new ServerModelAccess() {
            @Override
            public String readModelNode(String objectReference, Fc fc) {
                ModelNode modelNode = serverModel.findModelNode(objectReference, fc);
                if (modelNode == null) {
                    throw new IEC61850NodeNotFoundException(objectReference);
                }
                return modelNode.getBasicDataAttributes().get(0).getValueString();
            }

            @Override
            public boolean checkNodeExists(String objectReference) {
                ModelNode node = serverModel.findModelNode(objectReference, null);
                return node != null;
            }
        };
    }

    public static class IEC61850NodeNotFoundException extends RuntimeException {
        final String nodeName;

        public IEC61850NodeNotFoundException(String nodeName) {
            super("Node not found: " + nodeName);
            this.nodeName = nodeName;
        }
    }
}
