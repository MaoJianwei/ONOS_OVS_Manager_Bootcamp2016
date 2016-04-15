/*
 * Copyright 2015-present Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.ovsmanage.cli;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
//import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.ovsmanage.intf.OvsManageService;

/**
 * CLI to create an OVS switch
 */
@Command(scope = "onos", name = "delete-bridge",
        description = "delete a bridge on specific OVS")
public class DeleteBridgeCommand extends AbstractShellCommand {

    private static final String DELETE_BRIDGE_FORMAT = "Delete Bridge: %s";

    @Argument(index = 0, name = "bridge-name", description = "name of Bridge",
            required = true, multiValued = false)
    String bridgeName;

    @Override
    protected void execute() {

        if(bridgeName == null){
            return;
        }

        OvsManageService ovsService = AbstractShellCommand.get(OvsManageService.class);

        if(ovsService.deleteOVS(bridgeName)){
            print(DELETE_BRIDGE_FORMAT, bridgeName);
        }else {
            print(DELETE_BRIDGE_FORMAT, "fail");
        }
    }
}
