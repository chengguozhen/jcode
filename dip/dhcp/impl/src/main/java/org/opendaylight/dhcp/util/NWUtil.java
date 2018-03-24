/*
 * Copyright (c) 2015 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.dhcp.util;

import java.net.InetAddress;

import com.google.common.primitives.UnsignedBytes;

public class NWUtil {

    public static  long convertInetAddressToLong(InetAddress address) {
        byte[] ipAddressRaw = address.getAddress();
        return (((ipAddressRaw[0] & 0xFF) << (3 * 8))
                + ((ipAddressRaw[1] & 0xFF) << (2 * 8))
                + ((ipAddressRaw[2] & 0xFF) << (1 * 8))
                + (ipAddressRaw[3] & 0xFF))
                & 0xffffffffL;
    }

    //ip string to byte
    public static byte[] toIPv4AddressBytes(String ipAddress) {
        String[] octets = ipAddress.split("\\.");
        if (octets.length != 4)
            throw new IllegalArgumentException("Specified IPv4 address must" +
                    "contain 4 sets of numerical digits separated by periods");

        byte[] result = new byte[4];
        for (int i = 0; i < 4; ++i) {
            result[i] = Integer.valueOf(octets[i]).byteValue();
        }
        return result;
    }

    //ip byte to int
    public static int toIPv4Address(byte[] ipAddress) {
        int ip = 0;
        for (int i = 0; i < 4; i++) {
            int t = (ipAddress[i] & 0xff) << ((3-i)*8);
            ip |= t;
        }
        return ip;
    }

    //ip int to string
    public static String fromIPv4Address(int ipAddress) {
        StringBuffer sb = new StringBuffer();
        int result = 0;
        for (int i = 0; i < 4; ++i) {
            result = (ipAddress >> ((3-i)*8)) & 0xff;
            sb.append(Integer.valueOf(result).toString());
            if (i != 3)
                sb.append(".");
        }
        return sb.toString();
    }

    //ip int to byte
    public static byte[] toIPv4AddressBytes(int ipAddress) {
        return new byte[] {
                (byte)(ipAddress >>> 24),
                (byte)(ipAddress >>> 16),
                (byte)(ipAddress >>> 8),
                (byte)ipAddress};
    }


    //ip byte to string
    public static String toStringIpAddress(byte[] bytes)
    {
        return String.valueOf ( bytes[0] & 0xFF) + '.' + (bytes[1] & 0xFF)
                + '.' + (bytes[2] & 0xFF) + '.' + (bytes[3] & 0xFF);
    }

    //mac byte to string
    public static String toStringMacAddress(byte[] macAddress)
    {
        if (macAddress == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder(18);

        for (int i = 0; i < macAddress.length; i++) {
            String tmp = UnsignedBytes.toString(macAddress[i], 16).toUpperCase();
            if(tmp.length() == 1 || macAddress[i] == (byte)0) {
                sb.append("0");
            }
            if(i==macAddress.length-1){
                sb.append(tmp);
            }else{
                sb.append(tmp);
                sb.append(":");
            }

        }

        sb.setLength(17);
        return sb.toString();
    }

    //mac string to byte
    public static byte [] toByteMacAddress(String macAddress){
        byte[] macBytes = new byte[6];
        String [] strArr = macAddress.split(":");
        for(int i = 0;i < strArr.length; i++){
            int value = Integer.parseInt(strArr[i],16);
            macBytes[i] = (byte) value;
        }
        return macBytes;
    }

    public static int toMacByteInt(byte[] macAddress) {
        int ip = 0;
        for (int i = 0; i < 6; i++) {
            int t = (macAddress[i] & 0xff) << ((3-i)*8);
            ip |= t;
        }
        return ip;
    }

    //unknown function
    public static byte[] parseIpAddress(String ipAddress) {
        byte cur;

        String[] addressPart = ipAddress.split(".");
        int size = addressPart.length;

        byte[] part = new byte[size];
        for (int i = 0; i < size; i++) {
            cur = UnsignedBytes.parseUnsignedByte(addressPart[i], 16);
            part[i] = cur;
        }

        return part;
    }

    //unknown function
    public static byte[] parseMacAddress(String macAddress) {
        byte cur;

        String[] addressPart = macAddress.split(":");
        int size = addressPart.length;

        byte[] part = new byte[size];
        for (int i = 0; i < size; i++) {
            cur = UnsignedBytes.parseUnsignedByte(addressPart[i], 16);
            part[i] = cur;
        }

        return part;
    }
}