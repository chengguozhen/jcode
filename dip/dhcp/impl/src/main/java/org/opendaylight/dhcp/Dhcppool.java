/*
 * Copyright (c) 2015 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.dhcp;

import java.util.Arrays;
import java.util.ArrayList;
import org.opendaylight.dhcp.util.NWUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Dhcppool {

    private static final Logger LOG = LoggerFactory.getLogger(Dhcppool.class);

    private volatile static ArrayList<Dhcpbinding> DHCP_POOL = new ArrayList<Dhcpbinding>();
    private volatile int POOL_SIZE;
    private volatile int POOL_AVAILABILITY;
    private volatile boolean POOL_FULL;
    private volatile byte[] STARTING_ADDRESS;
    private final byte[] UNASSIGNED_MAC = NWUtil.toByteMacAddress("00:00:00:00:00:00");


    Dhcppool(byte[] startingIPv4Address, int size) {
        int IPv4AsInt = NWUtil.toIPv4Address(startingIPv4Address);
        this.setPoolSize(size);
        this.setPoolAvailability(size);
        STARTING_ADDRESS = Arrays.copyOf(startingIPv4Address, startingIPv4Address.length);
        for (int i = 0; i < size; i++){
            DHCP_POOL.add(new Dhcpbinding(NWUtil.toIPv4AddressBytes(IPv4AsInt + i), UNASSIGNED_MAC));
        }
    }
    private void setPoolFull(boolean full) {
        POOL_FULL = full;
    }

    private boolean isPoolFull() {
        return POOL_FULL;
    }

    private void setPoolSize(int size) {
        POOL_SIZE = size;
    }

    private int getPoolSize() {
        return POOL_SIZE;
    }

    public ArrayList<Dhcpbinding> getPool() {
        return DHCP_POOL;
    }

    private int getPoolAvailability() {
        return POOL_AVAILABILITY;
    }

    private void setPoolAvailability(int size) {
        POOL_AVAILABILITY = size;
    }


    public Dhcpbinding getDhcpbindingFromIPv4(byte[] ip) {
        if (ip == null) return null;
        for (Dhcpbinding binding : DHCP_POOL) {
            if (Arrays.equals(binding.getIPv4AddressBytes(), ip)) {
                return binding;
            }
        }
        return null;
    }

    public Dhcpbinding getDhcpbindingFromMAC(byte[] mac) {
        if (mac == null) return null;
        for (Dhcpbinding binding : DHCP_POOL) {
            if (Arrays.equals(binding.getMACAddressBytes(), mac)) {
                return binding;
            }
        }
        return null;
    }

    public boolean isIPv4Leased(byte[] ip) {
        Dhcpbinding binding = this.getDhcpbindingFromIPv4(ip);
        if (binding != null) return binding.isActiveLease();
        else return false;
    }

    //time is during time

    public void setDhcpbinding(Dhcpbinding binding, byte[] mac, int time) {
        int index = DHCP_POOL.indexOf(binding);
        binding.setMACAddress(mac);
        binding.setLeaseStatus(true);
        this.setPoolAvailability(this.getPoolAvailability() - 1);
        DHCP_POOL.set(index, binding);
        if (this.getPoolAvailability() == 0) setPoolFull(true);
        binding.setLeaseStartTimeSeconds();
        binding.setLeaseDurationSeconds(time);
    }

    public void removeIPv4FromDHCPPool(byte [] ip) {
        if (ip == null || getDhcpbindingFromIPv4(ip) == null) return;
        if (NWUtil.toIPv4Address(ip) == NWUtil.toIPv4Address(STARTING_ADDRESS)) {
            Dhcpbinding lowest = null;
            // Locate the lowest address (other than ip), which will be the new starting address
            for (Dhcpbinding binding : DHCP_POOL) {
                if (lowest == null) {
                    lowest = binding;
                } else if (NWUtil.toIPv4Address(binding.getIPv4AddressBytes()) < NWUtil.toIPv4Address(lowest.getIPv4AddressBytes())
                        && NWUtil.toIPv4Address(binding.getIPv4AddressBytes()) != NWUtil.toIPv4Address(ip))
                {
                    lowest = binding;
                }
            }
            // lowest is new starting address
            STARTING_ADDRESS = Arrays.copyOf(lowest.getIPv4AddressBytes(), lowest.getIPv4AddressBytes().length);
        }
        DHCP_POOL.remove(this.getDhcpbindingFromIPv4(ip));
        this.setPoolSize(this.getPoolSize() - 1);
        this.setPoolAvailability(this.getPoolAvailability() - 1);
        if (this.getPoolAvailability() == 0) this.setPoolFull(true);
    }

    public Dhcpbinding addIPv4ToDHCPPool(byte[] ip) {
        Dhcpbinding binding = null;
        if (this.getDhcpbindingFromIPv4(ip) == null) {
            if (NWUtil.toIPv4Address(ip) < NWUtil.toIPv4Address(STARTING_ADDRESS)) {
                STARTING_ADDRESS = Arrays.copyOf(ip, ip.length);
            }
            binding = new Dhcpbinding(ip, null);
            DHCP_POOL.add(binding);
            this.setPoolSize(this.getPoolSize() + 1);
            this.setPoolFull(false);
        }
        return binding;
    }

    public boolean hasAvailableAddresses() {
        return !isPoolFull() && getPoolAvailability() != 0;
    }

    public Dhcpbinding getAnyAvailableLease(byte[] mac) {
        if (isPoolFull()) return null;
        Dhcpbinding usedBinding = null;
        usedBinding = this.getDhcpbindingFromMAC(mac);
        if (usedBinding != null) return usedBinding;

        for (Dhcpbinding binding : DHCP_POOL) {
            if (!binding.isActiveLease()
                    && Arrays.equals(binding.getMACAddressBytes(), UNASSIGNED_MAC))
            {
                return binding;
            } else if (!binding.isActiveLease() && usedBinding == null && !binding.isStaticIPLease()) {
                usedBinding = binding;
            }
        }
        return usedBinding;
    }

    public Dhcpbinding getSpecificAvailableLease(byte[] ip, byte[] mac) {
        if (ip == null || mac == null || isPoolFull()) return null;

        Dhcpbinding binding = this.getDhcpbindingFromIPv4(ip);
        Dhcpbinding binding2 = this.getDhcpbindingFromMAC(mac);

        // For all of the following, the binding is also determined to be inactive:

        // If configured, we must return a fixed binding for a MAC address even if it's requesting another IP
        if (binding2 != null && !binding2.isActiveLease() && binding2.isStaticIPLease() && binding != binding2) {
            if (LOG != null) LOG.info("Fixed DHCP entry for MAC trumps requested IP. Returning binding for MAC");
            return binding2;
            // If configured, we must return a fixed binding for an IP if the binding is fixed to the provided MAC (ideal static request case)
        } else if (binding != null && !binding.isActiveLease() && binding.isStaticIPLease() && Arrays.equals(mac, binding.getMACAddressBytes())) {
            if (LOG != null) LOG.info("Found matching fixed DHCP entry for IP with MAC. Returning binding for IP with MAC");
            return binding;
            // The IP and MAC are not a part of a fixed binding, so return the binding of the requested IP
        } else if (binding != null && !binding.isActiveLease() && !binding.isStaticIPLease()) {
            if (LOG != null) LOG.info("No fixed DHCP entry for IP or MAC found. Returning dynamic binding for IP.");
            return binding;
            // Otherwise, the binding is fixed for both MAC and IP and this MAC does not match either, so we can't return it as available
        } else {
            if (LOG != null) LOG.debug("Invalid IP address request or IP is actively leased...check for any available lease to resolve");
            return null;
        }
    }

    public boolean renewLease(byte[] ip, int time) {
        Dhcpbinding binding = this.getDhcpbindingFromIPv4(ip);
        if (binding != null) {
            binding.setLeaseStartTimeSeconds();
            binding.setLeaseDurationSeconds(time);
            binding.setLeaseStatus(true);
            return true;
        }
        return false;
    }

    private boolean cancelLeaseOfIPv4(byte[] ip) {
        Dhcpbinding binding = this.getDhcpbindingFromIPv4(ip);
        if (binding != null) {
            binding.clearLeaseTimes();
            binding.setLeaseStatus(false);
            this.setPoolAvailability(this.getPoolAvailability() + 1);
            this.setPoolFull(false);
            return true;
        }
        return false;
    }

    public boolean cancelLeaseOfMAC(byte[] mac) {
        Dhcpbinding binding = getDhcpbindingFromMAC(mac);
        if (binding != null) {
            binding.clearLeaseTimes();
            binding.setLeaseStatus(false);
            this.setPoolAvailability(this.getPoolAvailability() + 1);
            this.setPoolFull(false);
            return true;
        }
        return false;
    }

    public ArrayList<Dhcpbinding> cleanExpiredLeases() {
        ArrayList<Dhcpbinding> newAvailableLeases = new ArrayList<Dhcpbinding>();
        for (Dhcpbinding binding : DHCP_POOL) {
            // isLeaseExpired() automatically excludes configured static leases
            if (binding.isLeaseExpired() && binding.isActiveLease()) {
                newAvailableLeases.add(binding);
                this.cancelLeaseOfIPv4(binding.getIPv4AddressBytes());
                this.setPoolAvailability(this.getPoolAvailability() + 1);
                this.setPoolFull(false);
                binding.setMACAddress(UNASSIGNED_MAC);
            }
        }
        return newAvailableLeases;
    }

    public boolean configureFixedIPLease(byte[] ip, byte[] mac) {
        Dhcpbinding binding = this.getDhcpbindingFromIPv4(ip);
        if (binding != null) {
            binding.setMACAddress(mac);
            binding.setStaticIPLease(true);
            binding.setLeaseStatus(false);
            return true;
        } else {
            return false;
        }
    }



}
