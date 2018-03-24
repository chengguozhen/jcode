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
import org.opendaylight.dhcp.dhcpcode.DHCPMConstants;
import org.opendaylight.dhcp.util.NWUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dipapp.address.config.rev150105.Subnets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dipapp.address.config.rev150105.addrs.domain.IpConf;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.annotation.Nonnull;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class DhcpConfig implements DataTreeChangeListener<Subnets> {


    private static final Logger LOG = LoggerFactory.getLogger(DhcpConfig.class);
    private final DataBroker dataBroker;
    private final InstanceIdentifier<Subnets> rid;
    private final ListenerRegistration listenerReg;
    private Dhcppool theDHCPPool;


    private String dhcpServerIp="50.0.0.254";
    private String dhcpCidr="50.0.0.254/24";
    private List<String> dhcpDnsServer= Arrays.asList("1.1.1.1","1.1.1.2");

    private String dhcpServerMac="00:0c:29:09:1a:17";
    private String dhcpBroadcastIP="50.0.0.255";
    private String dhcpSubNetMast="255.255.255.0";


    private int dhcpOptHoldTime;
    private int dhcpOptLeaseTime;
    private int dhcpOptRenewalTime;
    private int dhcpOptRebindingTime;

    private String dhcpOptDefDomainName;
    private String dhcpHostName;
    private String dhcpStartIp;
    private String dhcpStopIp;



    /**
    * @param db - dataBroker reference
    */
    public DhcpConfig(final DataBroker db) {
        dataBroker = db;
        rid = InstanceIdentifier.create(Subnets.class);
        listenerReg = dataBroker.registerDataTreeChangeListener(
                new DataTreeIdentifier<Subnets>(LogicalDatastoreType.CONFIGURATION,rid),this);
        configureLeaseDuration(DHCPMConstants.DEFAULT_LEASE_TIME);
    }

    @Override
    public void onDataTreeChanged(@Nonnull Collection<DataTreeModification<Subnets>> changes) {
        for (final DataTreeModification<Subnets> change : changes) {
            final DataObjectModification<Subnets> rootNode = change.getRootNode();
            switch (rootNode.getModificationType()) {
                case SUBTREE_MODIFIED:
                    LOG.info("Real IP config reader:node {} modified", rootNode.getIdentifier());
                    readrealipinformation();
                    break;
                case WRITE:
                    LOG.info("Real IP config reader:node {} created", rootNode.getIdentifier(), rootNode.getDataAfter());
                    readrealipinformation();
                    break;
                default:
                    LOG.error("Real IP ERROR:Data changed,but there are some errors");
            }
        }
    }

    public void readrealipinformation(){

        synchronized (this) {
            // Read Inventory
            Subnets subnets = null;
            ReadOnlyTransaction rt = dataBroker.newReadOnlyTransaction();
            try {
                Optional<Subnets> subnetsOptional =  rt.read(LogicalDatastoreType.CONFIGURATION, rid).get();
                if (subnetsOptional.isPresent()) {
                    subnets = subnetsOptional.get();
                }
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Failed to read nodes from Operation data store.");
                rt.close();
                throw new RuntimeException("Failed to read nodes from Operation data store.", e);
            }
            if(subnets != null){
                LOG.info("Read real IP information : {}", subnets);
                for(IpConf ipConf : subnets.getIpConf()){
                    try {
                        //initial real IP pool
                        dhcpStartIp=ipConf.getStartIp().getIpv4Address().getValue();
                        dhcpStopIp=ipConf.getEndIp().getIpv4Address().getValue();
                        LOG.info("Start real dhcp IP {}", dhcpStartIp);
                        LOG.info("End real dhcp IP {}", dhcpStopIp);
                        int dhcpserverIPsize = NWUtil.toIPv4Address(NWUtil.toIPv4AddressBytes(dhcpStopIp))-
                                NWUtil.toIPv4Address(NWUtil.toIPv4AddressBytes(dhcpStartIp))+1;
                        theDHCPPool=new Dhcppool(NWUtil.toIPv4AddressBytes(dhcpStartIp),dhcpserverIPsize);
                        dhcpOptLeaseTime=ipConf.getIpGlobalPeriod().intValue();
                        dhcpOptHoldTime=ipConf.getIpGlobalPeriod().intValue();
                        dhcpOptDefDomainName=ipConf.getDomainPrefix();
                        dhcpHostName=ipConf.getDomainPrefix();
                        LOG.info("Initialize real dhcp pool {}", theDHCPPool);

                    } catch (Exception e) {
                        LOG.error("Error initializing real services ", e);
                    }
                }
            }
            rt.close();
            LOG.info("Real Ip pool is: {}", theDHCPPool);
        }

    }



    public void close() throws Exception {
        LOG.info("DHCP real config Closed");
    }



    private String getDefaultDhcpFlowRef(BigInteger dpId, long tableId) {
        return new StringBuffer().append(DHCPMConstants.FLOWID_PREFIX).append(dpId)
                        .append(".").append(tableId).toString();
    }



    public int setLeaseDuration(int leaseDuration) {
        configureLeaseDuration(leaseDuration);
        return getDhcpLeaseTime();
    }

    public int setDhcpOptHoldTime(int dhcpOptHoldTime){
        this.dhcpOptHoldTime = dhcpOptHoldTime;
        return getDhcpOptHoldTime();
    }

    public String setDefaultDomain(String defaultDomain) {
        this.dhcpOptDefDomainName = defaultDomain;
        return getDhcpDefDomain();
    }

    public String setDhcpServerDhcpServerIp(String defaultIp){
        this.dhcpServerIp=defaultIp;
        return getDhcpServerDhcpServerIp();
    }

    public String setDhcpServerMac(String defaultMac){
        this.dhcpServerMac=defaultMac;
        return getDhcpServerMac();
    }

    public String setDhcpBroadcast(String defaultIP){
        this.dhcpBroadcastIP=defaultIP;
        return getDhcpBroadcastIP();
    }

    public String setDhcpSubNetMask(String defaultIP){
        this.dhcpSubNetMast=defaultIP;
        return getDhcpSubNetMast();
    }


//    public <String> setDhcpDnsServer(<String> defaultIp){
//        this.dhcpDnsServer=defaultIp;
//        return getDhcpDnsServer();
//    }

    public String setDhcpCidr(String defaultCidr){
        this.dhcpCidr=defaultCidr;
        return getDhcpCidr();
    }

    public String setDhcpStartIp(String defaultIp){
        this.dhcpStartIp=defaultIp;
        return getDhcpStartIpIp();
    }

    public String setDhcpStopIp(String defaultIp){
        this.dhcpStopIp=defaultIp;
        return getDhcpStopIp();
    }

    public String getDhcpServerDhcpServerIp(){
        return this.dhcpServerIp;
    }

    public String getDhcpServerMac(){
        return this.dhcpServerMac;
    }

    public String getDhcpBroadcastIP(){
        return this.dhcpBroadcastIP;
    }

    public String getDhcpSubNetMast(){
        return this.dhcpSubNetMast;
    }

    public String getDhcpHostName(){
        return this.dhcpHostName;
    }

    public List<String> getDhcpDnsServer(){
        return this.dhcpDnsServer;
    }

    public String getDhcpCidr(){
        return this.dhcpCidr;
    }

    public String getDhcpStartIpIp(){
        return this.dhcpStartIp;
    }

    public String getDhcpStopIp(){
        return this.dhcpStopIp;
    }

    public int getDhcpLeaseTime() {
        return this.dhcpOptLeaseTime;
    }

    public int getDhcpOptHoldTime() {
        return this.dhcpOptHoldTime;
    }

    public int getDhcpRenewalTime() {
        return this.dhcpOptLeaseTime;
    }

    public int getDhcpRebindingTime() {
        return this.dhcpOptLeaseTime;
    }

    public String getDhcpDefDomain() {
        return this.dhcpOptDefDomainName;
    }

    private void configureLeaseDuration(int leaseTime) {
        this.dhcpOptLeaseTime = leaseTime;
        if(leaseTime > 0) {
            this.dhcpOptRenewalTime = this.dhcpOptLeaseTime/2;
            this.dhcpOptRebindingTime = (this.dhcpOptLeaseTime*7)/8;
        } else {
            this.dhcpOptRenewalTime = -1;
            this.dhcpOptRebindingTime = -1;
        }
    }

    //Dhcppool!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    public void setDhcppool(String startingIPv4Address, int size) {
        theDHCPPool=new Dhcppool(NWUtil.toIPv4AddressBytes(startingIPv4Address),size);
    }

    public Dhcppool getDhcppool() {
        return this.theDHCPPool;
    }


    //Debug Dhcp. In future, this code should be removed!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    public void debugRealIPPool(){
        dhcpStartIp="50.0.0.1";
        dhcpStopIp="50.0.0.253";
        LOG.info("Start real dhcp IP {}", dhcpStartIp);
        LOG.info("End real dhcp IP {}", dhcpStopIp);
        int dhcpserverIPsize = NWUtil.toIPv4Address(NWUtil.toIPv4AddressBytes(dhcpStopIp))-
                NWUtil.toIPv4Address(NWUtil.toIPv4AddressBytes(dhcpStartIp))+1;
        theDHCPPool=new Dhcppool(NWUtil.toIPv4AddressBytes(dhcpStartIp),dhcpserverIPsize);
        dhcpOptLeaseTime=20000;
        dhcpOptHoldTime=20000;
        dhcpOptDefDomainName="testIPPool";
        dhcpHostName="testIPPool";
        LOG.info("Initialize real dhcp pool {}", theDHCPPool);

    }

}
