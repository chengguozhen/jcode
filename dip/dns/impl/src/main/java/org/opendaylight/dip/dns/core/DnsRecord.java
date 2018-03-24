/*
 * Copyright © 2017 NDSC and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.dip.dns.core;

import com.google.common.base.Optional;
import org.opendaylight.controller.md.sal.binding.api.*;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dipapp.rev150105.VAddressCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dipapp.rev150105.v.address.node.connector.VipAddressEntities;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class DnsRecord implements DataTreeChangeListener<VAddressCapableNodeConnector> {
    private static final Logger LOG = LoggerFactory.getLogger(DnsRecord.class);
    private final DataBroker dataBroker;
    private HashMap<String, VipAddressEntities> ipPool;

    DnsRecord(final DataBroker dataService) {
        dataBroker = dataService;
        ipPool = new HashMap<>();
    }
    public ListenerRegistration<DnsRecord> registerAsDataChangeListener() {
        InstanceIdentifier<VAddressCapableNodeConnector> id = InstanceIdentifier.create(Nodes.class)
                .child(Node.class)
                .child(NodeConnector.class)
                .augmentation(VAddressCapableNodeConnector.class);
        return dataBroker.registerDataTreeChangeListener(
                new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL, id), this);
    }

    HashMap<String, VipAddressEntities> getIpPool() {
        return this.ipPool;
    }

    private void readIpPool(){
        synchronized (this) {
            // Read Inventory
            InstanceIdentifier<Nodes> id = InstanceIdentifier.create(Nodes.class);
            Nodes nodes = null;
            ReadOnlyTransaction rt = dataBroker.newReadOnlyTransaction();
            try {
                Optional<Nodes> dataObjectOptional =  rt.read(LogicalDatastoreType.OPERATIONAL, id).get();
                if (dataObjectOptional.isPresent()) {
                    nodes = (Nodes) dataObjectOptional.get();
                }
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Failed to read nodes from data store");
                rt.close();
                throw new RuntimeException("Failed to read nodes from data store", e);
            }
            if (nodes != null) {
                // Get NodeConnectors for each node
                for (Node node : nodes.getNode()) {
                    List<NodeConnector> nodeConnectors = node.getNodeConnector();
                    if (nodeConnectors != null) {
                        for (NodeConnector nodeConnector : nodeConnectors) {
                            VAddressCapableNodeConnector vAddressCapable = nodeConnector
                                                        .getAugmentation(VAddressCapableNodeConnector.class);
                            if(vAddressCapable != null && vAddressCapable.getVipAddressEntities() != null){
                                List<VipAddressEntities> vipAddressEntities = vAddressCapable.getVipAddressEntities();
                                LOG.info("DNS:DatastoreIpPool : {}",vipAddressEntities);
                                for(VipAddressEntities vipAddress : vipAddressEntities){
                                    ipPool.put(vipAddress.getVdomain(),vipAddress);
                                }
                            }
                        }
                    }
                }
            }
            rt.close();
        }
    }
    @Override
    public void onDataTreeChanged(@Nonnull Collection<DataTreeModification<VAddressCapableNodeConnector>> changes) {
        for (final DataTreeModification<VAddressCapableNodeConnector> change : changes) {
            final DataObjectModification<VAddressCapableNodeConnector> rootNode = change.getRootNode();
            switch (rootNode.getModificationType()) {
                case SUBTREE_MODIFIED:
                    LOG.info("DNS:node modified");
                    Collection<DataObjectModification<?>> modifiedChildren = rootNode.getModifiedChildren();
                    for (DataObjectModification<?> modifiedChild : modifiedChildren) {
                        VipAddressEntities oldNode = (VipAddressEntities)modifiedChild.getDataBefore();
                        VipAddressEntities newNode = (VipAddressEntities)modifiedChild.getDataAfter();
                        LOG.info("DNS:Dns Pool oldNode : {}",oldNode);
                        LOG.info("DNS:Dns Pool newNode : {}",newNode);
                        switch (modifiedChild.getModificationType()){
                            case SUBTREE_MODIFIED:
                                if (oldNode != null && newNode != null) {
                                    ipPool.remove(oldNode.getVdomain());
                                    ipPool.put(newNode.getVdomain(),newNode);
                                }
                                break;
                            case WRITE:
                                if (newNode != null) {
                                    ipPool.put(newNode.getVdomain(),newNode);
                                }
                                break;
                            case DELETE:
                                if (oldNode != null) {
                                    ipPool.remove(oldNode.getVdomain());
                                }
                                break;
                            default:
                                LOG.error("DNS:ChildData changed,but there are some errors");
                        }
                    }
                    break;
                case WRITE:
                    LOG.info("DNS:node {} created,content {}", rootNode.getIdentifier(),rootNode.getDataAfter());
                    readIpPool();
                    break;
                case DELETE:
                    LOG.info("DNS:node {} deleted", rootNode.getIdentifier());
                    ipPool.clear();
                    break;
                default:
                    LOG.error("DNS:Data changed,but there are some errors");
            }
        }
    }

}
