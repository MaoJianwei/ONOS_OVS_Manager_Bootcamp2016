# ONOS_OVS_Manager_Bootcamp2016

Group 4: ONOS BeiXinTong

            ONOS 北信通

Member: Jianwei Mao, Peng Zhang, Ling Jin, Tianfeng Ma

            毛健炜，     张鹏，      金凌，    马田丰

----------------------------------------------------------------------------------

OVSPipeline install steps:

1. Move OVSPipeline.java to $ONOS_ROOT/driver/default/pipeline/

2. Modify $ONOS_ROOT/drivers/default/src/main/resources/onos-drivers.xml,

   change "DefaultSingleTablePipeline" to "OVSPipeline"

3. At $ONOS_ROOT/drivers/default/, run "mvn clean install"

4. onos-karaf clean ("clean" is necessary to update OVSPipeline into Karaf)
