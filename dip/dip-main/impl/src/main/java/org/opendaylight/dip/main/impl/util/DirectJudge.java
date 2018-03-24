/*
 * Copyright © 2017 NDSC and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.dip.main.impl.util;

import com.google.common.base.Optional;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dipapp.rev150105.VAddressCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dipapp.rev150105.v.address.node.connector.VipAddressEntities;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;

public class DirectJudge {
    private final DataBroker dataBroker;
    private static final Logger LOG = LoggerFactory.getLogger(DirectJudge.class);
    private MacAddress macAddress;
    public Boolean isDirect;


    public DirectJudge(final DataBroker dataBroker) {
        this.dataBroker = dataBroker;
        isDirect = false;
        macAddress = null;
    }


    public void isDirectConnect (Ipv4Address address, NodeConnectorRef nodeConnectorRef ){
        InstanceIdentifier<VAddressCapableNodeConnector> identifier = nodeConnectorRef.getValue()
                .firstIdentifierOf(NodeConnector.class).augmentation(VAddressCapableNodeConnector.class);
        VAddressCapableNodeConnector vAddressRecords = null;
        try (ReadOnlyTransaction rot = dataBroker.newReadOnlyTransaction()) {
            Optional<VAddressCapableNodeConnector> opData = rot.read(LogicalDatastoreType.OPERATIONAL,identifier).get();
            rot.close();
            if( opData.isPresent() ){
                vAddressRecords = opData.get();
            }
        }catch (InterruptedException | ExecutionException ex) {
            LOG.warn(ex.getLocalizedMessage());
            return;
        }
        if(vAddressRecords != null){
            for (VipAddressEntities record : vAddressRecords.getVipAddressEntities()){
                if(address.equals(record.getRip().getIpv4Address())){
                    isDirect = true;
                    macAddress = record.getMac();
                }
            }
        }
    }



    public MacAddress getMacAddress(){
        return macAddress;
    }

}
