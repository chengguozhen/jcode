/*
 * Copyright © 2017 NDSC and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.dip.main.impl.flow;

import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.TransmitPacketInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.TransmitPacketInputBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

public class PacketOutService {
    private final static Logger LOG = LoggerFactory.getLogger(PacketOutService.class);
    private final PacketProcessingService packetProcessingService;

    public PacketOutService(PacketProcessingService packetProcessingService){
        this.packetProcessingService = packetProcessingService;
    }

    /**
     * Dispatches the packet in the appropriate way.
     */
    public void dispatchPacket(byte[] payload, NodeConnectorRef ingress,NodeConnectorRef egress,List<Action> actions) {
        LOG.info("DIP:Start Dispatch");
        if (ingress != null) {
            InstanceIdentifier<Node> egressNodePath = egress.getValue().firstIdentifierOf(Node.class);
            TransmitPacketInput input = new TransmitPacketInputBuilder()
                    .setAction(actions)
                    .setPayload(payload)
                    .setNode(new NodeRef(egressNodePath))
                    .setEgress(egress)
                    .setIngress(ingress)
                    .build();
            LOG.info("DIP: Send Packet {}",ingress);
            packetProcessingService.transmitPacket(input);
        } else {
            LOG.info("Cannot send packet out as controller node connector is not available for node ");
        }

    }
}
