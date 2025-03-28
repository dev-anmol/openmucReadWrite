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
package org.openmuc.framework.lib.rest1.rest.objects;

import com.google.gson.annotations.SerializedName;

public class RestUserConfig {

    private String id;
    @SerializedName("password")
    private String pass;
    @SerializedName("oldPassword")
    private String oldPasswd;
    private String[] groups;
    private String description;

    protected RestUserConfig() {
    }

    public RestUserConfig(String id) {
        this.id = id;
        this.pass = "*****";
        this.groups = new String[] { "" };
        this.description = "";
    }

    public String getId() {
        return id;
    }

    public String getPassword() {
        return pass;
    }

    public String getOldPassword() {
        return oldPasswd;
    }

    public String[] getGroups() {
        if (groups != null) {
            return groups.clone();
        }
        else {
            return new String[] {};
        }
    }

    public String getDescription() {
        return description;
    }

}
