/*
 * Copyright © 2017 NDSC and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.dip.dns.core;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.NotificationService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.dns.yang.dns.config.rev171124.DnsConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yangtools.concepts.Registration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;

public class DnsProvider {

    private static final Logger LOG = LoggerFactory.getLogger(DnsProvider.class);
    private List<Registration> listenerReg;
    private final DnsConfig config;
    private final DataBroker dataBroker;
    private final NotificationService notificationService;
    private final SalFlowService salFlowService;
    private final PacketProcessingService packetProcessingService;

    public DnsProvider(final DnsConfig config,
                       final DataBroker dataBroker,
                       final NotificationService notificationService,
                       final SalFlowService salFlowService,
                       final PacketProcessingService packetProcessingService) {
        this.config = config;
        this.dataBroker = dataBroker;
        this.notificationService = notificationService;
        this.salFlowService = salFlowService;
        this.packetProcessingService = packetProcessingService;
        listenerReg = new ArrayList<>();
    }

    /**
     * Method called when the blueprint container is created.
     */
    public void init() {

        InitialFlowWriter initialFlowWriter = new InitialFlowWriter(salFlowService);
        initialFlowWriter.setDnsAddress(config.getDnsAddress().getValue());
        initialFlowWriter.setDnsForwardAddress(config.getDnsForwardAddress().getValue());
        initialFlowWriter.setFlowTableId(config.getDnsFlowTableId());
        initialFlowWriter.setFlowPriority(config.getDnsFlowPriority());
        initialFlowWriter.setFlowIdleTimeout(config.getDnsFlowIdleTimeout());
        initialFlowWriter.setFlowHardTimeout(config.getDnsFlowHardTimeout());
        initialFlowWriter.setIsForward(config.isIsForwardMode());
        listenerReg.add( initialFlowWriter.registerAsDataChangeListener(dataBroker) );
        DnsPacketDispatcher dnsPacketDispatcher = new DnsPacketDispatcher(packetProcessingService);
        DnsRecord dnsRecord = new DnsRecord(dataBroker);
        listenerReg.add(dnsRecord.registerAsDataChangeListener());
        DnsPacketHandler dnsPacketHandler = new DnsPacketHandler(dnsPacketDispatcher,dataBroker,dnsRecord);
        listenerReg.add( notificationService.registerNotificationListener(dnsPacketHandler) );
        if(config.isIsForwardMode()){
            initialFlowWriter.setForwardPriority(config.getForwardFlowPriority());
            initialFlowWriter.setForwardIdleTimeout(config.getForwardFlowIdleTimeout());
            initialFlowWriter.setForwardHardTimeout(config.getForwardFlowHardTimeout());
            DnsForward dnsForward = new DnsForward(packetProcessingService);
            dnsForward.setDnsForwardAddress(config.getDnsForwardAddress().getValue());
            dnsPacketHandler.setDnsForward(dnsForward);
        }
        LOG.info("DNS:DnsHandlerProvider Session Initiated");
    }

    /**
     * Method called when the blueprint container is destroyed.
     */
    public void close() throws Exception {
        for(Registration registration : listenerReg){
            if(registration != null) {
                registration.close();
            }
        }
        LOG.info("DNS:DnsHandlerProvider Closed");
    }
}