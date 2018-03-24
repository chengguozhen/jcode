/*
 * Copyright (c) 2015 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.dhcp.Virtual;

import org.opendaylight.dhcp.util.NWUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dipapp.rev150105.v.address.node.connector.VipAddressEntities;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;

import java.util.Arrays;

public class DhcpVirtualbinding {
    public static final int IP_ADDRESS_LENGTH = 4;
    public static final int MAC_ADDRESS_LENGTH = 6;

    private byte[] MAC = new byte[MAC_ADDRESS_LENGTH];
    private byte[] vIP = new byte[IP_ADDRESS_LENGTH];
    private Uri root;//virtual ip store path
    private String DOMAINNAME;
    private boolean LEASE_STATUS;
    private boolean PERMANENT_LEASE;

    private long LEASE_START_TIME_SECONDS;
    private long LEASE_DURATION_SECONDS;

    protected DhcpVirtualbinding(byte[] vip, byte[] mac) {
        this.setMACAddress(mac);
        this.setIPv4Addresss(vip);

        this.setLeaseStatus(false);
    }

    public Uri getStorePath(){return root;} //get virtual ip store path


    public byte[] getIPv4AddressBytes() {
        return vIP;
    }

    public String getIPv4AddresString() {
        return NWUtil.toStringIpAddress(vIP);
    }

    public byte[] getMACAddressBytes() {
        return MAC;
    }

    public String getMACAddressString() {
        return NWUtil.toStringMacAddress(MAC);
    }

    public void setIPv4Addresss(byte[] vip) {
        vIP = Arrays.copyOf(vip, IP_ADDRESS_LENGTH);
    }

    public void setMACAddress(byte[] mac) {
        MAC = Arrays.copyOf(mac, MAC_ADDRESS_LENGTH);
    }

    //set virtual ip store path
    public void setStorePath(Uri uri) {
        root = uri;
    }

    public void setDomainName(String domainname) {
        DOMAINNAME = domainname;
    }

    public void setMACAddress(String mac) {
        MAC = NWUtil.toByteMacAddress(mac);
    }

    public boolean isActiveLease() {
        return LEASE_STATUS;
    }

    public void setStaticIPLease(boolean staticIP) {
        PERMANENT_LEASE = staticIP;
    }

    public boolean isStaticIPLease() {
        return PERMANENT_LEASE;
    }

    public void setLeaseStatus(boolean status) {
        LEASE_STATUS = status;
    }

    public boolean isLeaseExpired() {
        long currentTime = System.currentTimeMillis();
        if ((currentTime / 1000) >= (LEASE_START_TIME_SECONDS + LEASE_DURATION_SECONDS)) {
            return true;
        } else {
            return false;
        }
    }

    protected void setLeaseStartTimeSeconds() {
        LEASE_START_TIME_SECONDS = System.currentTimeMillis() / 1000;
    }

    protected void setLeaseDurationSeconds(long time) {
        LEASE_DURATION_SECONDS = time;
    }

    protected void clearLeaseTimes() {
        LEASE_START_TIME_SECONDS = 0;
        LEASE_DURATION_SECONDS = 0;
    }

    protected boolean cancelLease() {
        this.clearLeaseTimes();
        this.setLeaseStatus(false);
        return true;
    }
}
