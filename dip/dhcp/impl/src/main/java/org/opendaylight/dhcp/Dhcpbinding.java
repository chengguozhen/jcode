/*
 * Copyright (c) 2015 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.dhcp;

import java.util.Arrays;
import org.opendaylight.dhcp.util.NWUtil;
import java.lang.String;

public class Dhcpbinding {
    public static final int IP_ADDRESS_LENGTH = 4;
    public static final int MAC_ADDRESS_LENGTH = 6;

    private byte[] MAC = new byte[MAC_ADDRESS_LENGTH];
    private byte[] IP = new byte[IP_ADDRESS_LENGTH];
    private boolean LEASE_STATUS;
    private boolean PERMANENT_LEASE;

    private long LEASE_START_TIME_SECONDS;
    private long LEASE_DURATION_SECONDS;

    protected Dhcpbinding(byte[] ip, byte[] mac) {
        this.setMACAddress(mac);
        this.setIPv4Addresss(ip);
        this.setLeaseStatus(false);
    }

    public byte[] getIPv4AddressBytes() {
        return IP;
    }

    public String getIPv4AddresString() {
        return NWUtil.toStringIpAddress(IP);
    }

    public byte[] getMACAddressBytes() {
        return MAC;
    }

    public String getMACAddressString() {
        return NWUtil.toStringMacAddress(MAC);
    }

    private void setIPv4Addresss(byte[] ip) {
        IP = Arrays.copyOf(ip, IP_ADDRESS_LENGTH);
    }

    public void setMACAddress(byte[] mac) {
        MAC = Arrays.copyOf(mac, MAC_ADDRESS_LENGTH);
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
