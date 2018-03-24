/*
 * Copyright © 2017 My Company and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.future.network.cli.impl;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.future.network.cli.api.TrafficCliCommands;

public class TrafficCliCommandsImpl implements TrafficCliCommands {

    private static final Logger LOG = LoggerFactory.getLogger(TrafficCliCommandsImpl.class);
    private final DataBroker dataBroker;

    public TrafficCliCommandsImpl(final DataBroker db) {
        this.dataBroker = db;
        LOG.info("TrafficCliCommandImpl initialized");
    }

    @Override
    public Object testCommand(Object testArgument) {
        return "This is a test implementation of test-command";
    }
}