/*
 * Copyright © 2017 NDSC and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.dip.dns.core;

import com.google.common.collect.ImmutableList;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.openflowplugin.api.OFConstants;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetNwSrcActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.output.action._case.OutputActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.nw.src.action._case.SetNwSrcActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.address.address.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.FlowTableRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowCookie;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowModFlags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.OutputPortValues;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.EtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.IpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.UdpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.KnownEtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv4.rev140528.KnownIpProtocols;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.concurrent.Executors.*;

/**
 * Adds a flow, which sends all Dns packets to the controller, on all switches.
 * Registers as ODL Inventory listener so that it can add flows once a new node i.e. switch is added
 */
public class InitialFlowWriter implements DataChangeListener {
    private static final Logger LOG = LoggerFactory.getLogger(InitialFlowWriter.class);

    private final ExecutorService initialFlowExecutor = newCachedThreadPool();
    private final SalFlowService salFlowService;
    private final String FLOW_ID_PREFIX = "DNS-";
    private final Integer DNS_PORT = 53;
    private final String Mask = "/32";
    private String dnsAddress;
    private String dnsForwardAddress;
    private short flowTableId;
    private int flowPriority;
    private int flowIdleTimeout;
    private int flowHardTimeout;
    private int forwardPriority;
    private int forwardIdleTimeout;
    private int forwardHardTimeout;
    boolean isForward;

    private AtomicLong flowIdInc = new AtomicLong();
    private AtomicLong flowCookieInc = new AtomicLong(0x1a00000000000000L);


    InitialFlowWriter(SalFlowService salFlowService) {
        this.salFlowService = salFlowService;
    }

    void setDnsAddress(String dnsAddress) {
        this.dnsAddress = dnsAddress;
    }

    void setDnsForwardAddress(String dnsForwardAddress) {
        this.dnsForwardAddress = dnsForwardAddress;
    }

    void setFlowTableId(short flowTableId) {
        this.flowTableId = flowTableId;
    }

    void setFlowPriority(int flowPriority) {
        this.flowPriority = flowPriority;
    }

    void setFlowIdleTimeout(int flowIdleTimeout) {
        this.flowIdleTimeout = flowIdleTimeout;
    }

    void setFlowHardTimeout(int flowHardTimeout) {
        this.flowHardTimeout = flowHardTimeout;
    }

    void setIsForward(boolean isForward) {
        this.isForward = isForward;
    }

    void setForwardPriority(int forwardPriority) {
        this.forwardPriority = forwardPriority;
    }

    void setForwardIdleTimeout(int forwardIdleTimeout) {
        this.forwardIdleTimeout = forwardIdleTimeout;
    }

    void setForwardHardTimeout(int forwardHardTimeout) {
        this.forwardHardTimeout = forwardHardTimeout;
    }

    ListenerRegistration<DataChangeListener> registerAsDataChangeListener(DataBroker dataBroker) {
        InstanceIdentifier<Node> nodeInstanceIdentifier = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class).build();
        return dataBroker.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL, nodeInstanceIdentifier, this, AsyncDataBroker.DataChangeScope.BASE);
    }

    @Override
    public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> instanceIdentifierDataObjectAsyncDataChangeEvent) {
        Map<InstanceIdentifier<?>, DataObject> createdData = instanceIdentifierDataObjectAsyncDataChangeEvent.getCreatedData();
        if(createdData != null && !createdData.isEmpty()) {
            Set<InstanceIdentifier<?>> nodeIds = createdData.keySet();
            if(!nodeIds.isEmpty()) {
                initialFlowExecutor.submit(new InitialFlowWriterProcessor(nodeIds));
            }
        }
    }

    /**
     * A private class to process the node updated event in separate thread. Allows to release the
     * thread that invoked the data node updated event. Avoids any thread lock it may cause.
     */
    private class InitialFlowWriterProcessor implements Runnable {
        Set<InstanceIdentifier<?>> nodeIds = null;

        InitialFlowWriterProcessor(Set<InstanceIdentifier<?>> nodeIds) {
            this.nodeIds = nodeIds;
        }

        @Override
        public void run() {

            if(nodeIds == null) {
                return;
            }

            for(InstanceIdentifier<?> nodeId : nodeIds) {
                if(Node.class.isAssignableFrom(nodeId.getTargetType())) {
                    InstanceIdentifier<Node> invNodeId = (InstanceIdentifier<Node>)nodeId;
                    if(invNodeId.firstKeyOf(Node.class,NodeKey.class).getId().getValue().contains("openflow:")) {
                        addInitialFlows(invNodeId);
                    }
                }
            }

        }

        /**
         * Adds a flow, which sends all Dns packets to the controller, to the specified node.
         * @param nodeId The node to write the flow on.
         */
        void addInitialFlows(InstanceIdentifier<Node> nodeId) {

            InstanceIdentifier<Table> tableId = getTableInstanceId(nodeId);
            InstanceIdentifier<Flow> flowId = getFlowInstanceId(tableId);
            Flow initialFlow = createDnsToControllerFlow(flowTableId, flowPriority);

            LOG.trace("DNS: Adding flow to node {}",nodeId.firstKeyOf(Node.class, NodeKey.class).getId().getValue());
            LOG.info("DNS: initial flows : {} ", initialFlow);

            final AddFlowInputBuilder builder = new AddFlowInputBuilder(initialFlow);
            builder.setNode(new NodeRef(nodeId));
            builder.setFlowRef(new FlowRef(flowId));
            builder.setFlowTable(new FlowTableRef(tableId));
            builder.setTransactionUri(new Uri(initialFlow.getId().getValue()));

            //add DnsToController flow
            salFlowService.addFlow(builder.build());

            if(isForward){
                Flow changeFlow = createDnsAddressChangeFlow(flowTableId, forwardPriority);
                final AddFlowInputBuilder changeBuilder = new AddFlowInputBuilder(changeFlow);
                changeBuilder.setNode(new NodeRef(nodeId));
                changeBuilder.setFlowRef(new FlowRef(flowId));
                changeBuilder.setFlowTable(new FlowTableRef(tableId));
                changeBuilder.setTransactionUri(new Uri(changeFlow.getId().getValue()));
                salFlowService.addFlow(changeBuilder.build());
            }
            LOG.info("Bingo: Added initial flows : {} ", initialFlow);
        }

        private InstanceIdentifier<Table> getTableInstanceId(InstanceIdentifier<Node> nodeId) {
            // get flow table key
            TableKey flowTableKey = new TableKey(flowTableId);

            return nodeId.builder()
                    .augmentation(FlowCapableNode.class)
                    .child(Table.class, flowTableKey)
                    .build();
        }

        private InstanceIdentifier<Flow> getFlowInstanceId(InstanceIdentifier<Table> tableId) {
            // generate unique flow key
            FlowId flowId = new FlowId(FLOW_ID_PREFIX+String.valueOf(flowIdInc.getAndIncrement()));
            FlowKey flowKey = new FlowKey(flowId);
            return tableId.child(Flow.class, flowKey);
        }

        private Flow createDnsToControllerFlow(Short tableId, int priority) {

            // start building flow
            FlowBuilder dnsFlow = new FlowBuilder().setTableId(tableId).setFlowName("dnstocntrl");
            // use its own hash code for id.
            dnsFlow.setId(new FlowId(Long.toString(dnsFlow.hashCode())));

            EthernetMatchBuilder ethernetMatchBuilder = new EthernetMatchBuilder()
                    .setEthernetType(new EthernetTypeBuilder()
                            .setType(new EtherType(Long.valueOf(KnownEtherType.Ipv4.getIntValue()))).build());

            Ipv4MatchBuilder ipv4MatchBuilder = new Ipv4MatchBuilder()
                    .setIpv4Destination(new Ipv4Prefix(dnsAddress+Mask));

            IpMatchBuilder ipMatchBuilder = new IpMatchBuilder()
                    .setIpProtocol((short) KnownIpProtocols.Udp.getIntValue());

            UdpMatchBuilder udpMatchBuilder = new UdpMatchBuilder()
                    .setUdpDestinationPort(new PortNumber(DNS_PORT));

            Match match = new MatchBuilder()
                    .setEthernetMatch(ethernetMatchBuilder.build())
                    .setLayer3Match(ipv4MatchBuilder.build())
                    .setIpMatch(ipMatchBuilder.build())
                    .setLayer4Match(udpMatchBuilder.build())
                    .build();

            List<Action> actions = new ArrayList<>();
            actions.add(getSendToControllerAction());

            // Create an Apply Action
            ApplyActions applyActions = new ApplyActionsBuilder()
                    .setAction(ImmutableList.copyOf(actions))
                    .build();

            // Wrap our Apply Action in an Instruction
            Instruction applyActionsInstruction = new InstructionBuilder()
                    .setOrder(0)
                    .setInstruction(new ApplyActionsCaseBuilder()
                            .setApplyActions(applyActions)
                            .build())
                    .build();

            // Put our Instruction in a list of Instructions
            dnsFlow.setMatch(match)
                    .setInstructions(new InstructionsBuilder()
                            .setInstruction(ImmutableList.of(applyActionsInstruction))
                            .build())
                    .setPriority(priority)
                    .setBufferId(OFConstants.OFP_NO_BUFFER)
                    .setHardTimeout(flowHardTimeout)
                    .setIdleTimeout(flowIdleTimeout)
                    .setCookie(new FlowCookie(BigInteger.valueOf(flowCookieInc.getAndIncrement())))
                    .setFlags(new FlowModFlags(true ,
                            false,
                            false,
                            false,
                            false));
            return dnsFlow.build();
        }

        private Action getSendToControllerAction() {
            return new ActionBuilder().setOrder(0)
                    .setKey(new ActionKey(0))
                    .setAction(new OutputActionCaseBuilder()
                            .setOutputAction(new OutputActionBuilder()
                                    .setMaxLength(0xffff)
                                    .setOutputNodeConnector(new Uri(OutputPortValues.CONTROLLER.toString()))
                                    .build())
                            .build())
                    .build();
        }

        private Flow createDnsAddressChangeFlow(Short tableId, int priority) {

            // start building flow
            FlowBuilder dnsFlow = new FlowBuilder().setTableId(tableId).setFlowName("dnsforwardresponse");
            // use its own hash code for id.
            dnsFlow.setId(new FlowId(Long.toString(dnsFlow.hashCode())));

            EthernetMatchBuilder ethernetMatchBuilder = new EthernetMatchBuilder()
                    .setEthernetType(new EthernetTypeBuilder()
                            .setType(new EtherType(Long.valueOf(KnownEtherType.Ipv4.getIntValue()))).build());

            Ipv4MatchBuilder ipv4MatchBuilder = new Ipv4MatchBuilder()
                    .setIpv4Source(new Ipv4Prefix(dnsForwardAddress+Mask));

            IpMatchBuilder ipMatchBuilder = new IpMatchBuilder()
                    .setIpProtocol((short) KnownIpProtocols.Udp.getIntValue());

            UdpMatchBuilder udpMatchBuilder = new UdpMatchBuilder()
                    .setUdpSourcePort(new PortNumber(DNS_PORT));

            Match match = new MatchBuilder()
                    .setEthernetMatch(ethernetMatchBuilder.build())
                    .setLayer3Match(ipv4MatchBuilder.build())
                    .setIpMatch(ipMatchBuilder.build())
                    .setLayer4Match(udpMatchBuilder.build())
                    .build();

            List<Action> actions = getAddressChangeAction();

            // Create an Apply Action
            ApplyActions applyActions = new ApplyActionsBuilder()
                    .setAction(ImmutableList.copyOf(actions))
                    .build();

            // Wrap our Apply Action in an Instruction
            Instruction applyActionsInstruction = new InstructionBuilder()
                    .setOrder(0)
                    .setInstruction(new ApplyActionsCaseBuilder()
                            .setApplyActions(applyActions)
                            .build())
                    .build();

            // Put our Instruction in a list of Instructions
            dnsFlow.setMatch(match)
                    .setInstructions(new InstructionsBuilder()
                            .setInstruction(ImmutableList.of(applyActionsInstruction))
                            .build())
                    .setPriority(priority)
                    .setBufferId(OFConstants.OFP_NO_BUFFER)
                    .setHardTimeout(forwardHardTimeout)
                    .setIdleTimeout(forwardIdleTimeout)
                    .setCookie(new FlowCookie(BigInteger.valueOf(flowCookieInc.getAndIncrement())))
                    .setFlags(new FlowModFlags(true,
                            false,
                            false,
                            false,
                            false));
            return dnsFlow.build();
        }

        private List<Action> getAddressChangeAction() {
            List<Action> actions = new ArrayList<>();
            Action ipChange = new ActionBuilder()
                    .setOrder(0)
                    .setKey(new ActionKey(0))
                    .setAction(new SetNwSrcActionCaseBuilder()
                            .setSetNwSrcAction(new SetNwSrcActionBuilder()
                                    .setAddress(new Ipv4Builder().setIpv4Address(new Ipv4Prefix(dnsAddress+Mask)).build())
                                    .build())
                            .build())
                    .build();
            actions.add(ipChange);

            Action normal = new ActionBuilder()
                    .setOrder(0)
                    .setKey(new ActionKey(0))
                    .setAction(new OutputActionCaseBuilder()
                            .setOutputAction(new OutputActionBuilder()
                                    .setMaxLength(0xffff)
                                    .setOutputNodeConnector(new Uri(OutputPortValues.NORMAL.toString()))
                                    .build())
                            .build())
                    .build();
            actions.add(normal);
            return actions;
        }
    }
}
