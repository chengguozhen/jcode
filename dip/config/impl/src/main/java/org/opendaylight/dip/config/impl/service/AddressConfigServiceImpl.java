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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dipapp.address.config.rev150105.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dipapp.address.config.rev150105.addrs.domain.IpConf;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dipapp.address.config.rev150105.addrs.domain.IpConfBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dipapp.address.config.rev150105.addrs.domain.IpConfKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Future;

/**
 * Created by Guozhen Cheng at 12/13/17 5:59 PM
 * Dept: ndsc.org
 */
public class AddressConfigServiceImpl implements AddressConfigService{
    private static final Logger LOG = LoggerFactory.getLogger(AddressConfigServiceImpl.class);
    private final NotificationPublishService notificationPublishService;
    private final DataBroker dataBroker;

    public AddressConfigServiceImpl(NotificationPublishService notificationPublishService, DataBroker dataBroker) {
        this.notificationPublishService = notificationPublishService;
        this.dataBroker = dataBroker;
    }

    private SubnetConfigOutput realAddrConfig(List<IpConf> ipConfList) {
        SubnetConfigOutputBuilder subnetConfigOutputBuilder = new SubnetConfigOutputBuilder();
        StringBuffer subnetinformation = new StringBuffer();
        try {
            for (IpConf ipConf : ipConfList) {
                subnetinformation.append(ipConf.getRipsId());
                subnetinformation.append(",");
                subnetinformation.append(ipConf.getRipsName());
                subnetinformation.append(",");
                subnetinformation.append(ipConf.getStartIp());
                subnetinformation.append(",");
                subnetinformation.append(ipConf.getEndIp());
                subnetinformation.append(",");
                subnetinformation.append(ipConf.getDomainPrefix());
                subnetinformation.append(",");
                subnetinformation.append(ipConf.getIpGlobalPeriod());
                subnetinformation.append(";");
            }

            subnetConfigOutputBuilder.setMessage(subnetinformation.toString());
            subnetConfigOutputBuilder.setSuccess(true);
            subnetConfigOutputBuilder.setState(StateModel.Success);

        } catch (Exception e) {
            subnetConfigOutputBuilder.setSuccess(false);
            subnetConfigOutputBuilder.setState(StateModel.False);
        }
        return subnetConfigOutputBuilder.build();
    }


    private void realAddrNotification(List<IpConf> ipConfList) {
        AddrSegmentBuilder addrSegmentBuilder = new AddrSegmentBuilder();
        addrSegmentBuilder.setIpConf(ipConfList);
        try {
            notificationPublishService.putNotification(addrSegmentBuilder.build());
            LOG.info("The binding message has been broadcasted {}", addrSegmentBuilder.build());

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void realAddrDataStore(List<IpConf> ipConfList) {
        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        InstanceIdentifier<IpConf> id =
                InstanceIdentifier.create(Subnets.class)
                        .child(IpConf.class,
                                new IpConfKey(ipConfList.get(0).getRipsId()));
        for (IpConf ipConf : ipConfList) {
            IpConfBuilder builder = new IpConfBuilder();
            builder.setRipsId(ipConf.getRipsId());
            builder.setRipsName(ipConf.getRipsName());
            builder.setStartIp(ipConf.getStartIp());
            builder.setEndIp(ipConf.getEndIp());
            builder.setDomainPrefix(ipConf.getDomainPrefix());
            builder.setIpGlobalPeriod(ipConf.getIpGlobalPeriod());
            builder.setKey(new IpConfKey(ipConf.getRipsId()));
            tx.put(LogicalDatastoreType.CONFIGURATION,id,builder.build());
            CheckedFuture<Void,TransactionCommitFailedException> future = tx.submit();
            LOG.info("The datastore has inserted {}", tx.getIdentifier());
        }
    }

    /**
     * vaddr configuration service
     *
     * @param input
     */
    @Override
    public Future<RpcResult<SubnetConfigOutput>> subnetConfig(SubnetConfigInput input) {
        //Ip config Rpc
        SettableFuture<RpcResult<SubnetConfigOutput>> future = SettableFuture.create();
        SubnetConfigOutput result = realAddrConfig(input.getIpConf());
        future.set(RpcResultBuilder.success(result).build());

        //Ip config Notification and Datastore
        realAddrNotification(input.getIpConf());
        realAddrDataStore(input.getIpConf());

        //return Ip config Rpc
        return future;
    }
}
