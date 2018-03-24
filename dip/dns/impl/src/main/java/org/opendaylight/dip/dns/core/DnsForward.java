/*
 * Copyright © 2017 NDSC and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.dip.dns.core;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetNwDstActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.output.action._case.OutputActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.nw.dst.action._case.SetNwDstActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.address.address.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.OutputPortValues;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.TransmitPacketInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.TransmitPacketInputBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class DnsForward {
    private static final Logger LOG = LoggerFactory.getLogger(DnsForward.class);
    private final PacketProcessingService packetProcessingService;
    private final String Mask = "/32";
    private String dnsForwardAddress;

    DnsForward(PacketProcessingService packetProcessingService){
        this.packetProcessingService = packetProcessingService;
    }

    void setDnsForwardAddress(String dnsForwardAddress) {
        this.dnsForwardAddress = dnsForwardAddress;
    }

    void forwardPacket(byte[] payload, NodeConnectorRef ingress) {
        LOG.info("DNS:Start Forward");
        if (ingress != null) {
            InstanceIdentifier<Node> egressNodePath = getNodePath(ingress.getValue());
            TransmitPacketInput input = new TransmitPacketInputBuilder()
                    .setAction(getChangeAction())
                    .setPayload(payload)
                    .setNode(new NodeRef(egressNodePath))
                    .setEgress(ingress)
                    .setIngress(ingress)
                    .build();
            LOG.info("DNS: Forward Packet {}",ingress);
            packetProcessingService.transmitPacket(input);
        } else {
            LOG.info("Cannot forward packet out as controller node connector is not available for node ");
        }

    }

    private List<Action> getChangeAction() {
        List<Action> actions = new ArrayList<>();

        Action ipChange = new ActionBuilder()
                .setOrder(0)
                .setKey(new ActionKey(0))
                .setAction(new SetNwDstActionCaseBuilder()
                        .setSetNwDstAction(new SetNwDstActionBuilder()
                            .setAddress(new Ipv4Builder().setIpv4Address(new Ipv4Prefix(dnsForwardAddress+Mask)).build())
                            .build())
                        .build())
                .build();
        actions.add(ipChange);

        Action normal = new ActionBuilder()
            .setOrder(0)
            .setKey(new ActionKey(0))
            .setAction(new OutputActionCaseBuilder()
                    .setOutputAction(new OutputActionBuilder()
                            .setMaxLength(0xffff)
                            .setOutputNodeConnector(new Uri(OutputPortValues.NORMAL.toString()))
                            .build())
                    .build())
            .build();
        actions.add(normal);

        return actions;
    }

    private InstanceIdentifier<Node> getNodePath(final InstanceIdentifier<?> nodeChild) {
        return nodeChild.firstIdentifierOf(Node.class);
    }
}
