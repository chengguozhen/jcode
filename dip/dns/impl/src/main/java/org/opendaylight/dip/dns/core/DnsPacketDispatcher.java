/*
 * Copyright © 2017 NDSC and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.dip.dns.core;

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
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.TransmitPacketInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.TransmitPacketInputBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * DnsPacketDispatcher sends packets out to the network.
 * @author Chen
 */
public class DnsPacketDispatcher {
    private final static Logger LOG = LoggerFactory.getLogger(DnsPacketDispatcher.class);
    private final PacketProcessingService packetProcessingService;

    DnsPacketDispatcher(PacketProcessingService packetProcessingService){
        this.packetProcessingService = packetProcessingService;
    }

    /**
     * Dispatches the packet in the appropriate way.
     */
    void dispatchPacket(byte[] payload, NodeConnectorRef ingress) {
        LOG.info("DNS:Start Dispatch");
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
            LOG.info("DNS: Send Packet {}",ingress);
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

}

