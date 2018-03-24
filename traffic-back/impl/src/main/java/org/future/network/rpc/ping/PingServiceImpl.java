/*
 * Copyright © 2017 My Company and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.future.network.rpc.ping;

import com.google.common.util.concurrent.SettableFuture;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ping.rev170826.PingHostsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ping.rev170826.PingHostsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ping.rev170826.PingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ping.rev170826.PingHostsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ping.rev170826.PingHostsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ping.rev170826.PingHostsOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ping.rev170826.ping.hosts.input.DestinationAll;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ping.rev170826.ping.hosts.output.EchoResultAll;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ping.rev170826.ping.hosts.output.EchoResultAllBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by 10032272 on 2017/8/26 0026.
 */
public class PingServiceImpl implements PingService {
    private static final Logger LOG = LoggerFactory.getLogger(PingServiceImpl.class);

    ExecutorService executorService = Executors.newFixedThreadPool(1);

    private PingHostsOutput pingAllHost(List<DestinationAll> destinationAllList) {
        PingHostsOutputBuilder outputBuilder = new PingHostsOutputBuilder();
        List<EchoResultAll> resultList = new ArrayList<>(destinationAllList.size());
        for (DestinationAll dest : destinationAllList) {
            EchoResultAllBuilder echo = new EchoResultAllBuilder();
            echo.setDestination(dest.getDestination());
            try {
                InetAddress destination = InetAddress.getByName(dest.getDestination().getValue());

                if (destination.isReachable(5000)) {
                    echo.setEchoResult(EchoResultAll.EchoResult.Reachable);
                } else {
                    echo.setEchoResult(EchoResultAll.EchoResult.Unreachable);
                }
            } catch (IOException e) {
                echo.setEchoResult(EchoResultAll.EchoResult.Error);
            }
            resultList.add(echo.build());
        }

        return outputBuilder.setEchoResultAll(resultList).build();
    }

    private Future<RpcResult<PingHostsOutput>> syncImpl(PingHostsInput destination) {
        PingHostsOutput result = pingAllHost(destination.getDestinationAll());
        return RpcResultBuilder.success(result).buildFuture();
    }

    private Future<RpcResult<PingHostsOutput>> asyncImpl1(PingHostsInput destination) {
        SettableFuture<RpcResult<PingHostsOutput>> future = SettableFuture.create();
        new Thread(() -> {
            PingHostsOutput result = pingAllHost(destination.getDestinationAll());
            future.set(RpcResultBuilder.success(result).build());
        }).start();
        return future;
    }

    private Future<?> asyncImpl2(PingHostsInput destination) {
        return executorService.submit(pingAllCallable(destination));
    }
    private Callable<RpcResult<PingHostsOutput>> pingAllCallable(PingHostsInput destination) {
        return () -> (RpcResultBuilder.success(pingAllHost(destination.getDestinationAll())).build());
    }
    private Future<RpcResult<PingHostsOutput>> asyncImpl3(PingHostsInput destination) {
        return CompletableFuture.supplyAsync(()->{
            PingHostsOutput result = pingAllHost(destination.getDestinationAll());
                    return RpcResultBuilder.success(result).build();
                });
    }


    /**
     * Method called when the blueprint container is created.
     */
    public void init() {
        LOG.info("PingServiceImpl Session Initiated");
    }

    /**
     * Method called when the blueprint container is destroyed.
     */
    public void close() {
        LOG.info("PingServiceImpl Closed");
    }

    @Override
    public Future<RpcResult<PingHostsOutput>> pingHosts(PingHostsInput input) {
        if (input.isAsync()) {
            return asyncImpl1(input);
        } else {
            return syncImpl(input);
        }
    }
}
