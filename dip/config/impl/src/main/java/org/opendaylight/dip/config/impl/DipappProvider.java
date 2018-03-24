/*
 * Copyright © 2017 NDSC and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.dip.config.impl;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DipappProvider{

    private static final Logger LOG = LoggerFactory.getLogger(DipappProvider.class);
    private final DataBroker dataBroker;


    /**
     * As defined on
     * controller/opendaylight/md-sal/topology-manager/src/main/java/org/opendaylight/md/controller/topology/manager/FlowCapableTopologyProvider.java
     */
    private static final String TOPOLOGY_NAME = "flow:1";

    public DipappProvider(final DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    /**
     * Method called when the blueprint container is created.
     */
    public void init() {
        LOG.info("Bingo:DipappProvider Session Initiated");
    }

    /**
     * Method called when the blueprint container is destroyed.
     */
    public void close() {
        LOG.info("DipappProvider Closed");
    }

    private int vAddressImutation(){
        int numHostsImutated = 0;

        // TODO: 添加IP地址轮换操作。

        LOG.debug("Number of imutated hosts during current imutation interval - {}. ", numHostsImutated);
        return numHostsImutated;
    }
}