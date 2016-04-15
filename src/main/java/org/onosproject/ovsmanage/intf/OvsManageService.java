package org.onosproject.ovsmanage.intf;

import org.onosproject.net.behaviour.BridgeDescription;

import java.util.List;

/**
 * Created by mao on 4/15/16.
 */
public interface OvsManageService {

    boolean createOVS(String DeviceName, OvsDeviceType DeviceType);
    boolean deleteOVS(String DeviceName);
    List<BridgeDescription> getOVS(OvsDeviceType type);


    enum OvsDeviceType{
        CORE,
        ACCESS
    }

}
