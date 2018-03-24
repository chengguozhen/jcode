/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.dhcp.Inventory;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Inventory reads the opendaylight-inventory tree in MD-SAL data store.
 */
public class InventoryReader implements DataChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(InventoryReader.class);
    private DataBroker dataService;
    // Key: SwitchId, Value: NodeConnectorRef that corresponds to NC between
    // controller & switch
    private HashMap<String, NodeConnectorRef> controllerSwitchConnectors;
    // Key: SwitchId, Value: List of node connectors on this switch
    private HashMap<String, List<NodeConnectorRef>> switchNodeConnectors;
    private List<ListenerRegistration<DataChangeListener>> listenerRegistrationList = new ArrayList<>();

    public void setRefreshData(boolean refreshData) {
        this.refreshData = refreshData;
    }

    private boolean refreshData = false;
    private long refreshDataDelay = 20L;
    private boolean refreshDataScheduled = false;
    private final ScheduledExecutorService nodeConnectorDataChangeEventProcessor = Executors.newScheduledThreadPool(1);

    /**
     * Construct an InventoryService object with the specified inputs.
     *
     * @param dataService
     *            The DataBrokerService associated with the InventoryService.
     */
    public InventoryReader(DataBroker dataService) {
        this.dataService = dataService;
        controllerSwitchConnectors = new HashMap<String, NodeConnectorRef>();
        switchNodeConnectors = new HashMap<String, List<NodeConnectorRef>>();
    }




    public HashMap<String, NodeConnectorRef> getControllerSwitchConnectors() {
        return controllerSwitchConnectors;
    }

    public HashMap<String, List<NodeConnectorRef>> getSwitchNodeConnectors() {
        return switchNodeConnectors;
    }

    @Override
    public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> asyncDataChangeEvent){
        if (asyncDataChangeEvent == null) {
            LOG.info("In onDataChanged: No processing done as change even is null.");
            return;
        }

        if(!refreshDataScheduled) {
            synchronized(this) {
                if(!refreshDataScheduled) {
                    nodeConnectorDataChangeEventProcessor.schedule(new NodeConnectorDataChangeEventProcessor(),refreshDataDelay, TimeUnit.MILLISECONDS);
                    refreshDataScheduled = true;
                }
            }
        }

    }


    public void close() {
        for (ListenerRegistration lr:listenerRegistrationList){
            lr.close();
        }
    }

    /**
     * Read the Inventory data tree to find information about the Nodes and
     * NodeConnectors. Create the list of NodeConnectors for a given switch.
     * Also determine the STP status of each NodeConnector.
     */




    private class NodeConnectorDataChangeEventProcessor implements Runnable {

        @Override
        public void run() {
            controllerSwitchConnectors.clear();
            switchNodeConnectors.clear();
            refreshDataScheduled = false;
            setRefreshData(true);
        }

    }

}