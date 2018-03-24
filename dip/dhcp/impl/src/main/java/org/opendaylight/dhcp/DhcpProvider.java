/*
 * Copyright (c) 2015 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.dhcp;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yangtools.concepts.Registration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DhcpProvider  {
    private final static Logger LOG = LoggerFactory.getLogger(DhcpProvider.class);
    private Registration listenerRegistration = null, floodTopoListenerReg = null, floodInvListenerReg = null, topoNodeListenerReg = null;

    private final NotificationProviderService notificationService;
    private final DataBroker dataBroker;
    private final SalFlowService salFlowService;
    private final PacketProcessingService packetProcessingService;
    private Registration packetListener = null;


    public DhcpProvider(NotificationProviderService notificationService, DataBroker dataBroker,
                        SalFlowService salFlowService, PacketProcessingService packetProcessingService
    ) {
        this.notificationService = notificationService;
        this.dataBroker = dataBroker;
        this.salFlowService = salFlowService;
        this.packetProcessingService = packetProcessingService;
    }


    public void init() {
        LOG.info("DhcpProvider Session Initiated");
        try {
            //DhcpConfig dhcpConfig=new DhcpConfig(dataBroker);
            //DhcpVConfig dhcpVConfig=new DhcpVConfig(dataBroker);
            //DhcpVupConfig dhcpVupConfig=new DhcpVupConfig(dataBroker);

            //initialize and register the DhcpPktHandler Module
            //DhcpPktHandler dhcpPktHandler = new DhcpPktHandler(packetProcessingService, dhcpConfig,dhcpVConfig,
            //        dhcpVupConfig, dataBroker);
            DhcpPktHandler dhcpPktHandler = new DhcpPktHandler(packetProcessingService, dataBroker);

            this.listenerRegistration = notificationService.registerNotificationListener(dhcpPktHandler);

            LOG.info("Finish DhcpProvider Session Initiated");
        } catch (Exception e) {
            LOG.error("Error initializing services ", e);
        }
    }

    public void close() throws Exception {
        if(listenerRegistration != null) {
            listenerRegistration.close();
        }
        if(packetListener != null) {
            packetListener.close();
        }
        LOG.info("DhcpProvider closed");
    }

}
