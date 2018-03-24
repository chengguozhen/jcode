/*
 * Copyright © 2017 NDSC and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.dip.main.impl.topology;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;

import java.util.List;

/**
 * Service that allows to build a network graph using Topology links
 * and exposes operation that can be performed on such graph.
 */
public interface NetworkGraphService {

    /**
     * Adds links to existing graph or creates new graph with given links if
     * graph was not initialized.
     *
     * @param links the links to add
     */
    void addLinks(List<Link> links);

    /**
     * Removes links from existing graph.
     *
     * @param links the links to remove
     */
    void removeLinks(List<Link> links);

    /**
     * Returns a path between 2 nodes. Implementation should ideally return
     * shortest path.
     *
     * @param srcNodeId the source node Id
     * @param dstNodeId the destination node Id
     */
   List<Link> getPath(NodeId srcNodeId, NodeId dstNodeId);

    /**
     * Forms MST(minimum spanning tree) from network graph and returns links
     * that are not in MST.
     */
    List<Link> getLinksInMst();

    /**
     * Returns all the links in current network graph.
     */
    List<Link> getAllLinks();

    /**
     * Clears the prebuilt graph, in case same service instance is required to
     * process a new graph.
     */
    void clear();
}
