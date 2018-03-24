/*
 * Copyright © 2017 NDSC and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.dip.main.impl.topology;

import com.google.common.base.Optional;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.dip.main.impl.util.InstanceIdentifierUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;

public class DynamicTopo {
    private static final Logger LOG = LoggerFactory.getLogger(DynamicTopo.class);
    private final DataBroker dataBroker;
    private static final String DEFAULT_TOPOLOGY_ID = "flow:1";

    public DynamicTopo(final DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    public List<Link> buildPath (Uri srcNodeId, Uri dstNodeId) {
        NetworkGraphImpl networkGraph = new NetworkGraphImpl();
        networkGraph.addLinks(getLinksFromTopology());
        return networkGraph.getPath(new NodeId(srcNodeId.getValue()),new NodeId(dstNodeId.getValue()));
    }

    private List<Link> getLinksFromTopology() {
        InstanceIdentifier<Topology> topologyInstanceIdentifier = InstanceIdentifierUtils
                .generateTopologyInstanceIdentifier(DEFAULT_TOPOLOGY_ID);
        Topology topology = null;
        ReadOnlyTransaction readOnlyTransaction = dataBroker.newReadOnlyTransaction();
        try {
            Optional<Topology> topologyOptional = readOnlyTransaction
                    .read(LogicalDatastoreType.OPERATIONAL, topologyInstanceIdentifier).get();
            if (topologyOptional.isPresent()) {
                topology = topologyOptional.get();
            }
        } catch (Exception e) {
            LOG.error("Error reading topology {}", topologyInstanceIdentifier);
            readOnlyTransaction.close();
            throw new RuntimeException(
                    "Error reading from operational store, topology : " + topologyInstanceIdentifier, e);
        }
        readOnlyTransaction.close();
        if (topology == null) {
            return null;
        }
        List<Link> links = topology.getLink();
        if (links == null || links.isEmpty()) {
            return null;
        }
        List<Link> internalLinks = new ArrayList<>();
        for (Link link : links) {
            if (!(link.getLinkId().getValue().contains("host"))) {
                internalLinks.add(link);
            }
        }
        return internalLinks;
    }

}
