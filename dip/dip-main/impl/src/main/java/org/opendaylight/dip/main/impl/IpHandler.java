/*
 * Copyright © 2017 NDSC and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.dip.main.impl;

import org.opendaylight.controller.liblldp.BitBufferHelper;
import org.opendaylight.controller.liblldp.BufferException;
import org.opendaylight.controller.liblldp.NetUtils;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.dip.main.impl.flow.FlowWriterService;
import org.opendaylight.dip.main.impl.flow.PacketOutService;
import org.opendaylight.dip.main.impl.session.SessionRecord;
import org.opendaylight.dip.main.impl.topology.DynamicTopo;
import org.opendaylight.dip.main.impl.util.InstanceIdentifierUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.*;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.output.action._case.OutputActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.dl.dst.action._case.SetDlDstActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.nw.dst.action._case.SetNwDstActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.nw.src.action._case.SetNwSrcActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.address.address.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.EtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.IpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.TcpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.UdpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.PacketChain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.packet.chain.packet.RawPacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.KnownEtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.ethernet.packet.received.packet.chain.packet.EthernetPacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv4.rev140528.Ipv4PacketListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv4.rev140528.Ipv4PacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv4.rev140528.KnownIpProtocols;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv4.rev140528.ipv4.packet.received.packet.chain.packet.Ipv4Packet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dipapp.rev150105.sessions.FlowSession;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dipapp.rev150105.sessions.FlowSessionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dipapp.rev150105.sessions.FlowSessionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dipapp.rev150105.sessions.flow.session.Path;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dipapp.rev150105.sessions.flow.session.PathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.rpc.judge.real.ip.rev170528.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.rpc.judge.virtual.ip.rev170528.*;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * @author chenyang
 */
public class IpHandler implements Ipv4PacketListener {
    private static final Logger LOG = LoggerFactory.getLogger(IpHandler.class);
    private final DataBroker dataBroker;
    private FlowWriterService flowWriterService;
    private PacketOutService packetOutService;
    private SessionRecord sessionRecord;
    private JudgeRealIpService realJudgeIp;
    private JudgeVirtualIpService virtualIPJudge;
    private final String FLOW_ID_PREFIX = "Dip-";
    private final String Mask = "/32";

    IpHandler(DataBroker dataBroker, FlowWriterService flowWriterService,
              PacketOutService packetOutService, SessionRecord sessionRecord,
              JudgeRealIpService realJudgeIp, JudgeVirtualIpService virtualIPJudge) {
        this.dataBroker = dataBroker;
        this.flowWriterService = flowWriterService;
        this.packetOutService = packetOutService;
        this.sessionRecord = sessionRecord;
        this.realJudgeIp = realJudgeIp;
        this.virtualIPJudge = virtualIPJudge;
    }

    @Override
    public void onIpv4PacketReceived(Ipv4PacketReceived packetReceived) {
        if (packetReceived == null || packetReceived.getPacketChain() == null) {
            return;
        }
        LOG.info("DIP: get an ipv4 packet");
        RawPacket rawPacket = null;
        EthernetPacket ethernetPacket = null;
        Ipv4Packet ipv4Packet = null;
        for (PacketChain packetChain : packetReceived.getPacketChain()) {
            if (packetChain.getPacket() instanceof RawPacket) {
                rawPacket = (RawPacket) packetChain.getPacket();
            } else if (packetChain.getPacket() instanceof EthernetPacket) {
                ethernetPacket = (EthernetPacket) packetChain.getPacket();
            } else if (packetChain.getPacket() instanceof Ipv4Packet) {
                ipv4Packet = (Ipv4Packet) packetChain.getPacket();
            }
        }
        if (rawPacket == null || ethernetPacket == null || ipv4Packet == null) {
            return;
        }

        if(ipv4Packet.getProtocol().equals(KnownIpProtocols.Tcp) || ipv4Packet.getProtocol().equals(KnownIpProtocols.Udp)){
            LOG.info("DIP: it is an {} packet",ipv4Packet.getProtocol().getName());
            //get the src port and dst port
            int bitOffset = ipv4Packet.getPayloadOffset() * NetUtils.NumBitsInAByte;
            byte[] data = packetReceived.getPayload();
            int srcPort = 0,dstPort = 0;
            try {
                srcPort = BitBufferHelper.getInt(BitBufferHelper.getBits(data, bitOffset, 16));
                dstPort = BitBufferHelper.getInt(BitBufferHelper.getBits(data, bitOffset + 16, 16));
            } catch (BufferException e) {
                e.printStackTrace();
            }
            if( srcPort ==53 || dstPort == 53 ){
                return;
            }
            Ipv4Address dstAddress = ipv4Packet.getDestinationIpv4();
            Ipv4Address srcAddress = ipv4Packet.getSourceIpv4();
            NodeConnectorRef ingress = rawPacket.getIngress();
            KnownIpProtocols protocol = ipv4Packet.getProtocol();

            VirtualJudgeIpOutput srcVirtualJudge = null;
            RealJudgeIpOutput dstRealJudge = null;
            try {
                srcVirtualJudge = isVirtualIP(srcAddress);
                dstRealJudge = isRealIp(dstAddress);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }

            if(srcVirtualJudge != null ){
                if(dstRealJudge != null){

                    Ipv4Address srcRAddress = srcVirtualJudge.getRealIp().getIpv4Address();
                    Ipv4Address dstVAddress = dstRealJudge.getVirtualIp().getIpv4Address();

                    LOG.info("DIP: it is an indirect packet");

                    FlowSessionBuilder sessionBuilder = new FlowSessionBuilder()
                            .setDstIp(new IpAddress(dstAddress))
                            .setDestVIp(new IpAddress(dstVAddress))
                            .setSrcIp(new IpAddress(srcRAddress))
                            .setSrcVIp(new IpAddress(srcAddress))
                            .setSrcPort(srcPort)
                            .setDstPort(dstPort)
                            .setProto(protocol)
                            .setTtl((long) 1800);
                    int key = getHash(sessionBuilder);
                    FlowSession session = sessionRecord.readSession(new FlowSessionKey((long)key));
                    LOG.info("DIP: session is : {}",session);
                    if (session != null) {
                        LOG.info("DIP: it is a right packet");

                        //set the flowID prefix
                        String flowIdPre = FLOW_ID_PREFIX + Integer.toString(key) + ':';

                        NodeConnectorRef dstNodeConnector = null;
                        Boolean isInSameSwitch = false;

                        Uri srcNode = ingress.getValue().firstKeyOf(Node.class,NodeKey.class).getId();
                        Uri srcNC = ingress.getValue().firstKeyOf(NodeConnector.class, NodeConnectorKey.class).getId();
                        Uri dstNC = dstRealJudge.getStorePath();

                        //如果都是在同一台交换机上
                        if (dstNC.getValue().contains(srcNode.getValue())){
                            dstNodeConnector = new NodeConnectorRef(
                                    InstanceIdentifierUtils.createNodeConnectorIdentifier(srcNode.getValue(),dstNC.getValue()));
                            isInSameSwitch = true;
                        }else {
                            List<Path> path = session.getPath();
                            for(Path p : path){

                                if(p.getSource().getSourceNode().getValue().equals(srcNode.getValue())
                                        && !(p.getSource().getSourceTp().getValue().equals(srcNC.getValue()))){
                                    dstNodeConnector = new NodeConnectorRef(
                                            InstanceIdentifierUtils.createNodeConnectorIdentifier(p.getSource().getSourceNode().getValue(),
                                                    p.getSource().getSourceTp().getValue()));
                                }
                                if(p.getDestination().getDestNode().getValue().equals(srcNode.getValue())
                                        && !(p.getDestination().getDestTp().getValue().equals(srcNC.getValue()))){
                                    dstNodeConnector = new NodeConnectorRef(
                                            InstanceIdentifierUtils.createNodeConnectorIdentifier(p.getDestination().getDestNode().getValue(),
                                                    p.getDestination().getDestTp().getValue()));
                                }
                            }
                        }

                        if(dstNodeConnector == null){
                            return;
                        }

                        Match match = getMatch(srcAddress.getValue(), dstAddress.getValue(), srcPort, dstPort, protocol);
                        Uri dstPortUri = dstNodeConnector.getValue().firstKeyOf(NodeConnector.class, NodeConnectorKey.class).getId();
                        List<Action> actions = new ArrayList<>();

                        if( isInSameSwitch ){
                            actions.add(changeDstMacAction(dstRealJudge.getMac()));
                        }
                        actions.add(outAction(dstPortUri));

                        packetOutService.dispatchPacket(packetReceived.getPayload(),ingress,dstNodeConnector,actions);
                        flowWriterService.addIpToIpFlow(dstNodeConnector, flowIdPre, match, actions);
                        LOG.info("DIP: send change flow");

                        //set the comeback packet flow
                        setComebackFlow(ingress,session,flowIdPre,isInSameSwitch,null);
                        LOG.info("DIP: send back flow");
                    }
                }
            }else {

                RealJudgeIpOutput srcRealJudge = null;
                VirtualJudgeIpOutput dstVirtualJudge = null;
                try {
                    srcRealJudge = isRealIp(srcAddress);
                    dstVirtualJudge = isVirtualIP(dstAddress);
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }

                if( srcRealJudge != null ){
                    if( dstVirtualJudge != null ){

                        Ipv4Address srcVAddress = srcRealJudge.getVirtualIp().getIpv4Address();
                        Ipv4Address dstRAddress = dstVirtualJudge.getRealIp().getIpv4Address();

                        Uri srcNode = ingress.getValue().firstKeyOf(Node.class,NodeKey.class).getId();
                        Uri srcNC = ingress.getValue().firstKeyOf(NodeConnector.class, NodeConnectorKey.class).getId();
                        Uri dstNC = dstVirtualJudge.getStorePath();

                        if( srcNC.getValue().equals(srcRealJudge.getStorePath().getValue())) {
                              LOG.info("DIP: it is a direct packet");

                              NodeConnectorRef dstNodeConnector;
                              List<Path> path =new ArrayList<>();
                              Boolean isInSameSwitch = false;

                              //如果都是在同一台交换机上
                              if (dstNC.getValue().contains(srcNode.getValue())){
                                  dstNodeConnector = new NodeConnectorRef(
                                          InstanceIdentifierUtils.createNodeConnectorIdentifier(srcNode.getValue(),dstNC.getValue()));
                                  isInSameSwitch = true;
                              }else {
                                  //源，目的不在同一台交换机上
                                  Uri dstNode = new Uri(dstNC.getValue().substring(0, dstNC.getValue().lastIndexOf(":")));
                                  DynamicTopo dynamicTopo = new DynamicTopo(dataBroker);
                                  List<Link> links = dynamicTopo.buildPath(srcNode, dstNode);
                                  if (links == null) {
                                      return;
                                  }

                                  if (links.get(0).getSource().getSourceNode().getValue().equals(srcNode.getValue())) {
                                      dstNodeConnector = InstanceIdentifierUtils.getSrcNodeConnectorRef(links.get(0));
                                  } else if (links.get(0).getDestination().getDestNode().getValue().equals(srcNode.getValue())) {
                                      dstNodeConnector = InstanceIdentifierUtils.getDstNodeConnectorRef(links.get(0));
                                  } else {
                                      return;
                                  }

                                  for (Link link : links) {
                                      Path p = new PathBuilder().setDestination(link.getDestination())
                                              .setSource(link.getSource())
                                              .setLinkId(link.getLinkId())
                                              .build();
                                      path.add(p);
                                  }
                              }

                              FlowSessionBuilder sessionBuilder = new FlowSessionBuilder()
                                      .setDstIp(new IpAddress(dstRAddress))
                                      .setDestVIp(new IpAddress(dstAddress))
                                      .setSrcIp(new IpAddress(srcAddress))
                                      .setSrcVIp(new IpAddress(srcVAddress))
                                      .setSrcPort(srcPort)
                                      .setDstPort(dstPort)
                                      .setProto(protocol)
                                      .setTtl((long) 1800)
                                      .setPath(path);
                              int key = getHash(sessionBuilder);
                              FlowSession session = sessionBuilder.setId((long)key).build();
                              sessionRecord.changeSession(session, SessionRecord.ChangeType.Add);
                              LOG.info("DIP: session is : {}",session);
                              //set the flowID prefix
                              String flowIdPre = FLOW_ID_PREFIX + Integer.toString(key) + ':';

                              Uri dstPortUri = dstNodeConnector.getValue().firstKeyOf(NodeConnector.class, NodeConnectorKey.class).getId();

                              Match match = getMatch(srcAddress.getValue(), dstAddress.getValue(), srcPort, dstPort, protocol);
                              List<Action> actions = new ArrayList<>();
                              if( isInSameSwitch){
                                  actions.add(changeDstMacAction(dstVirtualJudge.getMac()));
                              }
                              actions.add(changeSrcAction(srcVAddress.getValue()));
                              actions.add(changeDstAction(dstRAddress.getValue()));
                              actions.add(outAction(dstPortUri));

                              LOG.info("DIP: send change flow");
                              packetOutService.dispatchPacket(packetReceived.getPayload(),ingress,dstNodeConnector,actions);
                              flowWriterService.addIpToIpFlow(dstNodeConnector, flowIdPre, match, actions);

                              LOG.info("DIP: send back flow");
                              //set the comeback packet flow
                              setComebackFlow(ingress,session,flowIdPre,isInSameSwitch,new MacAddress(srcRealJudge.getMac()));
                        }
                    }
                }
            }
        }
    }

    private void setComebackFlow(NodeConnectorRef ingress, FlowSession session,
                                                     String flowIdPre, Boolean isDirect, MacAddress changeMac ){
        Match backMatch;
        Uri srcPortUri = ingress.getValue().firstKeyOf(NodeConnector.class, NodeConnectorKey.class).getId();
        List<Action> backActions = new ArrayList<>();

        if ( isDirect ) {
            backMatch = getMatch(session.getDstIp().getIpv4Address().getValue(),
                                                 session.getSrcVIp().getIpv4Address().getValue(),
                                                 session.getDstPort(), session.getSrcPort(), session.getProto());
            backActions.add(changeSrcAction(session.getDestVIp().getIpv4Address().getValue()));
            backActions.add(changeDstAction(session.getSrcIp().getIpv4Address().getValue()));

        } else {
            backMatch = getMatch(session.getDestVIp().getIpv4Address().getValue(),
                                                 session.getSrcIp().getIpv4Address().getValue(),
                                                 session.getDstPort(), session.getSrcPort(), session.getProto());
        }

        if (changeMac != null){
            backActions.add(changeDstMacAction(changeMac));
        }
        backActions.add(outAction(srcPortUri));

        flowWriterService.addIpToIpFlow(ingress, flowIdPre, backMatch, backActions);
    }

    /**
     * if ipAddress is real ip ,return it's virtual ip
     *
     */
    private RealJudgeIpOutput isRealIp(Ipv4Address ipv4Address) throws ExecutionException, InterruptedException {

        IpAddress ipAddress=new IpAddress(ipv4Address);
        //String exampleIP="50.0.0.1";
        //IpAddress ipAddress=new IpAddress(new Ipv4Address(exampleIP));
        RealJudgeIpInputBuilder realJudgeIpInputBuilder = new RealJudgeIpInputBuilder();
        realJudgeIpInputBuilder.setRealIp(ipAddress);
        RealJudgeIpOutput realJudgeIpOutput = realJudgeIp.realJudgeIp(realJudgeIpInputBuilder.build()).get().getResult();

        if(realJudgeIpOutput.isSuccess()){
            LOG.info("This IP {} is in real IP Pool, and its virtual IP is {}", ipv4Address.getValue(), realJudgeIpOutput.getVirtualIp().getIpv4Address().getValue());
            return realJudgeIpOutput;
        }else {
            LOG.info("This IP {} is not in real IP Pool", ipv4Address.getValue());
            return null;
        }
    }

    /**
     *
     *if ipAddress is virtual ip ,return it's real ip
     */
    private VirtualJudgeIpOutput isVirtualIP(Ipv4Address ipv4Address) throws ExecutionException, InterruptedException {

        IpAddress ipAddress=new IpAddress(ipv4Address);
        VirtualJudgeIpInputBuilder virtualJudgeIpInputBuilder = new VirtualJudgeIpInputBuilder();
        virtualJudgeIpInputBuilder.setVirtualIp(ipAddress);
        VirtualJudgeIpOutput virtualJudgeIpOutput = virtualIPJudge.virtualJudgeIp(virtualJudgeIpInputBuilder.build()).get().getResult();

        if(virtualJudgeIpOutput.isSuccess()){
            LOG.info("This IP is in virtual IP Pool, and its real IP is {}", virtualJudgeIpOutput.getRealIp().getIpv4Address().getValue());
            return virtualJudgeIpOutput;
        }else {
            LOG.info("This IP is not in virtual IP Pool");
            return null;
        }
    }

    private static int getHash(FlowSessionBuilder session) {
        String str  =  session.getDstIp().getIpv4Address().getValue()+';'+
                session.getDestVIp().getIpv4Address().getValue()+';'+
                session.getSrcIp().getIpv4Address().getValue()+';'+
                session.getSrcVIp().getIpv4Address().getValue()+';'+
                session.getSrcPort().toString()+';'+
                session.getDstPort().toString()+';'+
                session.getProto().getName()+';'+
                session.getTtl().toString();
        LOG.info("DIP:Session Key :{}",str);
        final int p = 16777619;
        int hash = (int)2166136261L;
        for (int i = 0; i < str.length(); i++) {
            hash = (hash ^ str.charAt(i)) * p;
        }
        hash += hash << 13;
        hash ^= hash >> 7;
        hash += hash << 3;
        hash ^= hash >> 17;
        hash += hash << 5;
        // 如果算出来的值为负数则取其绝对值
        if (hash < 0)
            hash = Math.abs(hash);
        LOG.info("DIP:Session Key is :{}",hash);
        return hash;
    }

    private Action changeSrcAction(String ip){
        return new ActionBuilder()
                .setOrder(0)
                .setKey(new ActionKey(0))
                .setAction(new SetNwSrcActionCaseBuilder()
                        .setSetNwSrcAction(new SetNwSrcActionBuilder()
                                .setAddress(new Ipv4Builder().setIpv4Address(new Ipv4Prefix(ip+Mask)).build())
                                .build())
                        .build())
                .build();
    }

    private Action changeDstAction(String ip){
        return new ActionBuilder()
                .setOrder(0)
                .setKey(new ActionKey(0))
                .setAction(new SetNwDstActionCaseBuilder()
                        .setSetNwDstAction(new SetNwDstActionBuilder()
                                .setAddress(new Ipv4Builder().setIpv4Address(new Ipv4Prefix(ip+Mask)).build())
                                .build())
                        .build())
                .build();
    }

    private  Action changeDstMacAction(MacAddress mac){
        return new ActionBuilder()
                .setOrder(0)
                .setKey(new ActionKey(0))
                .setAction(new SetDlDstActionCaseBuilder()
                        .setSetDlDstAction(new SetDlDstActionBuilder()
                                .setAddress(new MacAddress(mac))
                                .build())
                        .build())
                .build();
    }


    private Action outAction(Uri port){
        return new ActionBuilder()
                .setOrder(0)
                .setAction(new OutputActionCaseBuilder()
                        .setOutputAction(new OutputActionBuilder()
                                .setMaxLength(0xffff)
                                .setOutputNodeConnector(port)
                                .build())
                        .build())
                .build();
    }

    private Match getMatch (String srcAddress,String dstAddress,int srcPort,int dstPort,KnownIpProtocols protocol){
        MatchBuilder matchBuilder = new MatchBuilder();

        EthernetMatchBuilder ethernetMatchBuilder = new EthernetMatchBuilder()
                .setEthernetType(new EthernetTypeBuilder()
                        .setType(new EtherType(Long.valueOf(KnownEtherType.Ipv4.getIntValue()))).build());

        // create a match that has ip to ip ipv4 match
        Ipv4MatchBuilder ipv4MatchBuilder = new Ipv4MatchBuilder()
                .setIpv4Source(new Ipv4Prefix(srcAddress+Mask))
                .setIpv4Destination(new Ipv4Prefix(dstAddress+Mask));

        IpMatchBuilder ipMatchBuilder = new IpMatchBuilder()
                .setIpProtocol((short) protocol.getIntValue());

        if(protocol.equals(KnownIpProtocols.Udp)){

            UdpMatchBuilder udpMatchBuilder = new UdpMatchBuilder()
                    .setUdpSourcePort(new PortNumber(srcPort))
                    .setUdpDestinationPort(new PortNumber(dstPort));

            matchBuilder.setEthernetMatch(ethernetMatchBuilder.build())
                    .setLayer3Match(ipv4MatchBuilder.build())
                    .setIpMatch(ipMatchBuilder.build())
                    .setLayer4Match(udpMatchBuilder.build());

        }else if(protocol.equals(KnownIpProtocols.Tcp)){

            TcpMatchBuilder tcpMatchBuilder = new TcpMatchBuilder()
                    .setTcpSourcePort(new PortNumber(srcPort))
                    .setTcpDestinationPort(new PortNumber(dstPort));

            matchBuilder.setEthernetMatch(ethernetMatchBuilder.build())
                    .setLayer3Match(ipv4MatchBuilder.build())
                    .setIpMatch(ipMatchBuilder.build())
                    .setLayer4Match(tcpMatchBuilder.build());
        }else {
            return null;
        }

        return matchBuilder.build();
    }

}
