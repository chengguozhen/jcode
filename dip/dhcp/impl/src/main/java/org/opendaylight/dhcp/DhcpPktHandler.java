/*
 * Copyright (c) 2015 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.dhcp;

import java.net.UnknownHostException;
import java.util.*;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;
import org.opendaylight.controller.liblldp.NetUtils;
import org.opendaylight.controller.liblldp.PacketException;
import org.opendaylight.dhcp.Virtual.DhcpVirtualbinding;
import org.opendaylight.dhcp.Inventory.InventoryReader;
import org.opendaylight.dhcp.dhcpcode.DHCP;
import org.opendaylight.dhcp.dhcpcode.DHCPConstants;
import org.opendaylight.dhcp.dhcpcode.DHCPMConstants;
import org.opendaylight.dhcp.dhcpcode.DHCPUtils;
import org.opendaylight.dhcp.util.NWUtil;
import org.opendaylight.dip.main.impl.packet.*;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.OutputPortValues;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.output.action._case.OutputActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import com.google.common.util.concurrent.CheckedFuture;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dipapp.rev150105.VAddressCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dipapp.rev150105.VAddressCapableNodeConnectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dipapp.rev150105.v.address.node.connector.VipAddressEntities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dipapp.rev150105.v.address.node.connector.VipAddressEntitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dipapp.rev150105.v.address.node.connector.VipAddressEntitiesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.rpc.get.change.ip.rev170528.ChangeIpInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.rpc.get.change.ip.rev170528.ChangeIpOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.rpc.get.change.ip.rev170528.ChangeIpOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.rpc.get.change.ip.rev170528.GetChangeIpService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.rpc.judge.real.ip.rev170528.JudgeRealIpService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.rpc.judge.real.ip.rev170528.RealJudgeIpInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.rpc.judge.real.ip.rev170528.RealJudgeIpOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.rpc.judge.real.ip.rev170528.RealJudgeIpOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.rpc.judge.virtual.ip.rev170528.JudgeVirtualIpService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.rpc.judge.virtual.ip.rev170528.VirtualJudgeIpInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.rpc.judge.virtual.ip.rev170528.VirtualJudgeIpOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.rpc.judge.virtual.ip.rev170528.VirtualJudgeIpOutputBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Random;
import java.util.concurrent.Future;

public class DhcpPktHandler implements AutoCloseable, PacketProcessingListener,JudgeVirtualIpService,JudgeRealIpService,GetChangeIpService {

    private static final Logger LOG = LoggerFactory.getLogger(DhcpPktHandler.class);
    private final DataBroker dataBroker;
    private DhcpConfig dhcpConfig;
    private DhcpVConfig dhcpVConfig;
    private DhcpVupConfig dhcpVupConfig;
    private boolean computeUdpChecksum = true;
    private PacketProcessingService pktService;
    private InventoryReader inventoryReader;
    private byte[] desiredIPAddr;
    private Ethernet eth;
    private PacketReceived rawpkt;
    boolean sendAck = true;
    private static Runnable leasePolicePatrol;

    public DhcpPktHandler(final PacketProcessingService packetProcessingService,
                          //final DhcpConfig dhcpConfig,final DhcpVConfig dhcpVConfig,final DhcpVupConfig dhcpVupConfig,
                          final DataBroker dataBroker) {
        this.pktService = packetProcessingService;
        //this.dhcpConfig = dhcpConfig;
        //this.dhcpVConfig=dhcpVConfig;
        this.dataBroker=dataBroker;
        //this.dhcpVupConfig=dhcpVupConfig;
        dhcpConfig=new DhcpConfig(dataBroker);
        dhcpVConfig=new DhcpVConfig(dataBroker);
        dhcpVupConfig=new DhcpVupConfig(dataBroker);

        //Debug Dhcp. In future, this code should be removed!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

        LOG.info("Debug Code");
        dhcpConfig.debugRealIPPool();
        dhcpVConfig.debugVirtualIPPool();

        //leasePolicePatrol = new DHCPLeasePolice();

    }

    //TODO: Handle this in a separate thread
    @Override
    public void onPacketReceived(PacketReceived packet) {

//        LOG.info("Pkt received: {}", packet);
        Class<? extends PacketInReason> pktInReason = packet.getPacketInReason();
        if (isPktInReasonSendtoCtrl(pktInReason)) {
            byte[] inPayload = packet.getPayload();
            Ethernet ethPkt = new Ethernet();

            try {
                ethPkt.deserialize(inPayload, 0, inPayload.length * NetUtils.NumBitsInAByte);
            } catch (Exception e) {
                LOG.info("Failed to decode DHCP Packet", e);
                return;
            }

            try {
                DHCP pktIn;
                pktIn = getDhcpPktIn(ethPkt);

                //globe eth variant
                eth=ethPkt;
                rawpkt=packet;

                if (pktIn != null) {

                    if(dhcpConfig.getDhcppool()==null){
                        LOG.info("Please input real IP pool");
                    }

                    if(dhcpVConfig.getVirtualDhcppool()==null&& dhcpVupConfig.getVirtualIpup()){
                        LOG.info("Please input virtual IP pool");
                    }

                    DHCP replyPkt = handleDhcpPacket(pktIn);
                    byte[] pktOut = getDhcpPacketOut(replyPkt, ethPkt,
                            NWUtil.toByteMacAddress(dhcpConfig.getDhcpServerMac()));

                    NodeConnectorRef ingress =packet.getIngress();
                    NodeConnectorRef outgress =packet.getIngress();
                    String nodemac =NWUtil.toStringMacAddress(ethPkt.getSourceMACAddress());

                    String nodeId = ingress.getValue().firstIdentifierOf(Node.class).firstKeyOf(Node.class, NodeKey.class).getId()
                            .getValue();

                    if (ingress != null) {
                        sendPacketOut(pktOut, ingress, outgress);
                        LOG.info("Sending Dhcp packet for node {}. Its Mac is{}",
                                nodeId, nodemac);
//                        LOG.info("Ingress is {}.",
//                                ingress);
                    } else {
                        LOG.info("Cannot send packet out or flood as controller node connector is not available for node {}.",
                                nodeId);
                    }


                }
            } catch (Exception e) {
                LOG.info("Failed to get DHCP Reply");
                LOG.info("Reason for failure ", e);
            }


        }
    }


    /**
     * Sends the specified packet on the specified port.
     *
     * @param payload
     *            The payload to be sent.
     * @param ingress
     *            The NodeConnector where the payload came from.
     * @param egress
     *            The NodeConnector where the payload will go.
     */
    private void sendPacketOut(byte[] payload, NodeConnectorRef ingress, NodeConnectorRef egress) {
        if (ingress == null || egress == null)
            return;

        List<Action> actions = new ArrayList<>();
        actions.add(getNormalAction());

        InstanceIdentifier<Node> egressNodePath = getNodePath(egress.getValue());
        TransmitPacketInput input = new TransmitPacketInputBuilder() //
                .setPayload(payload)
                .setNode(new NodeRef(egressNodePath))
                .setEgress(egress)
                .setIngress(ingress)
                .setAction(actions)
                .build();
        pktService.transmitPacket(input);
    }

    private Action getNormalAction() {
        return new ActionBuilder()
                .setOrder(0)
                .setKey(new ActionKey(0))
                .setAction(new OutputActionCaseBuilder()
                        .setOutputAction(new OutputActionBuilder()
                                .setMaxLength(0xffff)
                                .setOutputNodeConnector(new Uri(OutputPortValues.INPORT.toString()))
                                .build())
                        .build())
                .build();
    }




    private DHCP handleDhcpPacket(DHCP dhcpPkt) {
//        LOG.info("DHCP pkt rcvd {}", dhcpPkt);
        byte msgType = dhcpPkt.getMsgType();
        if (msgType == DHCPConstants.MSG_DECLINE) {
            LOG.info("DHCPDECLINE received");
            return null;
        } else if (msgType == DHCPConstants.MSG_RELEASE) {
            LOG.info("DHCPRELEASE received");
            return null;
        }
        DhcpInfo dhcpInfo = getDhcpInfo(dhcpPkt);

        DHCP reply = null;
        if (dhcpInfo != null) {
            if (msgType == DHCPConstants.MSG_DISCOVER) {
                reply = getReplyToDiscover(dhcpPkt, dhcpInfo);
            } else if (msgType == DHCPConstants.MSG_REQUEST) {
                reply = getReplyToRequest(dhcpPkt, dhcpInfo);
            }
        }

        return reply;
    }

    private DhcpInfo getDhcpInfo(DHCP dhcppkt) {

        if (dhcppkt.getMsgType() == DHCPConstants.MSG_DISCOVER){
            byte[] yiaddr = getDhcpDiscoveryiaddr(dhcppkt);
            String clientIp=NWUtil.fromIPv4Address(NWUtil.toIPv4Address(yiaddr));
            String serverIp = dhcpConfig.getDhcpServerDhcpServerIp();
            String cidr=dhcpConfig.getDhcpCidr();
            List<String> dnsserver=dhcpConfig.getDhcpDnsServer();
            DhcpInfo dhcpInfo = new DhcpInfo();
            dhcpInfo.setClientIp(clientIp).setServerIp(serverIp).setCidr(cidr).setDnsServers(dnsserver).setGatewayIp(serverIp);
            LOG.info("dhcpInfo{}",dhcpInfo);
            return dhcpInfo;
        } else if (dhcppkt.getMsgType() == DHCPConstants.MSG_REQUEST){
            byte[] yiaddr = getDhcpRequestyiaddr(dhcppkt);
            String clientIp=NWUtil.fromIPv4Address(NWUtil.toIPv4Address(yiaddr));
            String serverIp = dhcpConfig.getDhcpServerDhcpServerIp();
            String cidr=dhcpConfig.getDhcpCidr();
            List<String> dnsserver=dhcpConfig.getDhcpDnsServer();
            DhcpInfo dhcpInfo = new DhcpInfo();
            dhcpInfo.setClientIp(clientIp).setServerIp(serverIp).setCidr(cidr).setDnsServers(dnsserver).setGatewayIp(serverIp);
            LOG.info("dhcpInfo{}",dhcpInfo);
            return dhcpInfo;
        }
        return null;
    }

    private byte[] getDhcpDiscoveryiaddr(DHCP dhcppkt){
        int xid = dhcppkt.getXid();
        byte[] yiaddr = dhcppkt.getYiaddr();
        byte[] giaddr = dhcppkt.getGiaddr();
        byte[] chaddr = dhcppkt.getChaddr();
        if (dhcppkt.containsOption(DHCPConstants.OPT_REQUESTED_ADDRESS)){
            desiredIPAddr = dhcppkt.getOptionInetAddr(DHCPConstants.OPT_REQUESTED_ADDRESS).getAddress();
            LOG.info("Got requested IP");
        }

        synchronized (dhcpConfig.getDhcppool()) {
            if (!dhcpConfig.getDhcppool().hasAvailableAddresses()) {
                LOG.info("DHCP Pool is full! Consider increasing the pool size.");
                LOG.info("Device with MAC {} was not granted an IP lease", NWUtil.toStringMacAddress(chaddr));
                return null;
            }
            Dhcpbinding lease = dhcpConfig.getDhcppool().getSpecificAvailableLease(desiredIPAddr, chaddr);

            if (lease != null) {
                LOG.info("Checking new lease with specific IP");
                dhcpConfig.getDhcppool().setDhcpbinding(lease, chaddr, dhcpConfig.getDhcpOptHoldTime());
                yiaddr = lease.getIPv4AddressBytes();
                LOG.info("Got new lease for {}.", NWUtil.toStringIpAddress(yiaddr));
            } else {
                LOG.info("Checking new lease for any IP");
                lease = dhcpConfig.getDhcppool().getAnyAvailableLease(chaddr);
                dhcpConfig.getDhcppool().setDhcpbinding(lease, chaddr, dhcpConfig.getDhcpOptHoldTime());
                yiaddr = lease.getIPv4AddressBytes();
                LOG.info("Got new lease for {}.", NWUtil.toStringIpAddress(yiaddr));
            }
        }
        return yiaddr;
    }

    private byte[] getDhcpRequestyiaddr(DHCP dhcppkt){
        LOG.info(": DHCP REQUEST received");
        int xid = dhcppkt.getXid();
        byte[] yiaddr = dhcppkt.getYiaddr();
        byte[] giaddr = dhcppkt.getGiaddr();
        byte[] chaddr = dhcppkt.getChaddr();
        chaddr=NWUtil.toByteMacAddress(NWUtil.toStringMacAddress(chaddr).toLowerCase().trim());


        if (dhcppkt.containsOption(DHCPConstants.OPT_REQUESTED_ADDRESS)) {
            desiredIPAddr = dhcppkt.getOptionInetAddr(DHCPConstants.OPT_REQUESTED_ADDRESS).getAddress();
            if (dhcpConfig.getDhcppool().getDhcpbindingFromMAC(chaddr)!=null){
                if (!Arrays.equals(desiredIPAddr,
                        dhcpConfig.getDhcppool().getDhcpbindingFromMAC(chaddr).getIPv4AddressBytes())) {
                    // This client wants a different IP than what we have on file, so cancel its HOLD lease now (if we have one)
                    dhcpConfig.getDhcppool().cancelLeaseOfMAC(chaddr);
                    LOG.info(": DHCP OPTREQUEST ADDRESS");
                    return null;
                }
            }
        } else if (dhcppkt.containsOption(DHCPConstants.OPT_SERVER_IDENTIFIER)) {
            if (!Arrays.equals(NetUtils.longToByteArray6(NWUtil.convertInetAddressToLong(dhcppkt.
                            getOptionInetAddr(DHCPConstants.OPT_SERVER_IDENTIFIER))),
                    NWUtil.toIPv4AddressBytes(dhcpConfig.getDhcpServerDhcpServerIp()))) {
                // We're not the DHCPServer the client wants to use, so cancel its HOLD lease now and ignore the client
                dhcpConfig.getDhcppool().cancelLeaseOfMAC(chaddr);
                LOG.info(": DHCP OPTServer IDENIFIER");
                return null;
            }
        }

        synchronized (dhcpConfig.getDhcppool()) {
            if (!dhcpConfig.getDhcppool().hasAvailableAddresses()) {
                LOG.info("DHCP Pool is full! Consider increasing the pool size.");
                LOG.info("Device with MAC {} was not granted an IP lease", NWUtil.toStringMacAddress(chaddr).toLowerCase());
                return null;
            }
            Dhcpbinding lease;
            // Get any binding, in use now or not
            if (NWUtil.toIPv4Address(desiredIPAddr)!=NWUtil.toIPv4Address(NWUtil.toIPv4AddressBytes("0.0.0.0"))) {
                lease = dhcpConfig.getDhcppool().getDhcpbindingFromIPv4(desiredIPAddr);
            } else {
                lease = dhcpConfig.getDhcppool().getAnyAvailableLease(chaddr);
            }
            // This IP is not in our allocation range
            if (lease == null) {
                LOG.info("The IP {} is not in the range {} to {}", NWUtil.fromIPv4Address(NWUtil.toIPv4Address(desiredIPAddr)),
                        NWUtil.fromIPv4Address(NWUtil.toIPv4Address(NWUtil.toIPv4AddressBytes(dhcpConfig.getDhcpStartIpIp()))),
                        NWUtil.fromIPv4Address(NWUtil.toIPv4Address(NWUtil.toIPv4AddressBytes(dhcpConfig.getDhcpStopIp()))));
                LOG.info("Device with MAC {} was not granted an IP lease", NWUtil.toStringMacAddress(chaddr).toLowerCase());
                sendAck = false;
                // Determine if the IP in the binding we just retrieved is okay to allocate to the MAC requesting it
            } else if (!(NWUtil.toStringMacAddress(lease.getMACAddressBytes()).compareTo(NWUtil.toStringMacAddress(chaddr))==0)
                    && lease.isActiveLease()) {
                LOG.info("Tried to REQUEST an IP that is currently assigned to another MAC");
                LOG.info("Device with MAC {} was not granted an IP lease", NWUtil.toStringMacAddress(chaddr).toLowerCase());
                sendAck = false;
                // Check if we want to renew the MAC's current lease
            } else if ((NWUtil.toStringMacAddress(lease.getMACAddressBytes()).compareTo(NWUtil.toStringMacAddress(chaddr))==0)
                    && lease.isActiveLease()) {
                LOG.info("Renewing lease for MAC {}", NWUtil.toStringMacAddress(chaddr));
                dhcpConfig.getDhcppool().renewLease(lease.getIPv4AddressBytes(), dhcpConfig.getDhcpLeaseTime());
                yiaddr = lease.getIPv4AddressBytes();
                LOG.info("Finalized renewed lease for {} ", NWUtil.toStringIpAddress(yiaddr));
                sendAck = true;

                //Virtual up or not
                if (dhcpVupConfig.getVirtualIpup()){
                    LOG.info("The MTD model is: {}", dhcpVupConfig.getVirtualIpup());
                    if(dhcpVConfig.getVirtualDhcppool().hasAvailableAddresses()){
                        //Virtual IP allocate
                        byte[] vyiaddr=virtualIPAssignment(chaddr,dhcpVConfig.getVirtualDhcpLeaseTime());

                        //Store in data store
                        storerIPandvIP(chaddr,yiaddr,vyiaddr,dhcpVConfig.getVirtualDhcpLeaseTime());
                    }else{
                        LOG.info("Available IP is null in Virtual IP pool");
                    }

                } else{
                    LOG.info("The MTD model is: {}", dhcpVupConfig.getVirtualIpup());
                    byte[] vyiaddr=NWUtil.toIPv4AddressBytes("0.0.0.0");
                    //Store in data store
                    storerIPandvIP(chaddr,yiaddr,vyiaddr,dhcpConfig.getDhcpLeaseTime());
                }

                // Check if we want to create a new lease for the MAC
            } else if (!lease.isActiveLease()){
                LOG.info("Assigning new lease for MAC {}", NWUtil.toStringMacAddress(chaddr).toLowerCase());
                dhcpConfig.getDhcppool().setDhcpbinding(lease, chaddr, dhcpConfig.getDhcpLeaseTime());
                yiaddr = lease.getIPv4AddressBytes();
                LOG.info("Finalized new lease for {}", NWUtil.toStringIpAddress(yiaddr));
                sendAck = true;

                //Virtual up or not
                if (dhcpVupConfig.getVirtualIpup()) {
                    LOG.info("The MTD model is: {}", dhcpVupConfig.getVirtualIpup());
                    //Virtual IP allocate
                    if(dhcpVConfig.getVirtualDhcppool().hasAvailableAddresses()){
                        byte[] vyiaddr=virtualIPAssignment(chaddr,dhcpVConfig.getVirtualDhcpLeaseTime());

                        //Store in data store
                        storerIPandvIP(chaddr,yiaddr,vyiaddr,dhcpVConfig.getVirtualDhcpLeaseTime());
                    }else {
                        LOG.info("Available IP is null in Virtual IP pool");
                    }

                } else {
                    LOG.info("The MTD model is: {}", dhcpVupConfig.getVirtualIpup());
                    byte[] vyiaddr=NWUtil.toIPv4AddressBytes("0.0.0.0");
                    //Store in data store
                    storerIPandvIP(chaddr,yiaddr,vyiaddr,dhcpConfig.getDhcpLeaseTime());
                }

            } else {
                LOG.info("Don't know how we got here");
                return null;
            }
        }
        return yiaddr;
    }

    private byte[] virtualIPAssignment(byte[] mac, int time) {
        DhcpVirtualbinding vlease = dhcpVConfig.getVirtualDhcppool().getAnyAvailableLease(mac);
        dhcpVConfig.getVirtualDhcppool().setVitualDhcpbinding(vlease, mac, time);
        byte[] vyiaddr = vlease.getIPv4AddressBytes();
        LOG.info("Assigned a virtual IP {} with {} holding time to a real MAC {}",
                NWUtil.toStringIpAddress(vyiaddr), dhcpVConfig.getVirtualDhcpLeaseTime(),
                NWUtil.toStringMacAddress(mac).toLowerCase());
        LOG.info("The virtual pool is {}", dhcpVConfig.getVirtualDhcppool());
        return vyiaddr;
    }

    private void storerIPandvIP(byte[] mac, byte[] yiaddr, byte[] vyiaddr,int time) {
        DhcpVirtualbinding vlease = dhcpVConfig.getVirtualDhcppool().getAnyAvailableLease(mac);
        String vitualDomain;
        int Randommax=1000;
        int Randommin=0;
        Random random=new Random();
        int virtualNum=random.nextInt(Randommax)%(Randommax-Randommin+1)+Randommin;
        vitualDomain="ndsc"+virtualNum+".";

        VipAddressEntities newVip = new VipAddressEntitiesBuilder()
                .setVip(new IpAddress(new Ipv4Address(NWUtil.toStringIpAddress(vyiaddr).trim())))
                .setKey(new VipAddressEntitiesKey(new MacAddress(NWUtil.toStringMacAddress(eth.getSourceMACAddress()).toLowerCase().trim())))
                .setMac(new MacAddress(NWUtil.toStringMacAddress(eth.getSourceMACAddress()).toLowerCase().trim()))
                .setRip(new IpAddress(new Ipv4Address(new Ipv4Address(NWUtil.toStringIpAddress(yiaddr).trim()))))
                .setVdomain(vitualDomain)
                .setVipLastMutation(time)
                .setVdomainLastMutation(time)
                .setWIp(new IpAddress(new Ipv4Address("0.0.0.0")))
                .build();
        WriteTransaction wt = dataBroker.newWriteOnlyTransaction();

        List<VipAddressEntities> addresses = new ArrayList<>();
        addresses.add(newVip);
        VAddressCapableNodeConnector vAddressCapableNodeConnector = new VAddressCapableNodeConnectorBuilder()
                .setVipAddressEntities(addresses).build();
        InstanceIdentifier<VipAddressEntities> root =
                ((InstanceIdentifier<NodeConnector>) rawpkt.getIngress().getValue())
                        .augmentation(VAddressCapableNodeConnector.class)
                        .child(VipAddressEntities.class,new VipAddressEntitiesKey(newVip.getKey()));

        //store store path
        Uri vipStorePath = rawpkt.getIngress().getValue().firstKeyOf(NodeConnector.class,NodeConnectorKey.class).getId();
        dhcpVConfig.getVirtualDhcppool().getDhcpbindingFromMAC(mac).setStorePath(vipStorePath);

        wt.put(LogicalDatastoreType.OPERATIONAL,root,newVip,true);

        final CheckedFuture writeTxResultFuture = wt.submit();
        Futures.addCallback(writeTxResultFuture, new FutureCallback() {
            @Override
            public void onSuccess(Object o) {
                LOG.info("Bingo:Dhcp real IP and virtual IP have been written successful for tx :{}. Its information is: {}",
                        wt.getIdentifier(),newVip);
            }

            @Override
            public void onFailure(Throwable throwable) {
                LOG.info("Error:Dhcp real IP and virtual IP have been written transaction {} failed",
                        wt.getIdentifier(), throwable.getCause());
            }
        });
    }

    private DHCP getDhcpPktIn(Ethernet ethPkt) {
        LOG.info("Inside getDhcpPktIn ethPkt {}", ethPkt);
        if (ethPkt.getEtherType() == (short)0X8100) {
            ethPkt = (Ethernet)ethPkt.getPayload();
        }
        if (ethPkt.getPayload() instanceof IPv4) {
            IPv4 ipPkt = (IPv4) ethPkt.getPayload();
            if (ipPkt.getPayload() instanceof UDP) {
                UDP udpPkt = (UDP) ipPkt.getPayload();
                if ((udpPkt.getSourcePort() == DHCPMConstants.dhcpClientPort)
                        && (udpPkt.getDestinationPort() == DHCPMConstants.dhcpServerPort)) {
                    LOG.info("Matched dhcpClientPort and dhcpServerPort");
                    byte[] rawDhcpPayload = udpPkt.getRawPayload();
                    DHCP reply = new DHCP();
                    try {
                        reply.deserialize(rawDhcpPayload, 0, rawDhcpPayload.length);
                    } catch (PacketException e) {
                        LOG.info("Failed to deserialize DHCP pkt");
                        LOG.info("Reason for failure ", e);
                        return null;
                    }
                    return reply;
                }
            }
        }
        return null;
    }

    private DHCP getReplyToDiscover(DHCP dhcpPkt, DhcpInfo dhcpInfo) {
        DHCP reply = new DHCP();
        reply.setOp(DHCPConstants.BOOTREPLY);
        reply.setHtype(dhcpPkt.getHtype());
        reply.setHlen(dhcpPkt.getHlen());
        reply.setHops((byte) 0);
        reply.setXid(dhcpPkt.getXid());
        reply.setSecs((short) 0);

        reply.setYiaddr(dhcpInfo.getClientIp());
        reply.setSiaddr(dhcpInfo.getServerIp());

        reply.setFlags(dhcpPkt.getFlags());
        reply.setGiaddr(dhcpPkt.getGiaddr());
        reply.setChaddr(dhcpPkt.getChaddr());

        reply.setMsgType(DHCPConstants.MSG_OFFER);
        if(dhcpPkt.containsOption(DHCPConstants.OPT_PARAMETER_REQUEST_LIST)) {
            setParameterListOptions(dhcpPkt, reply, dhcpInfo);
        }
        setCommonOptions(reply, dhcpInfo);
        return reply;
    }

    private DHCP getReplyToRequest(DHCP dhcpPkt, DhcpInfo dhcpInfo) {

        byte[] requestedIp = null;
        DHCP reply = new DHCP();
        reply.setOp(DHCPConstants.BOOTREPLY);
        reply.setHtype(dhcpPkt.getHtype());
        reply.setHlen(dhcpPkt.getHlen());
        reply.setHops((byte) 0);
        reply.setXid(dhcpPkt.getXid());
        reply.setSecs((short) 0);

        reply.setFlags(dhcpPkt.getFlags());
        reply.setGiaddr(dhcpPkt.getGiaddr());
        reply.setChaddr(dhcpPkt.getChaddr());
        byte[] allocatedIp = DHCPUtils.strAddrToByteArray(dhcpInfo.getClientIp());
//        if(Arrays.equals(allocatedIp, dhcpPkt.getCiaddr())) {
//            //This means a renew request
//            sendAck = true;
//        } else {
//            requestedIp = dhcpPkt.getOptionBytes(DHCPConstants.OPT_REQUESTED_ADDRESS);
//            //sendAck = Arrays.equals(allocatedIp, requestedIp);
//        }

        if (sendAck) {
            reply.setCiaddr(dhcpPkt.getCiaddr());
            reply.setYiaddr(dhcpInfo.getClientIp());
            reply.setSiaddr(dhcpInfo.getServerIp());
            reply.setMsgType(DHCPConstants.MSG_ACK);
            if(dhcpPkt.containsOption(DHCPConstants.OPT_PARAMETER_REQUEST_LIST)) {
                setParameterListOptions(dhcpPkt, reply, dhcpInfo);
            }
            setCommonOptions(reply, dhcpInfo);
        }
        else {
            reply.setMsgType(DHCPConstants.MSG_NAK);
        }
        return reply;
    }

    private byte[] getDhcpPacketOut(DHCP reply, Ethernet etherPkt, byte[] phyAddrees) {
        if (reply == null) {
            /*
             * DECLINE or RELEASE don't result in reply packet
             */
            return null;
        }
//        LOG.info("Sending DHCP Pkt {}", reply);
        // create UDP pkt
        UDP udpPkt = new UDP();
        byte[] rawPkt;
        try {
            rawPkt = reply.serialize();
        } catch (PacketException e2) {
            // TODO Auto-generated catch block
            e2.printStackTrace();
            return null;
        }
        udpPkt.setRawPayload(rawPkt);
        udpPkt.setDestinationPort(DHCPMConstants.dhcpClientPort);
        udpPkt.setSourcePort(DHCPMConstants.dhcpServerPort);
        udpPkt.setLength((short) (rawPkt.length + 8));
        //Create IP Pkt
        IPv4 ip4Reply = new IPv4();
        try {
            rawPkt = udpPkt.serialize();
        } catch (PacketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
        short checkSum = 0;
        if(this.computeUdpChecksum) {
            checkSum = computeChecksum(rawPkt, reply.getSiaddr(),
                    NetUtils.intToByteArray4(DHCPMConstants.BCAST_IP));
        }
        udpPkt.setChecksum(checkSum);
        ip4Reply.setPayload(udpPkt);
        ip4Reply.setProtocol(IPProtocols.UDP.byteValue());
        ip4Reply.setSourceAddress(reply.getSiaddrAsInetAddr());
        ip4Reply.setDestinationAddress(DHCPMConstants.BCAST_IP);
        ip4Reply.setTotalLength((short) (rawPkt.length+20));
        ip4Reply.setTtl((byte) 64);
        // create Ethernet Frame
        Ethernet ether = new Ethernet();
        if (etherPkt.getEtherType() == (short)0X8100) {
            IEEE8021Q vlanPacket = (IEEE8021Q) etherPkt.getPayload();
            IEEE8021Q vlanTagged = new IEEE8021Q();
            vlanTagged.setCFI(vlanPacket.getCfi());
            vlanTagged.setPriority(vlanPacket.getPriority());
            vlanTagged.setVlanId(vlanPacket.getVlanId());
            vlanTagged.setPayload(ip4Reply);
            vlanTagged.setEtherType(EtherTypes.IPv4.shortValue());
            ether.setPayload(vlanTagged);
            ether.setEtherType((short) 0X8100);
        } else {
            ether.setEtherType(EtherTypes.IPv4.shortValue());
            ether.setPayload(ip4Reply);
        }
        //TODO:
        ether.setSourceMACAddress(phyAddrees);
        ether.setDestinationMACAddress(etherPkt.getSourceMACAddress());

        try {
            rawPkt = ether.serialize();
        } catch (PacketException e) {
            LOG.info("Failed to serialize ethernet reply",e);
            return null;
        }
        return rawPkt;
    }


    private short computeChecksum(byte[] inData, byte[] srcAddr, byte[] destAddr) {
        short checkSum = (short) 0;
        int sum = 0, carry = 0;
        int wordData, i;

        for (i = 0; i < inData.length - 1; i = i + 2) {
            // Skip, if the current bytes are checkSum bytes
            wordData = ((inData[i] << 8) & 0xFF00) + (inData[i + 1] & 0xFF);
            sum = sum + wordData;
        }

        if (i < inData.length) {
            wordData = ((inData[i] << 8) & 0xFF00) + (0 & 0xFF);
            sum = sum + wordData;
        }

        for (i = 0; i < 4; i = i + 2) {
            wordData = ((srcAddr[i] << 8) & 0xFF00) + (srcAddr[i + 1] & 0xFF);
            sum = sum + wordData;
        }

        for (i = 0; i < 4; i = i + 2) {
            wordData = ((destAddr[i] << 8) & 0xFF00) + (destAddr[i + 1] & 0xFF);
            sum = sum + wordData;
        }
        sum = sum + 17 + inData.length;

        while((sum >> 16) != 0) {
            carry = (sum >> 16);
            sum = (sum & 0xFFFF)+ carry;
        }
        checkSum = (short) ~((short) sum & 0xFFFF);
        if(checkSum == 0) {
            checkSum = (short)0xffff;
        }
        return checkSum;
    }

    private void setCommonOptions(DHCP pkt, DhcpInfo dhcpInfo) {
        pkt.setOptionInt(DHCPConstants.OPT_LEASE_TIME, dhcpConfig.getDhcpLeaseTime());
        if (dhcpConfig.getDhcpDefDomain() != null) {
            pkt.setOptionString(DHCPConstants.OPT_DOMAIN_NAME, dhcpConfig.getDhcpDefDomain());
        }
        if(dhcpConfig.getDhcpLeaseTime() > 0) {
            pkt.setOptionInt(DHCPConstants.OPT_REBINDING_TIME, dhcpConfig.getDhcpRebindingTime());
            pkt.setOptionInt(DHCPConstants.OPT_RENEWAL_TIME, dhcpConfig.getDhcpRenewalTime());
        }
//        SubnetUtils util = null;
//        SubnetInfo info = null;
//        util = new SubnetUtils(dhcpInfo.getCidr());
//        info = util.getInfo();
        String gwIp = dhcpInfo.getGatewayIp();
        List<String> dnServers = dhcpInfo.getDnsServers();
        try {
            /*
             * setParameterListOptions may have initialized some of these
             * options to maintain order. If we can't fill them, unset to avoid
             * sending wrong information in reply.
             */
            if (gwIp != null) {
                pkt.setOptionInetAddr(DHCPConstants.OPT_SERVER_IDENTIFIER, gwIp);
                pkt.setOptionInetAddr(DHCPConstants.OPT_ROUTERS, gwIp);
            } else {
                pkt.unsetOption(DHCPConstants.OPT_SERVER_IDENTIFIER);
                pkt.unsetOption(DHCPConstants.OPT_ROUTERS);
            }
            //xiugaidaima
//            pkt.unsetOption(DHCPConstants.OPT_SUBNET_MASK);
//            pkt.unsetOption(DHCPConstants.OPT_BROADCAST_ADDRESS);
            pkt.setOptionInetAddr(DHCPConstants.OPT_SUBNET_MASK, dhcpConfig.getDhcpSubNetMast());
            pkt.setOptionInetAddr(DHCPConstants.OPT_BROADCAST_ADDRESS, dhcpConfig.getDhcpBroadcastIP());
            pkt.setOptionString(DHCPConstants.OPT_HOST_NAME, dhcpConfig.getDhcpHostName());
//            if (info != null) {
//                pkt.setOptionInetAddr(DHCPConstants.OPT_SUBNET_MASK, info.getNetmask());
//                pkt.setOptionInetAddr(DHCPConstants.OPT_BROADCAST_ADDRESS, info.getBroadcastAddress());
//            } else {
//                pkt.unsetOption(DHCPConstants.OPT_SUBNET_MASK);
//                pkt.unsetOption(DHCPConstants.OPT_BROADCAST_ADDRESS);
//            }
            if ((dnServers != null) && (dnServers.size() > 0)) {
                pkt.setOptionStrAddrs(DHCPConstants.OPT_DOMAIN_NAME_SERVERS, dnServers);
            } else {
                pkt.unsetOption(DHCPConstants.OPT_DOMAIN_NAME_SERVERS);
            }
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void setParameterListOptions(DHCP req, DHCP reply, DhcpInfo dhcpInfo) {
        byte[] paramList = req.getOptionBytes(DHCPConstants.OPT_PARAMETER_REQUEST_LIST);
        for (byte aParamList : paramList) {
            switch (aParamList) {
                case DHCPConstants.OPT_SUBNET_MASK:
                    break;
                case DHCPConstants.OPT_ROUTERS:
                    break;
                case DHCPConstants.OPT_SERVER_IDENTIFIER:
                    break;
                case DHCPConstants.OPT_DOMAIN_NAME_SERVERS:
                    break;
                case DHCPConstants.OPT_BROADCAST_ADDRESS:
                    break;
                case DHCPConstants.OPT_LEASE_TIME:
                    reply.setOptionInt(aParamList, 0);
                    break;
                case DHCPConstants.OPT_RENEWAL_TIME:
                    break;
                case DHCPConstants.OPT_REBINDING_TIME:
                    /* These values will be filled in setCommonOptions
                     * Setting these just to preserve order as
                     * specified in PARAMETER_REQUEST_LIST.
                     */
                    reply.setOptionInt(aParamList, 0);
                    break;
                case DHCPConstants.OPT_DOMAIN_NAME:
                    reply.setOptionString(aParamList, " ");
                    break;
                case DHCPConstants.OPT_CLASSLESS_ROUTE:
                    break;
                default:
                    //LOG.info("DHCP Option code {} not supported yet", paramList[i]);
                    break;
            }
        }
    }


    private boolean isPktInReasonSendtoCtrl(Class<? extends PacketInReason> pktInReason) {
        return (pktInReason == SendToController.class);
    }
    public void setPacketProcessingService(PacketProcessingService packetService) {
        this.pktService = packetService;
    }
    public void setDhcpManager(DhcpConfig dhcpConfig) {
        this.dhcpConfig = dhcpConfig;
    }
    private void refreshInventoryReader() {
        inventoryReader.setRefreshData(true);
    }

    private InstanceIdentifier<Node> getNodePath(final InstanceIdentifier<?> nodeChild) {
        return nodeChild.firstIdentifierOf(Node.class);
    }

    //Virtual IP judge
    private VirtualJudgeIpOutput virtualJudgeIp(IpAddress input) {
        VirtualJudgeIpOutputBuilder virtualJudgeIpOutputBuilder = new VirtualJudgeIpOutputBuilder();
        try {
            byte[] targetIp=NWUtil.toIPv4AddressBytes(input.getIpv4Address().getValue());
            if(dhcpVConfig.getVirtualDhcppool().isIPv4Leased(targetIp)){
                DhcpVirtualbinding dhcpVirtualbinding=dhcpVConfig.getVirtualDhcppool().getDhcpbindingFromIPv4(targetIp);
                if (dhcpConfig.getDhcppool().getDhcpbindingFromMAC(dhcpVirtualbinding.getMACAddressBytes())!=null){
                    Dhcpbinding dhcpbinding=dhcpConfig.getDhcppool().getDhcpbindingFromMAC(dhcpVirtualbinding.getMACAddressBytes());
                    String targetIpst=NWUtil.toStringIpAddress(dhcpbinding.getIPv4AddressBytes()).trim();
                    virtualJudgeIpOutputBuilder.setRealIp(new IpAddress(new Ipv4Address(targetIpst)));
                    virtualJudgeIpOutputBuilder.setStorePath(dhcpVirtualbinding.getStorePath());
                    virtualJudgeIpOutputBuilder.setMac(new MacAddress(dhcpbinding.getMACAddressString().toLowerCase().trim()));
                    virtualJudgeIpOutputBuilder.setSuccess(true);
                    LOG.info("Real IP has been found and its datapath is {}, its mac is {}",dhcpVirtualbinding.getStorePath(),
                            dhcpbinding.getMACAddressString().toLowerCase().trim());
                }else {
                    LOG.info("Real IP is not dispatched");
                }
            }
            else {
                virtualJudgeIpOutputBuilder.setRealIp(new IpAddress(new Ipv4Address("0.0.0.0")));
                virtualJudgeIpOutputBuilder.setSuccess(false);
                LOG.info("Virtual IP is not dispatched");
            }
        } catch (Exception e) {
            virtualJudgeIpOutputBuilder.setSuccess(false);
        }
        return virtualJudgeIpOutputBuilder.build();
    }

    @Override
    public Future<RpcResult<VirtualJudgeIpOutput>> virtualJudgeIp(VirtualJudgeIpInput input) {
        //Ip config Rpc
        SettableFuture<RpcResult<VirtualJudgeIpOutput>> future = SettableFuture.create();
        VirtualJudgeIpOutput result = virtualJudgeIp(input.getVirtualIp());
        future.set(RpcResultBuilder.success(result).build());

        //return Ip config Rpc
        return future;
    }

    //Real IP judge
    private RealJudgeIpOutput realJudgeIp(IpAddress input) {
        RealJudgeIpOutputBuilder realJudgeIpOutputBuilder = new RealJudgeIpOutputBuilder();
        try {
            byte[] targetIp=NWUtil.toIPv4AddressBytes(input.getIpv4Address().getValue());
            if(dhcpConfig.getDhcppool().isIPv4Leased(targetIp)){
                Dhcpbinding dhcpbinding=dhcpConfig.getDhcppool().getDhcpbindingFromIPv4(targetIp);
                if (dhcpVConfig.getVirtualDhcppool().getDhcpbindingFromMAC(dhcpbinding.getMACAddressBytes())!=null) {
                    DhcpVirtualbinding dhcpVirtualbinding = dhcpVConfig.getVirtualDhcppool().getDhcpbindingFromMAC(dhcpbinding.getMACAddressBytes());
                    String targetIpst=NWUtil.toStringIpAddress(dhcpVirtualbinding.getIPv4AddressBytes()).trim();
                    realJudgeIpOutputBuilder.setVirtualIp(new IpAddress(new Ipv4Address(targetIpst)));
                    realJudgeIpOutputBuilder.setStorePath(dhcpVirtualbinding.getStorePath());
                    realJudgeIpOutputBuilder.setMac(new MacAddress(dhcpbinding.getMACAddressString().toLowerCase().trim()));
                    realJudgeIpOutputBuilder.setSuccess(true);
                    LOG.info("Virtual IP has been found and its datapath is {}, its mac is {}",dhcpVirtualbinding.getStorePath(),
                            dhcpbinding.getMACAddressString().toLowerCase().trim());
                }
                else {
                    LOG.info("Virtual IP is not dispatched");
                }
            }
            else {
                realJudgeIpOutputBuilder.setVirtualIp(new IpAddress(new Ipv4Address("0.0.0.0")));
                realJudgeIpOutputBuilder.setSuccess(false);
                LOG.info("Real IP is not dispatched");
            }
        } catch (Exception e) {
            realJudgeIpOutputBuilder.setSuccess(false);
        }
        return realJudgeIpOutputBuilder.build();
    }

    @Override
    public Future<RpcResult<RealJudgeIpOutput>> realJudgeIp(RealJudgeIpInput input) {
        //Ip config Rpc
        SettableFuture<RpcResult<RealJudgeIpOutput>> future = SettableFuture.create();
        RealJudgeIpOutput result = realJudgeIp(input.getRealIp());
        future.set(RpcResultBuilder.success(result).build());

        //return Ip config Rpc
        return future;
    }

    // Virtual IP change
    private ChangeIpOutput changeIpOutput(String input) {
        DhcpVirtualbinding vlease;
        byte[] chaddr=NWUtil.toByteMacAddress(input);
        ChangeIpOutputBuilder changeIpOutputBuilder = new ChangeIpOutputBuilder();
        try {
            byte[] bindip=dhcpVConfig.getVirtualDhcppool().getDhcpbindingFromMAC(chaddr).getIPv4AddressBytes();
            dhcpVConfig.getVirtualDhcppool().cancelLeaseOfMAC(chaddr);
            vlease=dhcpVConfig.getVirtualDhcppool().getAnyAvailableLeaseforDip(chaddr,bindip);
            dhcpVConfig.getVirtualDhcppool().setVitualDhcpbindingDip(vlease,chaddr,dhcpVConfig.getVirtualDhcpLeaseTime());

            //store new Virtual IP information
            //Dhcpbinding dhcpbinding=dhcpConfig.getDhcppool().getDhcpbindingFromMAC(chaddr);
            //storerIPandvIP(chaddr,dhcpbinding.getIPv4AddressBytes(),vlease.getIPv4AddressBytes(),dhcpVConfig.getVirtualDhcpLeaseTime());

            //continue generating changeIpOutputBuilder
            changeIpOutputBuilder.setVip(NWUtil.toStringIpAddress(vlease.getIPv4AddressBytes()));
            changeIpOutputBuilder.setVdomain("testDomain");
            changeIpOutputBuilder.setVipLastMutation(dhcpVConfig.getVirtualDhcpLeaseTime());
            changeIpOutputBuilder.setVdomainLastMutation(dhcpVConfig.getVirtualDhcpLeaseTime());
            //changeIpOutputBuilder.setStorePath(dhcpVConfig.getVirtualDhcppool().getDhcpbindingFromMAC(chaddr).getStorePath());

            changeIpOutputBuilder.setSuccess(true);


        } catch (Exception e) {
            changeIpOutputBuilder.setSuccess(false);
        }
        return changeIpOutputBuilder.build();
    }

    @Override
    public Future<RpcResult<ChangeIpOutput>> changeIp(ChangeIpInput input) {
        //Ip config Rpc
        SettableFuture<RpcResult<ChangeIpOutput>> future = SettableFuture.create();
        ChangeIpOutput result = changeIpOutput(input.getMac());
        future.set(RpcResultBuilder.success(result).build());

        //return Ip config Rpc
        return future;
    }

    @Override
    public void close()  {
        // TODO Auto-generated method stub
    }


    //test code, unstart now!!!!!!!!!!!!!!!!!!!!!!!!!
    //line 107 should be resumed, if this function is going to work
    //cancel some useless lease
//    class DHCPLeasePolice implements Runnable {
//        @Override
//        public void run() {
//            LOG.info("Cleaning any expired DHCP leases...");
//            ArrayList<Dhcpbinding> newAvailableBindings;
//            if(!dhcpConfig.getDhcppool().getPool().isEmpty()){
//                synchronized(dhcpConfig.getDhcppool()) {
//                    // Loop through lease pool and check all leases to see if they are expired
//                    // If a lease is expired, then clean it up and make the binding available
//                    newAvailableBindings = dhcpConfig.getDhcppool().cleanExpiredLeases();
//                }
//                if(!newAvailableBindings.isEmpty()){
//                    for (Dhcpbinding binding : newAvailableBindings) {
//                        LOG.info("MAC " + binding.getMACAddressString() + " has expired");
//                        LOG.info("Lease now available for IP " + binding.getIPv4AddresString());
//                        LOG.info("Lease now available for Virtual IP " + dhcpVConfig.getVirtualDhcppool().
//                                getDhcpbindingFromMAC(binding.getMACAddressBytes()).getIPv4AddresString());
//                        dhcpVConfig.getVirtualDhcppool().cancelLeaseOfMAC(binding.getMACAddressBytes());
//                    }
//                }
//            }
//        }
//    } // END DHCPLeasePolice Class


}