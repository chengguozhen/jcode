/*
 * Copyright (c) 2015 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.dhcp;

import com.google.common.base.Optional;
import org.opendaylight.controller.md.sal.binding.api.*;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.dhcp.Virtual.DhcpVirtualpool;
import org.opendaylight.dhcp.util.NWUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dipapp.v.address.config.rev150105.VaddrConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dipapp.v.address.config.rev150105.v.addrs.VIpConf;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dipapp.v.address.config.rev150105.vaddr.config.VAddrList;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.concurrent.ExecutionException;

public class DhcpVConfig implements DataTreeChangeListener<VaddrConfig> {


    private static final Logger LOG = LoggerFactory.getLogger(DhcpVConfig.class);
    private final DataBroker dataBroker;
    private final InstanceIdentifier<VaddrConfig> vid;
    private final ListenerRegistration listenerReg;
    private DhcpVirtualpool theVirtualDHCPPool;

    //virtual start IP and stop IP
    private String dhcpVirtualStartIp;
    private String dhcpVirtualStopIp;
    private int dhcpVirtualOptLeaseTime;
    private int dhcpVirtualOptRenewalTime;
    private int dhcpVirtualOptRebindingTime;



    /**
    * @param db - dataBroker reference
    */
    public DhcpVConfig(final DataBroker db) {
        dataBroker = db;
        vid = InstanceIdentifier.create(VaddrConfig.class);
        listenerReg = dataBroker.registerDataTreeChangeListener(
                new DataTreeIdentifier<VaddrConfig>(LogicalDatastoreType.CONFIGURATION,vid),this);
    }

    @Override
    public void onDataTreeChanged(@Nonnull Collection<DataTreeModification<VaddrConfig>> changes) {
        for (final DataTreeModification<VaddrConfig> change : changes) {
            final DataObjectModification<VaddrConfig> rootNode = change.getRootNode();
            switch (rootNode.getModificationType()) {
                case SUBTREE_MODIFIED:
                    LOG.info("Virtual IP config reader:node {} modified", rootNode.getIdentifier());
                    readrealidinformation();
                    break;
                case WRITE:
                    LOG.info("Virtual IP config reader:node {} created", rootNode.getIdentifier(), rootNode.getDataAfter());
                    readrealidinformation();
                    break;
                default:
                    LOG.error("Virtual IP ERROR:Data changed,but there are some errors");
            }
        }
    }

    private void readrealidinformation(){

        synchronized (this) {
            // Read Inventory
            VaddrConfig vaddrConfig = null;
            ReadOnlyTransaction rt = dataBroker.newReadOnlyTransaction();
            try {
                Optional<VaddrConfig> vaddrConfigOptional =  rt.read(LogicalDatastoreType.CONFIGURATION, vid).get();
                if (vaddrConfigOptional.isPresent()) {
                    vaddrConfig = vaddrConfigOptional.get();
                }
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Failed to real ID information.");
                rt.close();
                throw new RuntimeException("Failed to read nodes from virtual data store.", e);
            }
            if(vaddrConfig != null){
                LOG.info("Read virtual IP config information : {}", vaddrConfig);
                for(VAddrList vAddrList : vaddrConfig.getVAddrList()){
                    try {
                        //read virtual IP
                        readvirtualipinformation(vAddrList);
                        LOG.info("Read real ID information : {}", vAddrList.getRipsId());

                    } catch (Exception e) {
                        LOG.error("Error initializing real ID ", e);
                    }
                }
            }
            rt.close();
        }

    }

    private void readvirtualipinformation(VAddrList vAddrList){

        synchronized (this) {
            // Read Inventory
            if(vAddrList != null){
                LOG.info("Read virtual IP information : {}", vAddrList);
                for(VIpConf vIpConf : vAddrList.getVIpConf()){
                    try {
                        //initial real IP pool
                        dhcpVirtualStartIp=vIpConf.getVStartIp().getIpv4Address().getValue();
                        dhcpVirtualStopIp=vIpConf.getVEndIp().getIpv4Address().getValue();
                        LOG.info("Start virtual dhcp IP {}", dhcpVirtualStartIp);
                        LOG.info("End virtual dhcp IP {}", dhcpVirtualStopIp);
                        int dhcpserverIPsize = NWUtil.toIPv4Address(NWUtil.toIPv4AddressBytes(dhcpVirtualStopIp))-
                                NWUtil.toIPv4Address(NWUtil.toIPv4AddressBytes(dhcpVirtualStartIp))+1;
                        theVirtualDHCPPool=new DhcpVirtualpool(NWUtil.toIPv4AddressBytes(dhcpVirtualStartIp),dhcpserverIPsize);
                        dhcpVirtualOptLeaseTime=vIpConf.getVIpGlobalPeriod().intValue();
                        LOG.info("Initialize virtual dhcp pool {}", dhcpVirtualOptLeaseTime);

                    } catch (Exception e) {
                        LOG.error("Error initializing virtual services ", e);
                    }
                }
            }
            LOG.info("Virtual Ip pool is: {}", theVirtualDHCPPool);
        }

    }



    public void close() throws Exception {
        LOG.info("DHCP Virtual Config Closed");
    }

    //virtual IP set module
    public String setVirtualDhcpStartIp(String defaultIp){
        this.dhcpVirtualStartIp=defaultIp;
        return getDhcpVirtualStartIp();
    }

    public String setVirtualDhcpStopIp(String defaultIp){
        this.dhcpVirtualStopIp=defaultIp;
        return getDhcpVirtualStopIp();
    }

    public int setVirtualLeaseDuration(int leaseDuration) {
        configureVirtualLeaseDuration(leaseDuration);
        return getVirtualDhcpLeaseTime();
    }

    public String getDhcpVirtualStartIp(){
        return this.dhcpVirtualStartIp;
    }

    public String getDhcpVirtualStopIp(){
        return this.dhcpVirtualStopIp;
    }

    public int getVirtualDhcpLeaseTime() {
        return this.dhcpVirtualOptLeaseTime;
    }

    private void configureVirtualLeaseDuration(int leaseTime) {
        this.dhcpVirtualOptLeaseTime = leaseTime;
        if(leaseTime > 0) {
            this.dhcpVirtualOptRenewalTime = this.dhcpVirtualOptLeaseTime/2;
            this.dhcpVirtualOptRebindingTime = (this.dhcpVirtualOptLeaseTime*7)/8;
        } else {
            this.dhcpVirtualOptRenewalTime = -1;
            this.dhcpVirtualOptRebindingTime = -1;
        }
    }


    public void setVirtualDhcppool(String startingIPv4Address, int size) {
        theVirtualDHCPPool=new DhcpVirtualpool(NWUtil.toIPv4AddressBytes(startingIPv4Address),size);
    }

    public DhcpVirtualpool getVirtualDhcppool() {
        return this.theVirtualDHCPPool;
    }

    //Debug Dhcp. In future, this code should be removed!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    public void debugVirtualIPPool(){
        dhcpVirtualStartIp="50.0.1.1";
        dhcpVirtualStopIp="50.0.1.254";
        LOG.info("Start virtual dhcp IP {}", dhcpVirtualStartIp);
        LOG.info("End virtual dhcp IP {}", dhcpVirtualStopIp);
        int dhcpserverIPsize = NWUtil.toIPv4Address(NWUtil.toIPv4AddressBytes(dhcpVirtualStopIp))-
                NWUtil.toIPv4Address(NWUtil.toIPv4AddressBytes(dhcpVirtualStartIp))+1;
        theVirtualDHCPPool=new DhcpVirtualpool(NWUtil.toIPv4AddressBytes(dhcpVirtualStartIp),dhcpserverIPsize);
        dhcpVirtualOptLeaseTime=2000;
        LOG.info("Initialize virtual dhcp pool {}", theVirtualDHCPPool);

    }

}
