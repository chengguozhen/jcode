/*
 * Copyright © 2017 My Company and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.future.network.datastore.listener;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.datastoreconfig.rev150105.DatastoreConfig;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Created by 10032272 on 2017/8/28 0028.
 */
public class DatastoreConfigChangeListener implements DataChangeListener,DataTreeChangeListener<DatastoreConfig> {
    private static final Logger LOG = LoggerFactory.getLogger(DatastoreConfigChangeListener.class);
    private final DataBroker dataBroker;
    ListenerRegistration<DataChangeListener> lisReg;
    ListenerRegistration<DatastoreConfigChangeListener> listenerReg;
    public DatastoreConfigChangeListener(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }
    public void init() {
        InstanceIdentifier<DatastoreConfig> id = InstanceIdentifier.create(DatastoreConfig.class);
        lisReg = dataBroker.registerDataChangeListener(LogicalDatastoreType.CONFIGURATION,
                id, this, AsyncDataBroker.DataChangeScope.ONE);
        listenerReg = dataBroker.registerDataTreeChangeListener(
                new DataTreeIdentifier(LogicalDatastoreType.CONFIGURATION,id),this);
    }

    public void close() {
        lisReg.close();
        listenerReg.close();
    }
    @Override
    public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        Map<InstanceIdentifier<?>, DataObject> updatedData = change.getUpdatedData();
        Map<InstanceIdentifier<?>, DataObject> createdData = change.getCreatedData();
        Map<InstanceIdentifier<?>, DataObject> originalData = change.getOriginalData();
        Set<InstanceIdentifier<?>> deletedData = change.getRemovedPaths();

        for (InstanceIdentifier<?> iid : deletedData) {
            LOG.info("onDataChanged,delete {}",iid);
        }
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entrySet : updatedData.entrySet()) {
            InstanceIdentifier<?> iiD = entrySet.getKey();
            final DataObject dataObject = entrySet.getValue();
            LOG.info("onDataChanged,updated {},content {}",iiD,dataObject);
        }
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entrySet : createdData.entrySet()) {
            InstanceIdentifier<?> iiD = entrySet.getKey();
            final DataObject dataObject = entrySet.getValue();
            LOG.info("onDataChanged,created {},content {}",iiD,dataObject);
        }
    }

    @Override
    public void onDataTreeChanged(@Nonnull Collection<DataTreeModification<DatastoreConfig>> collection) {
        for (final DataTreeModification<DatastoreConfig> change : collection) {
            final DataObjectModification<DatastoreConfig> rootNode = change.getRootNode();
            switch (rootNode.getModificationType()) {
                case SUBTREE_MODIFIED:
                    LOG.info("node {} modified,content {}", rootNode.getIdentifier(),rootNode.getDataAfter());
                    break;
                case WRITE:
                    LOG.info("node {} created,content {}", rootNode.getIdentifier(),rootNode.getDataAfter());
                    break;
                case DELETE:
                    LOG.debug("node {} deleted", rootNode.getIdentifier());
                    break;
            }
        }
    }
}
