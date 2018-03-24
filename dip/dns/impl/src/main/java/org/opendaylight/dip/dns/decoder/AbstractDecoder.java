/*
 * Copyright © 2017 NDSC and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.dip.dns.decoder;

import org.opendaylight.controller.liblldp.BufferException;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.yangtools.yang.binding.Notification;
import java.util.concurrent.ExecutorService;
import static java.util.concurrent.Executors.newFixedThreadPool;

/**
 * @author chenyang
 */
public abstract class AbstractDecoder<ConsumedPacketNotification, ProducedPacketNotification extends Notification>
        implements AutoCloseable{

    private NotificationPublishService publishService;
    private static final int CPUS = Runtime.getRuntime().availableProcessors();
    private final ExecutorService decodeAndPublishExecutor = newFixedThreadPool(CPUS);

    AbstractDecoder(NotificationPublishService publishService){
        this.publishService = publishService;
    }

    void decodeAndPublish(final ConsumedPacketNotification consumedPacketNotification) {
        decodeAndPublishExecutor.submit(new Runnable() {
            @Override
            public void run() {
                ProducedPacketNotification packetNotification = null;
                try {
                    if (consumedPacketNotification != null && canDecode(consumedPacketNotification)) {
                        packetNotification = decode(consumedPacketNotification);
                    }
                } catch (BufferException e) {
                    e.printStackTrace();
                }
                if (packetNotification != null) {
                    try {
                        publishService.putNotification(packetNotification);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    public abstract ProducedPacketNotification decode(ConsumedPacketNotification consumedPacketNotification);

    public abstract boolean canDecode(ConsumedPacketNotification consumedPacketNotification) throws BufferException;

    @Override
    public void close() throws Exception {
        decodeAndPublishExecutor.shutdown();
    }

}
