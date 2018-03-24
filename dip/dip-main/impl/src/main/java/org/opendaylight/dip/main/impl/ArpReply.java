/*
 * Copyright © 2017 NDSC and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.dip.main.impl;

import com.google.common.base.Splitter;
import org.opendaylight.controller.liblldp.PacketException;
import org.opendaylight.dip.main.impl.packet.ARP;
import org.opendaylight.dip.main.impl.packet.EtherTypes;
import org.opendaylight.dip.main.impl.packet.Ethernet;
import org.opendaylight.dip.main.impl.packet.IEEE8021Q;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.output.action._case.OutputActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.OutputPortValues;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.ArpPacketListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.ArpPacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.KnownOperation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.arp.packet.received.packet.chain.packet.ArpPacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.PacketChain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.packet.chain.packet.RawPacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.Header8021q;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.KnownEtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.ethernet.packet.received.packet.chain.packet.EthernetPacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.TransmitPacketInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.TransmitPacketInputBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;

/**
 * @author chenyang
 */
public class ArpReply implements ArpPacketListener {

    private static final Logger LOG = LoggerFactory.getLogger(ArpReply.class);
    private final PacketProcessingService packetProcessingService;

    public ArpReply(PacketProcessingService packetProcessingService){
        this.packetProcessingService = packetProcessingService;
    }

    @Override
    public void onArpPacketReceived(ArpPacketReceived packetReceived) {
        if (packetReceived == null || packetReceived.getPacketChain() == null) {
            return;
        }
        LOG.info("ARP:get a arp request");
        RawPacket rawPacket = null;
        EthernetPacket ethernetPacket = null;
        ArpPacket arpPacket = null;
        for (PacketChain packetChain : packetReceived.getPacketChain()) {
            if (packetChain.getPacket() instanceof RawPacket) {
                rawPacket = (RawPacket) packetChain.getPacket();
            } else if (packetChain.getPacket() instanceof EthernetPacket) {
                ethernetPacket = (EthernetPacket) packetChain.getPacket();
            } else if (packetChain.getPacket() instanceof ArpPacket) {
                arpPacket = (ArpPacket) packetChain.getPacket();
            }
        }
        if (rawPacket == null || ethernetPacket == null || arpPacket == null) {
            return;
        }
        if( arpPacket.getOperation().equals(KnownOperation.Request)
                && ("50.0.0.254").equals(arpPacket.getDestinationProtocolAddress()) ){
            LOG.info("ARP: arp request is for gateway");
            ARP reply = new ARP();
            reply.setHardwareType((short)arpPacket.getHardwareType().getIntValue());
            reply.setProtocolType((short)arpPacket.getProtocolType().getIntValue());
            reply.setHardwareAddressLength(arpPacket.getHardwareLength().byteValue());
            reply.setProtocolAddressLength(arpPacket.getProtocolLength().byteValue());
            reply.setOpCode((short)2);
            reply.setSenderHardwareAddress(macChange("78:45:c4:2d:32:1d"));
            reply.setSenderProtocolAddress(addressChange(arpPacket.getDestinationProtocolAddress()));
            reply.setTargetHardwareAddress(macChange(arpPacket.getSourceHardwareAddress()));
            reply.setTargetProtocolAddress(addressChange(arpPacket.getSourceProtocolAddress()));
            LOG.info("ARP:arp replay is :{}",reply);
            // create Ethernet Frame
            Ethernet ether = new Ethernet();
            if (ethernetPacket.getEthertype() == KnownEtherType.VlanTagged) {
                Header8021q eader8021q = ethernetPacket.getHeader8021q().get(0);
                IEEE8021Q vlanTagged = new IEEE8021Q();
                vlanTagged.setCFI((short)eader8021q.isDropEligible().compareTo(true));
                vlanTagged.setPriority(eader8021q.getPriorityCode());
                vlanTagged.setVlanId(eader8021q.getVlan().getValue().shortValue());
                vlanTagged.setPayload(reply);
                vlanTagged.setEtherType(EtherTypes.ARP.shortValue());
                ether.setPayload(vlanTagged);
                ether.setEtherType((short) KnownEtherType.VlanTagged.getIntValue());
            } else {
                ether.setEtherType(EtherTypes.ARP.shortValue());
                ether.setPayload(reply);
            }
            ether.setSourceMACAddress(reply.getSenderHardwareAddress());
            ether.setDestinationMACAddress(reply.getTargetHardwareAddress());
            try {
                byte[] rawPkt = ether.serialize();
                LOG.info("ARP:arp packet is :{}",ether);
                dispatchPacket(rawPkt,rawPacket.getIngress());
            } catch (PacketException e) {
                LOG.warn("Failed to serialize ethernet reply",e);
            }
        }
    }

    /**
     * Dispatches the packet in the appropriate way.
     */
    private void dispatchPacket(byte[] payload, NodeConnectorRef ingress) {
        LOG.info("Bingo:Start Dispatch");
        if (ingress != null) {
            InstanceIdentifier<Node> egressNodePath = getNodePath(ingress.getValue());
            List<Action> actions = new ArrayList<>();
            actions.add(getNormalAction());
            TransmitPacketInput input = new TransmitPacketInputBuilder()
                    .setAction(actions)
                    .setPayload(payload)
                    .setNode(new NodeRef(egressNodePath))
                    .setEgress(ingress)
                    .setIngress(ingress)
                    .build();
            LOG.info("Dip: Send Packet {}",ingress);
            packetProcessingService.transmitPacket(input);
        } else {
            LOG.info("Cannot send packet out as controller node connector is not available for node ");
        }

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

    private InstanceIdentifier<Node> getNodePath(final InstanceIdentifier<?> nodeChild) {
        return nodeChild.firstIdentifierOf(Node.class);
    }

    private byte[] addressChange(String ipv4Address)  {
        Iterable<String> ips = Splitter.on('.').trimResults().omitEmptyStrings().split(ipv4Address);
        byte[] ipB = new byte[4];
        int i = 0;
        for(String ip : ips){
            ipB[i++] = (byte) Integer.parseInt(ip);
        }
        return ipB;
    }
    //mac Address String to byte
    private byte[] macChange(String macAddress) {
        Iterable<String> macS = Splitter.on(':').trimResults().omitEmptyStrings().split(macAddress);
        byte[] macB = new byte[6];
        int i = 0;
        for(String mac : macS){
            macB[i++] = (byte) Integer.parseInt(mac,16);
        }
        return macB;
    }
}
