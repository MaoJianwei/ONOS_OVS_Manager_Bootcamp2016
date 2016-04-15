package org.onosproject.ovsmanage.intf;

/**
 * Created by mao on 4/15/16.
 */
public interface OvsManageService {

    boolean createOVS(String brName, String dpid, String exPort);
}
