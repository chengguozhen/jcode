/*
 * Copyright © 2017 NDSC and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.dip.dns.core;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import org.opendaylight.controller.liblldp.BitBufferHelper;
import org.opendaylight.controller.liblldp.BufferException;
import org.opendaylight.controller.liblldp.NetUtils;
import org.opendaylight.controller.liblldp.PacketException;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.dip.dns.dns.*;
import org.opendaylight.dip.main.impl.packet.*;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.dns.yang.dns.packet.rev171124.DnsPacketListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.dns.yang.dns.packet.rev171124.DnsPacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.dns.yang.dns.packet.rev171124.dns.packet.received.packet.chain.packet.DnsPacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.dns.yang.dns.udp.packet.rev171124.udp.packet.received.packet.chain.packet.UdpPacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.PacketChain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.packet.chain.packet.RawPacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.Header8021q;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.KnownEtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.ethernet.packet.received.packet.chain.packet.EthernetPacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv4.rev140528.ipv4.packet.received.packet.chain.packet.Ipv4Packet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dipapp.rev150105.VAddressCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dipapp.rev150105.v.address.node.connector.VipAddressEntities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dipapp.rev150105.v.address.node.connector.VipAddressEntitiesKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutionException;

public class DnsPacketHandler implements DnsPacketListener {

    private final static Logger LOG = LoggerFactory.getLogger(DnsPacketHandler.class);
    private final DataBroker dataBroker;

    private boolean computeUdpChecksum = true;
    private DnsPacketDispatcher packetDispatcher;
    private DnsForward dnsForward;
    private DnsRecord dnsRecord;
    private boolean isForward = false;

    DnsPacketHandler(DnsPacketDispatcher packetDispatcher,
                     DataBroker dataBroker,
                     DnsRecord record) {
        this.packetDispatcher = packetDispatcher;
        this.dataBroker = dataBroker;
        dnsRecord = record;
    }

    void setDnsForward(DnsForward dnsForward){
        this.dnsForward = dnsForward;
        isForward = true;
    }

    @Override
    public void onDnsPacketReceived(DnsPacketReceived packetReceived) {
        LOG.info("DNS:Handler a DnsPacket");
        if (packetReceived == null || packetReceived.getPacketChain() == null) {
            return;
        }

        RawPacket rawPacket = null;
        EthernetPacket ethernetPacket = null;
        Ipv4Packet ipv4Packet = null;
        UdpPacket udpPacket = null;
        DnsPacket dnsPacket = null;
        for (PacketChain packetChain : packetReceived.getPacketChain()) {
            if (packetChain.getPacket() instanceof RawPacket) {
                rawPacket = (RawPacket) packetChain.getPacket();
            } else if (packetChain.getPacket() instanceof EthernetPacket) {
                ethernetPacket = (EthernetPacket) packetChain.getPacket();
            } else if (packetChain.getPacket() instanceof Ipv4Packet) {
                ipv4Packet = (Ipv4Packet) packetChain.getPacket();
            } else if (packetChain.getPacket() instanceof UdpPacket) {
                udpPacket = (UdpPacket) packetChain.getPacket();
            } else if (packetChain.getPacket() instanceof DnsPacket) {
                dnsPacket = (DnsPacket) packetChain.getPacket();
            }
        }
        if (rawPacket == null || ethernetPacket == null || ipv4Packet == null || udpPacket == null || dnsPacket == null) {
            return;
        }

        //handle the dns packet
        byte[] data = packetReceived.getPayload();
        int bitOffset = dnsPacket.getPayloadOffset() * NetUtils.NumBitsInAByte;
        int len = dnsPacket.getPayloadLength() * NetUtils.NumBitsInAByte;
        try {
            Message dnsmessage = new Message(BitBufferHelper.getBits(data,bitOffset,len));
            InstanceIdentifier<NodeConnector> identifier = rawPacket.getIngress()
                                                      .getValue().firstIdentifierOf(NodeConnector.class);
            Message responseMessage = getResponse(dnsmessage,ethernetPacket,identifier);
            if(responseMessage == null){
                if(isForward){
                    dnsForward.forwardPacket(data,rawPacket.getIngress());
                }
                return;
            }
            InetAddress srcIp = addressChange(ipv4Packet.getSourceIpv4().getValue());
            InetAddress destIp = addressChange(ipv4Packet.getDestinationIpv4().getValue());
            byte[] response = packageDns(responseMessage,ethernetPacket,
                    destIp,srcIp, udpPacket.getDestinationPort().shortValue(),udpPacket.getSourcePort().shortValue());
            if(response == null){
                return;
            }
            packetDispatcher.dispatchPacket(response,rawPacket.getIngress());
        }catch (BufferException | IOException e) {
            LOG.debug("Exception while decoding Dns packet", e.getMessage());
        }
    }

    private Message getResponse(Message dnsmessage, EthernetPacket ethernetPacket,InstanceIdentifier<NodeConnector> identifier){
        int count = 0;
        Message responseMessage = new Message(dnsmessage.getHeader().getID());
        responseMessage.getHeader().setFlag(Flags.QR);
        responseMessage.getHeader().setFlag(Flags.AA);
        responseMessage.getHeader().setFlag(Flags.RD);
        responseMessage.getHeader().setFlag(Flags.RA);
        Record[] records = dnsmessage.getSectionArray(Section.QUESTION);
        try {
            for(Record record : records){
                if(record.getType() == 1){
                    String name = dnsmessage.getQuestion().getName().toString();
                    LOG.info("DNS:Ask for:{}",name);
                    String vIp = dnsRecord.getIpPool().get(name).getVip().getIpv4Address().getValue();
                    if(vIp != null){
                        InetAddress address = addressChange(vIp);
                        ARecord answer = new ARecord(record.getName(), record.getDClass(), 500, address);
                        responseMessage.addRecord(record, Section.QUESTION);
                        responseMessage.addRecord(answer, Section.ANSWER);
                        count++;
                        LOG.info("DNS:DNS answer is:{}",answer);
                    }
                }else if (record.getType() == 12){
                    String address = ptrAddressChange(dnsmessage.getQuestion().getName().toString());
                    LOG.info("DNS:Ask for:{}",address);
                    MacAddress srcMac = new MacAddress(ethernetPacket.getSourceMac().getValue());
                    InstanceIdentifier<VipAddressEntities> id =
                            identifier.augmentation(VAddressCapableNodeConnector.class)
                                      .child(VipAddressEntities.class,new VipAddressEntitiesKey(srcMac));

                    ReadTransaction tr = dataBroker.newReadOnlyTransaction();
                    Optional<VipAddressEntities> opData = tr.read(LogicalDatastoreType.OPERATIONAL,id).get();
                    if (opData.isPresent()){
                        VipAddressEntities vRecord = opData.get();
                        if(address.equals(vRecord.getRip().getIpv4Address().getValue())){
                            Name name = new Name(vRecord.getVdomain());
                            LOG.info("DNS:vRecord is:{}",name);
                            PTRRecord answer = new PTRRecord(record.getName(), record.getDClass(), 500, name);
                            responseMessage.addRecord(record, Section.QUESTION);
                            responseMessage.addRecord(answer, Section.ANSWER);
                            count++;
                        }
                    }
                }
            }
        } catch (InterruptedException | ExecutionException | UnknownHostException | TextParseException e) {
            e.printStackTrace();
        }
        if(count <= 0){
            responseMessage = null;
        }
        return responseMessage;
    }

    public void doNotChecksum(){
        computeUdpChecksum = false;
    }

    private byte[] packageDns(Message reply, EthernetPacket ethernetPacket,
                              InetAddress srcIp, InetAddress destIp,
                              short srcPort, short destPort) throws UnknownHostException {
        if (reply == null) {
            /*
             * DECLINE or RELEASE don't result in reply packet
             */
            return null;
        }
        LOG.debug("Sending Dns Pkt {}", reply);
        // create UDP pkt
        UDP udpPkt = new UDP();
        byte[] rawPkt = reply.toWire();
        udpPkt.setRawPayload(rawPkt);
        udpPkt.setDestinationPort(destPort);
        udpPkt.setSourcePort(srcPort);
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
            checkSum = computeChecksum(rawPkt, srcIp.getAddress(), destIp.getAddress());
        }
        udpPkt.setChecksum(checkSum);
        ip4Reply.setPayload(udpPkt);
        ip4Reply.setProtocol(IPProtocols.UDP.byteValue());
        ip4Reply.setSourceAddress(srcIp);
        ip4Reply.setDestinationAddress(destIp);
        ip4Reply.setTotalLength((short) (rawPkt.length+20));
        ip4Reply.setTtl((byte) 32);
        // create Ethernet Frame
        Ethernet ether = new Ethernet();
        if (ethernetPacket.getEthertype() == KnownEtherType.VlanTagged) {
            Header8021q eader8021q = ethernetPacket.getHeader8021q().get(0);
            IEEE8021Q vlanTagged = new IEEE8021Q();
            vlanTagged.setCFI((short)eader8021q.isDropEligible().compareTo(true));
            vlanTagged.setPriority(eader8021q.getPriorityCode());
            vlanTagged.setVlanId(eader8021q.getVlan().getValue().shortValue());
            vlanTagged.setPayload(ip4Reply);
            vlanTagged.setEtherType(EtherTypes.IPv4.shortValue());
            ether.setPayload(vlanTagged);
            ether.setEtherType((short) KnownEtherType.VlanTagged.getIntValue());
        } else {
            ether.setEtherType(EtherTypes.IPv4.shortValue());
            ether.setPayload(ip4Reply);
        }
        ether.setSourceMACAddress(macChange(ethernetPacket.getDestinationMac().getValue()));
        ether.setDestinationMACAddress(macChange(ethernetPacket.getSourceMac().getValue()));

        try {
            rawPkt = ether.serialize();
        } catch (PacketException e) {
            LOG.warn("Failed to serialize ethernet reply",e);
            return null;
        }
        return rawPkt;
    }

    private short computeChecksum(byte[] inData, byte[] srcAddr, byte[] destAddr) {
        short checkSum = 0;
        int sum = 0, carry = 0;
        int wordData, i;

        for (i = 0; i < inData.length - 1; i = i + 2) {
            // Skip, if the current bytes are checkSum bytes
            wordData = ((inData[i] << 8) & 0xFF00) + (inData[i + 1] & 0xFF);
            sum = sum + wordData;
        }

        if (i < inData.length) {
            wordData = (inData[i] << 8) & 0xFF00;
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

    private InetAddress addressChange(String ipv4Address) throws UnknownHostException {
        Iterable<String> ips = Splitter.on('.').trimResults().omitEmptyStrings().split(ipv4Address);
        byte[] ipB = new byte[4];
        int i = 0;
        for(String ip : ips){
            ipB[i++] = (byte) Integer.parseInt(ip);
        }
        return InetAddress.getByAddress(ipB);
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
    //change ptr address to ip address
    private String ptrAddressChange(String ptrAddress){
        Iterable<String> ips = Splitter.on('.').trimResults().omitEmptyStrings().split(ptrAddress.substring(0,ptrAddress.length()-14));
        String[] ipn = new String[4];
        int n = 3;
        for (String i : ips){
            ipn[n--] = i;
        }
        return Joiner.on(".").skipNulls().join(ipn);
    }
}
