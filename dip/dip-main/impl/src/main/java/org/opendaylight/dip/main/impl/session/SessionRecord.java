/*
 * Copyright © 2017 NDSC and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.dip.main.impl.session;

import com.google.common.base.Optional;
import org.opendaylight.controller.md.sal.binding.api.*;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.dip.main.impl.DipChange;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dipapp.rev150105.FlowSessionsList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dipapp.rev150105.sessions.FlowSession;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dipapp.rev150105.sessions.FlowSessionKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newFixedThreadPool;

/**
 * @author chenyang
 */
public class SessionRecord {
    private static final Logger LOG = LoggerFactory.getLogger(DipChange.class);
    private final DataBroker dataBroker;
    private static final int CPUS = Runtime.getRuntime().availableProcessors();
    private final ExecutorService executorService = newFixedThreadPool(CPUS);

    public enum  ChangeType{
        Add,
        Del;
    }

    public SessionRecord(final DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    public void changeSession(FlowSession flowSession,ChangeType type) {
        WriteTransaction wt = dataBroker.newWriteOnlyTransaction();
        InstanceIdentifier<FlowSession> root = InstanceIdentifier.create(FlowSessionsList.class)
                        .child(FlowSession.class,flowSession.getKey());
        switch (type){
            case Add:
                wt.put(LogicalDatastoreType.OPERATIONAL,root,flowSession,true);
                dynamicChange(flowSession);
                break;
            case Del:
                wt.delete(LogicalDatastoreType.OPERATIONAL,root);
                break;
            default:
                LOG.error("Bingo:the change type is not right");
        }
        try {
            wt.submit().checkedGet();
        } catch (TransactionCommitFailedException e) {
            e.printStackTrace();
        }
    }

    public FlowSession readSession(FlowSessionKey session) {

        InstanceIdentifier<FlowSession> root = InstanceIdentifier.create(FlowSessionsList.class)
                .child(FlowSession.class, session);
        ReadOnlyTransaction rt = dataBroker.newReadOnlyTransaction();
        try {
            Optional<FlowSession> dataObjectOptional = rt.read(LogicalDatastoreType.OPERATIONAL, root).get();
            if (dataObjectOptional.isPresent()) {
                rt.close();
                return dataObjectOptional.get();
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed to read Session from data store");
            rt.close();
        }
        return  null;
    }


    private void dynamicChange(FlowSession flowSession) {
        executorService.submit(() -> {
            LOG.info("DSession:thread Start Session: {}",flowSession);
            long start = System.currentTimeMillis();
            try {
                Thread.sleep(flowSession.getTtl() * 1000);
                changeSession(flowSession,ChangeType.Del);
                LOG.info("DSession:thread start to change Session: {}", System.currentTimeMillis() - start);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

}
