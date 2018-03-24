/*
 * Copyright (c) 2015 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.dhcp;

import com.google.common.base.Optional;
import org.opendaylight.controller.md.sal.binding.api.*;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dipapp.v.address.up.config.rev150105.VAddressUpConfig;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.concurrent.ExecutionException;

public class DhcpVupConfig implements DataTreeChangeListener<VAddressUpConfig> {


    private static final Logger LOG = LoggerFactory.getLogger(DhcpVupConfig.class);
    private final DataBroker dataBroker;
    private final InstanceIdentifier<VAddressUpConfig> upid;
    private final ListenerRegistration listenerReg;
    private boolean vipup=true;


    /**
    * @param db - dataBroker reference
    */
    DhcpVupConfig(final DataBroker db) {
        dataBroker = db;
        upid = InstanceIdentifier.create(VAddressUpConfig.class);
        listenerReg = dataBroker.registerDataTreeChangeListener(
                new DataTreeIdentifier<VAddressUpConfig>(LogicalDatastoreType.CONFIGURATION,upid),this);
    }

    @Override
    public void onDataTreeChanged(@Nonnull Collection<DataTreeModification<VAddressUpConfig>> changes) {
        for (final DataTreeModification<VAddressUpConfig> change : changes) {
            final DataObjectModification<VAddressUpConfig> rootNode = change.getRootNode();
            switch (rootNode.getModificationType()) {
                case SUBTREE_MODIFIED:
                    LOG.info("Virtual UP config reader:node {} modified", rootNode.getIdentifier());
                    readVupconfig();
                    break;
                case WRITE:
                    LOG.info("Virtual UP config reader:node {} created", rootNode.getIdentifier(), rootNode.getDataAfter());
                    readVupconfig();
                    break;
                default:
                    LOG.error("Virtual UP config ERROR:Data changed,but there are some errors");
            }
        }
    }

    private void readVupconfig(){

        synchronized (this) {
            // Read Inventory
            VAddressUpConfig vAddressUpConfig = null;
            ReadOnlyTransaction rt = dataBroker.newReadOnlyTransaction();
            try {
                Optional<VAddressUpConfig> vAddressUpConfigOptional =  rt.read(LogicalDatastoreType.CONFIGURATION, upid).get();
                if (vAddressUpConfigOptional.isPresent()) {
                    vAddressUpConfig = vAddressUpConfigOptional.get();
                }
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Failed to read Virtual up config information.");
                rt.close();
                throw new RuntimeException("Failed to read Virtual up config information.", e);
            }
            if(vAddressUpConfig != null){
                LOG.info("Read Virtual up config information : {}", vAddressUpConfig);
                if(vAddressUpConfig.getMutationModel().getIntValue()==0){
                    vipup=false;
                }else {
                    vipup=true;
                }
            }
            rt.close();
        }

    }



    public void close() throws Exception {
        LOG.info("DHCP Virtual Config Closed");
    }

    //virtual up read module

    public boolean getVirtualIpup(){
        return this.vipup;
    }

}
