/*
 * Copyright © 2017 NDSC and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.nat.impl;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dipapp.rev150105.VAddressCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dipapp.rev150105.v.address.node.connector.VipAddressEntities;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.HashMap;

/**
 * Created by Guozhen Cheng at 1/4/18 4:36 PM
 * Dept: ndsc.org
 */
public class VIPChangedProvider implements DataTreeChangeListener<VAddressCapableNodeConnector>{

    private static final Logger LOG = LoggerFactory.getLogger(VIPChangedProvider.class);
    private final DataBroker dataBroker;
    private final InstanceIdentifier<VAddressCapableNodeConnector> iid;
    private final ListenerRegistration listenerReg;
    private HashMap<String, VipAddressEntities> vipAddressEntitiesHashMap;

    public VIPChangedProvider(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
        iid = InstanceIdentifier.create(VAddressCapableNodeConnector.class);
        listenerReg = dataBroker.registerDataTreeChangeListener(
                new DataTreeIdentifier<VAddressCapableNodeConnector>(LogicalDatastoreType.CONFIGURATION,iid), this);
        vipAddressEntitiesHashMap = new HashMap<>();
    }


    /**
     * Invoked when there was data change for the supplied path, which was used
     * to register this listener.
     * <p>
     * <p>
     * This method may be also invoked during registration of the listener if
     * there is any pre-existing data in the conceptual data tree for supplied
     * path. This initial event will contain all pre-existing data as created.
     * <p>
     * <p>
     * A data change event may be triggered spuriously, e.g. such that data before
     * and after compare as equal. Implementations of this interface are expected
     * to recover from such events. Event producers are expected to exert reasonable
     * effort to suppress such events.
     * <p>
     * In other words, it is completely acceptable to observe
     * a {@link DataObjectModification}, while the state observed before and
     * after- data items compare as equal.
     *
     * @param changes Collection of change events, may not be null or empty.
     */
    @Override
    public void onDataTreeChanged(@Nonnull Collection<DataTreeModification<VAddressCapableNodeConnector>> changes) {

        for (final DataTreeModification<VAddressCapableNodeConnector> change : changes) {
            final DataObjectModification<VAddressCapableNodeConnector> rootNode = change.getRootNode();
            switch (rootNode.getModificationType()){
                case SUBTREE_MODIFIED:
                    LOG.info("Nat:node {} modified", rootNode.getIdentifier());
                    Collection<DataObjectModification<?>> modifiedChildren = rootNode.getModifiedChildren();
                    for (DataObjectModification<?> modifiedChild : modifiedChildren) {
                        VipAddressEntities oldNode = (VipAddressEntities) modifiedChild.getDataBefore();
                        VipAddressEntities newNode = (VipAddressEntities) modifiedChild.getDataAfter();
                        LOG.info("Nat:vipAddress oldNode : {}",oldNode);
                        LOG.info("Nat:vipAddress newNode : {}",newNode);
                        switch (modifiedChild.getModificationType()){
                            case SUBTREE_MODIFIED:
                                if(oldNode != null && newNode != null){
                                    vipAddressEntitiesHashMap.replace(oldNode.getMac().getValue(), newNode);
                                    //TODO: 下发相应的流表
                                }
                                break;
                            case DELETE:
                                if (oldNode != null) {
                                    vipAddressEntitiesHashMap.remove(oldNode.getMac().getValue());
                                    //TODO: 下发删除对应流表的操作
                                }
                            case WRITE:
                                if (newNode != null) {
                                    vipAddressEntitiesHashMap.put(oldNode.getMac().getValue(),newNode);
                                    //TODO: 下发相应的流表
                                }
                                break;
                            default:
                                LOG.error("Nat:vipAddress changed,but there are some errors");
                        }
                    }
                    break;
                case WRITE:
                    LOG.info("Nat: node {} created,content {}", rootNode.getIdentifier(),rootNode.getDataAfter());
                case DELETE:
                    vipAddressEntitiesHashMap.clear();
                    //TODO: 添加清空流表的操作
                    LOG.info("Nat: node {} deleted", rootNode.getIdentifier());
                default:
                    LOG.error("Nat:VAddressCapableNodeConnector Data changed,but there are some errors");
            }
        }

    }

    public DataBroker getDataBroker() {
        return dataBroker;
    }

    public InstanceIdentifier<VAddressCapableNodeConnector> getIid() {
        return iid;
    }

    public HashMap<String, VipAddressEntities> getVipAddressEntitiesHashMap() {
        return vipAddressEntitiesHashMap;
    }

    public void setVipAddressEntitiesHashMap(HashMap<String, VipAddressEntities> vipAddressEntitiesHashMap) {
        this.vipAddressEntitiesHashMap = vipAddressEntitiesHashMap;
    }

    public void close() {
        listenerReg.close();
    }
}
