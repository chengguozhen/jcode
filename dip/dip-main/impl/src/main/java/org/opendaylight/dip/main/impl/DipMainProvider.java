/*
 * Copyright © 2017 NDSC and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.dip.main.impl;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.NotificationService;
import org.opendaylight.dip.main.impl.flow.FlowWriterService;
import org.opendaylight.dip.main.impl.flow.InitialFlowWriter;
import org.opendaylight.dip.main.impl.flow.PacketOutService;
import org.opendaylight.dip.main.impl.session.SessionRecord;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dipapp.main.config.rev140528.DipMainConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.rpc.get.change.ip.rev170528.GetChangeIpService;
import org.opendaylight.yangtools.concepts.Registration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.rpc.judge.real.ip.rev170528.JudgeRealIpService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.rpc.judge.virtual.ip.rev170528.JudgeVirtualIpService;
import java.util.ArrayList;
import java.util.List;


/**
 * @author chenyang
 */
public class DipMainProvider {

    private static final Logger LOG = LoggerFactory.getLogger(DipMainProvider.class);
    private List<Registration> listenerReg;
    private final SalFlowService salFlowService;
    private final PacketProcessingService packetProcessingService;
    private final NotificationService notificationService;
    private final DataBroker dataBroker;
    private final GetChangeIpService virtualChange;
    private final DipMainConfig dipMainConfig;
    private final JudgeRealIpService realJudgeIp;
    private final JudgeVirtualIpService virtualIPJudge;

    public DipMainProvider(final DataBroker dataBroker,
                           final SalFlowService salFlowService,
                           final PacketProcessingService packetProcessingService,
                           final NotificationService notificationService,
                           final GetChangeIpService virtualChange,
                           final DipMainConfig dipMainConfig,
                           final JudgeRealIpService realJudgeIp,
                           final JudgeVirtualIpService virtualIPJudge) {
        this.dataBroker = dataBroker;
        this.salFlowService = salFlowService;
        this.packetProcessingService = packetProcessingService;
        this.notificationService = notificationService;
        this.virtualChange = virtualChange;
        this.dipMainConfig = dipMainConfig;
        this.realJudgeIp = realJudgeIp;
        this.virtualIPJudge = virtualIPJudge;
        listenerReg = new ArrayList<>();
    }

    /**
     * Method called when the blueprint container is created.
     */
    public void init() {
        LOG.info("DipMainProvider Session Initiated");

        // Write initial flows
        if (dipMainConfig.isIsInstallDropallFlow()) {
            LOG.info("Dip will install a dropall flow on each switch");
            InitialFlowWriter initialFlowWriter = new InitialFlowWriter(salFlowService);
            initialFlowWriter.setFlowTableId(dipMainConfig.getDropallFlowTableId());
            initialFlowWriter.setFlowPriority(dipMainConfig.getDropallFlowPriority());
            initialFlowWriter.setFlowIdleTimeout(dipMainConfig.getDropallFlowIdleTimeout());
            initialFlowWriter.setFlowHardTimeout(dipMainConfig.getDropallFlowHardTimeout());
            listenerReg.add(initialFlowWriter.registerAsDataChangeListener(dataBroker));
        }
        else {
            LOG.info("Dropall flows will not be installed");
        }

        if (dipMainConfig.isIsDipChangeMode()) {

            //start Ip change model
            DipChange dipChange = new DipChange(dataBroker,virtualChange);
            listenerReg.add(dipChange.registerAsDataChangeListener());
            dipChange.initialRead();

            // Setup FlowWrtierService
            FlowWriterService flowWriterService = new FlowWriterService(salFlowService);
            flowWriterService.setFlowTableId(dipMainConfig.getReactiveFlowTableId());
            flowWriterService.setFlowPriority(dipMainConfig.getReactiveFlowPriority());
            flowWriterService.setFlowIdleTimeout(dipMainConfig.getReactiveFlowIdleTimeout());
            flowWriterService.setFlowHardTimeout(dipMainConfig.getReactiveFlowHardTimeout());

            // Setup reactive flow writer
            LOG.info("Dip will react to network traffic and install flows");
            SessionRecord sessionRecord = new SessionRecord(dataBroker);
            PacketOutService packetOutService = new PacketOutService(packetProcessingService);
            IpHandler ipHandler = new IpHandler(dataBroker, flowWriterService,packetOutService, sessionRecord,realJudgeIp,virtualIPJudge);
            listenerReg.add(notificationService.registerNotificationListener(ipHandler));
        }else {
            LOG.info("Dip is not start");
        }


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
        LOG.info("DipMainProvider(instance {}) torn down.", this);
    }
}