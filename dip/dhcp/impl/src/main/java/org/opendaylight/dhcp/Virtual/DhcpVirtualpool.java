/*
 * Copyright (c) 2015 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.dhcp.Virtual;

import org.opendaylight.dhcp.util.NWUtil;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;

public class DhcpVirtualpool {

    private static final Logger LOG = LoggerFactory.getLogger(DhcpVirtualpool.class);

    private volatile static ArrayList<DhcpVirtualbinding> vDHCP_POOL = new ArrayList<DhcpVirtualbinding>();
    private volatile int POOL_SIZE;
    private volatile int POOL_AVAILABILITY;
    private volatile boolean POOL_FULL;
    private volatile byte[] STARTING_ADDRESS;
    private final byte[] UNASSIGNED_MAC = NWUtil.toByteMacAddress("00:00:00:00:00:00");
    private String UNASSIGNEDDOMAINNAME = "NODOMAIN";


    public DhcpVirtualpool(byte[] startingIPv4Address, int size) {
        int IPv4AsInt = NWUtil.toIPv4Address(startingIPv4Address);
        this.setPoolSize(size);
        this.setPoolAvailability(size);

        STARTING_ADDRESS = Arrays.copyOf(startingIPv4Address, startingIPv4Address.length);
        for (int i = 0; i < size; i++){
            vDHCP_POOL.add(new DhcpVirtualbinding(NWUtil.toIPv4AddressBytes(IPv4AsInt + i),
                    UNASSIGNED_MAC));
//            DHCP_POOL.add(new DhcpVirtualbinding(NWUtil.toIPv4AddressBytes(IPv4AsInt + i),
//                    UNASSIGNED_MAC, UNASSIGNEDDOMAINNAME));
        }
    }
    private void setPoolFull(boolean full) {
        POOL_FULL = full;
    }

    private boolean isPoolFull() {
        return POOL_FULL;
    }

    public ArrayList<DhcpVirtualbinding> getPool() {
        return vDHCP_POOL;
    }

    private void setPoolSize(int size) {
        POOL_SIZE = size;
    }

    private int getPoolSize() {
        return POOL_SIZE;
    }

    private int getPoolAvailability() {
        return POOL_AVAILABILITY;
    }

    private void setPoolAvailability(int size) {
        POOL_AVAILABILITY = size;
    }


    public DhcpVirtualbinding getDhcpbindingFromIPv4(byte[] ip) {
        if (ip == null) return null;
        for (DhcpVirtualbinding binding : vDHCP_POOL) {
            if (Arrays.equals(binding.getIPv4AddressBytes(), ip)) {
                return binding;
            }
        }
        return null;
    }

    public DhcpVirtualbinding getDhcpbindingFromMAC(byte[] mac) {
        if (mac == null) return null;
        for (DhcpVirtualbinding binding : vDHCP_POOL) {
            if (Arrays.equals(binding.getMACAddressBytes(), mac)) {
                return binding;
            }
        }
        return null;
    }

    public boolean isIPv4Leased(byte[] ip) {
        DhcpVirtualbinding binding = this.getDhcpbindingFromIPv4(ip);
        if (binding != null) return binding.isActiveLease();
        else return false;
    }

    public void setVitualDhcpbinding(DhcpVirtualbinding binding, byte[] mac, long time) {
        int index = vDHCP_POOL.indexOf(binding);
        binding.setMACAddress(mac);
        binding.setLeaseStatus(true);
        this.setPoolAvailability(this.getPoolAvailability() - 1);
        vDHCP_POOL.set(index, binding);
        if (this.getPoolAvailability() == 0) setPoolFull(true);
        binding.setLeaseStartTimeSeconds();
        binding.setLeaseDurationSeconds(time);
    }

    public void setVitualDhcpbindingDip(DhcpVirtualbinding binding, byte[] mac, long time) {
        int index = vDHCP_POOL.indexOf(binding);
        binding.setMACAddress(mac);
        binding.setLeaseStatus(true);
        this.setPoolAvailability(this.getPoolAvailability() - 1);
        vDHCP_POOL.set(index, binding);
        if (this.getPoolAvailability() == 0) setPoolFull(true);
        binding.setLeaseStartTimeSeconds();
        binding.setLeaseDurationSeconds(time);
    }

    public void removeIPv4FromDHCPPool(byte [] ip) {
        if (ip == null || getDhcpbindingFromIPv4(ip) == null) return;
        if (NWUtil.toIPv4Address(ip) == NWUtil.toIPv4Address(STARTING_ADDRESS)) {
            DhcpVirtualbinding lowest = null;
            // Locate the lowest address (other than ip), which will be the new starting address
            for (DhcpVirtualbinding binding : vDHCP_POOL) {
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
        vDHCP_POOL.remove(this.getDhcpbindingFromIPv4(ip));
        this.setPoolSize(this.getPoolSize() - 1);
        this.setPoolAvailability(this.getPoolAvailability() - 1);
        if (this.getPoolAvailability() == 0) this.setPoolFull(true);
    }

    public DhcpVirtualbinding addIPv4ToDHCPPool(byte[] vip) {
        DhcpVirtualbinding binding = null;
        if (this.getDhcpbindingFromIPv4(vip) == null) {
            if (NWUtil.toIPv4Address(vip) < NWUtil.toIPv4Address(STARTING_ADDRESS)) {
                STARTING_ADDRESS = Arrays.copyOf(vip, vip.length);
            }
            binding = new DhcpVirtualbinding(vip, null);
            vDHCP_POOL.add(binding);
            this.setPoolSize(this.getPoolSize() + 1);
            this.setPoolFull(false);
        }
        return binding;
    }

    public boolean hasAvailableAddresses() {
        if (isPoolFull() || getPoolAvailability() == 0) return false;
        else return true;
    }

    public DhcpVirtualbinding getAnyAvailableLease(byte[] mac) {
        if (isPoolFull()) return null;
        DhcpVirtualbinding usedBinding = null;
        usedBinding = this.getDhcpbindingFromMAC(mac);
        if (usedBinding != null) return usedBinding;

        for (DhcpVirtualbinding binding : vDHCP_POOL) {
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

    public DhcpVirtualbinding getAnyAvailableLeaseforDip(byte[] mac,byte[] ip) {
        if (isPoolFull()) return null;
        DhcpVirtualbinding usedBinding = null;
        usedBinding = this.getDhcpbindingFromMAC(mac);
        if (usedBinding != null) {
            for (DhcpVirtualbinding binding : vDHCP_POOL) {
                if (!binding.isActiveLease() &&  !binding.isStaticIPLease() && binding.getIPv4AddressBytes()!=ip) {
                    usedBinding.setMACAddress(UNASSIGNED_MAC);
                    return binding;
                }
            }
            return usedBinding;
        }
        return usedBinding;
    }

    public DhcpVirtualbinding getSpecificAvailableLease(byte[] ip, byte[] mac) {
        if (ip == null || mac == null || isPoolFull()) return null;

        DhcpVirtualbinding binding = this.getDhcpbindingFromIPv4(ip);
        DhcpVirtualbinding binding2 = this.getDhcpbindingFromMAC(mac);

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
        DhcpVirtualbinding binding = this.getDhcpbindingFromIPv4(ip);
        if (binding != null) {
            binding.setLeaseStartTimeSeconds();
            binding.setLeaseDurationSeconds(time);
            binding.setLeaseStatus(true);
            return true;
        }
        return false;
    }

    public boolean cancelLeaseOfIPv4(byte[] ip) {
        DhcpVirtualbinding binding = this.getDhcpbindingFromIPv4(ip);
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
        DhcpVirtualbinding binding = getDhcpbindingFromMAC(mac);
        if (binding != null) {
            binding.clearLeaseTimes();
            binding.setLeaseStatus(false);
            this.setPoolAvailability(this.getPoolAvailability() + 1);
            this.setPoolFull(false);
            binding.setMACAddress(UNASSIGNED_MAC);//test code!!!!!!!!!!!!
            return true;
        }
        return false;
    }

    public ArrayList<DhcpVirtualbinding> cleanExpiredLeases() {
        ArrayList<DhcpVirtualbinding> newAvailableLeases = new ArrayList<DhcpVirtualbinding>();
        for (DhcpVirtualbinding binding : vDHCP_POOL) {
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
        DhcpVirtualbinding binding = this.getDhcpbindingFromIPv4(ip);
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
