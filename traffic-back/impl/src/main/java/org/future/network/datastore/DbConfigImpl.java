/*
 * Copyright © 2017 My Company and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.future.network.datastore;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.OptimisticLockFailedException;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChain;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.datastoreconfig.rev150105.DatastoreConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.datastoreconfig.rev150105.DatastoreConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.datastoreconfig.rev150105.DatastoreconfigService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.datastoreconfig.rev150105.DbSettingInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.datastoreconfig.rev150105.DbSettingOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.datastoreconfig.rev150105.DbSettingOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.datastoreconfig.rev150105.datastore.config.DatastoreShard;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.datastoreconfig.rev150105.datastore.config.DatastoreShardBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.datastoreconfig.rev150105.datastore.config.DatastoreShardKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.traffic.rev150105.HelloWorldInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.traffic.rev150105.HelloWorldOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.traffic.rev150105.HelloWorldOutputBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

public class DbConfigImpl implements DatastoreconfigService, TransactionChainListener {

    private static final Logger LOG = LoggerFactory.getLogger(DbConfigImpl.class);
    private static final int QUEUE_DEPTH = 512;
    private TransactionChain transactionChain;
    private final DataBroker dataBroker;
    private final BlockingQueue<DbOperation> queue;

    public DbConfigImpl(final DataBroker dataBroker) {
        this.dataBroker = dataBroker;
        transactionChain = this.dataBroker.createTransactionChain(this);
        this.queue = new LinkedBlockingQueue<>(QUEUE_DEPTH);
    }

    /**
     * Method called when the blueprint container is created.
     */
    public void init() {
        LOG.info("TrafficProvider Session Initiated");
        ReadWriteTransaction txr = dataBroker.newReadWriteTransaction();
        //WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        InstanceIdentifier<DatastoreConfig> root = InstanceIdentifier.
                create(DatastoreConfig.class);
        try {
            CheckedFuture<Optional<DatastoreConfig>, ReadFailedException> future =
                    txr.read(LogicalDatastoreType.CONFIGURATION,root);
            Optional<DatastoreConfig> opData = future.get();
            if(!opData.isPresent()) {
                DatastoreConfigBuilder builder = new DatastoreConfigBuilder();
                txr.put(LogicalDatastoreType.CONFIGURATION,root,builder.build());
                txr.submit().checkedGet();
            }

        } catch (InterruptedException e) {
            LOG.warn(" init DatastoreConfig error",e);
        } catch (ExecutionException e) {
            LOG.warn(" init DatastoreConfig error",e);
        } catch (TransactionCommitFailedException e) {
            LOG.warn(" init DatastoreConfig error",e);
        }
    }

    /**
     * Method called when the blueprint container is destroyed.
     */
    public void close() {
        LOG.info("DbConfig Closed");
        if (transactionChain != null) {
            transactionChain.close();
        }
    }


    @Override
    public Future<RpcResult<DbSettingOutput>> dbSetting(DbSettingInput input) {
        SettableFuture<RpcResult<DbSettingOutput>> futureResult = SettableFuture.create();
        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        InstanceIdentifier<DatastoreShard> id =
                InstanceIdentifier.create(DatastoreConfig.class)
                        .child(DatastoreShard.class,
                                new DatastoreShardKey(input.getShardName()));
        DatastoreShard cfg = createDbConfig(input);
        tx.put(LogicalDatastoreType.CONFIGURATION,id,cfg);
        CheckedFuture<Void,TransactionCommitFailedException> future = tx.submit();
        Futures.addCallback(future, new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void aVoid) {
                futureResult.set(RpcResultBuilder.success(
                        new DbSettingOutputBuilder().setResult(true).build()).build());
            }
            @Override
            public void onFailure(Throwable throwable) {
                futureResult.set(RpcResultBuilder.success(
                        new DbSettingOutputBuilder().setResult(false).build()).build());
            }
        });
        return futureResult;
    }

    private DatastoreShard createDbConfig(DbSettingInput input) {
        DatastoreShardBuilder builder = new DatastoreShardBuilder();
        builder.setCustomRaftPolicyImplementation(input.getCustomRaftPolicyImplementation());
        builder.setDbType(input.getDbType());
        builder.setPersistent(input.isPersistent());
        builder.setShardName(input.getShardName());
        builder.setTransactionCommitQueueCapacity(input.getTransactionCommitQueueCapacity());
        builder.setKey(new DatastoreShardKey(input.getShardName()));
        return builder.build();
    }

    public void enqueueOperation(DbOperation op) {
        try {
            queue.put(op);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void submitTransaction(final AsyncReadWriteTransaction tx, final int tries) {
        Futures.addCallback(tx.submit(), new FutureCallback<Object>() {
            public void onSuccess(Object o) {
                LOG.trace("tx {} succeeded", tx.getIdentifier());
            }

            public void onFailure(Throwable t) {
                if (t instanceof OptimisticLockFailedException) {
                    if ((tries - 1) > 0) {
                        LOG.warn("tx {} failed, retrying", tx.getIdentifier());
                        // do retry
                        submitTransaction(tx, tries - 1);
                    } else {
                        LOG.warn("tx {} failed, out of retries", tx.getIdentifier());
                        // out of retries
                        chainFailure();
                    }
                } else {
                    // failed due to another type of
                    // TransactionCommitFailedException.
                    LOG.warn("tx {} failed: {}", tx.getIdentifier(), t.getMessage());
                    chainFailure();
                }
            }
        });
    }
    @Override
    public void onTransactionChainFailed(TransactionChain<?, ?> transactionChain, AsyncTransaction<?, ?> asyncTransaction, Throwable throwable) {
        chainFailure();
    }
    private void chainFailure() {
        try {
            transactionChain.close();
            transactionChain = dataBroker.createTransactionChain(this);
            clearQueue();
        } catch (IllegalStateException e) {
            LOG.warn(e.getLocalizedMessage());
        }
    }

    @Override
    public void onTransactionChainSuccessful(TransactionChain<?, ?> transactionChain) {
        LOG.debug("transactionChain OK");
    }

    private void clearQueue() {
        while (!queue.isEmpty()) {
            queue.poll();
        }
    }

    public void execute() {
        boolean done = false;
        while (!done) {
            try {
                DbOperation op = queue.take();
                AsyncReadWriteTransaction tx = transactionChain.newReadWriteTransaction();

                int ops = 0;
                while ((op != null) && (ops < 100)) {
                    op.applyOperation(tx);
                    ops += 1;
                    op = queue.poll();
                }

                submitTransaction(tx, 3);
            } catch (InterruptedException e) {
                done = true;
            }
        }
        clearQueue();

    }
}