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
import org.onlab.packet.EthType;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.util.KryoNamespace;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.drivers.ovsdb.OvsdbBridgeConfig;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.behaviour.BridgeConfig;
import org.onosproject.net.behaviour.BridgeDescription;
import org.onosproject.net.behaviour.BridgeName;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.ovsmanage.intf.OvsManageService;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.AsyncDistributedSet;
import org.onosproject.store.service.AtomicCounter;
import org.onosproject.store.service.AtomicValue;
import org.onosproject.store.service.DistributedSet;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

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

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    FlowObjectiveService flowObjectiveService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    StorageService storageService;



    private static final int BOTH_TABLE_PRIORITY = 60000;
    private static final int ONE_TABLE_PRIORITY = 65535;

    private static final int ACCESS_DEVICEID_MANDATORY = 0;
    private static final int CORE_DEVICEID_MANDATORY = 100000000;


    private InnerDeviceListener innerDeviceListener;
    private ApplicationId applicationId;

    private AtomicCounter brCoreNumber;
    private AtomicCounter brAccessNumber;

    private DistributedSet<String> brNameSet;


    DeviceId controllerId;


    @Activate
    protected void activate() {
        log.info("Started");

        applicationId = coreService.registerApplication("org.onosproject.ovsmanager");


        Serializer serializer = Serializer.using(
                new KryoNamespace
                        .Builder()
                        .register(KryoNamespaces.API)
                        .register(String.class)
                        .build());

        brNameSet = storageService
                .<String>setBuilder()
                .withSerializer(serializer)
                .withName("OVS Manager Bridge Name Set")
                .withApplicationId(applicationId)
                .build()
                .asDistributedSet();

        brCoreNumber = storageService
                .atomicCounterBuilder()
                .withName("OVS Manager Core Bridge accumulated counter")
                .withApplicationId(applicationId)
                .build()
                .asAtomicCounter();

        brAccessNumber = storageService
                .atomicCounterBuilder()
                .withName("OVS Manager Access Bridge accumulated counter")
                .withApplicationId(applicationId)
                .build()
                .asAtomicCounter();



        innerDeviceListener = new InnerDeviceListener();
        deviceService.addListener(innerDeviceListener);

        Iterator deviceIter = deviceService.getDevices().iterator();
        while(deviceIter.hasNext()){
            Device device = ((Device)deviceIter.next());
            if(device.type() == Device.Type.CONTROLLER){
                controllerId = device.id();
            }
        }
        if(controllerId == null) {
            log.info("controllerId not ready now !!!");
        }
    }

    @Deactivate
    protected void deactivate() {
        deviceService.removeListener(innerDeviceListener);
        brNameSet.clear();
        brCoreNumber.set(0);
        brAccessNumber.set(0);
        log.info("Stopped");
    }

    @Override
    public boolean createOVS(String brName, OvsDeviceType DeviceType){
        if(controllerId == null) {
            log.info("controllerId not ready!!!");
            return false;
        }

        if(brNameSet == null){
            log.info("Bridge name set not ready!!!");
            return false;
        }

        if(brNameSet.contains(brName)){
            log.info("Bridge name existed");
            return false;
        } else {
            brNameSet.add(brName);
        }

        String DeviceId;
        switch(DeviceType){
            case CORE:
                DeviceId = String.format("%16d",brCoreNumber.incrementAndGet()
                        + CORE_DEVICEID_MANDATORY).replaceAll(" ","0");
                break;
            case ACCESS:
                DeviceId = String.format("%16d",brAccessNumber.incrementAndGet()
                        + ACCESS_DEVICEID_MANDATORY).replaceAll(" ","0");
                break;
            default:
                log.info("OvsDeviceType error");
                return false;
        }


        DriverHandler handler = driverService.createHandler(controllerId);
        BridgeConfig bridgeConfig = handler.behaviour(BridgeConfig.class);
        bridgeConfig.addBridge(BridgeName.bridgeName(brName), DeviceId, (String)null);

        return true;
    }

    @Override
    public List<BridgeDescription> getOVS(OvsDeviceType type){

        if(controllerId == null) {
            log.info("controllerId not ready!!!");
            return Collections.emptyList();
        }

        DriverHandler handler = driverService.createHandler(controllerId);
        BridgeConfig bridgeConfig = handler.behaviour(BridgeConfig.class);
        Collection<BridgeDescription> devices;
        try {
            devices = bridgeConfig.getBridges();
        }catch(Exception e){
            return Collections.emptyList();
        }

        if(type == null){
            return devices.stream().collect(Collectors.toList());
        }else{
            if(type == OvsDeviceType.CORE) {
                return devices.stream()
                        .filter(device -> {
                            String datapathId = device.deviceId().toString().split(":")[1];
                            if (Integer.valueOf(datapathId) > CORE_DEVICEID_MANDATORY) {
                                return true;
                            } else {
                                return false;
                            }
                        })
                        .collect(Collectors.toList());
            }else if(type == OvsDeviceType.ACCESS){
                return devices.stream()
                        .filter(device -> {
                            String datapathId = device.deviceId().toString().split(":")[1];
                            if (Integer.valueOf(datapathId) < CORE_DEVICEID_MANDATORY) {
                                return true;
                            } else {
                                return false;
                            }
                        })
                        .collect(Collectors.toList());
            }else{
                return Collections.emptyList();
            }
        }
    }

    @Override
    public boolean deleteOVS(String DeviceName){
        if(controllerId == null) {
            log.info("controllerId not ready!!!");
            return false;
        }

        if(brNameSet == null){
            log.info("Bridge name set");
            return false;
        }

        if(!brNameSet.contains(DeviceName)) {
            log.info("Bridge not exist");
            return false;
        }

        DriverHandler handler = driverService.createHandler(controllerId);
        BridgeConfig bridgeConfig = handler.behaviour(BridgeConfig.class);
        bridgeConfig.deleteBridge(BridgeName.bridgeName(DeviceName));

        brNameSet.remove(DeviceName);

        return true;
    }


    private class InnerDeviceListener implements DeviceListener{

        @Override
        public void event(DeviceEvent event){

            if(!event.type().equals(DeviceEvent.Type.DEVICE_ADDED))
                return;

            Device device = event.subject();

            if(device.type()==Device.Type.CONTROLLER){
                dealController(device.id());
            } else {
                dealSwitch(device.id());
            }
        }

        private void dealController(DeviceId deviceId){
            controllerId = deviceId;
            log.info("controllerId is ready !!!");
        }

        private void dealSwitch(DeviceId deviceId){
            String datapathId = deviceId.toString().split(":")[1];
            if (Integer.valueOf(datapathId) > CORE_DEVICEID_MANDATORY) {
                dealCoreSwitch(deviceId);
            } else {
                dealAccessSwitch(deviceId);
            }
        }
        private void dealCoreSwitch(DeviceId deviceId){
            TrafficSelector.Builder trafficSelectorBuilder0 = DefaultTrafficSelector.builder();
            trafficSelectorBuilder0.matchEthType(EthType.EtherType.IPV4.ethType().toShort())
                    .matchIPDst(IpPrefix.valueOf("192.168.1.1/28"))
                    .matchIPSrc(IpPrefix.valueOf("1.2.3.4/32"));

            TrafficTreatment.Builder trafficTreatmentBuilder0 = DefaultTrafficTreatment.builder();
            trafficTreatmentBuilder0.setEthDst(MacAddress.BROADCAST).transition(1);

            addForward(deviceId, BOTH_TABLE_PRIORITY,
                       trafficSelectorBuilder0.build(), trafficTreatmentBuilder0.build());
        }
        private void dealAccessSwitch(DeviceId deviceId){
            TrafficSelector.Builder trafficSelectorBuilder0 = DefaultTrafficSelector.builder();
            trafficSelectorBuilder0.matchEthType(EthType.EtherType.IPV4.ethType().toShort())
                    .matchIPSrc(IpPrefix.valueOf("10.0.0.0/24"));

            TrafficTreatment.Builder trafficTreatmentBuilder0 = DefaultTrafficTreatment.builder();
            trafficTreatmentBuilder0.setEthDst(MacAddress.BROADCAST).transition(1);

            addForward(deviceId, BOTH_TABLE_PRIORITY,
                       trafficSelectorBuilder0.build(), trafficTreatmentBuilder0.build());


            TrafficSelector.Builder trafficSelectorBuilder1 = DefaultTrafficSelector.builder();
            trafficSelectorBuilder1.matchEthDst(MacAddress.BROADCAST);

            TrafficTreatment.Builder trafficTreatmentBuilder1 = DefaultTrafficTreatment.builder();
            trafficTreatmentBuilder1.drop();

            addForward(deviceId, BOTH_TABLE_PRIORITY,
                       trafficSelectorBuilder1.build(), trafficTreatmentBuilder1.build());
        }

        private void addForward(DeviceId deviceId, int priority, TrafficSelector selector, TrafficTreatment treatment){

            ForwardingObjective.Builder forwardingObjectiveBuilder = DefaultForwardingObjective.builder();
            forwardingObjectiveBuilder
                    .withFlag(ForwardingObjective.Flag.SPECIFIC)
                    .withTreatment(treatment)
                    .withSelector(selector)
                    .withPriority(priority)
                    .fromApp(applicationId)
                    .makePermanent();

            flowObjectiveService.forward(deviceId, forwardingObjectiveBuilder.add());
        }
    }
}
