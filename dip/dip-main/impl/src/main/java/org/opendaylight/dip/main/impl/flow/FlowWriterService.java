/*
 * Copyright © 2017 NDSC and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.dip.main.impl.flow;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.opendaylight.dip.main.impl.util.InstanceIdentifierUtils;
import org.opendaylight.openflowplugin.api.OFConstants;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.FlowTableRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowCookie;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowModFlags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author chenyang
 */
public class FlowWriterService {

    private static final Logger LOG = LoggerFactory.getLogger(FlowWriterService.class);

    private SalFlowService salFlowService;
    private short flowTableId;
    private int flowPriority;
    private int flowIdleTimeout;
    private int flowHardTimeout;

    private AtomicLong flowIdInc = new AtomicLong();
    private AtomicLong flowCookieInc = new AtomicLong(0x1a00000000000000L);

    public FlowWriterService(SalFlowService salFlowService) {
        Preconditions.checkNotNull(salFlowService, "salFlowService should not be null.");
        this.salFlowService = salFlowService;
    }

    public void setFlowTableId(short flowTableId) {
        this.flowTableId = flowTableId;
    }

    public void setFlowPriority(int flowPriority) {
        this.flowPriority = flowPriority;
    }

    public void setFlowIdleTimeout(int flowIdleTimeout) {
        this.flowIdleTimeout = flowIdleTimeout;
    }

    public void setFlowHardTimeout(int flowHardTimeout) {
        this.flowHardTimeout = flowHardTimeout;
    }


    public void addIpToIpFlow(NodeConnectorRef dstNodeConnector, String flowIdPre, Match match, List<Action> actions) {

        FlowId flowId = new FlowId(flowIdPre+String.valueOf(flowIdInc.getAndIncrement()));

        // get flow table key
        TableKey flowTableKey = new TableKey((short) flowTableId);

        // build a flow path based on node connector to program flow
        InstanceIdentifier<Flow> flowPath = InstanceIdentifierUtils
                .generateFlowInstanceIdentifier(dstNodeConnector, flowTableKey, new FlowKey(flowId));

        // start building flow
        FlowBuilder ipToIpFlow = new FlowBuilder()
                .setTableId(flowTableKey.getId())
                .setFlowName("ip2ip");

        // Create an Apply Action
        ApplyActions applyActions = new ApplyActionsBuilder().setAction(ImmutableList.copyOf(actions))
                .build();

        // Wrap our Apply Action in an Instruction
        Instruction applyActionsInstruction = new InstructionBuilder()
                .setOrder(0)
                .setInstruction(new ApplyActionsCaseBuilder()
                        .setApplyActions(applyActions)
                        .build())
                .build();

        // Put our Instruction in a list of Instructions
        ipToIpFlow.setMatch(match)
                .setId(flowId)
                .setInstructions(new InstructionsBuilder()
                        .setInstruction(ImmutableList.of(applyActionsInstruction))
                        .build())
                .setPriority(flowPriority)
                .setBufferId(OFConstants.OFP_NO_BUFFER)
                .setHardTimeout(flowHardTimeout)
                .setIdleTimeout(flowIdleTimeout)
                .setCookie(new FlowCookie(BigInteger.valueOf(flowCookieInc.getAndIncrement())))
                .setFlags(new FlowModFlags(false, false, false, false, false));

        // commit the flow in config data
        writeFlowToConfigData(flowPath, ipToIpFlow.build());
    }

    /**
     * Starts and commits data change transaction which modifies provided flow
     * path with supplied body.
     */
    private Future<RpcResult<AddFlowOutput>> writeFlowToConfigData(InstanceIdentifier<Flow> flowPath, Flow flow) {
        final InstanceIdentifier<Table> tableInstanceId = flowPath.<Table>firstIdentifierOf(Table.class);
        final InstanceIdentifier<Node> nodeInstanceId = flowPath.<Node>firstIdentifierOf(Node.class);
        final AddFlowInputBuilder builder = new AddFlowInputBuilder(flow);
        builder.setNode(new NodeRef(nodeInstanceId));
        builder.setFlowRef(new FlowRef(flowPath));
        builder.setFlowTable(new FlowTableRef(tableInstanceId));
        builder.setTransactionUri(new Uri(flow.getId().getValue()));
        return salFlowService.addFlow(builder.build());
    }

}
