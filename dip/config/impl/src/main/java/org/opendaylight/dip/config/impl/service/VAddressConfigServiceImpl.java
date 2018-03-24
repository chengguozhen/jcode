/*
 * Copyright © 2017 NDSC and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.dip.config.impl.service;

import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.SettableFuture;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dipapp.v.address.config.rev150105.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dipapp.v.address.config.rev150105.v.addrs.VIpConf;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dipapp.v.address.config.rev150105.v.addrs.VIpConfBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dipapp.v.address.config.rev150105.v.addrs.VIpConfKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dipapp.v.address.config.rev150105.vaddr.config.VAddrList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dipapp.v.address.config.rev150105.vaddr.config.VAddrListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dipapp.v.address.config.rev150105.vaddr.config.VAddrListKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

/**
 * Created by Guozhen Cheng at 12/16/17 8:45 AM
 * Dept: ndsc.org
 */
public class VAddressConfigServiceImpl implements VAddressConfigService {
    private static final Logger LOG = LoggerFactory.getLogger(VAddressConfigServiceImpl.class);
    private final NotificationPublishService notificationPublishService;
    private final DataBroker dataBroker;

    public VAddressConfigServiceImpl(NotificationPublishService notificationPublishService, DataBroker dataBroker) {
        this.notificationPublishService = notificationPublishService;
        this.dataBroker = dataBroker;
    }

    private VaddrConfOutput virtualAddrConfig(List<VIpConf> vIpConfList, String ripsId) {
        VaddrConfOutputBuilder vaddrConfOutputBuilder = new VaddrConfOutputBuilder();
        StringBuffer vaddrconfiginformation = new StringBuffer();
        vaddrconfiginformation.append(ripsId);
        vaddrconfiginformation.append(":");
        try {
            for (VIpConf vIpConf : vIpConfList) {
                vaddrconfiginformation.append(vIpConf.getVipsId());
                vaddrconfiginformation.append(",");
                vaddrconfiginformation.append(vIpConf.getVStartIp());
                vaddrconfiginformation.append(",");
                vaddrconfiginformation.append(vIpConf.getVEndIp());
                vaddrconfiginformation.append(",");
                vaddrconfiginformation.append(vIpConf.getVIpGlobalPeriod());
                vaddrconfiginformation.append(";");
            }

            vaddrConfOutputBuilder.setMessage(vaddrconfiginformation.toString());
            vaddrConfOutputBuilder.setSuccess(true);
            vaddrConfOutputBuilder.setVstate(VstateModel.Success);

        } catch (Exception e) {
            vaddrConfOutputBuilder.setSuccess(false);
            vaddrConfOutputBuilder.setVstate(VstateModel.False);
        }
        return vaddrConfOutputBuilder.build();
    }

    private void virtualAddrNotification(List<VIpConf> vIpConfList,String ripsId) {
        VaddrSegmentBuilder vaddrSegmentBuilder = new VaddrSegmentBuilder();
        vaddrSegmentBuilder.setVIpConf(vIpConfList);
        vaddrSegmentBuilder.setRipsId(ripsId);
        try {
            notificationPublishService.putNotification(vaddrSegmentBuilder.build());
            LOG.info("The binding message has been broadcasted {}", vaddrSegmentBuilder.build());

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private List<VIpConf> virtualAddrDataStore(List<VIpConf> vIpConfList,String ripsId) {
        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        InstanceIdentifier<VIpConf> id =
                InstanceIdentifier.create(VaddrConfig.class)
                        .child(VAddrList.class, new VAddrListKey(ripsId)).
                        child(VIpConf.class, new VIpConfKey(vIpConfList.get(0).getKey()));
        List<VIpConf> vIpConfDataStoreList= new ArrayList<>();
        for (VIpConf vIpConf : vIpConfList) {
            VIpConfBuilder vIpConfBuilder = new VIpConfBuilder();
            vIpConfBuilder.setVipsId(vIpConf.getVipsId());
            vIpConfBuilder.setVStartIp(vIpConf.getVStartIp());
            vIpConfBuilder.setVEndIp(vIpConf.getVEndIp());
            vIpConfBuilder.setVIpGlobalPeriod(vIpConf.getVIpGlobalPeriod());
            vIpConfBuilder.setKey(new VIpConfKey(vIpConf.getKey()));
            tx.put(LogicalDatastoreType.CONFIGURATION,id,vIpConfBuilder.build());
            CheckedFuture<Void,TransactionCommitFailedException> future = tx.submit();
            LOG.info("The virtualAddr datastore has inserted {}", tx.getIdentifier());
            vIpConfDataStoreList.add(vIpConfBuilder.build());
        }
        return vIpConfDataStoreList;
    }

    private void virtualAddrMappingDataStore(List<VIpConf> vIpConfList,String ripsId) {
        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        InstanceIdentifier<VAddrList> id =
                InstanceIdentifier.create(VaddrConfig.class)
                        .child(VAddrList.class, new VAddrListKey(ripsId));

        VAddrListBuilder vAddrListBuilder=new VAddrListBuilder();
        vAddrListBuilder.setRipsId(ripsId);
        vAddrListBuilder.setKey(new VAddrListKey(ripsId));
        vAddrListBuilder.setVIpConf(virtualAddrDataStore(vIpConfList,ripsId));

        tx.put(LogicalDatastoreType.CONFIGURATION,id,vAddrListBuilder.build());
        CheckedFuture<Void,TransactionCommitFailedException> future = tx.submit();
        LOG.info("The virtualAddrMapping datastore has inserted {}", tx.getIdentifier());
    }

    /**
     * vaddr configuration service
     *
     * @param input
     */
    @Override
    public Future<RpcResult<VaddrConfOutput>> vaddrConf(VaddrConfInput input) {
        //Ip config Rpc
        SettableFuture<RpcResult<VaddrConfOutput>> future = SettableFuture.create();
        VaddrConfOutput result = virtualAddrConfig(input.getVIpConf(),input.getRipsId());
        future.set(RpcResultBuilder.success(result).build());

        //Ip config Notification and Datastore
        virtualAddrNotification(input.getVIpConf(),input.getRipsId());
        virtualAddrMappingDataStore(input.getVIpConf(),input.getRipsId());

        //return Ip config Rpc
        return future;
    }
}
