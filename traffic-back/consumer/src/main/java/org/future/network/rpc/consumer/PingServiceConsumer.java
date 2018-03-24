/*
 * Copyright © 2017 My Company and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.future.network.rpc.consumer;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.JdkFutureAdapters;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ping.rev170826.PingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ping.rev170826.PingHostsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ping.rev170826.PingHostsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ping.rev170826.ping.hosts.input.DestinationAll;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ping.rev170826.ping.hosts.input.DestinationAllBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

/**
 * Created by 10032272 on 2017/8/26 0026.
 */
public class PingServiceConsumer {
    private static final Logger LOG = LoggerFactory.getLogger(PingServiceConsumer.class);
    private PingService pingService;
    public PingServiceConsumer(PingService pingService) {
        this.pingService = pingService;
        init();
    }
    public void init() {
        new Thread(()->{testRpcCall();}).start();
    }

    public void close() {

    }
    private void testRpcCall() {
        LOG.info("testRpcCall start!");
        PingHostsInputBuilder inputBuilder = new PingHostsInputBuilder();
        inputBuilder.setAsync(false);
        List<DestinationAll> destinations = new ArrayList<>();
        DestinationAllBuilder localhostBuilder = new DestinationAllBuilder();
        localhostBuilder.setDestination(new Ipv4Address("127.0.0.1"));
        destinations.add(localhostBuilder.build());

        DestinationAllBuilder gwBuilder = new DestinationAllBuilder();
        gwBuilder.setDestination(new Ipv4Address("192.168.0.1"));
        destinations.add(gwBuilder.build());
        DestinationAllBuilder destinationBuilder = new DestinationAllBuilder();
        destinationBuilder.setDestination(new Ipv4Address("192.168.0.105"));
        destinations.add(destinationBuilder.build());
        DestinationAllBuilder unknownBuilder = new DestinationAllBuilder();
        unknownBuilder.setDestination(new Ipv4Address("111.111.0.1"));
        destinations.add(unknownBuilder.build());

        inputBuilder.setDestinationAll(destinations);
        long start = System.currentTimeMillis();
        LOG.info("\n\n***sync call start time="+start);
        try {
            PingHostsOutput results = pingService.pingHosts(inputBuilder.build()).get().getResult();
            LOG.info(results.toString());
        } catch (InterruptedException e) {
            LOG.warn("rpc call error,",e);
        } catch (ExecutionException e) {
            LOG.warn("rpc call error,",e);
        }
        long end = System.currentTimeMillis();
        LOG.info("sync call end time="+end+",spend time="+(end-start));
        LOG.info("***sync call end\n\n\n\n");


        inputBuilder.setAsync(true);
        final long start2 = System.currentTimeMillis();
        LOG.info("\n***async call start time="+start2);
        Future<RpcResult<PingHostsOutput>> future = pingService.pingHosts(inputBuilder.build());

        end = System.currentTimeMillis();
        LOG.info("async call end time="+end+",spend time="+(end-start2));
        ListenableFuture<RpcResult<PingHostsOutput>> listenableFuture = JdkFutureAdapters.listenInPoolThread(future);
        Futures.addCallback(listenableFuture, new FutureCallback<RpcResult<PingHostsOutput>>() {

            @Override
            public void onSuccess(@Nullable RpcResult<PingHostsOutput> PingHostsOutputRpcResult) {
                long end2 = System.currentTimeMillis();
                LOG.info("***async call final end time="+end2+",spend time="+(end2-start2));
                LOG.info(PingHostsOutputRpcResult.getResult().toString());
                LOG.info("rpc async call Success\n\n\n\n");
            }

            @Override
            public void onFailure(Throwable throwable) {
                LOG.error("onFailure",throwable);
            }
        });
    }
}
