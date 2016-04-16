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
import org.onosproject.net.behaviour.BridgeDescription;
import org.onosproject.ovsmanage.intf.OvsManageService;

import java.util.List;

/**
 * CLI to show OVS switches.
 */
@Command(scope = "onos", name = "show-bridge",
        description = "delete a bridge on specific OVS")
public class ShowBridgeCommand extends AbstractShellCommand {

    private static final String DELETE_BRIDGE_FORMAT = "Delete Bridge: %s";

    @Argument(index = 0, name = "bridge-type", description = "type of Bridge",
            required = false, multiValued = false)
    private String bridgeType;

    @Override
    protected void execute() {

        OvsManageService.OvsDeviceType type = null;

        OvsManageService ovsService = AbstractShellCommand.get(OvsManageService.class);

        String typeStr = "All";
        if (bridgeType != null) {
            if (bridgeType.toLowerCase().equals("core")) {
                type = OvsManageService.OvsDeviceType.CORE;
                typeStr = "Core";

            } else if (bridgeType.toLowerCase().equals("access")) {
                type = OvsManageService.OvsDeviceType.ACCESS;
                typeStr = "Access";
            }
            printBridge(ovsService.getOvs(type), typeStr);
        } else {
            printBridge(ovsService.getOvs(OvsManageService.OvsDeviceType.CORE), "Core");
            print("\n--------------------------------------");
            printBridge(ovsService.getOvs(OvsManageService.OvsDeviceType.ACCESS), "Access");
        }
    }

    /**
     * .
     * @param bridges
     * @param typeStr
     */
    private void printBridge(List<BridgeDescription> bridges, String typeStr) {
        print("\n%s Device count: %d\n", typeStr, bridges.size());
        for (BridgeDescription desc : bridges) {
            print("Device name: %-20.20s Device Id: %s",
                  desc.bridgeName().name(), desc.deviceId().toString());
        }
    }
}
