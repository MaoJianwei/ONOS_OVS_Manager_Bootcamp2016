/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.ovsmanage.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.drivers.ovsdb.OvsdbBridgeConfig;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.behaviour.BridgeConfig;
import org.onosproject.net.behaviour.BridgeName;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverService;
import org.onosproject.ovsmanage.intf.OvsManageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

/**
 * Skeletal ONOS application component.
 */
@Component(immediate = true)
@Service
public class OvsManageManager implements OvsManageService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    DriverService driverService;

    DeviceId controllerId;


    @Activate
    protected void activate() {
        log.info("Started");

        Iterable iter = deviceService.getDevices();
        Iterator iterkey = iter.iterator();
        while(iterkey.hasNext()){
            Device device = ((Device)iterkey.next());
            if(device.type() == Device.Type.CONTROLLER){
                controllerId = device.id();
            }
        }
        if(controllerId == null) {
            log.info("controllerId not ready!!!");
        }

        int a = 0;
    }

    @Deactivate
    protected void deactivate() {
        log.info("Stopped");
    }

    @Override
    public boolean createOVS(String brName, String dpid, String exPort){
        if(controllerId == null)
            return false;

        DriverHandler handler = driverService.createHandler(controllerId);
        BridgeConfig bridgeConfig = handler.behaviour(BridgeConfig.class);
        bridgeConfig.addBridge(BridgeName.bridgeName(brName), dpid, exPort);

        return true;
    }

}
