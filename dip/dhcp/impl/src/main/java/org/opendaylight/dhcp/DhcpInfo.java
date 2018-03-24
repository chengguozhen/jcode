/*
 * Copyright (c) 2015 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.dhcp;

import java.util.List;

public class DhcpInfo  {
    private String _clientIp;
    private String _serverIp;
    private String _gatewayIp;
    private String _cidr;
    private List<String> _dnsServers;
    
    public DhcpInfo() {
        //Empty constructor
    }

    protected DhcpInfo setClientIp(String clientIp) {
        _clientIp = clientIp;
        return this;
    }

    protected DhcpInfo setCidr(String cidr) {
        _cidr = cidr;
        return this;
    }

    protected DhcpInfo setServerIp(String serverIp) {
        _serverIp = serverIp;
        return this;
    }

    protected DhcpInfo setGatewayIp(String gwIp) {
        _gatewayIp = gwIp;
        return this;
    }


    protected DhcpInfo setDnsServers(List<String> dnsServers) {
        _dnsServers = dnsServers;
        return this;
    }

//    protected DhcpInfo setDnsServersIpAddrs(List<IpAddress> dnsServers) {
//        for (IpAddress ipAddr: dnsServers) {
//            addDnsServer(ipAddr.getIpv4Address().getValue());
//        }
//        return this;
//    }

//    protected DhcpInfo addDnsServer(String dnsServerIp) {
//        if(_dnsServers == null) {
//            _dnsServers = new ArrayList<String>();
//        }
//        _dnsServers.add(dnsServerIp);
//        return this;
//    }


    protected String getClientIp() {
        return _clientIp;
    }

    protected String getCidr() {
        return _cidr;
    }

    protected String getServerIp() {
        return _serverIp ;
    }

    protected String getGatewayIp() {
        return _gatewayIp ;
    }

    protected List<String> getDnsServers() {
        return _dnsServers;
    }


}
