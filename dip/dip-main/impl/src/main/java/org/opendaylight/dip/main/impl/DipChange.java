/*
 * Copyright © 2017 NDSC and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.dip.main.impl;

import com.google.common.base.Optional;
import org.opendaylight.controller.md.sal.binding.api.*;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dipapp.rev150105.VAddressCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dipapp.rev150105.v.address.node.connector.VipAddressEntities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dipapp.rev150105.v.address.node.connector.VipAddressEntitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.rpc.get.change.ip.rev170528.*;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import static java.util.concurrent.Executors.newFixedThreadPool;

/**
 * @author chenyang
 */
public class DipChange implements DataTreeChangeListener<VAddressCapableNodeConnector> {
    private static final Logger LOG = LoggerFactory.getLogger(DipChange.class);
    private final DataBroker dataBroker;
    private GetChangeIpService virtualChange;
    private static final int CPUS = Runtime.getRuntime().availableProcessors();
    private final ExecutorService executorService = newFixedThreadPool(CPUS);

    DipChange(DataBroker dataService, GetChangeIpService virtualChange) {
        dataBroker = dataService;
        this.virtualChange = virtualChange;
    }

    ListenerRegistration<DipChange> registerAsDataChangeListener() {
        InstanceIdentifier<VAddressCapableNodeConnector> id = InstanceIdentifier.create(Nodes.class)
                .child(Node.class)
                .child(NodeConnector.class)
                .augmentation(VAddressCapableNodeConnector.class);
        return dataBroker.registerDataTreeChangeListener(
                new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL, id), this);
    }

    void initialRead(){
        synchronized (this) {
            // Read Inventory
            InstanceIdentifier<Nodes> id = InstanceIdentifier.create(Nodes.class);
            Nodes nodes = null;
            ReadOnlyTransaction rt = dataBroker.newReadOnlyTransaction();
            try {
                Optional<Nodes> dataObjectOptional = rt.read(LogicalDatastoreType.OPERATIONAL, id).get();
                if (dataObjectOptional.isPresent()) {
                    nodes = dataObjectOptional.get();
                }
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Failed to read nodes from data store.");
                rt.close();
                throw new RuntimeException("Failed to read nodes from data store.", e);
            }
            if (nodes != null) {
                // Get NodeConnectors for each node
                for (Node node : nodes.getNode()) {
                    List<NodeConnector> nodeConnectors = node.getNodeConnector();
                    if (nodeConnectors != null) {
                        for (NodeConnector nodeConnector : nodeConnectors) {
                            VAddressCapableNodeConnector vAddressCapable = nodeConnector
                                    .getAugmentation(VAddressCapableNodeConnector.class);
                            if (vAddressCapable != null && vAddressCapable.getVipAddressEntities() != null) {
                                List<VipAddressEntities> vipAddressEntities = vAddressCapable.getVipAddressEntities();
                                for (VipAddressEntities vipAddress : vipAddressEntities) {
                                    InstanceIdentifier<VipAddressEntities> root = InstanceIdentifier.create(Nodes.class)
                                            .child(Node.class, node.getKey())
                                            .child(NodeConnector.class, nodeConnector.getKey())
                                            .augmentation(VAddressCapableNodeConnector.class)
                                            .child(VipAddressEntities.class, vipAddress.getKey());
                                    dynamicChange(vipAddress, root);
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
                    LOG.info("DipC:node {} modified", rootNode.getIdentifier());
                    Collection<DataObjectModification<?>> modifiedChildren = rootNode.getModifiedChildren();
                    for (DataObjectModification<?> modifiedChild : modifiedChildren) {
                        VipAddressEntities newNode = (VipAddressEntities) modifiedChild.getDataAfter();
                        if (newNode != null) {
                            InstanceIdentifier<VipAddressEntities> identifier =
                                    change.getRootPath().getRootIdentifier().child(VipAddressEntities.class, newNode.getKey());
                            dynamicChange(newNode, identifier);
                        }
                    }
                    break;
                case WRITE:
                    LOG.info("DipC:ip node {} created,content {}", rootNode.getIdentifier(), rootNode.getDataAfter());
                    for (VipAddressEntities vipAddress : rootNode.getDataAfter().getVipAddressEntities()) {
                        InstanceIdentifier<VipAddressEntities> root = change.getRootPath().getRootIdentifier()
                                                                            .child(VipAddressEntities.class, vipAddress.getKey());
                        dynamicChange(vipAddress, root);
                    }
                    break;
                case DELETE:
                    LOG.info("DipC:ip node {} deleted", rootNode.getIdentifier());
                    break;
                default:
                    LOG.error("DipC:Data changed,but there are some errors");
            }
        }
    }

    private void dynamicChange(VipAddressEntities vipAddressEntities, InstanceIdentifier<VipAddressEntities> identifier) {
        executorService.submit(() -> {
            LOG.info("DipC:thread Start to ip change: {}", vipAddressEntities.getVip().getIpv4Address());
            long start = System.currentTimeMillis();
            try {
                Thread.sleep(vipAddressEntities.getVipLastMutation() * 1000);
                ChangeIpOutput changeIpOutput = vIpChange(vipAddressEntities.getMac().getValue());
                if (changeIpOutput != null) {
                    VipAddressEntities newVip = new VipAddressEntitiesBuilder(vipAddressEntities)
                            .setVip(new IpAddress(new Ipv4Address(changeIpOutput.getVip())))
                            .setVipLastMutation(changeIpOutput.getVipLastMutation()).build();
                    WriteTransaction wt = dataBroker.newWriteOnlyTransaction();
                    wt.merge(LogicalDatastoreType.OPERATIONAL, identifier, newVip);
                    wt.submit().checkedGet();
                    LOG.info("DipC:thread restart : {}", System.currentTimeMillis() - start);
                }
            } catch (TransactionCommitFailedException | InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        });
    }

    private ChangeIpOutput vIpChange(String mac) throws ExecutionException, InterruptedException {
        ChangeIpInputBuilder changeIpInputBuilder = new ChangeIpInputBuilder();
        changeIpInputBuilder.setMac(mac);
        ChangeIpOutput changeIpOutput = virtualChange.changeIp(changeIpInputBuilder.build()).get().getResult();

        if(changeIpOutput.isSuccess()){
            LOG.info("DipC: The new virtual IP is {}, and its mac is {}", changeIpOutput.getVip(), mac);
            return changeIpOutput;
        }else {
            LOG.info("DipC: We cannot find this mac");
            return null;
        }
    }
}
