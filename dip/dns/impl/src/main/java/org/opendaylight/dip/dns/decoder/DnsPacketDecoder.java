/*
 * Copyright © 2017 NDSC and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.dip.dns.decoder;

import org.opendaylight.controller.liblldp.BitBufferHelper;
import org.opendaylight.controller.liblldp.BufferException;
import org.opendaylight.controller.liblldp.NetUtils;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.dns.yang.dns.packet.rev171124.DnsPacketHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.dns.yang.dns.packet.rev171124.DnsPacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.dns.yang.dns.packet.rev171124.DnsPacketReceivedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.dns.yang.dns.packet.rev171124.dns.packet.received.packet.chain.packet.DnsPacketBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.dns.yang.dns.udp.packet.rev171124.UdpPacketListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.dns.yang.dns.udp.packet.rev171124.UdpPacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.dns.yang.dns.udp.packet.rev171124.udp.packet.received.packet.chain.packet.UdpPacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.PacketChain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.PacketChainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.packet.chain.Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author chenayng
 */
public class DnsPacketDecoder extends AbstractDecoder<UdpPacketReceived,DnsPacketReceived> implements UdpPacketListener {
    private static final Logger LOG = LoggerFactory.getLogger(DnsPacketDecoder.class);

    public DnsPacketDecoder(NotificationPublishService publishService){
        super(publishService);
    }
    @Override
    public DnsPacketReceived decode(UdpPacketReceived udpPacketReceived) {
        LOG.info("Bingo:Decode UdpPaket");
        DnsPacketReceivedBuilder dnsReceivedBuilder = new DnsPacketReceivedBuilder();

        // Find the latest packet in the packet-chain, which is an udpPacket
        List<PacketChain> packetChainList = udpPacketReceived.getPacketChain();
        UdpPacket udpPacket = (UdpPacket) packetChainList.get(packetChainList.size() - 1).getPacket();
        int bitOffset = udpPacket.getPayloadOffset() * NetUtils.NumBitsInAByte;
        byte[] data = udpPacketReceived.getPayload();

        DnsPacketBuilder builder = new DnsPacketBuilder();
        try {
            builder.setID(BitBufferHelper.getInt(BitBufferHelper.getBits(data, bitOffset, 16)));
            builder.setQR(1 == (BitBufferHelper.getBits(data, bitOffset + 16, 1)[0] & 0xff));
            if(builder.isQR()){
                LOG.error("this is not a Dns query!");
                return null;
            }
            builder.setOPCODE(DnsPacketHeader.OPCODE.forValue(BitBufferHelper.getShort(BitBufferHelper.getBits(data, bitOffset + 17, 4))));
            builder.setAA(1 == (BitBufferHelper.getBits(data, bitOffset + 21, 1)[0] & 0xff));
            builder.setTC(1 == (BitBufferHelper.getBits(data, bitOffset + 22, 1)[0] & 0xff));
            builder.setRD(1 == (BitBufferHelper.getBits(data, bitOffset + 23, 1)[0] & 0xff));
            builder.setRA(1 == (BitBufferHelper.getBits(data, bitOffset + 24, 1)[0] & 0xff));
            builder.setZ(BitBufferHelper.getShort(BitBufferHelper.getBits(data, bitOffset+25, 3)));
            builder.setRCODE(DnsPacketHeader.RCODE.forValue(BitBufferHelper.getShort(BitBufferHelper.getBits(data, bitOffset + 28, 4))));
            builder.setQDCOUNT(BitBufferHelper.getInt(BitBufferHelper.getBits(data, bitOffset+32, 16)));
            builder.setANCOUNT(BitBufferHelper.getInt(BitBufferHelper.getBits(data, bitOffset+48, 16)));
            builder.setNSCOUNT(BitBufferHelper.getInt(BitBufferHelper.getBits(data, bitOffset+64, 16)));
            builder.setARCOUNT(BitBufferHelper.getInt(BitBufferHelper.getBits(data, bitOffset+80, 16)));

            builder.setPayloadOffset(udpPacket.getPayloadOffset());
            builder.setPayloadLength(udpPacket.getPayloadLength()+4);

        }catch (BufferException e) {
            LOG.debug("Exception while decoding Dns packet", e.getMessage());
        }
        packetChainList.add(new PacketChainBuilder().setPacket(builder.build()).build());
        dnsReceivedBuilder.setPacketChain(packetChainList);

        // carry forward the original payload.
        dnsReceivedBuilder.setPayload(udpPacketReceived.getPayload());

        return dnsReceivedBuilder.build();
    }

    @Override
    public boolean canDecode(UdpPacketReceived udpPacketReceived) throws BufferException {
        LOG.info("Bingo:Get UdpPaket");
        if (udpPacketReceived == null || udpPacketReceived.getPacketChain() == null) {
            {
                return false;
            }
        }

        // Only decode the latest packet in the chain
        UdpPacket udpPacket = null;
        if (!udpPacketReceived.getPacketChain().isEmpty()) {
            Packet packet = udpPacketReceived.getPacketChain()
                    .get(udpPacketReceived.getPacketChain().size() - 1).getPacket();
            if (packet instanceof UdpPacket) {
                udpPacket = (UdpPacket) packet;
            }
        }

        return udpPacket != null && (udpPacket.getDestinationPort()==53);
    }

    @Override
    public void onUdpPacketReceived(UdpPacketReceived notification) {
        decodeAndPublish(notification);
        LOG.info("Bingo:Publish a DnsPaket");
    }
}
