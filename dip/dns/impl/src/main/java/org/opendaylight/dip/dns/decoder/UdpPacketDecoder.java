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
import org.opendaylight.yang.gen.v1.urn.opendaylight.dns.yang.dns.udp.packet.rev171124.UdpPacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.dns.yang.dns.udp.packet.rev171124.UdpPacketReceivedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.dns.yang.dns.udp.packet.rev171124.udp.packet.received.packet.chain.packet.UdpPacketBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.PacketChain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.PacketChainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.packet.chain.Packet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv4.rev140528.Ipv4PacketListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv4.rev140528.Ipv4PacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv4.rev140528.KnownIpProtocols;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv4.rev140528.ipv4.packet.received.packet.chain.packet.Ipv4Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author chenyang
 */
public class UdpPacketDecoder extends AbstractDecoder<Ipv4PacketReceived,UdpPacketReceived>
        implements Ipv4PacketListener {

    private static final Logger LOG = LoggerFactory.getLogger(UdpPacketDecoder.class);

    public UdpPacketDecoder(NotificationPublishService publishService){
        super(publishService);
    }
    @Override
    public UdpPacketReceived decode(Ipv4PacketReceived ipv4PacketReceived) {
        LOG.info("Bingo:Decode a UdpPaket");
        UdpPacketReceivedBuilder udpPacketReceivedBuilder = new UdpPacketReceivedBuilder();

        // Find the latest packet in the packet-chain, which is an Ipv4
        List<PacketChain> packetChainList = ipv4PacketReceived.getPacketChain();
        Ipv4Packet ipv4Packet = (Ipv4Packet) packetChainList.get(packetChainList.size() - 1).getPacket();
        int bitOffset = ipv4Packet.getPayloadOffset() * NetUtils.NumBitsInAByte;
        byte[] data = ipv4PacketReceived.getPayload();

        UdpPacketBuilder builder = new UdpPacketBuilder();
        try {

            builder.setSourcePort(BitBufferHelper.getInt(BitBufferHelper.getBits(data, bitOffset, 16)));
            builder.setDestinationPort(BitBufferHelper.getInt(BitBufferHelper.getBits(data, bitOffset + 16, 16)));
            builder.setDataLength(BitBufferHelper.getInt(BitBufferHelper.getBits(data, bitOffset + 32, 16)));
            builder.setCheckSum(BitBufferHelper.getInt(BitBufferHelper.getBits(data, bitOffset + 48, 16)));

            // Decode the Payload
            int payloadStartInBits = bitOffset + 64;
            int payloadEndInBits = data.length * NetUtils.NumBitsInAByte - payloadStartInBits - 4 * NetUtils.NumBitsInAByte;
            int start = payloadStartInBits / NetUtils.NumBitsInAByte;
            int end = start + payloadEndInBits / NetUtils.NumBitsInAByte;
            builder.setPayloadOffset(start);
            builder.setPayloadLength(end - start);
        } catch (BufferException e) {
            LOG.debug("Exception while decoding ICMP packet", e.getMessage());
        }

        // build udp
        packetChainList.add(new PacketChainBuilder().setPacket(builder.build()).build());
        udpPacketReceivedBuilder.setPacketChain(packetChainList);

        // carry forward the original payload.
        udpPacketReceivedBuilder.setPayload(ipv4PacketReceived.getPayload());

        return udpPacketReceivedBuilder.build();
    }

    @Override
    public boolean canDecode(Ipv4PacketReceived ipv4PacketReceived) throws BufferException {
        LOG.info("Bingo:Get IPv4Paket");
        if (ipv4PacketReceived == null || ipv4PacketReceived.getPacketChain() == null) {
            {
                return false;
            }
        }

        // Only decode the latest packet in the chain
        Ipv4Packet ipv4Packet = null;
        if (!ipv4PacketReceived.getPacketChain().isEmpty()) {
            Packet packet = ipv4PacketReceived.getPacketChain()
                    .get(ipv4PacketReceived.getPacketChain().size() - 1).getPacket();
            if (packet instanceof Ipv4Packet) {
                ipv4Packet = (Ipv4Packet) packet;
            }
        }
        return ipv4Packet != null && KnownIpProtocols.Udp.equals(ipv4Packet.getProtocol());
    }

    @Override
    public void onIpv4PacketReceived(Ipv4PacketReceived notification) {
        decodeAndPublish(notification);
        LOG.info("Bingo:Publish a UdpPaket");
    }
}
