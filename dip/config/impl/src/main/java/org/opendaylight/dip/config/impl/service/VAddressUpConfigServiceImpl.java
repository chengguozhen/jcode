/*
 * Copyright © 2017 NDSC and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.dip.config.impl.service;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.SettableFuture;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dipapp.v.address.up.config.rev150105.*;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Created by Guozhen Cheng at 12/16/17 8:46 AM
 * Dept: ndsc.org
 * @author Guozhen Cheng
 */
public class VAddressUpConfigServiceImpl implements VAddressUpConfigService{
    private static final Logger LOG = LoggerFactory.getLogger(VAddressUpConfigServiceImpl.class);
    private DataBroker dataBroker;
    private InstanceIdentifier<VAddressUpConfig> root;

    public VAddressUpConfigServiceImpl(final DataBroker dataBroker) {
        this.dataBroker = dataBroker;
        LOG.info("VAddressUpConfig Session Initiated");
    }

    private SelectModelOutput selectmodelConfig(SelectModelInput input) {
        SelectModelOutputBuilder subnetConfigOutputBuilder = new SelectModelOutputBuilder();
        StringBuffer subnetinformation = new StringBuffer();
        try {
                subnetinformation.append(input.getMutationModel());
                subnetinformation.append(",");
                subnetinformation.append(input.getMutationPeriod());
                subnetinformation.append(";");

            subnetConfigOutputBuilder.setMessage(subnetinformation.toString());
            subnetConfigOutputBuilder.setSuccess(true);

        } catch (Exception e) {
            subnetConfigOutputBuilder.setSuccess(false);
        }
        return subnetConfigOutputBuilder.build();
    }

    private void upconfigDataStore(SelectModelInput input) {
        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        InstanceIdentifier<VAddressUpConfig> id =
                InstanceIdentifier.create(VAddressUpConfig.class);
        VAddressUpConfigBuilder vAddressUpConfigBuilder= new VAddressUpConfigBuilder();
        vAddressUpConfigBuilder.setMutationModel(input.getMutationModel());
        vAddressUpConfigBuilder.setMutationPeriod(input.getMutationPeriod());
        tx.put(LogicalDatastoreType.CONFIGURATION,id,vAddressUpConfigBuilder.build());
        CheckedFuture<Void,TransactionCommitFailedException> future = tx.submit();
        LOG.info("The VAddressUpConfig datastore has inserted {}", tx.getIdentifier());
    }

    public void close() {
        LOG.info("VAddressUpConfig Closed");
    }

    @Override
    public Future<RpcResult<GetModelOutput>> getModel() {
        return CompletableFuture.supplyAsync(()->{
                    ReadTransaction tr = dataBroker.newReadOnlyTransaction();
                    MtdModel mtdModel = null;
                    try {
                        Optional<VAddressUpConfig> opData = tr.read(LogicalDatastoreType.CONFIGURATION,root).get();
                        if (opData.isPresent()){
                            mtdModel = opData.get().getMutationModel();
                        }
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                    return RpcResultBuilder.success(new GetModelOutputBuilder().setMutationModel(mtdModel).build()).build();
                });
    }

    @Override
    public Future<RpcResult<SelectModelOutput>> selectModel(SelectModelInput input) {
        SettableFuture<RpcResult<SelectModelOutput>> futureResult = SettableFuture.create();
        SelectModelOutput result = selectmodelConfig(input);
        futureResult.set(RpcResultBuilder.success(result).build());

        upconfigDataStore(input);
        return futureResult;
    }

}
